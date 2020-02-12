package mozilla.components.browser.session

import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class LegacySessionManagerTest {

    private lateinit var manager: LegacySessionManager

    private val session1 = Session("https://www.mozilla.org")
    private val session2 = Session("https://www.wikipedia.org")
    private val session3 = Session("https://www.duckduckgo.com")
    private val allSessions = listOf(session1, session2, session3)
    private val engineSessionMock1 = mock<EngineSession>()
    private val engineSessionMock2 = mock<EngineSession>()
    private val engineSessionMock3 = mock<EngineSession>()
    private val allEngineSessions = listOf(engineSessionMock1, engineSessionMock2, engineSessionMock3)

    @Test
    fun `the most recently added session should always be at the front of openSessions`() {
        manager = TestLegacySessionManager(6)
        manager.add(allSessions)

        // The first added session is always selected
        assertEquals(1, manager.openSessions.size)

        manager.select(session1)
        assertEquals(session1.id, manager.openSessions.peekFirst())

        manager.select(session2)
        assertEquals(session2.id, manager.openSessions.peekFirst())

        manager.select(session3)
        assertEquals(session3.id, manager.openSessions.peekFirst())

        manager.select(session1)
        assertEquals(session1.id, manager.openSessions.peekFirst())
    }

    @Test
    fun `when sessions are selected multiple times they should only exist once in openSessions`() {
        manager = TestLegacySessionManager(6)
        manager.add(allSessions)

        listOf(session1, session2, session3, session1, session2, session3, session2).forEach {
            manager.select(it)
        }

        assertEquals(3, manager.openSessions.size)
        assertEquals(listOf(session2.id, session3.id, session1.id), manager.openSessions.toList())
    }

    @Test
    fun `onLowMemory should trim openSessions to only the selected session`() {
        manager = TestLegacySessionManager(6)
        manager.add(allSessions)
        allSessions.forEach { manager.select(it) }

        assertEquals(3, manager.openSessions.size)
        assertEquals(session3, manager.selectedSession)

        manager.onLowMemory()

        assertEquals(1, manager.openSessions.size)
        assertEquals(listOf(session3.id), manager.openSessions.toList())
        assertEquals(session3, manager.selectedSession)
    }

    @Test
    fun `when open sessions are trimmed by a call to select, at least maxOpenSessions should remain`() {
        manager = TestLegacySessionManager(2)
        manager.add(allSessions)
        allSessions.forEach { manager.select(it) }
        assertEquals(manager.maxOpenSessions, manager.openSessions.size)

        manager.select(session2)
        assertEquals(manager.maxOpenSessions, manager.openSessions.size)

        manager.select(session1)
        assertEquals(manager.maxOpenSessions, manager.openSessions.size)

        manager.select(session3)
        assertEquals(manager.maxOpenSessions, manager.openSessions.size)

        manager.select(session2)
        assertEquals(manager.maxOpenSessions, manager.openSessions.size)
    }

    @Test
    fun `when open sessions are trimmed by a call to select, the LRU value should be trimmed`() {
        manager = TestLegacySessionManager(2)
        manager.add(allSessions)
        allSessions.forEach { manager.select(it) }
        assertEquals(listOf(session3.id, session2.id), manager.openSessions.toList())

        manager.select(session2)
        assertEquals(listOf(session2.id, session3.id), manager.openSessions.toList())

        manager.select(session1)
        assertEquals(listOf(session1.id, session2.id), manager.openSessions.toList())

        manager.select(session3)
        assertEquals(listOf(session3.id, session1.id), manager.openSessions.toList())

        manager.select(session2)
        assertEquals(listOf(session2.id, session3.id), manager.openSessions.toList())
    }

    @Test
    fun `engineSessions should be disabled as sessions are removed from openSessions`() {
        manager = TestLegacySessionManager(2)

        allSessions.zip(allEngineSessions)
            .forEach { (session, engineSession) ->
                manager.add(session = session, engineSession = engineSession, selected = true)
            }

        assertEquals(listOf(session3.id, session2.id), manager.openSessions.toList())
        verifySessionsClosed(1, engineSessionMock1)
        verifySessionsClosed(0, engineSessionMock2, engineSessionMock3)

        manager.onLowMemory()

        assertEquals(listOf(session3.id), manager.openSessions.toList())
        verifySessionsClosed(1, engineSessionMock1, engineSessionMock2)
        verifySessionsClosed(0, engineSessionMock3)

        manager.select(session1)

        assertEquals(listOf(session1.id, session3.id), manager.openSessions.toList())
        verifySessionsClosed(1, engineSessionMock1, engineSessionMock2)
        verifySessionsClosed(0, engineSessionMock3)

        manager.select(session2)

        assertEquals(listOf(session2.id, session1.id), manager.openSessions.toList())
        verifySessionsClosed(1, engineSessionMock1, engineSessionMock2, engineSessionMock3)
    }
}

/**
 * Allows changing [maxOpenSessions] to make tests less verbose.
 */
private class TestLegacySessionManager(override val maxOpenSessions: Int) :
    LegacySessionManager(mock(), SessionManager.EngineSessionLinker(null))

private fun verifySessionsClosed(times: Int, vararg sessions: EngineSession) {
    sessions.forEach {
        verify(it, times(times)).requestClose(any())
    }
}
