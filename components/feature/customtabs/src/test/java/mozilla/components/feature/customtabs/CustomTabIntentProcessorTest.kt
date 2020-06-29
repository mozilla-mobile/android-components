/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.customtabs

import android.content.Intent
import android.os.Bundle
import android.provider.Browser
import androidx.browser.customtabs.CustomTabsIntent
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.Session.Source
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.intent.ext.EXTRA_SESSION_ID
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import mozilla.components.support.utils.toSafeIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class CustomTabIntentProcessorTest {

    private val sessionManager = mock<SessionManager>()
    private val session = mock<Session>()
    private val engineSession = mock<EngineSession>()

    @Before
    fun setup() {
        whenever(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)
    }

    @Test
    fun processCustomTabIntentWithDefaultHandlers() {
        val engine = mock<Engine>()
        val sessionManager = spy(SessionManager(engine))
        doReturn(engineSession).`when`(sessionManager).getOrCreateEngineSession(anySession(), anyBoolean())
        val useCases = SessionUseCases(sessionManager)

        val handler =
            CustomTabIntentProcessor(sessionManager, useCases.loadUrl, testContext.resources)

        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION)).thenReturn(true)
        whenever(intent.dataString).thenReturn("http://mozilla.org")
        whenever(intent.putExtra(any<String>(), any<String>())).thenReturn(intent)

        handler.process(intent)
        verify(sessionManager).add(anySession(), eq(false), eq(null), eq(null), eq(null))
        verify(engineSession).loadUrl("http://mozilla.org", flags = LoadUrlFlags.external())
        verify(intent).putExtra(eq(EXTRA_SESSION_ID), any<String>())

        val customTabSession = sessionManager.all[0]
        assertNotNull(customTabSession)
        assertEquals("http://mozilla.org", customTabSession.url)
        assertEquals(Source.CUSTOM_TAB, customTabSession.source)
        assertNotNull(customTabSession.customTabConfig)
        assertFalse(customTabSession.private)
    }

    @Test
    fun processCustomTabIntentWithAdditionalHeaders() {
        val engine = mock<Engine>()
        val sessionManager = spy(SessionManager(engine))
        doReturn(engineSession).`when`(sessionManager).getOrCreateEngineSession(anySession(), anyBoolean())
        val useCases = SessionUseCases(sessionManager)

        val handler =
                CustomTabIntentProcessor(sessionManager, useCases.loadUrl, testContext.resources)

        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION)).thenReturn(true)
        whenever(intent.dataString).thenReturn("http://mozilla.org")
        whenever(intent.putExtra(any<String>(), any<String>())).thenReturn(intent)

        val headersBundle = Bundle().apply {
            putString("X-Extra-Header", "true")
        }
        whenever(intent.getBundleExtra(Browser.EXTRA_HEADERS)).thenReturn(headersBundle)
        val headers = handler.getAdditionalHeaders(intent.toSafeIntent())

        handler.process(intent)
        verify(sessionManager).add(anySession(), eq(false), eq(null), eq(null), eq(null))
        verify(engineSession).loadUrl("http://mozilla.org", flags = LoadUrlFlags.external(), additionalHeaders = headers)
        verify(intent).putExtra(eq(EXTRA_SESSION_ID), any<String>())

        val customTabSession = sessionManager.all[0]
        assertNotNull(customTabSession)
        assertEquals("http://mozilla.org", customTabSession.url)
        assertEquals(Source.CUSTOM_TAB, customTabSession.source)
        assertNotNull(customTabSession.customTabConfig)
        assertFalse(customTabSession.private)
    }

    @Test
    fun processPrivateCustomTabIntentWithDefaultHandlers() {
        val engine = mock<Engine>()
        val sessionManager = spy(SessionManager(engine))
        doReturn(engineSession).`when`(sessionManager).getOrCreateEngineSession(anySession(), anyBoolean())
        val useCases = SessionUseCases(sessionManager)

        val handler =
                CustomTabIntentProcessor(sessionManager, useCases.loadUrl, testContext.resources, true)

        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION)).thenReturn(true)
        whenever(intent.dataString).thenReturn("http://mozilla.org")
        whenever(intent.putExtra(any<String>(), any<String>())).thenReturn(intent)

        handler.process(intent)
        verify(sessionManager).add(anySession(), eq(false), eq(null), eq(null), eq(null))
        verify(engineSession).loadUrl("http://mozilla.org", flags = LoadUrlFlags.external())
        verify(intent).putExtra(eq(EXTRA_SESSION_ID), any<String>())

        val customTabSession = sessionManager.all[0]
        assertNotNull(customTabSession)
        assertEquals("http://mozilla.org", customTabSession.url)
        assertEquals(Source.CUSTOM_TAB, customTabSession.source)
        assertNotNull(customTabSession.customTabConfig)
        assertTrue(customTabSession.private)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anySession(): T {
        any<T>()
        return null as T
    }
}
