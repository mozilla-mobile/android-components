/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine

import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineSession
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineObserverTest {

    @Test
    fun testEngineSessionObserver() {
        val session = Session("")
        val engineSession = object : EngineSession() {
            override fun goBack() {}
            override fun goForward() {}
            override fun reload() {}
            override fun stopLoading() {}
            override fun restoreState(state: Map<String, Any>) {}
            override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {}
            override fun disableTrackingProtection() {}
            override fun saveState(): Map<String, Any> {
                return emptyMap()
            }

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
        Assert.assertEquals("http://mozilla.org", session.url)
        Assert.assertEquals("", session.searchTerms)
        Assert.assertEquals(100, session.progress)
        Assert.assertEquals(true, session.loading)
        Assert.assertEquals(true, session.canGoForward)
        Assert.assertEquals(true, session.canGoBack)
    }

    @Test
    fun testEngineSessionObserverWithSecurityChanges() {
        val session = Session("")
        val engineSession = object : EngineSession() {
            override fun goBack() {}
            override fun goForward() {}
            override fun stopLoading() {}
            override fun reload() {}
            override fun restoreState(state: Map<String, Any>) {}
            override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {}
            override fun disableTrackingProtection() {}
            override fun saveState(): Map<String, Any> {
                return emptyMap()
            }

            override fun loadData(data: String, mimeType: String, encoding: String) {}
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
    fun testEngineSessionObserverWithTrackingProtection() {
        val session = Session("")
        val engineSession = object : EngineSession() {
            override fun goBack() {}
            override fun goForward() {}
            override fun stopLoading() {}
            override fun reload() {}
            override fun restoreState(state: Map<String, Any>) {}
            override fun enableTrackingProtection(policy: TrackingProtectionPolicy) {
                notifyObservers { onTrackerBlockingEnabledChange(true) }
            }
            override fun disableTrackingProtection() {
                notifyObservers { onTrackerBlockingEnabledChange(false) }
            }
            override fun saveState(): Map<String, Any> {
                return emptyMap()
            }

            override fun loadUrl(url: String) {}
            override fun loadData(data: String, mimeType: String, encoding: String) {}
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
    fun testEngineObserverClearsWebsiteTitleIfNewPageStartsLoading() {
        val session = Session("https://www.mozilla.org")
        session.title = "Hello World"

        val observer = EngineObserver(session)
        observer.onTitleChange("Mozilla")

        assertEquals("Mozilla", session.title)

        observer.onLocationChange("https://getpocket.com")

        assertEquals("", session.title)
    }

    @Test
    fun testEngineObserverClearsBlockedTrackersNewPageStartsLoading() {
        val session = Session("https://www.mozilla.org")
        val observer = EngineObserver(session)

        observer.onTrackerBlocked("tracker1")
        observer.onTrackerBlocked("tracker2")
        assertEquals(listOf("tracker1", "tracker2"), session.trackersBlocked)

        observer.onLocationChange("https://getpocket.com")
        assertEquals(emptyList<String>(), session.trackersBlocked)
    }
}