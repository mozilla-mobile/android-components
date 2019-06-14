/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.UnsupportedSettingException
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoWebExecutor
import org.robolectric.Robolectric
import java.io.IOException
import org.mozilla.geckoview.WebExtension as GeckoWebExtension

@RunWith(AndroidJUnit4::class)
class GeckoEngineTest {

    private val runtime: GeckoRuntime = mock(GeckoRuntime::class.java)

    @Test
    fun createView() {
        assertTrue(GeckoEngine(testContext, runtime = runtime).createView(
            Robolectric.buildActivity(Activity::class.java).get()
        ) is GeckoEngineView)
    }

    @Test
    fun createSession() {
        assertTrue(GeckoEngine(testContext, runtime = runtime).createSession() is GeckoEngineSession)
    }

    @Test
    fun name() {
        assertEquals("Gecko", GeckoEngine(testContext, runtime = runtime).name())
    }

    @Test
    fun settings() {
        val defaultSettings = DefaultSettings()
        val contentBlockingSettings =
                ContentBlocking.Settings.Builder().categories(TrackingProtectionPolicy.none().categories).build()
        val runtime = mock(GeckoRuntime::class.java)
        val runtimeSettings = mock(GeckoRuntimeSettings::class.java)
        `when`(runtimeSettings.javaScriptEnabled).thenReturn(true)
        `when`(runtimeSettings.webFontsEnabled).thenReturn(true)
        `when`(runtimeSettings.automaticFontSizeAdjustment).thenReturn(true)
        `when`(runtimeSettings.contentBlocking).thenReturn(contentBlockingSettings)
        `when`(runtime.settings).thenReturn(runtimeSettings)
        val engine = GeckoEngine(testContext, runtime = runtime, defaultSettings = defaultSettings)

        assertTrue(engine.settings.javascriptEnabled)
        engine.settings.javascriptEnabled = false
        verify(runtimeSettings).javaScriptEnabled = false

        assertTrue(engine.settings.webFontsEnabled)
        engine.settings.webFontsEnabled = false
        verify(runtimeSettings).webFontsEnabled = false

        assertTrue(engine.settings.automaticFontSizeAdjustment)
        engine.settings.automaticFontSizeAdjustment = false
        verify(runtimeSettings).automaticFontSizeAdjustment = false

        assertFalse(engine.settings.remoteDebuggingEnabled)
        engine.settings.remoteDebuggingEnabled = true
        verify(runtimeSettings).remoteDebuggingEnabled = true

        assertFalse(engine.settings.testingModeEnabled)
        engine.settings.testingModeEnabled = true
        assertTrue(engine.settings.testingModeEnabled)

        // Specifying no ua-string default should result in GeckoView's default.
        assertEquals(GeckoSession.getDefaultUserAgent(), engine.settings.userAgentString)
        // It also should be possible to read and set a new default.
        engine.settings.userAgentString = engine.settings.userAgentString + "-test"
        assertEquals(GeckoSession.getDefaultUserAgent() + "-test", engine.settings.userAgentString)

        assertEquals(TrackingProtectionPolicy.none(), engine.settings.trackingProtectionPolicy)
        engine.settings.trackingProtectionPolicy = TrackingProtectionPolicy.all()
        assertEquals(TrackingProtectionPolicy.select(
                TrackingProtectionPolicy.AD,
                TrackingProtectionPolicy.SOCIAL,
                TrackingProtectionPolicy.ANALYTICS,
                TrackingProtectionPolicy.CONTENT,
                TrackingProtectionPolicy.TEST,
                TrackingProtectionPolicy.SAFE_BROWSING_HARMFUL,
                TrackingProtectionPolicy.SAFE_BROWSING_UNWANTED,
                TrackingProtectionPolicy.SAFE_BROWSING_MALWARE,
                TrackingProtectionPolicy.SAFE_BROWSING_PHISHING
        ).categories, contentBlockingSettings.categories)
        assertEquals(defaultSettings.trackingProtectionPolicy, TrackingProtectionPolicy.all())

        try {
            engine.settings.domStorageEnabled
            fail("Expected UnsupportedOperationException")
        } catch (e: UnsupportedSettingException) { }

        try {
            engine.settings.domStorageEnabled = false
            fail("Expected UnsupportedOperationException")
        } catch (e: UnsupportedSettingException) { }
    }

