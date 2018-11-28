/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.window.WindowRequest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import mozilla.components.support.test.any

class WindowFeatureTest {

    private lateinit var engine: Engine
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        engine = mock(Engine::class.java)
        sessionManager = mock(SessionManager::class.java)
    }

    @Test
    fun `start registers window observer`() {
        val feature = WindowFeature(engine, sessionManager)
        feature.start()
        verify(sessionManager).register(feature.windowObserver)
    }

    @Test
    fun `observer handles open window request`() {
        val session = Session("https://www.mozilla.org")
        val request = mock(WindowRequest::class.java)
        `when`(request.url).thenReturn("about:blank")

        val feature = WindowFeature(engine, sessionManager)
        feature.windowObserver.onOpenWindowRequested(session, request)

        verify(request).prepare(any())
        verify(sessionManager).add(any(), eq(true), any(), eq(session))
        verify(request).start()
    }

    @Test
    fun `session is removed when window should be closed`() {
        val session = Session("https://www.mozilla.org")

        val feature = WindowFeature(engine, sessionManager)
        feature.windowObserver.onCloseWindowRequested(session, mock(WindowRequest::class.java))
        verify(sessionManager).remove(session)
    }

    @Test
    fun `stop unregisters window observer`() {
        val feature = WindowFeature(engine, sessionManager)
        feature.stop()
        verify(sessionManager).unregister(feature.windowObserver)
    }
}