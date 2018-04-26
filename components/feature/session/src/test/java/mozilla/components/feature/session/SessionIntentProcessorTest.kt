/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import android.content.Intent
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionIntentProcessorTest {
    private val sessionManager = mock(SessionManager::class.java)
    private val engine = mock(Engine::class.java)
    private val engineSession = mock(EngineSession::class.java)
    private val sessionMapping = mock(SessionMapping::class.java)
    private val useCases = SessionUseCases(sessionManager, engine, sessionMapping)

    @Before
    fun setup() {
        `when`(sessionMapping.getOrCreate(engine, sessionManager.selectedSession)).thenReturn(engineSession)
    }

    @Test
    fun testProcessWIthDefaultHandlers() {
        val handler = SessionIntentProcessor(useCases)
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(Intent.ACTION_VIEW)
        `when`(intent.dataString).thenReturn("http://mozilla.org")

        handler.process(intent)
        verify(engineSession).loadUrl("http://mozilla.org")
    }

    @Test
    fun testProcessWithoutDefaultHandlers() {
        val handler = SessionIntentProcessor(useCases, useDefaultHandlers = false)
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(Intent.ACTION_VIEW)
        `when`(intent.dataString).thenReturn("http://mozilla.org")

        handler.process(intent)
        verifyZeroInteractions(engineSession)
    }

    @Test
    fun testProcessWithCustomHandlers() {
        val handler = SessionIntentProcessor(useCases, useDefaultHandlers = false)
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(Intent.ACTION_SEND)

        var handlerInvoked = false
        handler.registerHandler(Intent.ACTION_SEND, { intent ->
            handlerInvoked = true
            true
        })

        handler.process(intent)
        assertTrue(handlerInvoked)

        handlerInvoked = false
        handler.unregisterHandler(Intent.ACTION_SEND)

        handler.process(intent)
        assertFalse(handlerInvoked)
    }
}