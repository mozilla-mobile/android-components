/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.annotation.SuppressLint
import android.graphics.Bitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.engine.gecko.permission.GeckoPermissionRequest
import mozilla.components.browser.engine.gecko.prompt.GeckoPromptDelegate
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.HitResult
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.engine.history.HistoryTrackingDelegate
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.request.RequestInterceptor.InterceptionResponse
import mozilla.components.support.ktx.android.util.Base64
import mozilla.components.support.ktx.kotlin.isEmail
import mozilla.components.support.ktx.kotlin.isGeoLocation
import mozilla.components.support.ktx.kotlin.isPhone
import mozilla.components.support.utils.DownloadUtils
import org.mozilla.gecko.util.ThreadUtils
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_AUDIO
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_IMAGE
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_NONE
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_VIDEO
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSessionSettings

/**
 * Gecko-based EngineSession implementation.
 */
@Suppress("TooManyFunctions")
class GeckoEngineSession(
    private val runtime: GeckoRuntime,
    private val privateMode: Boolean = false,
    private val defaultSettings: Settings? = null
) : EngineSession() {

    internal lateinit var geckoSession: GeckoSession
    internal var currentUrl: String? = null

    /**
     * See [EngineSession.settings]
     */
    override val settings: Settings = object : Settings() {
        override var requestInterceptor: RequestInterceptor? = null
        override var historyTrackingDelegate: HistoryTrackingDelegate? = null
    }

    private var initialLoad = true

    init {
        initGeckoSession()
    }

    /**
     * See [EngineSession.loadUrl]
     */
    override fun loadUrl(url: String) {
        geckoSession.loadUri(url)
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
    }

    /**
     * See [EngineSession.goForward]
     */
    override fun goForward() {
        geckoSession.goForward()
    }

    /**
     * See [EngineSession.saveState]
     *
     * GeckoView provides a String representing the entire session state. We
     * store this String using a single Map entry with key GECKO_STATE_KEY.

     * See https://bugzilla.mozilla.org/show_bug.cgi?id=1441810 for
     * discussion on sync vs. async, where a decision was made that
     * callers should provide synchronous wrappers, if needed. In case we're
     * asking for the state when persisting, a separate (independent) thread
     * is used so we're not blocking anything else. In case of calling this
     * method from onPause or similar, we also want a synchronous response.
     */
    override fun saveState(): Map<String, Any> = runBlocking {
        val stateMap = CompletableDeferred<Map<String, Any>>()

        ThreadUtils.sGeckoHandler.post {
            geckoSession.saveState().then({ state ->
                stateMap.complete(mapOf(GECKO_STATE_KEY to state.toString()))
                GeckoResult<Void>()
            }, { throwable ->
                stateMap.cancel(throwable)
                GeckoResult<Void>()
            })
        }

        stateMap.await()
    }

    /**
     * See [EngineSession.restoreState]
     */
    override fun restoreState(state: Map<String, Any>) {
        if (state.containsKey(GECKO_STATE_KEY)) {
            val sessionState = GeckoSession.SessionState(state[GECKO_STATE_KEY] as String)
            geckoSession.restoreState(sessionState)
        }
    }

    /**
     * See [EngineSession.enableTrackingProtection]
     */
    override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {
        geckoSession.settings.setBoolean(GeckoSessionSettings.USE_TRACKING_PROTECTION, true)
        notifyObservers { onTrackerBlockingEnabledChange(true) }
    }

    /**
     * See [EngineSession.disableTrackingProtection]
     */
    override fun disableTrackingProtection() {
        geckoSession.settings.setBoolean(GeckoSessionSettings.USE_TRACKING_PROTECTION, false)
        notifyObservers { onTrackerBlockingEnabledChange(false) }
    }

    /**
     * See [EngineSession.settings]
     */
    override fun toggleDesktopMode(enable: Boolean, reload: Boolean) {
        val currentMode = geckoSession.settings.getInt(GeckoSessionSettings.USER_AGENT_MODE)
        val newMode = if (enable) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }

        if (newMode != currentMode) {
            geckoSession.settings.setInt(GeckoSessionSettings.USER_AGENT_MODE, newMode)
            notifyObservers { onDesktopModeChange(enable) }
        }

        if (reload) {
            geckoSession.reload()
        }
    }

    /**
     * See [EngineSession.clearData]
     */
    override fun clearData() {
        // API not available yet.
    }

    /**
     * See [EngineSession.findAll]
     */
    override fun findAll(text: String) {
        notifyObservers { onFind(text) }
        geckoSession.finder.find(text, 0).then { result: GeckoSession.FinderResult? ->
            result?.let {
                notifyObservers { onFindResult(it.current, it.total, true) }
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
                notifyObservers { onFindResult(it.current, it.total, true) }
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
     * See [EngineSession.close].
     */
    override fun close() {
        super.close()

        geckoSession.close()
    }

    /**
     * NavigationDelegate implementation for forwarding callbacks to observers of the session.
     */
    @Suppress("ComplexMethod")
    private fun createNavigationDelegate() = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(session: GeckoSession?, url: String) {
            // Ignore initial load of about:blank (see https://github.com/mozilla-mobile/android-components/issues/403)
            if (initialLoad && url == ABOUT_BLANK) {
                return
            }
            initialLoad = false

            notifyObservers { onLocationChange(url) }
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny> {
            val response = settings.requestInterceptor?.onLoadRequest(
                this@GeckoEngineSession,
                request.uri
            )?.apply {
                when (this) {
                    is InterceptionResponse.Content -> loadData(data, mimeType, encoding)
                    is InterceptionResponse.Url -> loadUrl(url)
                }
            }

            return GeckoResult.fromValue(if (response != null) AllowOrDeny.DENY else AllowOrDeny.ALLOW)
        }

        override fun onCanGoForward(session: GeckoSession?, canGoForward: Boolean) {
            notifyObservers { onNavigationStateChange(canGoForward = canGoForward) }
        }

        override fun onCanGoBack(session: GeckoSession?, canGoBack: Boolean) {
            notifyObservers { onNavigationStateChange(canGoBack = canGoBack) }
        }

        override fun onNewSession(
            session: GeckoSession,
            uri: String
        ): GeckoResult<GeckoSession> = GeckoResult.fromValue(null)

        override fun onLoadError(
            session: GeckoSession?,
            uri: String?,
            category: Int,
            error: Int
        ): GeckoResult<String> {
            settings.requestInterceptor?.onErrorRequest(
                this@GeckoEngineSession,
                geckoErrorToErrorType(error),
                uri
            )?.apply {
                return GeckoResult.fromValue(Base64.encodeToUriString(data))
            }
            return GeckoResult.fromValue(null)
        }
    }

    /**
     * ProgressDelegate implementation for forwarding callbacks to observers of the session.
     */
    private fun createProgressDelegate() = object : GeckoSession.ProgressDelegate {
        override fun onProgressChange(session: GeckoSession?, progress: Int) {
            notifyObservers { onProgress(progress) }
        }

        override fun onSecurityChange(
            session: GeckoSession?,
            securityInfo: GeckoSession.ProgressDelegate.SecurityInformation?
        ) {
            // Ignore initial load of about:blank (see https://github.com/mozilla-mobile/android-components/issues/403)
            if (initialLoad && securityInfo?.origin?.startsWith(MOZ_NULL_PRINCIPAL) == true) {
                return
            }

            notifyObservers {
                if (securityInfo == null) {
                    onSecurityChange(false)
                    return@notifyObservers
                }
                onSecurityChange(securityInfo.isSecure, securityInfo.host, securityInfo.issuerOrganization)
            }
        }

        override fun onPageStart(session: GeckoSession?, url: String?) {
            url?.let { currentUrl = it }

            notifyObservers {
                onProgress(PROGRESS_START)
                onLoadingStateChange(true)
            }
        }

        override fun onPageStop(session: GeckoSession?, success: Boolean) {
            if (success) {
                notifyObservers {
                    onProgress(PROGRESS_STOP)
                    onLoadingStateChange(false)
                }
            }
        }
    }

    @Suppress("ComplexMethod")
    internal fun createContentDelegate() = object : GeckoSession.ContentDelegate {
        override fun onContextMenu(
            session: GeckoSession,
            screenX: Int,
            screenY: Int,
            uri: String?,
            elementType: Int,
            elementSrc: String?
        ) {
            val hitResult = handleLongClick(elementSrc, elementType, uri)
            hitResult?.let {
                notifyObservers { onLongPress(it) }
            }
        }

        override fun onCrash(session: GeckoSession?) {
            geckoSession.close()
            initGeckoSession()
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            notifyObservers { onFullScreenChange(fullScreen) }
        }

        override fun onExternalResponse(session: GeckoSession, response: GeckoSession.WebResponseInfo) {
            notifyObservers {
                val fileName = response.filename
                    ?: DownloadUtils.guessFileName("", response.uri, response.contentType)
                onExternalResource(
                        url = response.uri,
                        contentLength = response.contentLength,
                        contentType = response.contentType,
                        fileName = fileName)
            }
        }

        override fun onCloseRequest(session: GeckoSession) = Unit

        override fun onTitleChange(session: GeckoSession, title: String) {
            currentUrl?.let { url ->
                settings.historyTrackingDelegate?.let { delegate ->
                    runBlocking {
                        delegate.onTitleChanged(url, title)
                    }
                }
            }
            notifyObservers { onTitleChange(title) }
        }

        override fun onFocusRequest(session: GeckoSession) = Unit
    }

    private fun createTrackingProtectionDelegate() = GeckoSession.TrackingProtectionDelegate {
        session, uri, _ ->
            session?.let { uri?.let { notifyObservers { onTrackerBlocked(it) } } }
    }

    private fun createPermissionDelegate() = object : GeckoSession.PermissionDelegate {
        override fun onContentPermissionRequest(
            session: GeckoSession?,
            uri: String?,
            type: Int,
            access: String?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            val request = GeckoPermissionRequest.Content(uri ?: "", type, callback)
            notifyObservers { onContentPermissionRequest(request) }
        }

        override fun onMediaPermissionRequest(
            session: GeckoSession?,
            uri: String?,
            video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback
        ) {
            val request = GeckoPermissionRequest.Media(
                    uri ?: "",
                    video?.toList() ?: emptyList(),
                    audio?.toList() ?: emptyList(),
                    callback)
            notifyObservers { onContentPermissionRequest(request) }
        }

        override fun onAndroidPermissionsRequest(
            session: GeckoSession?,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            val request = GeckoPermissionRequest.App(
                    permissions?.toList() ?: emptyList(),
                    callback)
            notifyObservers { onAppPermissionRequest(request) }
        }
    }

    @Suppress("ComplexMethod")
    fun handleLongClick(elementSrc: String?, elementType: Int, uri: String? = null): HitResult? {
        return when (elementType) {
            ELEMENT_TYPE_AUDIO ->
                elementSrc?.let {
                    HitResult.AUDIO(it)
                }
            ELEMENT_TYPE_VIDEO ->
                elementSrc?.let {
                    HitResult.VIDEO(it)
                }
            ELEMENT_TYPE_IMAGE -> {
                when {
                    elementSrc != null && uri != null ->
                        HitResult.IMAGE_SRC(elementSrc, uri)
                    elementSrc != null ->
                        HitResult.IMAGE(elementSrc)
                    else -> HitResult.UNKNOWN("")
                }
            }
            ELEMENT_TYPE_NONE -> {
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

    override fun captureThumbnail(): Bitmap? {
        // TODO Waiting for the Gecko team to create an API for this
        // See https://bugzilla.mozilla.org/show_bug.cgi?id=1462018
        return null
    }

    private fun initGeckoSession() {
        this.geckoSession = GeckoSession()
        defaultSettings?.trackingProtectionPolicy?.let { enableTrackingProtection(it) }
        defaultSettings?.requestInterceptor?.let { settings.requestInterceptor = it }
        defaultSettings?.historyTrackingDelegate?.let { settings.historyTrackingDelegate = it }

        geckoSession.settings.setBoolean(GeckoSessionSettings.USE_PRIVATE_MODE, privateMode)
        geckoSession.open(runtime)

        geckoSession.navigationDelegate = createNavigationDelegate()
        geckoSession.progressDelegate = createProgressDelegate()
        geckoSession.contentDelegate = createContentDelegate()
        geckoSession.trackingProtectionDelegate = createTrackingProtectionDelegate()
        geckoSession.permissionDelegate = createPermissionDelegate()
        geckoSession.promptDelegate = GeckoPromptDelegate(this)
    }

    companion object {
        internal const val PROGRESS_START = 25
        internal const val PROGRESS_STOP = 100
        internal const val GECKO_STATE_KEY = "GECKO_STATE"
        internal const val MOZ_NULL_PRINCIPAL = "moz-nullprincipal:"
        internal const val ABOUT_BLANK = "about:blank"

        /**
         * Provides an ErrorType corresponding to the error code provided.
         */
        @Suppress("ComplexMethod")
        internal fun geckoErrorToErrorType(@NavigationDelegate.LoadError errorCode: Int) =
            when (errorCode) {
                NavigationDelegate.ERROR_UNKNOWN -> ErrorType.UNKNOWN
                NavigationDelegate.ERROR_SECURITY_SSL -> ErrorType.ERROR_SECURITY_SSL
                NavigationDelegate.ERROR_SECURITY_BAD_CERT -> ErrorType.ERROR_SECURITY_BAD_CERT
                NavigationDelegate.ERROR_NET_INTERRUPT -> ErrorType.ERROR_NET_INTERRUPT
                NavigationDelegate.ERROR_NET_TIMEOUT -> ErrorType.ERROR_NET_TIMEOUT
                NavigationDelegate.ERROR_CONNECTION_REFUSED -> ErrorType.ERROR_CONNECTION_REFUSED
                NavigationDelegate.ERROR_UNKNOWN_SOCKET_TYPE -> ErrorType.ERROR_UNKNOWN_SOCKET_TYPE
                NavigationDelegate.ERROR_REDIRECT_LOOP -> ErrorType.ERROR_REDIRECT_LOOP
                NavigationDelegate.ERROR_OFFLINE -> ErrorType.ERROR_OFFLINE
                NavigationDelegate.ERROR_PORT_BLOCKED -> ErrorType.ERROR_PORT_BLOCKED
                NavigationDelegate.ERROR_NET_RESET -> ErrorType.ERROR_NET_RESET
                NavigationDelegate.ERROR_UNSAFE_CONTENT_TYPE -> ErrorType.ERROR_UNSAFE_CONTENT_TYPE
                NavigationDelegate.ERROR_CORRUPTED_CONTENT -> ErrorType.ERROR_CORRUPTED_CONTENT
                NavigationDelegate.ERROR_CONTENT_CRASHED -> ErrorType.ERROR_CONTENT_CRASHED
                NavigationDelegate.ERROR_INVALID_CONTENT_ENCODING -> ErrorType.ERROR_INVALID_CONTENT_ENCODING
                NavigationDelegate.ERROR_UNKNOWN_HOST -> ErrorType.ERROR_UNKNOWN_HOST
                NavigationDelegate.ERROR_MALFORMED_URI -> ErrorType.ERROR_MALFORMED_URI
                NavigationDelegate.ERROR_UNKNOWN_PROTOCOL -> ErrorType.ERROR_UNKNOWN_PROTOCOL
                NavigationDelegate.ERROR_FILE_NOT_FOUND -> ErrorType.ERROR_FILE_NOT_FOUND
                NavigationDelegate.ERROR_FILE_ACCESS_DENIED -> ErrorType.ERROR_FILE_ACCESS_DENIED
                NavigationDelegate.ERROR_PROXY_CONNECTION_REFUSED -> ErrorType.ERROR_PROXY_CONNECTION_REFUSED
                NavigationDelegate.ERROR_UNKNOWN_PROXY_HOST -> ErrorType.ERROR_UNKNOWN_PROXY_HOST
                NavigationDelegate.ERROR_SAFEBROWSING_MALWARE_URI -> ErrorType.ERROR_SAFEBROWSING_MALWARE_URI
                NavigationDelegate.ERROR_SAFEBROWSING_UNWANTED_URI -> ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI
                NavigationDelegate.ERROR_SAFEBROWSING_HARMFUL_URI -> ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI
                NavigationDelegate.ERROR_SAFEBROWSING_PHISHING_URI -> ErrorType.ERROR_SAFEBROWSING_PHISHING_URI
                else -> ErrorType.UNKNOWN
            }
    }
}
