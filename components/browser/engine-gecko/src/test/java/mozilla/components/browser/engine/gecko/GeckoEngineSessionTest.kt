/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.os.Handler
import android.os.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.HitResult
import mozilla.components.concept.engine.UnsupportedSettingException
import mozilla.components.concept.engine.history.HistoryTrackingDelegate
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.test.any
import mozilla.components.support.test.eq
import mozilla.components.support.test.expectException
import mozilla.components.support.test.mock
import mozilla.components.support.utils.ThreadUtils
import mozilla.components.test.ReflectionUtils
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement.TYPE_AUDIO
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement.TYPE_NONE
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ContextElement.TYPE_VIDEO
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.MockWebResponseInfo
import org.mozilla.geckoview.SessionFinder
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebRequestError.ERROR_CATEGORY_UNKNOWN
import org.mozilla.geckoview.WebRequestError.ERROR_MALFORMED_URI
import org.mozilla.geckoview.WebRequestError.ERROR_UNKNOWN
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class GeckoEngineSessionTest {
    private lateinit var geckoSession: GeckoSession
    private lateinit var geckoSessionProvider: () -> GeckoSession

    private lateinit var navigationDelegate: ArgumentCaptor<GeckoSession.NavigationDelegate>
    private lateinit var progressDelegate: ArgumentCaptor<GeckoSession.ProgressDelegate>
    private lateinit var contentDelegate: ArgumentCaptor<GeckoSession.ContentDelegate>
    private lateinit var permissionDelegate: ArgumentCaptor<GeckoSession.PermissionDelegate>
    private lateinit var contentBlockingDelegate: ArgumentCaptor<ContentBlocking.Delegate>
    private lateinit var historyDelegate: ArgumentCaptor<GeckoSession.HistoryDelegate>

    private val testMainScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    @Before
    fun setup() {
        ThreadUtils.setHandlerForTest(object : Handler() {
            override fun sendMessageAtTime(msg: Message?, uptimeMillis: Long): Boolean {
                val wrappedRunnable = Runnable {
                    try {
                        msg?.callback?.run()
                    } catch (t: Throwable) {
                        // We ignore this in the test as the runnable could be calling
                        // a native method (disposeNative) which won't work in Robolectric
                    }
                }
                return super.sendMessageAtTime(Message.obtain(this, wrappedRunnable), uptimeMillis)
            }
        })

        navigationDelegate = ArgumentCaptor.forClass(GeckoSession.NavigationDelegate::class.java)
        progressDelegate = ArgumentCaptor.forClass(GeckoSession.ProgressDelegate::class.java)
        contentDelegate = ArgumentCaptor.forClass(GeckoSession.ContentDelegate::class.java)
        permissionDelegate = ArgumentCaptor.forClass(GeckoSession.PermissionDelegate::class.java)
        contentBlockingDelegate = ArgumentCaptor.forClass(ContentBlocking.Delegate::class.java)
        historyDelegate = ArgumentCaptor.forClass(GeckoSession.HistoryDelegate::class.java)

        geckoSession = mockGeckoSession()
        geckoSessionProvider = { geckoSession }
    }

    private fun captureDelegates() {
        verify(geckoSession).navigationDelegate = navigationDelegate.capture()
        verify(geckoSession).progressDelegate = progressDelegate.capture()
        verify(geckoSession).contentDelegate = contentDelegate.capture()
        verify(geckoSession).permissionDelegate = permissionDelegate.capture()
        verify(geckoSession).contentBlockingDelegate = contentBlockingDelegate.capture()
        verify(geckoSession).historyDelegate = historyDelegate.capture()
    }

    @Test
    fun engineSessionInitialization() {
        val runtime = mock(GeckoRuntime::class.java)
        GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider)

        verify(geckoSession).open(any())

        captureDelegates()

        assertNotNull(navigationDelegate.value)
        assertNotNull(progressDelegate.value)
    }

    @Test
    fun progressDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var observedProgress = 0
        var observedLoadingState = false
        var observedSecurityChange = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLoadingStateChange(loading: Boolean) { observedLoadingState = loading }
            override fun onProgress(progress: Int) { observedProgress = progress }
            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
                // We cannot assert on actual parameters as SecurityInfo's fields can't be set
                // from the outside and its constructor isn't accessible either.
                observedSecurityChange = true
            }
        })

        captureDelegates()

        progressDelegate.value.onPageStart(mock(), "http://mozilla.org")
        assertEquals(GeckoEngineSession.PROGRESS_START, observedProgress)
        assertEquals(true, observedLoadingState)

        progressDelegate.value.onPageStop(mock(), true)
        assertEquals(GeckoEngineSession.PROGRESS_STOP, observedProgress)
        assertEquals(false, observedLoadingState)

        // Stop will update the loading state and progress observers even when
        // we haven't completed been successful.
        progressDelegate.value.onPageStart(mock(), "http://mozilla.org")
        assertEquals(GeckoEngineSession.PROGRESS_START, observedProgress)
        assertEquals(true, observedLoadingState)

        progressDelegate.value.onPageStop(mock(), false)
        assertEquals(GeckoEngineSession.PROGRESS_STOP, observedProgress)
        assertEquals(false, observedLoadingState)

        val securityInfo = mock(GeckoSession.ProgressDelegate.SecurityInformation::class.java)
        progressDelegate.value.onSecurityChange(mock(), securityInfo)
        assertTrue(observedSecurityChange)

        observedSecurityChange = false

        progressDelegate.value.onSecurityChange(mock(), mock())
        assertTrue(observedSecurityChange)
    }

    @Test
    fun navigationDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var observedUrl = ""
        var observedCanGoBack: Boolean = false
        var observedCanGoForward: Boolean = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLocationChange(url: String) { observedUrl = url }
            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
                canGoBack?.let { observedCanGoBack = canGoBack }
                canGoForward?.let { observedCanGoForward = canGoForward }
            }
        })

        captureDelegates()

        navigationDelegate.value.onLocationChange(mock(), "http://mozilla.org")
        assertEquals("http://mozilla.org", observedUrl)

        navigationDelegate.value.onCanGoBack(mock(), true)
        assertEquals(true, observedCanGoBack)

        navigationDelegate.value.onCanGoForward(mock(), true)
        assertEquals(true, observedCanGoForward)
    }

    @Test
    fun contentDelegateNotifiesObserverAboutDownloads() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        val observer: EngineSession.Observer = mock()
        engineSession.register(observer)

        val info: GeckoSession.WebResponseInfo = MockWebResponseInfo(
            uri = "https://download.mozilla.org",
            contentLength = 42,
            contentType = "image/png",
            filename = "image.png"
        )

        captureDelegates()
        contentDelegate.value.onExternalResponse(mock(), info)

        verify(observer).onExternalResource(
            url = "https://download.mozilla.org",
            fileName = "image.png",
            contentLength = 42,
            contentType = "image/png",
            userAgent = null,
            cookie = null)
    }

    @Test
    fun permissionDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var observedContentPermissionRequests: MutableList<PermissionRequest> = mutableListOf()
        var observedAppPermissionRequests: MutableList<PermissionRequest> = mutableListOf()
        engineSession.register(object : EngineSession.Observer {
            override fun onContentPermissionRequest(permissionRequest: PermissionRequest) {
                observedContentPermissionRequests.add(permissionRequest)
            }

            override fun onAppPermissionRequest(permissionRequest: PermissionRequest) {
                observedAppPermissionRequests.add(permissionRequest)
            }
        })

        captureDelegates()

        permissionDelegate.value.onContentPermissionRequest(
            geckoSession,
            "originContent",
            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION,
            mock(GeckoSession.PermissionDelegate.Callback::class.java)
        )

        permissionDelegate.value.onContentPermissionRequest(
            geckoSession,
            null,
            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION,
            mock(GeckoSession.PermissionDelegate.Callback::class.java)
        )

        permissionDelegate.value.onMediaPermissionRequest(
            geckoSession,
            "originMedia",
            emptyArray(),
            emptyArray(),
            mock(GeckoSession.PermissionDelegate.MediaCallback::class.java)
        )

        permissionDelegate.value.onMediaPermissionRequest(
            geckoSession,
            "about:blank",
            null,
            null,
            mock(GeckoSession.PermissionDelegate.MediaCallback::class.java)
        )

        permissionDelegate.value.onAndroidPermissionsRequest(
            geckoSession,
            emptyArray(),
            mock(GeckoSession.PermissionDelegate.Callback::class.java)
        )

        permissionDelegate.value.onAndroidPermissionsRequest(
            geckoSession,
            null,
            mock(GeckoSession.PermissionDelegate.Callback::class.java)
        )

        assertEquals(4, observedContentPermissionRequests.size)
        assertEquals("originContent", observedContentPermissionRequests[0].uri)
        assertEquals("", observedContentPermissionRequests[1].uri)
        assertEquals("originMedia", observedContentPermissionRequests[2].uri)
        assertEquals("about:blank", observedContentPermissionRequests[3].uri)
        assertEquals(2, observedAppPermissionRequests.size)
    }

    @Test
    fun loadUrl() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.loadUrl("http://mozilla.org")

        verify(geckoSession).loadUri("http://mozilla.org")
    }

    @Test
    fun loadData() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.loadData("<html><body>Hello!</body></html>")
        verify(geckoSession).loadString(any(), eq("text/html"))

        engineSession.loadData("Hello!", "text/plain", "UTF-8")
        verify(geckoSession).loadString(any(), eq("text/plain"))

        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", "text/plain", "base64")
        verify(geckoSession).loadData(any(), eq("text/plain"))

        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", encoding = "base64")
        verify(geckoSession).loadData(any(), eq("text/html"))
    }

    @Test
    fun loadDataBase64() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.loadData("Hello!", "text/plain", "UTF-8")
        verify(geckoSession).loadString(eq("Hello!"), anyString())

        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", "text/plain", "base64")
        verify(geckoSession).loadData(eq("ahr0cdovl21vemlsbgeub3jn==".toByteArray()), eq("text/plain"))

        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", encoding = "base64")
        verify(geckoSession).loadData(eq("ahr0cdovl21vemlsbgeub3jn==".toByteArray()), eq("text/plain"))
    }

    @Test
    fun stopLoading() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.stopLoading()

        verify(geckoSession).stop()
    }

    @Test
    fun reload() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)
        engineSession.loadUrl("http://mozilla.org")

        engineSession.reload()

        verify(geckoSession).reload()
    }

    @Test
    fun goBack() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.goBack()

        verify(geckoSession).goBack()
    }

    @Test
    fun goForward() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.goForward()

        verify(geckoSession).goForward()
    }

    @Test
    fun saveState() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)
        val currentState = GeckoSession.SessionState("<state>")

        `when`(geckoSession.saveState()).thenReturn(GeckoResult.fromValue(currentState))

        val savedState = engineSession.saveState() as GeckoEngineSessionState

        assertEquals(currentState, savedState.actualState)
        assertEquals("{\"GECKO_STATE\":\"<state>\"}", savedState.toJSON().toString())
    }

    @Test
    fun restoreState() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        val actualState: GeckoSession.SessionState = mock()
        val state = GeckoEngineSessionState(actualState)

        engineSession.restoreState(state)
        verify(geckoSession).restoreState(any())
    }

    @Test
    fun `restoreState does nothing for null state`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
            geckoSessionProvider = geckoSessionProvider)

        val state = GeckoEngineSessionState(null)

        engineSession.restoreState(state)
        verify(geckoSession, never()).restoreState(any())
    }

    class MockSecurityInformation(origin: String) : SecurityInformation() {
        init {
            ReflectionUtils.setField(this, "origin", origin)
        }
    }

    @Test
    fun progressDelegateIgnoresInitialLoadOfAboutBlank() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var observedSecurityChange = false
        engineSession.register(object : EngineSession.Observer {
            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
                observedSecurityChange = true
            }
        })

        captureDelegates()

        progressDelegate.value.onSecurityChange(mock(),
                MockSecurityInformation("moz-nullprincipal:{uuid}"))
        assertFalse(observedSecurityChange)

        progressDelegate.value.onSecurityChange(mock(),
                MockSecurityInformation("https://www.mozilla.org"))
        assertTrue(observedSecurityChange)
    }

    @Test
    fun navigationDelegateIgnoresInitialLoadOfAboutBlank() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var observedUrl = ""
        engineSession.register(object : EngineSession.Observer {
            override fun onLocationChange(url: String) { observedUrl = url }
        })

        captureDelegates()

        navigationDelegate.value.onLocationChange(mock(), "about:blank")
        assertEquals("", observedUrl)

        navigationDelegate.value.onLocationChange(mock(), "about:blank")
        assertEquals("", observedUrl)

        navigationDelegate.value.onLocationChange(mock(), "https://www.mozilla.org")
        assertEquals("https://www.mozilla.org", observedUrl)

        navigationDelegate.value.onLocationChange(mock(), "about:blank")
        assertEquals("about:blank", observedUrl)
    }

    @Test
    fun `keeps track of current url via onPageStart events`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        captureDelegates()

        assertNull(engineSession.currentUrl)
        progressDelegate.value.onPageStart(geckoSession, "https://www.mozilla.org")
        assertEquals("https://www.mozilla.org", engineSession.currentUrl)

        progressDelegate.value.onPageStart(geckoSession, "https://www.firefox.com")
        assertEquals("https://www.firefox.com", engineSession.currentUrl)
    }

    @Test
    fun `notifies configured history delegate of title changes`() = runBlocking {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        // Nothing breaks if history delegate isn't configured.
        contentDelegate.value.onTitleChange(geckoSession, "Hello World!")

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        contentDelegate.value.onTitleChange(geckoSession, "Hello World!")
        verify(historyTrackingDelegate, never()).onTitleChanged(anyString(), anyString())

        // This sets the currentUrl.
        progressDelegate.value.onPageStart(geckoSession, "https://www.mozilla.com")

        contentDelegate.value.onTitleChange(geckoSession, "Hello World!")
        verify(historyTrackingDelegate).onTitleChanged(eq("https://www.mozilla.com"), eq("Hello World!"))
    }

    @Test
    fun `does not notify configured history delegate of title changes for private sessions`() = runBlocking {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext,
                privateMode = true)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        // Nothing breaks if history delegate isn't configured.
        contentDelegate.value.onTitleChange(geckoSession, "Hello World!")

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        val observer: EngineSession.Observer = mock()
        engineSession.register(observer)

        contentDelegate.value.onTitleChange(geckoSession, "Hello World!")
        verify(historyTrackingDelegate, never()).onTitleChanged(anyString(), anyString())
        verify(observer).onTitleChange("Hello World!")

        // This sets the currentUrl.
        progressDelegate.value.onPageStart(geckoSession, "https://www.mozilla.com")

        contentDelegate.value.onTitleChange(geckoSession, "Mozilla")
        verify(historyTrackingDelegate, never()).onTitleChanged(anyString(), anyString())
        verify(observer).onTitleChange("Mozilla")
    }

    @Test
    fun `does not notify configured history delegate for redirects`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        // Nothing breaks if history delegate isn't configured.
        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com", null, GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL)
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
        }

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com", null, GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY)
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate, never()).onVisited(anyString(), any())
        }
    }

    @Test
    fun `does not notify configured history delegate for top-level visits to error pages`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        historyDelegate.value.onVisited(geckoSession, "about:neterror", null, GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL or GeckoSession.HistoryDelegate.VISIT_UNRECOVERABLE_ERROR)
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate, never()).onVisited(anyString(), any())
        }
    }

    @Test
    fun `notifies configured history delegate of visits`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate
        `when`(historyTrackingDelegate.shouldStoreUri("https://www.mozilla.com")).thenReturn(true)

        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com", null, GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL)
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com"), eq(VisitType.LINK))
        }
    }

    @Test
    fun `notifies configured history delegate of reloads`() = runBlocking {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        `when`(historyTrackingDelegate.shouldStoreUri("https://www.mozilla.com")).thenReturn(true)

        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com", "https://www.mozilla.com", GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL)
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com"), eq(VisitType.RELOAD))
        }
    }

    @Test
    fun `checks with the delegate before trying to record a visit`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate
        `when`(historyTrackingDelegate.shouldStoreUri("https://www.mozilla.com/allowed")).thenReturn(true)
        `when`(historyTrackingDelegate.shouldStoreUri("https://www.mozilla.com/not-allowed")).thenReturn(false)

        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com/allowed", null, GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL)

        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).shouldStoreUri("https://www.mozilla.com/allowed")
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com/allowed"), eq(VisitType.LINK))
        }

        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com/not-allowed", null, GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL)

        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).shouldStoreUri("https://www.mozilla.com/not-allowed")
            verify(historyTrackingDelegate, never()).onVisited(eq("https://www.mozilla.com/not-allowed"), mozilla.components.support.test.any())
        }
    }

    @Test
    fun `correctly processes redirect visit flags`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate
        `when`(historyTrackingDelegate.shouldStoreUri(mozilla.components.support.test.any())).thenReturn(true)

        historyDelegate.value.onVisited(
                geckoSession,
                "https://www.mozilla.com/tempredirect",
                null,
                // bitwise 'or'
                GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL
                        or GeckoSession.HistoryDelegate.VISIT_REDIRECT_SOURCE
        )

        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com/tempredirect"), eq(VisitType.REDIRECT_TEMPORARY))
        }

        historyDelegate.value.onVisited(
                geckoSession,
                "https://www.mozilla.com/permredirect",
                null,
                GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL
                        or GeckoSession.HistoryDelegate.VISIT_REDIRECT_SOURCE_PERMANENT
        )

        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com/permredirect"), eq(VisitType.REDIRECT_PERMANENT))
        }

        // Visits below are targets of redirects, not redirects themselves.
        // Check that they're mapped to "link".
        historyDelegate.value.onVisited(
                geckoSession,
                "https://www.mozilla.com/targettemp",
                null,
                GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL
                        or GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY
        )

        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com/targettemp"), eq(VisitType.LINK))
        }

        historyDelegate.value.onVisited(
                geckoSession,
                "https://www.mozilla.com/targetperm",
                null,
                GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL
                        or GeckoSession.HistoryDelegate.VISIT_REDIRECT_TEMPORARY
        )

        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).onVisited(eq("https://www.mozilla.com/targetperm"), eq(VisitType.LINK))
        }
    }

    @Test
    fun `does not notify configured history delegate of visits for private sessions`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext,
                privateMode = true)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        historyDelegate.value.onVisited(geckoSession, "https://www.mozilla.com", "https://www.mozilla.com", GeckoSession.HistoryDelegate.VISIT_TOP_LEVEL)
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate, never()).onVisited(anyString(), any())
        }
    }

    @Test
    fun `requests visited URLs from configured history delegate`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        // Nothing breaks if history delegate isn't configured.
        historyDelegate.value.getVisited(geckoSession, arrayOf("https://www.mozilla.com", "https://www.mozilla.org"))
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
        }

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        historyDelegate.value.getVisited(geckoSession, arrayOf("https://www.mozilla.com", "https://www.mozilla.org"))
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate).getVisited(eq(listOf("https://www.mozilla.com", "https://www.mozilla.org")))
        }
    }

    @Test
    fun `does not request visited URLs from configured history delegate in private sessions`() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider,
                context = testMainScope.coroutineContext,
                privateMode = true)
        val historyTrackingDelegate: HistoryTrackingDelegate = mock()

        captureDelegates()

        engineSession.settings.historyTrackingDelegate = historyTrackingDelegate

        historyDelegate.value.getVisited(geckoSession, arrayOf("https://www.mozilla.com", "https://www.mozilla.org"))
        runBlocking(testMainScope.coroutineContext) {
            engineSession.job.children.forEach { it.join() }
            verify(historyTrackingDelegate, never()).getVisited(anyList())
        }
    }

    @Test
    fun websiteTitleUpdates() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        val observer: EngineSession.Observer = mock()
        engineSession.register(observer)

        captureDelegates()

        contentDelegate.value.onTitleChange(geckoSession, "Hello World!")

        verify(observer).onTitleChange("Hello World!")
    }

    @Test
    fun trackingProtectionDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var trackerBlocked = ""
        engineSession.register(object : EngineSession.Observer {
            override fun onTrackerBlocked(url: String) {
                trackerBlocked = url
            }
        })

        captureDelegates()

        contentBlockingDelegate.value.onContentBlocked(geckoSession, ContentBlocking.BlockEvent("tracker1", 0))
        assertEquals("tracker1", trackerBlocked)
    }

    @Test
    fun enableTrackingProtection() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))
        val session = GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider)
        val privSession = GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider, privateMode = true)

        var trackerBlockingObserved = false
        session.register(object : EngineSession.Observer {
            override fun onTrackerBlockingEnabledChange(enabled: Boolean) {
                trackerBlockingObserved = enabled
            }
        })
        var privateTrackerBlockingObserved = false
        privSession.register(object : EngineSession.Observer {
            override fun onTrackerBlockingEnabledChange(enabled: Boolean) {
                privateTrackerBlockingObserved = enabled
            }
        })

        val allPolicy = TrackingProtectionPolicy.select(TrackingProtectionPolicy.AD)
        val regularOnlyPolicy = TrackingProtectionPolicy.select(TrackingProtectionPolicy.AD).forRegularSessionsOnly()
        val privateOnlyPolicy = TrackingProtectionPolicy.select(TrackingProtectionPolicy.AD).forPrivateSessionsOnly()

        session.enableTrackingProtection(allPolicy)
        assertTrue(trackerBlockingObserved)

        session.enableTrackingProtection(privateOnlyPolicy)
        assertFalse(trackerBlockingObserved)

        session.enableTrackingProtection(regularOnlyPolicy)
        assertTrue(trackerBlockingObserved)

        privSession.enableTrackingProtection(allPolicy)
        assertTrue(privateTrackerBlockingObserved)

        privSession.enableTrackingProtection(regularOnlyPolicy)
        assertFalse(privateTrackerBlockingObserved)

        privSession.enableTrackingProtection(privateOnlyPolicy)
        assertTrue(privateTrackerBlockingObserved)
    }

    @Test
    fun disableTrackingProtection() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))
        val engineSession = GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider)

        var trackerBlockingDisabledObserved = false
        engineSession.register(object : EngineSession.Observer {
            override fun onTrackerBlockingEnabledChange(enabled: Boolean) {
                trackerBlockingDisabledObserved = !enabled
            }
        })

        engineSession.disableTrackingProtection()
        assertTrue(trackerBlockingDisabledObserved)
    }

    @Test
    fun trackingProtectionCategoriesAreAligned() {
        assertEquals(TrackingProtectionPolicy.AD, ContentBlocking.AT_AD)
        assertEquals(TrackingProtectionPolicy.ANALYTICS, ContentBlocking.AT_ANALYTIC)
        assertEquals(TrackingProtectionPolicy.CONTENT, ContentBlocking.AT_CONTENT)
        assertEquals(TrackingProtectionPolicy.SOCIAL, ContentBlocking.AT_SOCIAL)
        assertEquals(TrackingProtectionPolicy.TEST, ContentBlocking.AT_TEST)

        assertEquals(
            TrackingProtectionPolicy.all().categories and
            TrackingProtectionPolicy.CRYPTOMINING.inv() and
            TrackingProtectionPolicy.FINGERPRINTING.inv() and
            TrackingProtectionPolicy.SAFE_BROWSING_ALL.inv(),
            ContentBlocking.AT_ALL)
    }

    @Test
    fun settingTestingMode() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))

        GeckoEngineSession(runtime,
                geckoSessionProvider = geckoSessionProvider,
                defaultSettings = DefaultSettings())
        verify(geckoSession.settings).fullAccessibilityTree = false

        GeckoEngineSession(runtime,
            geckoSessionProvider = geckoSessionProvider,
            defaultSettings = DefaultSettings(testingModeEnabled = true))
        verify(geckoSession.settings).fullAccessibilityTree = true
    }

    @Test
    fun settingUserAgent() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))

        val engineSession = GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider)
        engineSession.settings.userAgentString

        verify(geckoSession.settings).userAgentOverride

        engineSession.settings.userAgentString = "test-ua"

        verify(geckoSession.settings).userAgentOverride = "test-ua"
    }

    @Test
    fun settingUserAgentDefault() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))

        GeckoEngineSession(runtime,
                geckoSessionProvider = geckoSessionProvider,
                defaultSettings = DefaultSettings(userAgentString = "test-ua"))

        verify(geckoSession.settings).userAgentOverride = "test-ua"
    }

    @Test
    fun unsupportedSettings() {
        val settings = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider).settings

        expectException(UnsupportedSettingException::class) {
            settings.javascriptEnabled = true
        }

        expectException(UnsupportedSettingException::class) {
            settings.domStorageEnabled = false
        }

        expectException(UnsupportedSettingException::class) {
            settings.trackingProtectionPolicy = TrackingProtectionPolicy.all()
        }
    }

    @Test
    fun settingInterceptorToProvideAlternativeContent() {
        var interceptorCalledWithUri: String? = null

        val interceptor = object : RequestInterceptor {
            override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
                interceptorCalledWithUri = uri
                return RequestInterceptor.InterceptionResponse.Content("<h1>Hello World</h1>")
            }
        }

        val defaultSettings = DefaultSettings(requestInterceptor = interceptor)

        GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        navigationDelegate.value.onLoadRequest(geckoSession, mockLoadRequest("sample:about"))

        assertEquals("sample:about", interceptorCalledWithUri)
        verify(geckoSession).loadString("<h1>Hello World</h1>", "text/html")
    }

    @Test
    fun settingInterceptorToProvideAlternativeUrl() {
        var interceptorCalledWithUri: String? = null

        val interceptor = object : RequestInterceptor {
            override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
                interceptorCalledWithUri = uri
                return RequestInterceptor.InterceptionResponse.Url("https://mozilla.org")
            }
        }

        val defaultSettings = DefaultSettings(requestInterceptor = interceptor)

        GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        navigationDelegate.value.onLoadRequest(geckoSession, mockLoadRequest("sample:about"))

        assertEquals("sample:about", interceptorCalledWithUri)
        verify(geckoSession).loadUri("https://mozilla.org")
    }

    @Test
    fun onLoadRequestWithoutInterceptor() {
        val defaultSettings = DefaultSettings()

        GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        navigationDelegate.value.onLoadRequest(geckoSession, mockLoadRequest("sample:about"))

        verify(geckoSession, never()).loadString(anyString(), anyString())
    }

    @Test
    fun onLoadRequestWithInterceptorThatDoesNotIntercept() {
        var interceptorCalledWithUri: String? = null

        val interceptor = object : RequestInterceptor {
            override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
                interceptorCalledWithUri = uri
                return null
            }
        }

        val defaultSettings = DefaultSettings(requestInterceptor = interceptor)

        GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        navigationDelegate.value.onLoadRequest(geckoSession, mockLoadRequest("sample:about"))

        assertEquals("sample:about", interceptorCalledWithUri!!)
        verify(geckoSession, never()).loadString(anyString(), anyString())
    }

    @Test
    fun onLoadErrorCallsInterceptorWithNull() {
        var interceptedUri: String? = null
        val requestInterceptor: RequestInterceptor = mock()
        var defaultSettings = DefaultSettings()
        var engineSession = GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        // Interceptor is not called when there is none attached.
        var onLoadError = navigationDelegate.value.onLoadError(
            geckoSession,
            "",
            WebRequestError(
                ERROR_CATEGORY_UNKNOWN,
                ERROR_UNKNOWN)
        )
        verify(requestInterceptor, never()).onErrorRequest(engineSession, ErrorType.UNKNOWN, "")
        onLoadError!!.then { value: String? ->
            interceptedUri = value
            GeckoResult.fromValue(null)
        }
        assertNull(interceptedUri)

        // Interceptor is called correctly
        defaultSettings = DefaultSettings(requestInterceptor = requestInterceptor)
        geckoSession = mockGeckoSession()
        engineSession = GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        onLoadError = navigationDelegate.value.onLoadError(
            geckoSession,
            "",
            WebRequestError(
                ERROR_CATEGORY_UNKNOWN,
                ERROR_UNKNOWN)
        )

        verify(requestInterceptor).onErrorRequest(engineSession, ErrorType.UNKNOWN, "")
        onLoadError!!.then { value: String? ->
            interceptedUri = value
            GeckoResult.fromValue(null)
        }
        assertNull(interceptedUri)
    }

    @Test
    fun onLoadErrorCallsInterceptorWithErrorPage() {
        val requestInterceptor: RequestInterceptor = object : RequestInterceptor {
            override fun onErrorRequest(
                session: EngineSession,
                errorType: ErrorType,
                uri: String?
            ): RequestInterceptor.ErrorResponse? =
                RequestInterceptor.ErrorResponse("nonNullData")
        }

        val defaultSettings = DefaultSettings(requestInterceptor = requestInterceptor)
        GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider,
                defaultSettings = defaultSettings)

        captureDelegates()

        val onLoadError = navigationDelegate.value.onLoadError(
            geckoSession,
            "about:failed",
            WebRequestError(
                ERROR_CATEGORY_UNKNOWN,
                ERROR_UNKNOWN)
        )
        onLoadError!!.then { value: String? ->
            assertTrue(value!!.contains("data:text/html;base64,"))
            GeckoResult.fromValue(null)
        }
    }

    @Test
    fun onLoadErrorCallsInterceptorWithInvalidUri() {
        val requestInterceptor: RequestInterceptor = mock()
        val defaultSettings = DefaultSettings(requestInterceptor = requestInterceptor)
        val engineSession = GeckoEngineSession(mock(), defaultSettings = defaultSettings)

        engineSession.geckoSession.navigationDelegate!!.onLoadError(
            engineSession.geckoSession,
            null,
            WebRequestError(ERROR_MALFORMED_URI, ERROR_CATEGORY_UNKNOWN)
        )
        verify(requestInterceptor).onErrorRequest(engineSession, ErrorType.ERROR_MALFORMED_URI, null)
    }

    @Test
    fun geckoErrorMappingToErrorType() {
        Assert.assertEquals(
            ErrorType.ERROR_SECURITY_SSL,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_SECURITY_SSL)
        )
        Assert.assertEquals(
            ErrorType.ERROR_SECURITY_BAD_CERT,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_SECURITY_BAD_CERT)
        )
        Assert.assertEquals(
            ErrorType.ERROR_NET_INTERRUPT,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_NET_INTERRUPT)
        )
        Assert.assertEquals(
            ErrorType.ERROR_NET_TIMEOUT,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_NET_TIMEOUT)
        )
        Assert.assertEquals(
            ErrorType.ERROR_NET_RESET,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_NET_RESET)
        )
        Assert.assertEquals(
            ErrorType.ERROR_CONNECTION_REFUSED,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_CONNECTION_REFUSED)
        )
        Assert.assertEquals(
            ErrorType.ERROR_UNKNOWN_SOCKET_TYPE,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_UNKNOWN_SOCKET_TYPE)
        )
        Assert.assertEquals(
            ErrorType.ERROR_REDIRECT_LOOP,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_REDIRECT_LOOP)
        )
        Assert.assertEquals(
            ErrorType.ERROR_OFFLINE,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_OFFLINE)
        )
        Assert.assertEquals(
            ErrorType.ERROR_PORT_BLOCKED,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_PORT_BLOCKED)
        )
        Assert.assertEquals(
            ErrorType.ERROR_UNSAFE_CONTENT_TYPE,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_UNSAFE_CONTENT_TYPE)
        )
        Assert.assertEquals(
            ErrorType.ERROR_CORRUPTED_CONTENT,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_CORRUPTED_CONTENT)
        )
        Assert.assertEquals(
            ErrorType.ERROR_CONTENT_CRASHED,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_CONTENT_CRASHED)
        )
        Assert.assertEquals(
            ErrorType.ERROR_INVALID_CONTENT_ENCODING,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_INVALID_CONTENT_ENCODING)
        )
        Assert.assertEquals(
            ErrorType.ERROR_UNKNOWN_HOST,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_UNKNOWN_HOST)
        )
        Assert.assertEquals(
            ErrorType.ERROR_MALFORMED_URI,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_MALFORMED_URI)
        )
        Assert.assertEquals(
            ErrorType.ERROR_UNKNOWN_PROTOCOL,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_UNKNOWN_PROTOCOL)
        )
        Assert.assertEquals(
            ErrorType.ERROR_FILE_NOT_FOUND,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_FILE_NOT_FOUND)
        )
        Assert.assertEquals(
            ErrorType.ERROR_FILE_ACCESS_DENIED,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_FILE_ACCESS_DENIED)
        )
        Assert.assertEquals(
            ErrorType.ERROR_PROXY_CONNECTION_REFUSED,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_PROXY_CONNECTION_REFUSED)
        )
        Assert.assertEquals(
            ErrorType.ERROR_UNKNOWN_PROXY_HOST,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_UNKNOWN_PROXY_HOST)
        )
        Assert.assertEquals(
            ErrorType.ERROR_SAFEBROWSING_MALWARE_URI,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_SAFEBROWSING_MALWARE_URI)
        )
        Assert.assertEquals(
            ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_SAFEBROWSING_HARMFUL_URI)
        )
        Assert.assertEquals(
            ErrorType.ERROR_SAFEBROWSING_PHISHING_URI,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_SAFEBROWSING_PHISHING_URI)
        )
        Assert.assertEquals(
            ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI,
            GeckoEngineSession.geckoErrorToErrorType(WebRequestError.ERROR_SAFEBROWSING_UNWANTED_URI)
        )
        Assert.assertEquals(
            ErrorType.UNKNOWN,
            GeckoEngineSession.geckoErrorToErrorType(-500)
        )
    }

    @Test
    fun defaultSettings() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))

        val defaultSettings = DefaultSettings(trackingProtectionPolicy = TrackingProtectionPolicy.all())

        GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider,
                privateMode = false, defaultSettings = defaultSettings)

        assertFalse(geckoSession.settings.usePrivateMode)
        verify(geckoSession.settings).useTrackingProtection = true
    }

    @Test
    fun contentDelegate() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)
        val delegate = engineSession.createContentDelegate()

        var observedChanged = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLongPress(hitResult: HitResult) {
                observedChanged = true
            }
        })

        class MockContextElement(
            baseUri: String?,
            linkUri: String?,
            title: String?,
            altText: String?,
            typeStr: String,
            srcUri: String?
        ) : GeckoSession.ContentDelegate.ContextElement(baseUri, linkUri, title, altText, typeStr, srcUri)

        delegate.onContextMenu(geckoSession, 0, 0,
            MockContextElement(null, null, "title", "alt", "HTMLAudioElement", "file.mp3"))
        assertTrue(observedChanged)

        observedChanged = false
        delegate.onContextMenu(geckoSession, 0, 0,
            MockContextElement(null, null, "title", "alt", "HTMLAudioElement", null))
        assertFalse(observedChanged)

        observedChanged = false
        delegate.onContextMenu(geckoSession, 0, 0,
            MockContextElement(null, null, "title", "alt", "foobar", null))
        assertFalse(observedChanged)
    }

    @Test
    fun handleLongClick() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var result = engineSession.handleLongClick("file.mp3", TYPE_AUDIO)
        assertNotNull(result)
        assertTrue(result is HitResult.AUDIO && result.src == "file.mp3")

        result = engineSession.handleLongClick("file.mp4", TYPE_VIDEO)
        assertNotNull(result)
        assertTrue(result is HitResult.VIDEO && result.src == "file.mp4")

        result = engineSession.handleLongClick("file.png", TYPE_IMAGE)
        assertNotNull(result)
        assertTrue(result is HitResult.IMAGE && result.src == "file.png")

        result = engineSession.handleLongClick("file.png", TYPE_IMAGE, "https://mozilla.org")
        assertNotNull(result)
        assertTrue(result is HitResult.IMAGE_SRC && result.src == "file.png" && result.uri == "https://mozilla.org")

        result = engineSession.handleLongClick(null, TYPE_IMAGE)
        assertNotNull(result)
        assertTrue(result is HitResult.UNKNOWN && result.src == "")

        result = engineSession.handleLongClick("tel:+1234567890", TYPE_NONE)
        assertNotNull(result)
        assertTrue(result is HitResult.PHONE && result.src == "tel:+1234567890")

        result = engineSession.handleLongClick("geo:1,-1", TYPE_NONE)
        assertNotNull(result)
        assertTrue(result is HitResult.GEO && result.src == "geo:1,-1")

        result = engineSession.handleLongClick("mailto:asa@mozilla.com", TYPE_NONE)
        assertNotNull(result)
        assertTrue(result is HitResult.EMAIL && result.src == "mailto:asa@mozilla.com")

        result = engineSession.handleLongClick(null, TYPE_NONE, "https://mozilla.org")
        assertNotNull(result)
        assertTrue(result is HitResult.UNKNOWN && result.src == "https://mozilla.org")

        result = engineSession.handleLongClick("data://foobar", TYPE_NONE, "https://mozilla.org")
        assertNotNull(result)
        assertTrue(result is HitResult.UNKNOWN && result.src == "data://foobar")

        result = engineSession.handleLongClick(null, TYPE_NONE, null)
        assertNull(result)
    }

    @Test
    fun setDesktopMode() {
        val runtime = mock(GeckoRuntime::class.java)
        val engineSession = GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider)

        var desktopModeToggled = false
        engineSession.register(object : EngineSession.Observer {
            override fun onDesktopModeChange(enabled: Boolean) {
                desktopModeToggled = true
            }
        })
        engineSession.toggleDesktopMode(true)
        assertTrue(desktopModeToggled)

        desktopModeToggled = false
        `when`(geckoSession.settings.userAgentMode)
                .thenReturn(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
        `when`(geckoSession.settings.viewportMode)
                .thenReturn(GeckoSessionSettings.VIEWPORT_MODE_DESKTOP)

        engineSession.toggleDesktopMode(true)
        assertFalse(desktopModeToggled)

        engineSession.toggleDesktopMode(true)
        assertFalse(desktopModeToggled)

        engineSession.toggleDesktopMode(false)
        assertTrue(desktopModeToggled)
    }

    @Test
    fun findAll() {
        val finderResult = mock(GeckoSession.FinderResult::class.java)
        val sessionFinder = mock(SessionFinder::class.java)
        `when`(sessionFinder.find("mozilla", 0))
                .thenReturn(GeckoResult.fromValue(finderResult))

        `when`(geckoSession.finder).thenReturn(sessionFinder)

        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var findObserved: String? = null
        var findResultObserved = false
        engineSession.register(object : EngineSession.Observer {
            override fun onFind(text: String) {
                findObserved = text
            }

            override fun onFindResult(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {
                assertEquals(0, activeMatchOrdinal)
                assertEquals(0, numberOfMatches)
                assertTrue(isDoneCounting)
                findResultObserved = true
            }
        })

        engineSession.findAll("mozilla")

        assertEquals("mozilla", findObserved)
        assertTrue(findResultObserved)
        verify(sessionFinder).find("mozilla", 0)
    }

    @Test
    fun findNext() {
        val finderResult = mock(GeckoSession.FinderResult::class.java)
        val sessionFinder = mock(SessionFinder::class.java)
        `when`(sessionFinder.find(eq(null), anyInt()))
                .thenReturn(GeckoResult.fromValue(finderResult))

        `when`(geckoSession.finder).thenReturn(sessionFinder)

        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        var findResultObserved = false
        engineSession.register(object : EngineSession.Observer {
            override fun onFindResult(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {
                assertEquals(0, activeMatchOrdinal)
                assertEquals(0, numberOfMatches)
                assertTrue(isDoneCounting)
                findResultObserved = true
            }
        })

        engineSession.findNext(true)
        assertTrue(findResultObserved)
        verify(sessionFinder).find(null, 0)

        engineSession.findNext(false)
        assertTrue(findResultObserved)
        verify(sessionFinder).find(null, GeckoSession.FINDER_FIND_BACKWARDS)
    }

    @Test
    fun clearFindMatches() {
        val finder = mock(SessionFinder::class.java)
        `when`(geckoSession.finder).thenReturn(finder)

        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.clearFindMatches()

        verify(finder).clear()
    }

    @Test
    fun exitFullScreenModeTriggersExitEvent() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)
        val observer: EngineSession.Observer = mock()

        // Verify the event is triggered for exiting fullscreen mode and GeckoView is called.
        engineSession.exitFullScreenMode()
        verify(geckoSession).exitFullScreen()

        // Verify the call to the observer.
        engineSession.register(observer)

        captureDelegates()

        contentDelegate.value.onFullScreen(geckoSession, true)

        verify(observer).onFullScreenChange(true)
    }

    @Test
    fun exitFullscreenTrueHasNoInteraction() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
                geckoSessionProvider = geckoSessionProvider)

        engineSession.exitFullScreenMode()
        verify(geckoSession).exitFullScreen()
    }

    @Test
    fun clearData() {
        val runtime = mock(GeckoRuntime::class.java)
        val engineSession = GeckoEngineSession(runtime, geckoSessionProvider = geckoSessionProvider)
        val observer: EngineSession.Observer = mock()

        engineSession.register(observer)

        engineSession.clearData()

        verifyZeroInteractions(observer)
    }

    @Test
    fun `after onCrash get called geckoSession must be reset`() {
        val runtime = mock(GeckoRuntime::class.java)
        val engineSession = GeckoEngineSession(runtime)
        val oldGeckoSession = engineSession.geckoSession

        assertTrue(engineSession.geckoSession.isOpen)

        oldGeckoSession.contentDelegate!!.onCrash(mock())

        assertFalse(oldGeckoSession.isOpen)
        assertTrue(engineSession.geckoSession != oldGeckoSession)
    }

    @Test
    fun whenOnExternalResponseDoNotProvideAFileNameMustProvideMeaningFulFileNameToTheSessionObserver() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        var meaningFulFileName = ""

        val observer = object : EngineSession.Observer {
            override fun onExternalResource(
                url: String,
                fileName: String,
                contentLength: Long?,
                contentType: String?,
                cookie: String?,
                userAgent: String?
            ) {
                meaningFulFileName = fileName
            }
        }
        engineSession.register(observer)

        val info: GeckoSession.WebResponseInfo = MockWebResponseInfo(
            uri = "http://ipv4.download.thinkbroadband.com/1MB.zip",
            contentLength = 0,
            contentType = "",
            filename = null
        )

        engineSession.geckoSession.contentDelegate!!.onExternalResponse(mock(), info)

        assertEquals("1MB.zip", meaningFulFileName)
    }

    @Test
    fun `Closing engine session should close underlying gecko session`() {
        val geckoSession = mockGeckoSession()

        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java), geckoSessionProvider = { geckoSession })

        engineSession.close()

        verify(geckoSession).close()
    }

    @Test
    fun `Handle new window load requests`() {
        GeckoEngineSession(mock(), geckoSessionProvider = geckoSessionProvider)
        captureDelegates()

        val result = navigationDelegate.value.onLoadRequest(geckoSession,
                mockLoadRequest("sample:about", GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW))

        assertNotNull(result)
        assertEquals(result!!.poll(0), AllowOrDeny.DENY)
        verify(geckoSession).loadUri("sample:about")
    }

    @Test
    fun `recoverFromCrash does not restore`() {
        // Functionality requires GeckoView 68.0

        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java),
            geckoSessionProvider = geckoSessionProvider)

        captureDelegates()

        contentDelegate.value.onCrash(engineSession.geckoSession)

        assertFalse(engineSession.recoverFromCrash())

        verify(engineSession.geckoSession, never()).restoreState(any())
    }

    private fun mockGeckoSession(): GeckoSession {
        val session = mock(GeckoSession::class.java)
        `when`(session.settings).thenReturn(
            mock(GeckoSessionSettings::class.java))
        return session
    }

    private fun mockLoadRequest(uri: String, target: Int = 0): GeckoSession.NavigationDelegate.LoadRequest {
        val constructor = GeckoSession.NavigationDelegate.LoadRequest::class.java.getDeclaredConstructor(
            String::class.java,
            String::class.java,
            Int::class.java,
            Int::class.java)
        constructor.isAccessible = true

        return constructor.newInstance(uri, uri, target, 0)
    }
}
