/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.HitResult
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mozilla.gecko.util.BundleEventListener
import org.mozilla.gecko.util.GeckoBundle
import org.mozilla.geckoview.GeckoResponse
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_AUDIO
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_IMAGE
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_NONE
import org.mozilla.geckoview.GeckoSession.ContentDelegate.ELEMENT_TYPE_VIDEO
import org.mozilla.geckoview.GeckoSession.ProgressDelegate.SecurityInformation
import org.mozilla.geckoview.GeckoSessionSettings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeckoEngineSessionTest {

    @Test
    fun testEngineSessionInitialization() {
        val runtime = mock(GeckoRuntime::class.java)
        val engineSession = GeckoEngineSession(runtime)

        assertTrue(engineSession.geckoSession.isOpen)
        assertNotNull(engineSession.geckoSession.navigationDelegate)
        assertNotNull(engineSession.geckoSession.progressDelegate)
    }

    @Test
    fun testProgressDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var observedProgress = 0
        var observedLoadingState = false
        var observedSecurityChange = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLoadingStateChange(loading: Boolean) { observedLoadingState = loading }
            override fun onLocationChange(url: String) { }
            override fun onProgress(progress: Int) { observedProgress = progress }
            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) { }
            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
                // We cannot assert on actual parameters as SecurityInfo's fields can't be set
                // from the outside and its constructor isn't accessible either.
                observedSecurityChange = true
            }
        })

        engineSession.geckoSession.progressDelegate.onPageStart(null, "http://mozilla.org")
        assertEquals(GeckoEngineSession.PROGRESS_START, observedProgress)
        assertEquals(true, observedLoadingState)

        engineSession.geckoSession.progressDelegate.onPageStop(null, true)
        assertEquals(GeckoEngineSession.PROGRESS_STOP, observedProgress)
        assertEquals(false, observedLoadingState)

        val securityInfo = mock(GeckoSession.ProgressDelegate.SecurityInformation::class.java)
        engineSession.geckoSession.progressDelegate.onSecurityChange(null, securityInfo)
        assertTrue(observedSecurityChange)

        observedSecurityChange = false

        engineSession.geckoSession.progressDelegate.onSecurityChange(null, null)
        assertTrue(observedSecurityChange)
    }

    @Test
    fun testNavigationDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var observedUrl = ""
        var observedCanGoBack: Boolean = false
        var observedCanGoForward: Boolean = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLoadingStateChange(loading: Boolean) {}
            override fun onLocationChange(url: String) { observedUrl = url }
            override fun onProgress(progress: Int) { }
            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) { }
            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
                canGoBack?.let { observedCanGoBack = canGoBack }
                canGoForward?.let { observedCanGoForward = canGoForward }
            }
        })

        engineSession.geckoSession.navigationDelegate.onLocationChange(null, "http://mozilla.org")
        assertEquals("http://mozilla.org", observedUrl)

        engineSession.geckoSession.navigationDelegate.onCanGoBack(null, true)
        assertEquals(true, observedCanGoBack)

        engineSession.geckoSession.navigationDelegate.onCanGoForward(null, true)
        assertEquals(true, observedCanGoForward)
    }

    @Test
    fun testLoadUrl() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        var loadUriReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
                BundleEventListener { _, _, _ -> loadUriReceived = true },
                "GeckoView:LoadUri"
        )

        engineSession.loadUrl("http://mozilla.org")
        assertTrue(loadUriReceived)
    }

    @Test
    fun testLoadData() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var loadUriReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
            BundleEventListener { _, _, _ -> loadUriReceived = true },
            "GeckoView:LoadUri"
        )

        engineSession.loadData("<html><body>Hello!</body></html>")
        assertTrue(loadUriReceived)

        loadUriReceived = false
        engineSession.loadData("Hello!", "text/plain", "UTF-8")
        assertTrue(loadUriReceived)

        loadUriReceived = false
        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", "text/plain", "base64")
        assertTrue(loadUriReceived)

        loadUriReceived = false
        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", encoding = "base64")
        assertTrue(loadUriReceived)
    }

    @Test
    fun testLoadDataBase64() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        val geckoSession = mock(GeckoSession::class.java)

        engineSession.geckoSession = geckoSession

        engineSession.loadData("Hello!", "text/plain", "UTF-8")
        verify(geckoSession).loadString(eq("Hello!"), anyString())

        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", "text/plain", "base64")
        verify(geckoSession).loadData(eq("ahr0cdovl21vemlsbgeub3jn==".toByteArray()), eq("text/plain"))

        engineSession.loadData("ahr0cdovl21vemlsbgeub3jn==", encoding = "base64")
        verify(geckoSession).loadData(eq("ahr0cdovl21vemlsbgeub3jn==".toByteArray()), eq("text/plain"))
    }

    @Test
    fun testStopLoading() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        var stopLoadingReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
                BundleEventListener { _, _, _ -> stopLoadingReceived = true },
                "GeckoView:Stop"
        )

        engineSession.stopLoading()
        assertTrue(stopLoadingReceived)
    }

    @Test
    fun testReload() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        engineSession.loadUrl("http://mozilla.org")

        var reloadReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
                BundleEventListener { _, _, _ -> reloadReceived = true },
                "GeckoView:Reload"
        )

        engineSession.reload()
        assertTrue(reloadReceived)
    }

    @Test
    fun testGoBack() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        var eventReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
                BundleEventListener { _, _, _ -> eventReceived = true },
                "GeckoView:GoBack"
        )

        engineSession.goBack()
        assertTrue(eventReceived)
    }

    @Test
    fun testGoForward() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        var eventReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
                BundleEventListener { _, _, _ -> eventReceived = true },
                "GeckoView:GoForward"
        )

        engineSession.goForward()
        assertTrue(eventReceived)
    }

    @Test
    fun testSaveState() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        engineSession.geckoSession = mock(GeckoSession::class.java)
        val currentState = GeckoSession.SessionState("")
        val stateMap = mapOf(GeckoEngineSession.GECKO_STATE_KEY to currentState.toString())

        `when`(engineSession.geckoSession.saveState(any())).thenAnswer { inv ->
            (inv.arguments[0] as GeckoResponse<GeckoSession.SessionState>).respond(currentState)
        }

        assertEquals(stateMap, engineSession.saveState())
    }

    @Test
    fun testSaveStateThrowsExceptionOnNullResult() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        engineSession.geckoSession = mock(GeckoSession::class.java)
        `when`(engineSession.geckoSession.saveState(any())).thenAnswer { inv ->
            (inv.arguments[0] as GeckoResponse<GeckoSession.SessionState>).respond(null)
        }

        try {
            engineSession.saveState()
            fail("Expected GeckoEngineException")
        } catch (e: GeckoEngineException) { }
    }

    @Test
    fun testRestoreState() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        var eventReceived = false
        engineSession.geckoSession.eventDispatcher.registerUiThreadListener(
                BundleEventListener { _, _, _ -> eventReceived = true },
                "GeckoView:RestoreState"
        )

        engineSession.restoreState(mapOf(GeckoEngineSession.GECKO_STATE_KEY to ""))
        assertTrue(eventReceived)
    }

    @Test
    fun testProgressDelegateIgnoresInitialLoadOfAboutBlank() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var observedSecurityChange = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLoadingStateChange(loading: Boolean) { }
            override fun onLocationChange(url: String) { }
            override fun onProgress(progress: Int) { }
            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) { }
            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
                observedSecurityChange = true
            }
        })

        // We need to make the constructor accessible in order to test this behaviour
        val constructor = SecurityInformation::class.java.getDeclaredConstructor(GeckoBundle::class.java)
        constructor.isAccessible = true

        val bundle = mock(GeckoBundle::class.java)
        `when`(bundle.getBundle(any())).thenReturn(mock(GeckoBundle::class.java))
        `when`(bundle.getString("origin")).thenReturn("moz-nullprincipal:{uuid}")
        engineSession.geckoSession.progressDelegate.onSecurityChange(null, constructor.newInstance(bundle))
        Assert.assertFalse(observedSecurityChange)

        `when`(bundle.getBundle(any())).thenReturn(mock(GeckoBundle::class.java))
        `when`(bundle.getString("origin")).thenReturn("https://www.mozilla.org")
        engineSession.geckoSession.progressDelegate.onSecurityChange(null, constructor.newInstance(bundle))
        assertTrue(observedSecurityChange)
    }

    @Test
    fun testNavigationDelegateIgnoresInitialLoadOfAboutBlank() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var observedUrl = ""
        engineSession.register(object : EngineSession.Observer {
            override fun onLoadingStateChange(loading: Boolean) {}
            override fun onLocationChange(url: String) { observedUrl = url }
            override fun onProgress(progress: Int) { }
            override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) { }
            override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) { }
        })

        engineSession.geckoSession.navigationDelegate.onLocationChange(null, "about:blank")
        assertEquals("", observedUrl)

        engineSession.geckoSession.navigationDelegate.onLocationChange(null, "about:blank")
        assertEquals("", observedUrl)

        engineSession.geckoSession.navigationDelegate.onLocationChange(null, "https://www.mozilla.org")
        assertEquals("https://www.mozilla.org", observedUrl)

        engineSession.geckoSession.navigationDelegate.onLocationChange(null, "about:blank")
        assertEquals("about:blank", observedUrl)
    }

    @Test
    fun testTrackingProtectionDelegateNotifiesObservers() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var trackerBlocked = ""
        engineSession.register(object : EngineSession.Observer {
            override fun onTrackerBlocked(url: String) {
                trackerBlocked = url
            }
        })

        engineSession.geckoSession.trackingProtectionDelegate.onTrackerBlocked(engineSession.geckoSession, "tracker1", 0)
        assertEquals("tracker1", trackerBlocked)
    }

    @Test
    fun testEnableTrackingProtection() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))
        val engineSession = GeckoEngineSession(runtime)

        var trackerBlockingEnabledObserved = false
        engineSession.register(object : EngineSession.Observer {
            override fun onTrackerBlockingEnabledChange(enabled: Boolean) {
                trackerBlockingEnabledObserved = enabled
            }
        })

        engineSession.enableTrackingProtection(TrackingProtectionPolicy.select(
                TrackingProtectionPolicy.ANALYTICS, TrackingProtectionPolicy.AD))
        assertTrue(trackerBlockingEnabledObserved)
        assertTrue(engineSession.geckoSession.settings.getBoolean(GeckoSessionSettings.USE_TRACKING_PROTECTION))
    }

    @Test
    fun testDisableTrackingProtection() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))
        val engineSession = GeckoEngineSession(runtime)

        var trackerBlockingDisabledObserved = false
        engineSession.register(object : EngineSession.Observer {
            override fun onTrackerBlockingEnabledChange(enabled: Boolean) {
                trackerBlockingDisabledObserved = !enabled
            }
        })

        engineSession.disableTrackingProtection()
        assertTrue(trackerBlockingDisabledObserved)
        assertFalse(engineSession.geckoSession.settings.getBoolean(GeckoSessionSettings.USE_TRACKING_PROTECTION))
    }

    @Test
    fun testSettingPrivateMode() {
        val runtime = mock(GeckoRuntime::class.java)
        `when`(runtime.settings).thenReturn(mock(GeckoRuntimeSettings::class.java))

        val engineSession = GeckoEngineSession(runtime)
        assertFalse(engineSession.geckoSession.settings.getBoolean(GeckoSessionSettings.USE_PRIVATE_MODE))

        val privateEngineSession = GeckoEngineSession(runtime, true)
        assertTrue(privateEngineSession.geckoSession.settings.getBoolean(GeckoSessionSettings.USE_PRIVATE_MODE))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testSettings() {
        GeckoEngineSession(mock(GeckoRuntime::class.java)).settings
    }

    @Test
    fun testContentDelegate() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))
        val geckoSession = mock(GeckoSession::class.java)
        val delegate = engineSession.createContentDelegate()

        var observedChanged = false
        engineSession.register(object : EngineSession.Observer {
            override fun onLongPress(hitResult: HitResult) {
                observedChanged = true
            }
        })

        delegate.onContextMenu(geckoSession, 0, 0, null, ELEMENT_TYPE_AUDIO, "file.mp3")
        assertTrue(observedChanged)

        observedChanged = false
        delegate.onContextMenu(geckoSession, 0, 0, null, ELEMENT_TYPE_AUDIO, null)
        assertFalse(observedChanged)

        observedChanged = false
        delegate.onContextMenu(geckoSession, 0, 0, null, ELEMENT_TYPE_NONE, null)
        assertFalse(observedChanged)
    }

    @Test
    fun testHandleLongClick() {
        val engineSession = GeckoEngineSession(mock(GeckoRuntime::class.java))

        var result = engineSession.handleLongClick("file.mp3", ELEMENT_TYPE_AUDIO)
        assertNotNull(result)
        assertTrue(result is HitResult.AUDIO && result.src == "file.mp3")

        result = engineSession.handleLongClick("file.mp4", ELEMENT_TYPE_VIDEO)
        assertNotNull(result)
        assertTrue(result is HitResult.VIDEO && result.src == "file.mp4")

        result = engineSession.handleLongClick("file.png", ELEMENT_TYPE_IMAGE)
        assertNotNull(result)
        assertTrue(result is HitResult.IMAGE && result.src == "file.png")

        result = engineSession.handleLongClick("file.png", ELEMENT_TYPE_IMAGE, "https://mozilla.org")
        assertNotNull(result)
        assertTrue(result is HitResult.IMAGE_SRC && result.src == "file.png" && result.uri == "https://mozilla.org")

        result = engineSession.handleLongClick(null, ELEMENT_TYPE_IMAGE)
        assertNotNull(result)
        assertTrue(result is HitResult.UNKNOWN && result.src == "")

        result = engineSession.handleLongClick("tel:+1234567890", ELEMENT_TYPE_NONE)
        assertNotNull(result)
        assertTrue(result is HitResult.PHONE && result.src == "tel:+1234567890")

        result = engineSession.handleLongClick("geo:1,-1", ELEMENT_TYPE_NONE)
        assertNotNull(result)
        assertTrue(result is HitResult.GEO && result.src == "geo:1,-1")

        result = engineSession.handleLongClick("mailto:asa@mozilla.com", ELEMENT_TYPE_NONE)
        assertNotNull(result)
        assertTrue(result is HitResult.EMAIL && result.src == "mailto:asa@mozilla.com")

        result = engineSession.handleLongClick(null, ELEMENT_TYPE_NONE, "https://mozilla.org")
        assertNotNull(result)
        assertTrue(result is HitResult.UNKNOWN && result.src == "https://mozilla.org")

        result = engineSession.handleLongClick("data://foobar", ELEMENT_TYPE_NONE, "https://mozilla.org")
        assertNotNull(result)
        assertTrue(result is HitResult.UNKNOWN && result.src == "data://foobar")

        result = engineSession.handleLongClick(null, ELEMENT_TYPE_NONE, null)
        assertNull(result)
    }
}