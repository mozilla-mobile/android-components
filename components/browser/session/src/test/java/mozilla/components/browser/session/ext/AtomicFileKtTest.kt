/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.ext

import android.content.Context
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.storage.SnapshotSerializer
import mozilla.components.browser.session.storage.getFileForEngine
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class AtomicFileKtTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `writeSnapshot - Fails write on IOException`() {
        val file: AtomicFile = mock()
        Mockito.doThrow(IOException::class.java).`when`(file).startWrite()

        val snapshot = SessionManager.Snapshot(
            sessions = listOf(
                SessionManager.Snapshot.Item(Session("http://mozilla.org"))
            ),
            selectedSessionIndex = 0
        )

        file.writeSnapshot(snapshot, SnapshotSerializer())

        Mockito.verify(file).failWrite(any())
    }

    @Test
    fun `readSnapshot - Returns null on FileNotFoundException`() {
        val file: AtomicFile = mock()
        Mockito.doThrow(FileNotFoundException::class.java).`when`(file).openRead()

        val snapshot = file.readSnapshot(engine = mock(), serializer = SnapshotSerializer())
        assertNull(snapshot)
    }

    @Test
    fun `readSnapshot - Returns null on corrupt JSON`() {
        val file = getFileForEngine(context, engine = mock())

        val stream = file.startWrite()
        stream.bufferedWriter().write("{ name: 'Foo")
        file.finishWrite(stream)

        val snapshot = file.readSnapshot(engine = mock(), serializer = SnapshotSerializer())
        assertNull(snapshot)
    }

    @Test
    fun `Read snapshot should contain sessions of written snapshot`() {
        val session1 = Session("http://mozilla.org", id = "session1")
        val session2 = Session("http://getpocket.com", id = "session2")
        val session3 = Session("http://getpocket.com", id = "session3")
        session3.parentId = "session1"

        val engineSessionState = object : EngineSessionState {
            override fun toJSON() = JSONObject()
        }

        val engineSession = Mockito.mock(EngineSession::class.java)
        Mockito.`when`(engineSession.saveState()).thenReturn(engineSessionState)

        val engine = Mockito.mock(Engine::class.java)
        Mockito.`when`(engine.name()).thenReturn("gecko")
        Mockito.`when`(engine.createSession()).thenReturn(Mockito.mock(EngineSession::class.java))
        Mockito.`when`(engine.createSessionState(any())).thenReturn(engineSessionState)

        // Engine session just for one of the sessions for simplicity.
        val sessionsSnapshot = SessionManager.Snapshot(
            sessions = listOf(
                SessionManager.Snapshot.Item(session1),
                SessionManager.Snapshot.Item(session2),
                SessionManager.Snapshot.Item(session3)
            ),
            selectedSessionIndex = 0
        )

        val file = AtomicFile(File.createTempFile(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()))

        file.writeSnapshot(sessionsSnapshot)

        // Read it back
        val restoredSnapshot = file.readSnapshot(engine)
        Assert.assertNotNull(restoredSnapshot)
        Assert.assertEquals(3, restoredSnapshot!!.sessions.size)
        Assert.assertEquals(0, restoredSnapshot.selectedSessionIndex)

        Assert.assertEquals(session1, restoredSnapshot.sessions[0].session)
        Assert.assertEquals(session1.url, restoredSnapshot.sessions[0].session.url)
        Assert.assertEquals(session1.id, restoredSnapshot.sessions[0].session.id)
        assertNull(restoredSnapshot.sessions[0].session.parentId)

        Assert.assertEquals(session2, restoredSnapshot.sessions[1].session)
        Assert.assertEquals(session2.url, restoredSnapshot.sessions[1].session.url)
        Assert.assertEquals(session2.id, restoredSnapshot.sessions[1].session.id)
        assertNull(restoredSnapshot.sessions[1].session.parentId)

        Assert.assertEquals(session3, restoredSnapshot.sessions[2].session)
        Assert.assertEquals(session3.url, restoredSnapshot.sessions[2].session.url)
        Assert.assertEquals(session3.id, restoredSnapshot.sessions[2].session.id)
        Assert.assertEquals("session1", restoredSnapshot.sessions[2].session.parentId)

        val restoredEngineSession = restoredSnapshot.sessions[0].engineSessionState
        Assert.assertNotNull(restoredEngineSession)
    }
}