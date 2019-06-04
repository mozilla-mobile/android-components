/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.accounts

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class WebChannelFeatureTest {

    @Before
    fun setup() {
        WebChannelFeature.installedWebExt = null
    }

    @Test
    fun `start installs webextension`() {
        val engine = mock<Engine>()
        val sessionManager = mock<SessionManager>()

        val webchannelFeature = WebChannelFeature(testContext, engine, sessionManager)
        assertNull(WebChannelFeature.installedWebExt)
        webchannelFeature.start()

        val onSuccess = argumentCaptor<((WebExtension) -> Unit)>()
        val onError = argumentCaptor<((String, Throwable) -> Unit)>()
        verify(engine, times(1)).installWebExtension(
                eq(WebChannelFeature.WEB_CHANNEL_EXTENSION_ID),
                eq(WebChannelFeature.WEB_CHANNEL_EXTENSION_URL),
                eq(true),
                onSuccess.capture(),
                onError.capture()
        )

        onSuccess.value.invoke(mock())
        assertNotNull(WebChannelFeature.installedWebExt)

        webchannelFeature.start()
        verify(engine, times(1)).installWebExtension(
                eq(WebChannelFeature.WEB_CHANNEL_EXTENSION_ID),
                eq(WebChannelFeature.WEB_CHANNEL_EXTENSION_URL),
                eq(true),
                onSuccess.capture(),
                onError.capture()
        )

        onError.value.invoke(WebChannelFeature.WEB_CHANNEL_EXTENSION_ID, RuntimeException())
    }
    @Test
    fun `start registers observer for selected session`() {
        val engine = mock<Engine>()
        val sessionManager = mock<SessionManager>()

        val webchannelFeature = spy(WebChannelFeature(testContext, engine, sessionManager))
        webchannelFeature.start()

        verify(webchannelFeature).observeSelected()
    }

    @Test
    fun `start registers content message handler for selected session`() {
        val engine = mock<Engine>()
        val sessionManager = mock<SessionManager>()
        val session = mock<Session>()
        val engineSession = mock<EngineSession>()
        val ext = mock<WebExtension>()
        val messageHandler = argumentCaptor<MessageHandler>()

        WebChannelFeature.installedWebExt = ext

        whenever(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)
        whenever(sessionManager.selectedSession).thenReturn(session)
        val webchannelFeature = spy(WebChannelFeature(testContext, engine, sessionManager))

        webchannelFeature.start()
        verify(ext).registerContentMessageHandler(eq(engineSession), eq(WebChannelFeature.WEB_CHANNEL_EXTENSION_ID), messageHandler.capture())

        val port = mock<Port>()
        whenever(port.engineSession).thenReturn(engineSession)

        messageHandler.value.onPortConnected(port)
        assertTrue(WebChannelFeature.ports.containsValue(port))

        messageHandler.value.onPortDisconnected(port)
        assertFalse(WebChannelFeature.ports.containsValue(port))
    }

    @Test
    fun `port is removed with session`() {
        val port = mock<Port>()
        val selectedSession = mock<Session>()
        val webchannelFeature = prepareFeatureForTest(port, selectedSession)

        val size = WebChannelFeature.ports.size
        webchannelFeature.onSessionRemoved(selectedSession)
        assertEquals(size - 1, WebChannelFeature.ports.size)
    }

    @Test
    fun `register content message handler for added session`() {
        val engine = mock<Engine>()
        val sessionManager = mock<SessionManager>()
        val session = mock<Session>()
        val engineSession = mock<EngineSession>()
        val ext = mock<WebExtension>()
        val messageHandler = argumentCaptor<MessageHandler>()

        WebChannelFeature.installedWebExt = ext

        whenever(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)
        val webchannelFeature = spy(WebChannelFeature(testContext, engine, sessionManager))

        webchannelFeature.onSessionAdded(session)
        verify(ext).registerContentMessageHandler(eq(engineSession), eq(WebChannelFeature.WEB_CHANNEL_EXTENSION_ID), messageHandler.capture())

        val port = mock<Port>()
        whenever(port.engineSession).thenReturn(engineSession)

        messageHandler.value.onPortConnected(port)
        assertTrue(WebChannelFeature.ports.containsValue(port))
    }

    private fun prepareFeatureForTest(port: Port, session: Session = mock()): WebChannelFeature {
        val engine = mock<Engine>()
        val sessionManager = mock<SessionManager>()
        val ext = mock<WebExtension>()
        val engineSession = mock<EngineSession>()

        whenever(sessionManager.selectedSession).thenReturn(session)
        whenever(sessionManager.getEngineSession(session)).thenReturn(engineSession)
        whenever(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)

        val webchannelFeature = WebChannelFeature(testContext, engine, sessionManager)
        WebChannelFeature.installedWebExt = ext
        WebChannelFeature.ports[engineSession] = port
        return webchannelFeature
    }
}
