/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine

import android.graphics.Bitmap
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.manifest.WebAppManifest
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.engine.HitResult
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.engine.media.Media
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.test.mock
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineObserverTest {

    @Test
    fun engineSessionObserver() {
        val session = Session("")
        val engineSession = object : EngineSession() {
            override val settings: Settings
                get() = mock(Settings::class.java)

            override fun goBack() {}
            override fun goForward() {}
            override fun reload() {}
            override fun stopLoading() {}
            override fun restoreState(state: EngineSessionState) {}
            override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {}
            override fun disableTrackingProtection() {}
            override fun toggleDesktopMode(enable: Boolean, reload: Boolean) {
                notifyObservers { onDesktopModeChange(enable) }
            }
            override fun findAll(text: String) {}
            override fun findNext(forward: Boolean) {}
            override fun clearFindMatches() {}
            override fun exitFullScreenMode() {}
            override fun saveState(): EngineSessionState = mock()
            override fun recoverFromCrash(): Boolean { return false }

            override fun loadData(data: String, mimeType: String, encoding: String) {
                notifyObservers { onLocationChange(data) }
                notifyObservers { onProgress(100) }
                notifyObservers { onLoadingStateChange(true) }
                notifyObservers { onNavigationStateChange(true, true) }
            }
            override fun loadUrl(url: String) {
                notifyObservers { onLocationChange(url) }
                notifyObservers { onProgress(100) }
                notifyObservers { onLoadingStateChange(true) }
                notifyObservers { onNavigationStateChange(true, true) }
            }
        }
        engineSession.register(EngineObserver(session))

        engineSession.loadUrl("http://mozilla.org")
        engineSession.toggleDesktopMode(true)
        Assert.assertEquals("http://mozilla.org", session.url)
        Assert.assertEquals("", session.searchTerms)
        Assert.assertEquals(100, session.progress)
        Assert.assertEquals(true, session.loading)
        Assert.assertEquals(true, session.canGoForward)
        Assert.assertEquals(true, session.canGoBack)
        Assert.assertEquals(true, session.desktopMode)
    }

    @Test
    fun engineSessionObserverWithSecurityChanges() {
        val session = Session("")
        val engineSession = object : EngineSession() {
            override val settings: Settings
                get() = mock(Settings::class.java)

            override fun goBack() {}
            override fun goForward() {}
            override fun stopLoading() {}
            override fun reload() {}
            override fun restoreState(state: EngineSessionState) {}
            override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {}
            override fun disableTrackingProtection() {}
            override fun toggleDesktopMode(enable: Boolean, reload: Boolean) {}
            override fun findAll(text: String) {}
            override fun findNext(forward: Boolean) {}
            override fun clearFindMatches() {}
            override fun exitFullScreenMode() {}
            override fun saveState(): EngineSessionState = mock()
            override fun loadData(data: String, mimeType: String, encoding: String) {}
            override fun recoverFromCrash(): Boolean { return false }
            override fun loadUrl(url: String) {
                if (url.startsWith("https://")) {
                    notifyObservers { onSecurityChange(true, "host", "issuer") }
                } else {
                    notifyObservers { onSecurityChange(false) }
                }
            }
        }
        engineSession.register(EngineObserver(session))

        engineSession.loadUrl("http://mozilla.org")
        Assert.assertEquals(Session.SecurityInfo(false), session.securityInfo)

        engineSession.loadUrl("https://mozilla.org")
        Assert.assertEquals(Session.SecurityInfo(true, "host", "issuer"), session.securityInfo)
    }

    @Test
    fun engineSessionObserverWithTrackingProtection() {
        val session = Session("")
        val engineSession = object : EngineSession() {
            override val settings: Settings
                get() = mock(Settings::class.java)

            override fun goBack() {}
            override fun goForward() {}
            override fun stopLoading() {}
            override fun reload() {}
            override fun restoreState(state: EngineSessionState) {}
            override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {
                notifyObservers { onTrackerBlockingEnabledChange(true) }
            }
            override fun disableTrackingProtection() {
                notifyObservers { onTrackerBlockingEnabledChange(false) }
            }

            override fun toggleDesktopMode(enable: Boolean, reload: Boolean) {}
            override fun saveState(): EngineSessionState = mock()
            override fun loadUrl(url: String) {}
            override fun loadData(data: String, mimeType: String, encoding: String) {}
            override fun findAll(text: String) {}
            override fun findNext(forward: Boolean) {}
            override fun clearFindMatches() {}
            override fun exitFullScreenMode() {}
            override fun recoverFromCrash(): Boolean { return false }
        }
        val observer = EngineObserver(session)
        engineSession.register(observer)

        engineSession.enableTrackingProtection()
        assertTrue(session.trackerBlockingEnabled)

        engineSession.disableTrackingProtection()
        assertFalse(session.trackerBlockingEnabled)

        observer.onTrackerBlocked("tracker1")
        assertEquals(listOf("tracker1"), session.trackersBlocked)

        observer.onTrackerBlocked("tracker2")
        assertEquals(listOf("tracker1", "tracker2"), session.trackersBlocked)
    }

    @Test
    fun engineObserverClearsWebsiteTitleIfNewPageStartsLoading() {
        val session = Session("https://www.mozilla.org")
        session.title = "Hello World"

        val observer = EngineObserver(session)
        observer.onTitleChange("Mozilla")

        assertEquals("Mozilla", session.title)

        observer.onLocationChange("https://getpocket.com")

        assertEquals("", session.title)
    }

    @Test
    fun `EngineObserver does not clear title if the URL did not change`() {
        val session = Session("https://www.mozilla.org")
        session.title = "Hello World"

        val observer = EngineObserver(session)
        observer.onTitleChange("Mozilla")

        assertEquals("Mozilla", session.title)

        observer.onLocationChange("https://www.mozilla.org")

        assertEquals("Mozilla", session.title)
    }

    @Test
    fun engineObserverClearsBlockedTrackersIfNewPageStartsLoading() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        observer.onTrackerBlocked("tracker1")
        observer.onTrackerBlocked("tracker2")
        assertEquals(listOf("tracker1", "tracker2"), session.trackersBlocked)

        observer.onLoadingStateChange(true)
        assertEquals(emptyList<String>(), session.trackersBlocked)
    }

    @Test
    fun engineObserverPassingHitResult() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)
        val hitResult = HitResult.UNKNOWN("data://foobar")

        observer.onLongPress(hitResult)

        session.hitResult.consume {
            assertEquals("data://foobar", it.src)
            assertTrue(it is HitResult.UNKNOWN)
            true
        }
    }

    @Test
    fun engineObserverClearsFindResults() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        val result1 = Session.FindResult(0, 1, false)
        val result2 = Session.FindResult(1, 2, true)
        observer.onFindResult(0, 1, false)
        observer.onFindResult(1, 2, true)
        assertEquals(listOf(result1, result2), session.findResults)

        observer.onFind("mozilla")
        assertEquals(emptyList<Session.FindResult>(), session.findResults)
    }

    @Test
    fun engineObserverClearsFindResultIfNewPageStartsLoading() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        val result1 = Session.FindResult(0, 1, false)
        val result2 = Session.FindResult(1, 2, true)
        observer.onFindResult(0, 1, false)
        observer.onFindResult(1, 2, true)
        assertEquals(listOf(result1, result2), session.findResults)

        observer.onLoadingStateChange(true)
        assertEquals(emptyList<String>(), session.findResults)
    }

    @Test
    fun engineObserverNotifiesFullscreenMode() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        observer.onFullScreenChange(true)
        assertEquals(true, session.fullScreenMode)
        observer.onFullScreenChange(false)
        assertEquals(false, session.fullScreenMode)
    }

    @Test
    fun `Engine observer notified when thumbnail is assigned`() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)
        val emptyBitmap = spy(Bitmap::class.java)
        observer.onThumbnailChange(emptyBitmap)
        assertEquals(emptyBitmap, session.thumbnail)
    }

    @Test
    fun engineObserverNotifiesWebAppManifest() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        observer.onWebAppManifestLoaded(loadManifest("minimal.json"))
        val manifest = session.webAppManifest!!
        assertEquals("Minimal", manifest.name)
        assertNull(manifest.shortName)
        assertEquals("/", manifest.startUrl)
        assertEquals(WebAppManifest.DisplayMode.BROWSER, manifest.display)
        assertNull(manifest.backgroundColor)
        assertNull(manifest.description)
        assertEquals(WebAppManifest.TextDirection.AUTO, manifest.dir)
        assertNull(manifest.lang)
        assertEquals(WebAppManifest.Orientation.ANY, manifest.orientation)
        assertNull(manifest.scope)
        assertNull(manifest.themeColor)
        assertEquals(0, manifest.icons.size)

        observer.onWebAppManifestLoaded(loadManifest("invalid_json.json"))
        assertEquals(null, session.webAppManifest)
    }

    @Test
    fun engineSessionObserverWithContentPermissionRequests() {
        val permissionRequest = mock(PermissionRequest::class.java)
        val session = Session("")
        val observer = EngineObserver(session)

        assertTrue(session.contentPermissionRequest.isConsumed())
        observer.onContentPermissionRequest(permissionRequest)
        assertFalse(session.contentPermissionRequest.isConsumed())

        observer.onCancelContentPermissionRequest(permissionRequest)
        assertTrue(session.contentPermissionRequest.isConsumed())
    }

    @Test
    fun engineSessionObserverWithAppPermissionRequests() {
        val permissionRequest = mock(PermissionRequest::class.java)
        val session = Session("")
        val observer = EngineObserver(session)

        assertTrue(session.appPermissionRequest.isConsumed())
        observer.onAppPermissionRequest(permissionRequest)
        assertFalse(session.appPermissionRequest.isConsumed())
    }

    @Test
    fun engineObserverConsumesContentPermissionRequestIfNewPageStartsLoading() {
        val permissionRequest = mock(PermissionRequest::class.java)
        val session = Session("https://www.mozilla.org")
        session.contentPermissionRequest = Consumable.from(permissionRequest)

        val observer = EngineObserver(session)
        observer.onLocationChange("https://getpocket.com")

        verify(permissionRequest).reject()
        assertTrue(session.contentPermissionRequest.isConsumed())
    }

    @Test
    fun engineSessionObserverWithOnPromptRequest() {

        val promptRequest = mock(PromptRequest::class.java)
        val session = Session("")
        val observer = EngineObserver(session)

        assertTrue(session.promptRequest.isConsumed())
        observer.onPromptRequest(promptRequest)
        assertFalse(session.promptRequest.isConsumed())
    }

    @Test
    fun engineSessionObserverWithWindowRequests() {
        val windowRequest = mock(WindowRequest::class.java)
        val session = Session("")
        val observer = EngineObserver(session)

        assertTrue(session.openWindowRequest.isConsumed())
        observer.onOpenWindowRequest(windowRequest)
        assertFalse(session.openWindowRequest.isConsumed())

        assertTrue(session.closeWindowRequest.isConsumed())
        observer.onCloseWindowRequest(windowRequest)
        assertFalse(session.closeWindowRequest.isConsumed())
    }

    @Test
    fun `onMediaAdded will add media to session`() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        val media1: Media = mock()
        observer.onMediaAdded(media1)

        assertEquals(listOf(media1), session.media)

        val media2: Media = mock()
        observer.onMediaAdded(media2)

        assertEquals(listOf(media1, media2), session.media)

        val media3: Media = mock()
        observer.onMediaAdded(media3)

        assertEquals(listOf(media1, media2, media3), session.media)
    }

    @Test
    fun `onMediaRemoved will remove media from session`() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        val media1: Media = mock()
        val media2: Media = mock()
        val media3: Media = mock()

        session.media = listOf(media1)
        session.media = listOf(media1, media2)
        session.media = listOf(media1, media2, media3)

        observer.onMediaRemoved(media2)

        assertEquals(listOf(media1, media3), session.media)

        observer.onMediaRemoved(media1)

        assertEquals(listOf(media3), session.media)

        observer.onMediaRemoved(media3)

        assertEquals(emptyList<Media>(), session.media)
    }

    @Test
    fun `onCrashStateChanged will update session and notify observer`() {
        val session = Session("https://www.mozilla.org")
        assertFalse(session.crashed)

        val observer = EngineObserver(session)

        observer.onCrashStateChange(true)
        assertTrue(session.crashed)

        observer.onCrashStateChange(false)
        assertFalse(session.crashed)

        observer.onCrashStateChange(true)
        assertTrue(session.crashed)
    }

    @Test
    fun `onLocationChange clears icon`() {
        val session = Session("https://www.mozilla.org")
        session.icon = mock()

        val observer = EngineObserver(session)

        assertNotNull(session.icon)

        observer.onLocationChange("https://www.firefox.com")

        assertNull(session.icon)
    }

    @Test
    fun `onLocationChange does not clear search terms`() {
        val session = Session("https://www.mozilla.org")
        session.searchTerms = "Mozilla Foundation"

        val observer = EngineObserver(session)
        observer.onLocationChange("https://www.mozilla.org/en-US/")

        assertEquals("Mozilla Foundation", session.searchTerms)
    }

    @Test
    fun `onLoadRequest clears search terms for requests triggered by user interaction`() {
        val session = Session("https://www.mozilla.org")
        session.searchTerms = "Mozilla Foundation"

        val observer = EngineObserver(session)
        observer.onLoadRequest(triggeredByUserInteraction = true)

        assertEquals("", session.searchTerms)
    }

    @Test
    fun `onLoadRequest does not clear search terms for requests not triggered by user interaction`() {
        val session = Session("https://www.mozilla.org")
        session.searchTerms = "Mozilla Foundation"

        val observer = EngineObserver(session)
        observer.onLoadRequest(triggeredByUserInteraction = false)

        assertEquals("Mozilla Foundation", session.searchTerms)
    }

    private fun loadManifest(fileName: String): JSONObject =
        JSONObject(javaClass.getResourceAsStream("/manifests/$fileName")!!
            .bufferedReader().use {
                it.readText()
            }.also {
                assertNotNull(it)
            }
        )
}