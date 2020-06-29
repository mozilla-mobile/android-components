/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.annotation.SuppressLint
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.browser.engine.gecko.media.GeckoMediaDelegate
import mozilla.components.browser.engine.gecko.permission.GeckoPermissionRequest
import mozilla.components.browser.engine.gecko.prompt.GeckoPromptDelegate
import mozilla.components.browser.engine.gecko.window.GeckoWindowRequest
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.engine.HitResult
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.engine.content.blocking.Tracker
import mozilla.components.concept.engine.history.HistoryItem
import mozilla.components.concept.engine.history.HistoryTrackingDelegate
import mozilla.components.concept.engine.manifest.WebAppManifestParser
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.request.RequestInterceptor.InterceptionResponse
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.concept.storage.PageVisit
import mozilla.components.concept.storage.RedirectSource
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.ktx.android.util.Base64
import mozilla.components.support.ktx.kotlin.isEmail
import mozilla.components.support.ktx.kotlin.isGeoLocation
import mozilla.components.support.ktx.kotlin.isPhone
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebRequestError
import kotlin.coroutines.CoroutineContext

/**
 * Gecko-based EngineSession implementation.
 */
@Suppress("TooManyFunctions", "LargeClass")
class GeckoEngineSession(
    private val runtime: GeckoRuntime,
    private val privateMode: Boolean = false,
    private val defaultSettings: Settings? = null,
    contextId: String? = null,
    private val geckoSessionProvider: () -> GeckoSession = {
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(privateMode)
            .contextId(contextId)
            .build()
        GeckoSession(settings)
    },
    private val context: CoroutineContext = Dispatchers.IO,
    openGeckoSession: Boolean = true
) : CoroutineScope, EngineSession() {

    internal lateinit var geckoSession: GeckoSession
    internal var currentUrl: String? = null
    internal var scrollY: Int = 0

    internal var job: Job = Job()
    private var lastSessionState: GeckoSession.SessionState? = null
    private var stateBeforeCrash: GeckoSession.SessionState? = null
    private var canGoBack: Boolean = false

    /**
     * See [EngineSession.settings]
     */
    override val settings: Settings = object : Settings() {
        override var requestInterceptor: RequestInterceptor? = null
        override var historyTrackingDelegate: HistoryTrackingDelegate? = null
        override var userAgentString: String?
            get() = geckoSession.settings.userAgentOverride
            set(value) { geckoSession.settings.userAgentOverride = value }
        override var suspendMediaWhenInactive: Boolean
            get() = geckoSession.settings.suspendMediaWhenInactive
            set(value) { geckoSession.settings.suspendMediaWhenInactive = value }
    }

    private var initialLoad = true

    override val coroutineContext: CoroutineContext
        get() = context + job

    init {
        createGeckoSession(shouldOpen = openGeckoSession)
    }

    /**
     * See [EngineSession.loadUrl]
     */
    override fun loadUrl(
        url: String,
        parent: EngineSession?,
        flags: LoadUrlFlags,
        additionalHeaders: Map<String, String>?
    ) {
        geckoSession.loadUri(url, (parent as? GeckoEngineSession)?.geckoSession, flags.value, additionalHeaders)
    }

    /**
     * See [EngineSession.loadData]
     */
    override fun loadData(data: String, mimeType: String, encoding: String) {
        when (encoding) {
            "base64" -> geckoSession.loadData(data.toByteArray(), mimeType)
            else -> geckoSession.loadString(data, mimeType)
        }
    }

    /**
     * See [EngineSession.stopLoading]
     */
    override fun stopLoading() {
        geckoSession.stop()
    }

    /**
     * See [EngineSession.reload]
     */
    override fun reload() {
        geckoSession.reload()
    }

    /**
     * See [EngineSession.goBack]
     */
    override fun goBack() {
        geckoSession.goBack()
        if (canGoBack) {
            notifyObservers { onNavigateBack() }
        }
    }
    /**
     * See [EngineSession.goForward]
     */
    override fun goForward() {
        geckoSession.goForward()
    }

    /**
     * See [EngineSession.saveState]
     */
    override fun saveState(): EngineSessionState {
        return GeckoEngineSessionState(lastSessionState)
    }

    /**
     * See [EngineSession.restoreState]
     */
    override fun restoreState(state: EngineSessionState): Boolean {
        if (state !is GeckoEngineSessionState) {
            throw IllegalStateException("Can only restore from GeckoEngineSessionState")
        }

        if (state.actualState == null) {
            return false
        }

        geckoSession.restoreState(state.actualState)
        return true
    }

    /**
     * See [EngineSession.enableTrackingProtection]
     */
    override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {
        val enabled = if (privateMode) {
            policy.useForPrivateSessions
        } else {
            policy.useForRegularSessions
        }
        /**
         * As described on https://bugzilla.mozilla.org/show_bug.cgi?id=1579264,useTrackingProtection
         * is a misleading setting. When is set to true is blocking content (scripts/sub-resources).
         * Instead of just turn on/off tracking protection. Until, this issue is fixed consumers need
         * a way to indicate, if they want to block content or not, this is why we use
         * [TrackingProtectionPolicy.TrackingCategory.SCRIPTS_AND_SUB_RESOURCES].
         */
        val shouldBlockContent =
            policy.contains(TrackingProtectionPolicy.TrackingCategory.SCRIPTS_AND_SUB_RESOURCES)

        geckoSession.settings.useTrackingProtection = shouldBlockContent && enabled
        notifyAtLeastOneObserver { onTrackerBlockingEnabledChange(enabled) }
    }

    /**
     * See [EngineSession.disableTrackingProtection]
     */
    override fun disableTrackingProtection() {
        geckoSession.settings.useTrackingProtection = false
        notifyObservers { onTrackerBlockingEnabledChange(false) }
    }

    /**
     * Indicates if this [EngineSession] should be ignored the tracking protection policies.
     * @param onResult A callback to inform if this [EngineSession] is in
     * the exception list, true if it is in, otherwise false.
     */
    internal fun isIgnoredForTrackingProtection(onResult: (Boolean) -> Unit) {
        runtime.contentBlockingController.checkException(geckoSession).accept {
            if (it != null) {
                onResult(it)
            } else {
                onResult(false)
            }
        }
    }

    /**
     * See [EngineSession.settings]
     */
    override fun toggleDesktopMode(enable: Boolean, reload: Boolean) {
        val currentMode = geckoSession.settings.userAgentMode
        val currentViewPortMode = geckoSession.settings.viewportMode

        val newMode = if (enable) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }

        val newViewportMode = if (enable) {
            GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }

        if (newMode != currentMode || newViewportMode != currentViewPortMode) {
            geckoSession.settings.userAgentMode = newMode
            geckoSession.settings.viewportMode = newViewportMode
            notifyObservers { onDesktopModeChange(enable) }
        }

        if (reload) {
            this.reload()
        }
    }

    /**
     * See [EngineSession.findAll]
     */
    override fun findAll(text: String) {
        notifyObservers { onFind(text) }
        geckoSession.finder.find(text, 0).then { result: GeckoSession.FinderResult? ->
            result?.let {
                val activeMatchOrdinal = if (it.current > 0) it.current - 1 else it.current
                notifyObservers { onFindResult(activeMatchOrdinal, it.total, true) }
            }
            GeckoResult<Void>()
        }
    }

    /**
     * See [EngineSession.findNext]
     */
    @SuppressLint("WrongConstant") // FinderFindFlags annotation doesn't include a 0 value.
    override fun findNext(forward: Boolean) {
        val findFlags = if (forward) 0 else GeckoSession.FINDER_FIND_BACKWARDS
        geckoSession.finder.find(null, findFlags).then { result: GeckoSession.FinderResult? ->
            result?.let {
                val activeMatchOrdinal = if (it.current > 0) it.current - 1 else it.current
                notifyObservers { onFindResult(activeMatchOrdinal, it.total, true) }
            }
            GeckoResult<Void>()
        }
    }

    /**
     * See [EngineSession.clearFindMatches]
     */
    override fun clearFindMatches() {
        geckoSession.finder.clear()
    }

    /**
     * See [EngineSession.exitFullScreenMode]
     */
    override fun exitFullScreenMode() {
        geckoSession.exitFullScreen()
    }

    /**
     * See [EngineSession.recoverFromCrash]
     */
    @Synchronized
    override fun recoverFromCrash(): Boolean {
        val state = stateBeforeCrash

        return if (state != null) {
            geckoSession.restoreState(state)
            stateBeforeCrash = null
            true
        } else {
            false
        }
    }

    /**
     * See [EngineSession.markActiveForWebExtensions].
     */
    override fun markActiveForWebExtensions(active: Boolean) {
        runtime.webExtensionController.setTabActive(geckoSession, active)
    }

    /**
     * See [EngineSession.close].
     */
    override fun close() {
        super.close()
        job.cancel()
        geckoSession.close()
    }

    /**
     * NavigationDelegate implementation for forwarding callbacks to observers of the session.
     */
    @Suppress("ComplexMethod")
    private fun createNavigationDelegate() = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(session: GeckoSession, url: String?) {
            if (url == null) {
                return // ¯\_(ツ)_/¯
            }

            // Ignore initial load of about:blank (see https://github.com/mozilla-mobile/android-components/issues/403)
            if (initialLoad && url == ABOUT_BLANK) {
                return
            }

            currentUrl = url
            initialLoad = false
            isIgnoredForTrackingProtection { ignored ->
                notifyObservers {
                    onExcludedOnTrackingProtectionChange(ignored)
                }
            }
            notifyObservers { onLocationChange(url) }
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny> {
            if (request.target == NavigationDelegate.TARGET_WINDOW_NEW) {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            val interceptor = settings.requestInterceptor
            val response = if (
                interceptor != null && (!request.isDirectNavigation || interceptor.interceptsAppInitiatedRequests())
            ) {
                val engineSession = this@GeckoEngineSession
                val isSameDomain = engineSession.currentUrl?.tryGetHostFromUrl() == request.uri.tryGetHostFromUrl()
                interceptor.onLoadRequest(
                    engineSession,
                    request.uri,
                    request.hasUserGesture,
                    isSameDomain,
                    request.isRedirect
                )?.apply {
                    when (this) {
                        is InterceptionResponse.Content -> loadData(data, mimeType, encoding)
                        is InterceptionResponse.Url -> loadUrl(url)
                        is InterceptionResponse.AppIntent -> {
                            notifyObservers {
                                onLaunchIntentRequest(url = url, appIntent = appIntent)
                            }
                        }
                    }
                }
            } else {
                null
            }

            return if (response != null) {
                GeckoResult.fromValue(AllowOrDeny.DENY)
            } else {
                notifyObservers {
                    onLoadRequest(
                        url = request.uri,
                        triggeredByRedirect = request.isRedirect,
                        triggeredByWebContent = request.hasUserGesture
                    )
                }

                GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            notifyObservers { onNavigationStateChange(canGoForward = canGoForward) }
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            notifyObservers { onNavigationStateChange(canGoBack = canGoBack) }
            this@GeckoEngineSession.canGoBack = canGoBack
        }

        override fun onNewSession(
            session: GeckoSession,
            uri: String
        ): GeckoResult<GeckoSession> {
            val newEngineSession = GeckoEngineSession(runtime, privateMode, defaultSettings, openGeckoSession = false)
            notifyObservers {
                MainScope().launch {
                    onWindowRequest(GeckoWindowRequest(uri, newEngineSession))
                }
            }
            return GeckoResult.fromValue(newEngineSession.geckoSession)
        }

        override fun onLoadError(
            session: GeckoSession,
            uri: String?,
            error: WebRequestError
        ): GeckoResult<String> {
            val uriToLoad = settings.requestInterceptor?.onErrorRequest(
                this@GeckoEngineSession,
                geckoErrorToErrorType(error.code),
                uri
            )?.run {
                when (this) {
                    is RequestInterceptor.ErrorResponse.Content -> Base64.encodeToUriString(data)
                    is RequestInterceptor.ErrorResponse.Uri -> this.uri
                }
            }
            return GeckoResult.fromValue(uriToLoad)
        }
    }

    /**
     * ProgressDelegate implementation for forwarding callbacks to observers of the session.
     */
    private fun createProgressDelegate() = object : GeckoSession.ProgressDelegate {
        override fun onProgressChange(session: GeckoSession, progress: Int) {
            notifyObservers { onProgress(progress) }
        }

        override fun onSecurityChange(
            session: GeckoSession,
            securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
        ) {
            // Ignore initial load of about:blank (see https://github.com/mozilla-mobile/android-components/issues/403)
            if (initialLoad && securityInfo.origin?.startsWith(MOZ_NULL_PRINCIPAL) == true) {
                return
            }

            notifyObservers {
                // TODO provide full certificate info: https://github.com/mozilla-mobile/android-components/issues/5557
                onSecurityChange(securityInfo.isSecure, securityInfo.host, securityInfo.getIssuerName())
            }
        }

        override fun onPageStart(session: GeckoSession, url: String) {
            notifyObservers {
                onProgress(PROGRESS_START)
                onLoadingStateChange(true)
            }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            // by the time we reach here, any new request will come from web content.
            // If it comes from the chrome, loadUrl(url) or loadData(string) will set it to
            // false.
            notifyObservers {
                onProgress(PROGRESS_STOP)
                onLoadingStateChange(false)
            }
        }

        override fun onSessionStateChange(session: GeckoSession, sessionState: GeckoSession.SessionState) {
            lastSessionState = sessionState
        }
    }

    @Suppress("ComplexMethod")
    internal fun createHistoryDelegate() = object : GeckoSession.HistoryDelegate {
        @SuppressWarnings("ReturnCount")
        override fun onVisited(
            session: GeckoSession,
            url: String,
            lastVisitedURL: String?,
            flags: Int
        ): GeckoResult<Boolean>? {
            // Don't track:
            // - private visits
            // - error pages
            // - non-top level visits (i.e. iframes).
            if (privateMode ||
                (flags and GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL) == 0 ||
                (flags and GeckoSession.HistoryDelegate.VISIT_UNRECOVERABLE_ERROR) != 0) {
                return GeckoResult.fromValue(false)
            }

            val isReload = lastVisitedURL?.let { it == url } ?: false

            val visitType = if (isReload) {
                VisitType.RELOAD
            } else {
                // Note the difference between `VISIT_REDIRECT_PERMANENT`,
                // `VISIT_REDIRECT_TEMPORARY`, `VISIT_REDIRECT_SOURCE`, and
                // `VISIT_REDIRECT_SOURCE_PERMANENT`.
                //
                // The former two indicate if the visited page is the *target*
                // of a redirect; that is, another page redirected to it.
                //
                // The latter two indicate if the visited page is the *source*
                // of a redirect: it's redirecting to another page, because the
                // server returned an HTTP 3xy status code.
                if (flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_PERMANENT != 0) {
                    VisitType.REDIRECT_PERMANENT
                } else if (flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY != 0) {
                    VisitType.REDIRECT_TEMPORARY
                } else {
                    VisitType.LINK
                }
            }
            val redirectSource = when {
                flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_SOURCE_PERMANENT != 0 ->
                    RedirectSource.PERMANENT
                flags and GeckoSession.HistoryDelegate.VISIT_REDIRECT_SOURCE != 0 ->
                    RedirectSource.TEMPORARY
                else -> RedirectSource.NOT_A_SOURCE
            }

            val delegate = settings.historyTrackingDelegate ?: return GeckoResult.fromValue(false)

            // Check if the delegate wants this type of url.
            if (!delegate.shouldStoreUri(url)) {
                return GeckoResult.fromValue(false)
            }

            return launchGeckoResult {
                delegate.onVisited(url, PageVisit(visitType, redirectSource))
                true
            }
        }

        override fun getVisited(
            session: GeckoSession,
            urls: Array<out String>
        ): GeckoResult<BooleanArray>? {
            if (privateMode) {
                return GeckoResult.fromValue(null)
            }

            val delegate = settings.historyTrackingDelegate ?: return GeckoResult.fromValue(null)

            return launchGeckoResult {
                val visits = delegate.getVisited(urls.toList())
                visits.toBooleanArray()
            }
        }

        override fun onHistoryStateChange(
            session: GeckoSession,
            historyList: GeckoSession.HistoryDelegate.HistoryList
        ) {
            val items = historyList.map { HistoryItem(title = it.title, uri = it.uri) }
            notifyObservers { onHistoryStateChanged(items, historyList.currentIndex) }
        }
    }

    @Suppress("ComplexMethod")
    internal fun createContentDelegate() = object : GeckoSession.ContentDelegate {
        override fun onFirstComposite(session: GeckoSession) = Unit

        override fun onFirstContentfulPaint(session: GeckoSession) {
            notifyObservers { onFirstContentfulPaint() }
        }

        override fun onContextMenu(
            session: GeckoSession,
            screenX: Int,
            screenY: Int,
            element: GeckoSession.ContentDelegate.ContextElement
        ) {
            val hitResult = handleLongClick(element.srcUri, element.type, element.linkUri, element.title)
            hitResult?.let {
                notifyObservers { onLongPress(it) }
            }
        }

        override fun onCrash(session: GeckoSession) {
            stateBeforeCrash = lastSessionState

            recoverGeckoSession()

            notifyObservers { onCrash() }
        }

        override fun onKill(session: GeckoSession) {
            // The content process of this session got killed (resources reclaimed by Android).
            // Let's recover and restore the last known state.

            val state = lastSessionState

            recoverGeckoSession()

            state?.let { geckoSession.restoreState(it) }

            notifyObservers { onProcessKilled() }
        }

        private fun recoverGeckoSession() {
            // Recover the GeckoSession after the process getting killed or crashing. We create a
            // new underlying GeckoSession.
            // Eventually we may be able to re-use the same GeckoSession by re-opening it. However
            // that seems to have caused issues:
            // https://github.com/mozilla-mobile/android-components/issues/3640

            geckoSession.close()
            createGeckoSession()
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            notifyObservers { onFullScreenChange(fullScreen) }
        }

        override fun onExternalResponse(session: GeckoSession, response: GeckoSession.WebResponseInfo) {
            notifyObservers {
                onExternalResource(
                        url = response.uri,
                        contentLength = response.contentLength,
                        contentType = response.contentType,
                        fileName = response.filename)
            }
        }

        override fun onCloseRequest(session: GeckoSession) {
            notifyObservers {
                onWindowRequest(GeckoWindowRequest(
                        engineSession = this@GeckoEngineSession,
                        type = WindowRequest.Type.CLOSE
                    )
                )
            }
        }

        override fun onTitleChange(session: GeckoSession, title: String?) {
            if (!privateMode) {
                currentUrl?.let { url ->
                    settings.historyTrackingDelegate?.let { delegate ->
                        // NB: There's no guarantee that the title change will be processed by the
                        // delegate before the session is closed (and the corresponding coroutine
                        // job is cancelled). Observers will always be notified of the title
                        // change though.
                        launch(coroutineContext) {
                            delegate.onTitleChanged(url, title ?: "")
                        }
                    }
                }
            }
            notifyObservers { onTitleChange(title ?: "") }
        }

        override fun onFocusRequest(session: GeckoSession) = Unit

        override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
            val parsed = WebAppManifestParser().parse(manifest)
            if (parsed is WebAppManifestParser.Result.Success) {
                notifyObservers { onWebAppManifestLoaded(parsed.manifest) }
            }
        }

        override fun onMetaViewportFitChange(session: GeckoSession, viewportFit: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val layoutInDisplayCutoutMode = when (viewportFit) {
                    "cover" -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    "contain" -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                    else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }

                notifyObservers { onMetaViewportFitChanged(layoutInDisplayCutoutMode) }
            }
        }
    }

    private fun createContentBlockingDelegate() = object : ContentBlocking.Delegate {
        override fun onContentBlocked(session: GeckoSession, event: ContentBlocking.BlockEvent) {
            notifyObservers {
                onTrackerBlocked(event.toTracker())
            }
        }

        override fun onContentLoaded(session: GeckoSession, event: ContentBlocking.BlockEvent) {
            notifyObservers {
                onTrackerLoaded(event.toTracker())
            }
        }
    }

    private fun ContentBlocking.BlockEvent.toTracker(): Tracker {
        val blockedContentCategories = mutableListOf<TrackingProtectionPolicy.TrackingCategory>()

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.AD)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.AD)
        }

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.ANALYTIC)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.ANALYTICS)
        }

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.SOCIAL)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.SOCIAL)
        }

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.FINGERPRINTING)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.FINGERPRINTING)
        }

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.CRYPTOMINING)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.CRYPTOMINING)
        }

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.CONTENT)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.CONTENT)
        }

        if (antiTrackingCategory.contains(ContentBlocking.AntiTracking.TEST)) {
            blockedContentCategories.add(TrackingProtectionPolicy.TrackingCategory.TEST)
        }

        return Tracker(
            url = uri,
            trackingCategories = blockedContentCategories,
            cookiePolicies = getCookiePolicies()
        )
    }

    private fun ContentBlocking.BlockEvent.getCookiePolicies(): List<TrackingProtectionPolicy.CookiePolicy> {
        val cookiesPolicies = mutableListOf<TrackingProtectionPolicy.CookiePolicy>()

        if (cookieBehaviorCategory == ContentBlocking.CookieBehavior.ACCEPT_ALL) {
            cookiesPolicies.add(TrackingProtectionPolicy.CookiePolicy.ACCEPT_ALL)
        }

        if (cookieBehaviorCategory.contains(ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY)) {
            cookiesPolicies.add(TrackingProtectionPolicy.CookiePolicy.ACCEPT_ONLY_FIRST_PARTY)
        }

        if (cookieBehaviorCategory.contains(ContentBlocking.CookieBehavior.ACCEPT_NONE)) {
            cookiesPolicies.add(TrackingProtectionPolicy.CookiePolicy.ACCEPT_NONE)
        }

        if (cookieBehaviorCategory.contains(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)) {
            cookiesPolicies.add(TrackingProtectionPolicy.CookiePolicy.ACCEPT_NON_TRACKERS)
        }

        if (cookieBehaviorCategory.contains(ContentBlocking.CookieBehavior.ACCEPT_VISITED)) {
            cookiesPolicies.add(TrackingProtectionPolicy.CookiePolicy.ACCEPT_VISITED)
        }

        return cookiesPolicies
    }

    internal fun GeckoSession.ProgressDelegate.SecurityInformation.getIssuerName(): String? {
        return certificate?.issuerDN?.name?.substringAfterLast("O=")?.substringBeforeLast(",C=")
    }

    private operator fun Int.contains(mask: Int): Boolean {
        return (this and mask) != 0
    }

    private fun createPermissionDelegate() = object : GeckoSession.PermissionDelegate {
        override fun onContentPermissionRequest(
            session: GeckoSession,
            uri: String?,
            type: Int,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            val request = GeckoPermissionRequest.Content(uri ?: "", type, callback)
            notifyObservers { onContentPermissionRequest(request) }
        }

        override fun onMediaPermissionRequest(
            session: GeckoSession,
            uri: String,
            video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback
        ) {
            val request = GeckoPermissionRequest.Media(
                    uri,
                    video?.toList() ?: emptyList(),
                    audio?.toList() ?: emptyList(),
                    callback)
            notifyObservers { onContentPermissionRequest(request) }
        }

        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            val request = GeckoPermissionRequest.App(
                    permissions?.toList() ?: emptyList(),
                    callback)
            notifyObservers { onAppPermissionRequest(request) }
        }
    }

    private fun createScrollDelegate() = object : GeckoSession.ScrollDelegate {
        override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
            this@GeckoEngineSession.scrollY = scrollY
        }
    }

    @Suppress("ComplexMethod")
    fun handleLongClick(elementSrc: String?, elementType: Int, uri: String? = null, title: String? = null): HitResult? {
        return when (elementType) {
            GeckoSession.ContentDelegate.ContextElement.TYPE_AUDIO ->
                elementSrc?.let {
                    HitResult.AUDIO(it, title)
                }
            GeckoSession.ContentDelegate.ContextElement.TYPE_VIDEO ->
                elementSrc?.let {
                    HitResult.VIDEO(it, title)
                }
            GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE -> {
                when {
                    elementSrc != null && uri != null ->
                        HitResult.IMAGE_SRC(elementSrc, uri)
                    elementSrc != null ->
                        HitResult.IMAGE(elementSrc, title)
                    else -> HitResult.UNKNOWN("")
                }
            }
            GeckoSession.ContentDelegate.ContextElement.TYPE_NONE -> {
                elementSrc?.let {
                    when {
                        it.isPhone() -> HitResult.PHONE(it)
                        it.isEmail() -> HitResult.EMAIL(it)
                        it.isGeoLocation() -> HitResult.GEO(it)
                        else -> HitResult.UNKNOWN(it)
                    }
                } ?: uri?.let {
                    HitResult.UNKNOWN(it)
                }
            }
            else -> HitResult.UNKNOWN("")
        }
    }

    private fun createGeckoSession(shouldOpen: Boolean = true) {
        this.geckoSession = geckoSessionProvider()

        defaultSettings?.trackingProtectionPolicy?.let { enableTrackingProtection(it) }
        defaultSettings?.requestInterceptor?.let { settings.requestInterceptor = it }
        defaultSettings?.historyTrackingDelegate?.let { settings.historyTrackingDelegate = it }
        defaultSettings?.testingModeEnabled?.let { geckoSession.settings.fullAccessibilityTree = it }
        defaultSettings?.userAgentString?.let { geckoSession.settings.userAgentOverride = it }
        defaultSettings?.suspendMediaWhenInactive?.let { geckoSession.settings.suspendMediaWhenInactive = it }

        if (shouldOpen) {
            geckoSession.open(runtime)
        }

        geckoSession.navigationDelegate = createNavigationDelegate()
        geckoSession.progressDelegate = createProgressDelegate()
        geckoSession.contentDelegate = createContentDelegate()
        geckoSession.contentBlockingDelegate = createContentBlockingDelegate()
        geckoSession.permissionDelegate = createPermissionDelegate()
        geckoSession.promptDelegate = GeckoPromptDelegate(this)
        geckoSession.historyDelegate = createHistoryDelegate()
        geckoSession.mediaDelegate = GeckoMediaDelegate(this)
        geckoSession.scrollDelegate = createScrollDelegate()
    }

    companion object {
        internal const val PROGRESS_START = 25
        internal const val PROGRESS_STOP = 100
        internal const val MOZ_NULL_PRINCIPAL = "moz-nullprincipal:"
        internal const val ABOUT_BLANK = "about:blank"

        /**
         * Provides an ErrorType corresponding to the error code provided.
         */
        @Suppress("ComplexMethod")
        internal fun geckoErrorToErrorType(errorCode: Int) =
            when (errorCode) {
                WebRequestError.ERROR_UNKNOWN -> ErrorType.UNKNOWN
                WebRequestError.ERROR_SECURITY_SSL -> ErrorType.ERROR_SECURITY_SSL
                WebRequestError.ERROR_SECURITY_BAD_CERT -> ErrorType.ERROR_SECURITY_BAD_CERT
                WebRequestError.ERROR_NET_INTERRUPT -> ErrorType.ERROR_NET_INTERRUPT
                WebRequestError.ERROR_NET_TIMEOUT -> ErrorType.ERROR_NET_TIMEOUT
                WebRequestError.ERROR_CONNECTION_REFUSED -> ErrorType.ERROR_CONNECTION_REFUSED
                WebRequestError.ERROR_UNKNOWN_SOCKET_TYPE -> ErrorType.ERROR_UNKNOWN_SOCKET_TYPE
                WebRequestError.ERROR_REDIRECT_LOOP -> ErrorType.ERROR_REDIRECT_LOOP
                WebRequestError.ERROR_OFFLINE -> ErrorType.ERROR_OFFLINE
                WebRequestError.ERROR_PORT_BLOCKED -> ErrorType.ERROR_PORT_BLOCKED
                WebRequestError.ERROR_NET_RESET -> ErrorType.ERROR_NET_RESET
                WebRequestError.ERROR_UNSAFE_CONTENT_TYPE -> ErrorType.ERROR_UNSAFE_CONTENT_TYPE
                WebRequestError.ERROR_CORRUPTED_CONTENT -> ErrorType.ERROR_CORRUPTED_CONTENT
                WebRequestError.ERROR_CONTENT_CRASHED -> ErrorType.ERROR_CONTENT_CRASHED
                WebRequestError.ERROR_INVALID_CONTENT_ENCODING -> ErrorType.ERROR_INVALID_CONTENT_ENCODING
                WebRequestError.ERROR_UNKNOWN_HOST -> ErrorType.ERROR_UNKNOWN_HOST
                WebRequestError.ERROR_MALFORMED_URI -> ErrorType.ERROR_MALFORMED_URI
                WebRequestError.ERROR_UNKNOWN_PROTOCOL -> ErrorType.ERROR_UNKNOWN_PROTOCOL
                WebRequestError.ERROR_FILE_NOT_FOUND -> ErrorType.ERROR_FILE_NOT_FOUND
                WebRequestError.ERROR_FILE_ACCESS_DENIED -> ErrorType.ERROR_FILE_ACCESS_DENIED
                WebRequestError.ERROR_PROXY_CONNECTION_REFUSED -> ErrorType.ERROR_PROXY_CONNECTION_REFUSED
                WebRequestError.ERROR_UNKNOWN_PROXY_HOST -> ErrorType.ERROR_UNKNOWN_PROXY_HOST
                WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI -> ErrorType.ERROR_SAFEBROWSING_MALWARE_URI
                WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI -> ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI
                WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI -> ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI
                WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI -> ErrorType.ERROR_SAFEBROWSING_PHISHING_URI
                else -> ErrorType.UNKNOWN
            }
    }
}
