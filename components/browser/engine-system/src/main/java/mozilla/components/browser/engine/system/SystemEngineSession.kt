/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.engine.history.HistoryTrackingDelegate
import mozilla.components.concept.engine.request.RequestInterceptor
import kotlin.reflect.KProperty

internal val additionalHeaders = mapOf(
    // For every request WebView sends a "X-requested-with" header with the package name of the
    // application. We can't really prevent that but we can at least send an empty value.
    // Unfortunately the additional headers will not be propagated to subsequent requests
    // (e.g. redirects). See issue #696.
    "X-Requested-With" to ""
)

/**
 * WebView-based EngineSession implementation.
 */
@Suppress("TooManyFunctions")
class SystemEngineSession(
    private val context: Context,
    private val defaultSettings: Settings? = null
) : EngineSession() {

    @Volatile internal lateinit var internalSettings: Settings
    @Volatile internal var historyTrackingDelegate: HistoryTrackingDelegate? = null
    @Volatile internal var trackingProtectionPolicy: TrackingProtectionPolicy? = null
    @Volatile internal var webFontsEnabled = true
    @Volatile internal var currentUrl = ""
    @Volatile internal var fullScreenCallback: WebChromeClient.CustomViewCallback? = null

    // This is public for FFTV which needs access to the WebView instance. We can mark it internal once
    // https://github.com/mozilla-mobile/android-components/issues/1616 is resolved.
    @Volatile var webView: WebView = NestedWebView(context)
        internal set(value) {
            field = value
            initSettings()
        }

    init {
        initSettings()
    }

    /**
     * See [EngineSession.loadUrl]
     */
    override fun loadUrl(url: String) {
        if (!url.isEmpty()) {
            currentUrl = url
            webView.loadUrl(url, additionalHeaders)
        }
    }

    /**
     * See [EngineSession.loadData]
     */
    override fun loadData(data: String, mimeType: String, encoding: String) {
        webView.loadData(data, mimeType, encoding)
    }

    /**
     * See [EngineSession.stopLoading]
     */
    override fun stopLoading() {
        webView.stopLoading()
    }

    /**
     * See [EngineSession.reload]
     */
    override fun reload() {
        webView.reload()
    }

    /**
     * See [EngineSession.goBack]
     */
    override fun goBack() {
        webView.goBack()
    }

    /**
     * See [EngineSession.goForward]
     */
    override fun goForward() {
        webView.goForward()
    }

    /**
     * See [EngineSession.saveState]
     */
    override fun saveState(): EngineSessionState {
        return runBlocking(Dispatchers.Main) {
            val state = Bundle()
            webView.saveState(state)

            SystemEngineSessionState(state)
        }
    }

    /**
     * See [EngineSession.restoreState]
     */
    override fun restoreState(state: EngineSessionState) {
        if (state !is SystemEngineSessionState) {
            throw IllegalArgumentException("Can only restore from SystemEngineSessionState")
        }

        webView.restoreState(state.bundle)
    }

    /**
     * See [EngineSession.enableTrackingProtection]
     */
    override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {
        // Make sure Url matcher is preloaded now that tracking protection is enabled
        CoroutineScope(Dispatchers.IO).launch {
            SystemEngineView.getOrCreateUrlMatcher(context, policy)
        }

        // TODO check if policy should be applied for this session type
        // (regular|private) once we support private browsing in system engine:
        // https://github.com/mozilla-mobile/android-components/issues/649
        trackingProtectionPolicy = policy
        notifyObservers { onTrackerBlockingEnabledChange(true) }
    }

    /**
     * See [EngineSession.disableTrackingProtection]
     */
    override fun disableTrackingProtection() {
        trackingProtectionPolicy = null
        notifyObservers { onTrackerBlockingEnabledChange(false) }
    }

    /**
     * See [EngineSession.clearData]
     */
    override fun clearData() {
        webView.apply {
            clearFormData()
            clearHistory()
            clearMatches()
            clearSslPreferences()
            clearCache(true)

            // We don't care about the callback - we just want to make sure cookies are gone
            CookieManager.getInstance().removeAllCookies(null)

            webStorage().deleteAllData()

            webViewDatabase(context).clearHttpAuthUsernamePassword()
        }
    }

    /**
     * See [EngineSession.findAll]
     */
    override fun findAll(text: String) {
        notifyObservers { onFind(text) }
        webView.findAllAsync(text)
    }

    /**
     * See [EngineSession.findNext]
     */
    override fun findNext(forward: Boolean) {
        webView.findNext(forward)
    }

    /**
     * See [EngineSession.clearFindMatches]
     */
    override fun clearFindMatches() {
        webView.clearMatches()
    }

    /**
     * See [EngineSession.settings]
     */
    override val settings: Settings
        get() = internalSettings

    class WebSetting<T>(private val get: () -> T, private val set: (T) -> Unit) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
    }

    internal fun initSettings() {
        webView.settings?.let { webSettings ->
            // Explicitly set global defaults.
            webSettings.setAppCacheEnabled(false)
            webSettings.databaseEnabled = false

            setDeprecatedWebSettings(webSettings)

            // We currently don't implement the callback to support turning this on.
            webSettings.setGeolocationEnabled(false)

            // webViewSettings built-in zoom controls are the only supported ones, so they should be turned on.
            webSettings.builtInZoomControls = true

            initSettings(webView, webSettings)
        }
    }

    @Suppress("DEPRECATION")
    private fun setDeprecatedWebSettings(webSettings: WebSettings) {
        // Since API26 an autofill platform feature is used instead of WebView's form data. This
        // has no effect. Form data is supported on pre-26 API versions.
        webSettings.saveFormData = false
        // Deprecated in API18.
        webSettings.savePassword = false
    }

    private fun initSettings(webView: WebView, s: WebSettings) {
        internalSettings = object : Settings() {
            override var javascriptEnabled by WebSetting(s::getJavaScriptEnabled, s::setJavaScriptEnabled)
            override var domStorageEnabled by WebSetting(s::getDomStorageEnabled, s::setDomStorageEnabled)
            override var allowFileAccess by WebSetting(s::getAllowFileAccess, s::setAllowFileAccess)
            override var allowContentAccess by WebSetting(s::getAllowContentAccess, s::setAllowContentAccess)
            override var userAgentString by WebSetting(s::getUserAgentString, s::setUserAgentString)
            override var displayZoomControls by WebSetting(s::getDisplayZoomControls, s::setDisplayZoomControls)
            override var loadWithOverviewMode by WebSetting(s::getLoadWithOverviewMode, s::setLoadWithOverviewMode)
            override var supportMultipleWindows by WebSetting(s::supportMultipleWindows, s::setSupportMultipleWindows)
            override var allowFileAccessFromFileURLs by WebSetting(
                    s::getAllowFileAccessFromFileURLs, s::setAllowFileAccessFromFileURLs)
            override var allowUniversalAccessFromFileURLs by WebSetting(
                    s::getAllowUniversalAccessFromFileURLs, s::setAllowUniversalAccessFromFileURLs)
            override var mediaPlaybackRequiresUserGesture by WebSetting(
                    s::getMediaPlaybackRequiresUserGesture, s::setMediaPlaybackRequiresUserGesture)
            override var javaScriptCanOpenWindowsAutomatically by WebSetting(
                    s::getJavaScriptCanOpenWindowsAutomatically, s::setJavaScriptCanOpenWindowsAutomatically)

            override var verticalScrollBarEnabled
                get() = webView.isVerticalScrollBarEnabled
                set(value) { webView.isVerticalScrollBarEnabled = value }

            override var horizontalScrollBarEnabled
                get() = webView.isHorizontalScrollBarEnabled
                set(value) { webView.isHorizontalScrollBarEnabled = value }

            override var webFontsEnabled
                get() = this@SystemEngineSession.webFontsEnabled
                set(value) { this@SystemEngineSession.webFontsEnabled = value }

            override var trackingProtectionPolicy: TrackingProtectionPolicy?
                get() = this@SystemEngineSession.trackingProtectionPolicy
                set(value) = value?.let { enableTrackingProtection(it) } ?: disableTrackingProtection()

            override var historyTrackingDelegate: HistoryTrackingDelegate?
                get() = this@SystemEngineSession.historyTrackingDelegate
                set(value) { this@SystemEngineSession.historyTrackingDelegate = value }

            override var requestInterceptor: RequestInterceptor? = null
        }.apply {
            defaultSettings?.let {
                javascriptEnabled = it.javascriptEnabled
                domStorageEnabled = it.domStorageEnabled
                webFontsEnabled = it.webFontsEnabled
                displayZoomControls = it.displayZoomControls
                loadWithOverviewMode = it.loadWithOverviewMode
                trackingProtectionPolicy = it.trackingProtectionPolicy
                historyTrackingDelegate = it.historyTrackingDelegate
                requestInterceptor = it.requestInterceptor
                mediaPlaybackRequiresUserGesture = it.mediaPlaybackRequiresUserGesture
                javaScriptCanOpenWindowsAutomatically = it.javaScriptCanOpenWindowsAutomatically
                allowFileAccess = it.allowFileAccess
                allowContentAccess = it.allowContentAccess
                allowUniversalAccessFromFileURLs = it.allowUniversalAccessFromFileURLs
                allowFileAccessFromFileURLs = it.allowFileAccessFromFileURLs
                verticalScrollBarEnabled = it.verticalScrollBarEnabled
                horizontalScrollBarEnabled = it.horizontalScrollBarEnabled
                userAgentString = it.userAgentString
                supportMultipleWindows = it.supportMultipleWindows
            }
        }
    }

    /**
     * See [EngineSession.toggleDesktopMode]
     */
    override fun toggleDesktopMode(enable: Boolean, reload: Boolean) {
        val webSettings = webView.settings
        webSettings.userAgentString = toggleDesktopUA(webSettings.userAgentString, enable)
        webSettings.useWideViewPort = enable

        notifyObservers { onDesktopModeChange(enable) }

        if (reload) {
            webView.reload()
        }
    }

    /**
     * See [EngineSession.exitFullScreenMode]
     */
    override fun exitFullScreenMode() {
        fullScreenCallback?.onCustomViewHidden()
    }

    override fun captureThumbnail(): Bitmap? {
        webView.buildDrawingCache()
        val outBitmap = webView.drawingCache?.let { cache -> Bitmap.createBitmap(cache) }
        webView.destroyDrawingCache()
        return outBitmap
    }

    internal fun toggleDesktopUA(userAgent: String, requestDesktop: Boolean): String {
        return if (requestDesktop) {
            userAgent.replace("Mobile", "eliboM").replace("Android", "diordnA")
        } else {
            userAgent.replace("eliboM", "Mobile").replace("diordnA", "Android")
        }
    }

    internal fun webStorage(): WebStorage = WebStorage.getInstance()

    internal fun webViewDatabase(context: Context) = WebViewDatabase.getInstance(context)

    /**
     * Helper method to notify observers from other classes in this package. This is needed as
     * almost everything is implemented by WebView and its listeners. There is no actual concept of
     * a session when using WebView.
     */
    internal fun internalNotifyObservers(block: Observer.() -> Unit) {
        super.notifyObservers(block)
    }

    companion object {
        /**
         * Provides an ErrorType corresponding to the error code provided.
         *
         * Chromium's mapping (internal error code, to Android WebView error code) is described at:
         * https://goo.gl/vspwct (ErrorCodeConversionHelper.java)
         */
        @Suppress("ComplexMethod")
        internal fun webViewErrorToErrorType(errorCode: Int) =
            when (errorCode) {
                WebViewClient.ERROR_UNKNOWN -> ErrorType.UNKNOWN

                // This is probably the most commonly shown error. If there's no network, we inevitably
                // show this.
                WebViewClient.ERROR_HOST_LOOKUP -> ErrorType.ERROR_UNKNOWN_HOST

                WebViewClient.ERROR_CONNECT -> ErrorType.ERROR_CONNECTION_REFUSED

                // It's unclear what this actually means - it's not well documented. Based on looking at
                // ErrorCodeConversionHelper this could happen if networking is disabled during load, in which
                // case the generic error is good enough:
                WebViewClient.ERROR_IO -> ErrorType.ERROR_CONNECTION_REFUSED

                WebViewClient.ERROR_TIMEOUT -> ErrorType.ERROR_NET_TIMEOUT

                WebViewClient.ERROR_REDIRECT_LOOP -> ErrorType.ERROR_REDIRECT_LOOP

                WebViewClient.ERROR_UNSUPPORTED_SCHEME -> ErrorType.ERROR_UNKNOWN_PROTOCOL

                WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> ErrorType.ERROR_SECURITY_SSL

                WebViewClient.ERROR_BAD_URL -> ErrorType.ERROR_MALFORMED_URI

                // Seems to be an indication of OOM, insufficient resources, or too many queued DNS queries
                WebViewClient.ERROR_TOO_MANY_REQUESTS -> ErrorType.UNKNOWN

                WebViewClient.ERROR_FILE_NOT_FOUND -> ErrorType.ERROR_FILE_NOT_FOUND

                // There's no mapping for the following errors yet. At the time this library was
                // extracted from Focus we didn't use any of those errors.
                // WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME
                // WebViewClient.ERROR_AUTHENTICATION
                // WebViewClient.ERROR_FILE
                else -> ErrorType.UNKNOWN
            }
    }
}