    @Test
    fun defaultSettings() {
        val runtime = mock(GeckoRuntime::class.java)
        val runtimeSettings = mock(GeckoRuntimeSettings::class.java)
        val contentBlockingSettings =
                ContentBlocking.Settings.Builder().categories(TrackingProtectionPolicy.none().categories).build()
        `when`(runtimeSettings.javaScriptEnabled).thenReturn(true)
        `when`(runtime.settings).thenReturn(runtimeSettings)
        `when`(runtimeSettings.contentBlocking).thenReturn(contentBlockingSettings)

        val engine = GeckoEngine(testContext, DefaultSettings(
                trackingProtectionPolicy = TrackingProtectionPolicy.all(),
                javascriptEnabled = false,
                webFontsEnabled = false,
                automaticFontSizeAdjustment = false,
                remoteDebuggingEnabled = true,
                testingModeEnabled = true,
                userAgentString = "test-ua"), runtime)

        verify(runtimeSettings).javaScriptEnabled = false
        verify(runtimeSettings).webFontsEnabled = false
        verify(runtimeSettings).automaticFontSizeAdjustment = false
        verify(runtimeSettings).remoteDebuggingEnabled = true
        assertEquals(TrackingProtectionPolicy.select(
            TrackingProtectionPolicy.AD,
            TrackingProtectionPolicy.SOCIAL,
            TrackingProtectionPolicy.ANALYTICS,
            TrackingProtectionPolicy.CONTENT,
            TrackingProtectionPolicy.TEST,
            TrackingProtectionPolicy.SAFE_BROWSING_HARMFUL,
            TrackingProtectionPolicy.SAFE_BROWSING_UNWANTED,
            TrackingProtectionPolicy.SAFE_BROWSING_MALWARE,
            TrackingProtectionPolicy.SAFE_BROWSING_PHISHING
        ).categories, contentBlockingSettings.categories)
        assertTrue(engine.settings.testingModeEnabled)
        assertEquals("test-ua", engine.settings.userAgentString)
    }

    @Test
    fun `speculativeConnect forwards call to executor`() {
        val executor: GeckoWebExecutor = mock()

        val engine = GeckoEngine(testContext, runtime = runtime, executorProvider = { executor })

        engine.speculativeConnect("https://www.mozilla.org")

        verify(executor).speculativeConnect("https://www.mozilla.org")
    }

    @Test
    fun `install web extension successfully`() {
        val runtime = mock(GeckoRuntime::class.java)
        val engine = GeckoEngine(testContext, runtime = runtime)
        var onSuccessCalled = false
        var onErrorCalled = false
        var result = GeckoResult<Void>()

        `when`(runtime.registerWebExtension(any())).thenReturn(result)
        engine.installWebExtension(
                "test-webext",
                "resource://android/assets/extensions/test",
                onSuccess = { onSuccessCalled = true },
                onError = { _, _ -> onErrorCalled = true }
        )
        result.complete(null)

        val extCaptor = argumentCaptor<GeckoWebExtension>()
        verify(runtime).registerWebExtension(extCaptor.capture())
        assertEquals("test-webext", extCaptor.value.id)
        assertEquals("resource://android/assets/extensions/test", extCaptor.value.location)
        assertTrue(onSuccessCalled)
        assertFalse(onErrorCalled)
    }

    @Test
    fun `install web extension failure`() {
        val runtime = mock(GeckoRuntime::class.java)
        val engine = GeckoEngine(testContext, runtime = runtime)
        var onErrorCalled = false
        val expected = IOException()
        var result = GeckoResult<Void>()

        var throwable: Throwable? = null
        `when`(runtime.registerWebExtension(any())).thenReturn(result)
        engine.installWebExtension("test-webext-error", "resource://android/assets/extensions/error") { _, e ->
            onErrorCalled = true
            throwable = e
        }
        result.completeExceptionally(expected)

        assertTrue(onErrorCalled)
        assertEquals(expected, throwable)
    }

    @Test(expected = RuntimeException::class)
    fun `WHEN GeckoRuntime is shutting down THEN GeckoEngine throws runtime exception`() {
        val runtime: GeckoRuntime = mock()

        GeckoEngine(testContext, runtime = runtime)

        val captor = argumentCaptor<GeckoRuntime.Delegate>()
        verify(runtime).delegate = captor.capture()

        assertNotNull(captor.value)

        captor.value.onShutdown()
    }

    @Test
    fun `test parsing engine version`() {
        val runtime: GeckoRuntime = mock()
        val engine = GeckoEngine(testContext, runtime = runtime)
        val version = engine.version

        println(version)

        assertTrue(version.major >= 67)
        assertTrue(version.isAtLeast(67, 0, 0))
    }
}