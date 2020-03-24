/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.intent.ext

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.mock
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class IntentExtensionsTest {

    @Test
    fun `getSessionId should call getStringExtra`() {
        val id = "mock-session-id"
        val intent: Intent = mock()
        val safeIntent: SafeIntent = mock()

        `when`(intent.getStringExtra(EXTRA_SESSION_ID)).thenReturn(id)
        `when`(safeIntent.getStringExtra(EXTRA_SESSION_ID)).thenReturn(id)

        assertEquals(id, intent.getSessionId())
        assertEquals(id, safeIntent.getSessionId())
    }

    @Test
    fun `putSessionId should put string extra`() {
        val id = "mock-session-id"
        val intent = Intent()

        assertEquals(intent, intent.putSessionId(id))

        assertEquals(id, intent.getSessionId())
        assertEquals(id, intent.toSafeIntent().getSessionId())
    }
}
