/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class SessionFeatureTest {
    private val sessionManager = mock(SessionManager::class.java)
    private val engineView = mock(EngineView::class.java)
    private val sessionUseCases = SessionUseCases(sessionManager)

    @Test
    fun start() {
        val feature = SessionFeature(sessionManager, sessionUseCases, engineView)
        feature.start()
        verify(sessionManager).register(feature.presenter)
    }

    @Test
    fun `handleBackPressed uses selectedSession`() {
        val session = Session("https://www.mozilla.org")
        `when`(sessionManager.selectedSession).thenReturn(session)
        `when`(sessionManager.selectedSessionOrThrow).thenReturn(session)

        val engineSession = mock(EngineSession::class.java)
        `when`(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)

        val feature = SessionFeature(sessionManager, sessionUseCases, engineView)

        feature.onBackPressed()
        verify(engineSession, never()).goBack()

        session.canGoBack = true
        feature.onBackPressed()
        verify(engineSession).goBack()
    }

    @Test
    fun `handleBackPressed uses sessionId`() {
        val session = Session("https://www.mozilla.org")
        val sessionAlt = Session("https://firefox.com")
        val engineSession = mock(EngineSession::class.java)
        val sessionId = "123"

        `when`(sessionManager.selectedSession).thenReturn(sessionAlt)
        `when`(sessionManager.selectedSessionOrThrow).thenReturn(sessionAlt)
        `when`(sessionManager.findSessionById(sessionId)).thenReturn(session)
        `when`(sessionManager.getOrCreateEngineSession(session)).thenReturn(engineSession)
        session.canGoBack = true

        val feature = SessionFeature(sessionManager, sessionUseCases, engineView, sessionId)

        val result = feature.onBackPressed()

        verify(engineSession).goBack()
        assertTrue(result)
    }

    @Test
    fun `handleBackPressed does nothing when sessionId provided but no session found`() {
        val engineSession = mock(EngineSession::class.java)
        val sessionId = "123"

        val feature = SessionFeature(sessionManager, sessionUseCases, engineView, sessionId)

        val result = feature.onBackPressed()

        verify(engineSession, never()).goBack()
        assertFalse(result)
    }

    @Test
    fun `handleBackPressed does nothing when no session found`() {
        val engineSession = mock(EngineSession::class.java)

        val feature = SessionFeature(sessionManager, sessionUseCases, engineView)
        val result = feature.onBackPressed()

        verify(engineSession, never()).goBack()
        assertFalse(result)
    }

    @Test
    fun stop() {
        val feature = SessionFeature(sessionManager, sessionUseCases, engineView)
        feature.stop()
        verify(sessionManager).unregister(feature.presenter)
    }
}