/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.storage

import mozilla.components.browser.session.Session
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SnapshotSerializerTest {
    @Test
    fun `Serialize and deserialize session`() {
        val originalSession = Session(
            "https://www.mozilla.org",
            source = Session.Source.ACTION_VIEW,
            id = "test-id").apply {
            title = "Hello World"
            readerMode = true
        }

        val json = serializeSession(originalSession)
        val restoredSession = deserializeSession(json)

        assertEquals("https://www.mozilla.org", restoredSession.url)
        assertEquals(Session.Source.ACTION_VIEW, restoredSession.source)
        assertEquals("test-id", restoredSession.id)
        assertEquals("Hello World", restoredSession.title)
        assertTrue(restoredSession.readerMode)
    }

    @Test
    fun `Deserialize minimal session (without title)`() {
        val json = JSONObject().apply {
            put("url", "https://www.mozilla.org")
            put("source", "ACTION_VIEW")
            put("uuid", "test-id")
            put("parentUuid", "")
        }

        val restoredSession = deserializeSession(json)

        assertEquals("https://www.mozilla.org", restoredSession.url)
        assertEquals(Session.Source.ACTION_VIEW, restoredSession.source)
        assertEquals("test-id", restoredSession.id)
        assertFalse(restoredSession.readerMode)
    }
}
