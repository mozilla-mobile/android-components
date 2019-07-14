/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.mock
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mozilla.geckoview.GeckoSession

@RunWith(AndroidJUnit4::class)
class GeckoEngineSessionStateTest {

    @Test
    fun toJSON() {
        val geckoState: GeckoSession.SessionState = mock()
        doReturn("<state>").`when`(geckoState).toString()

        val state = GeckoEngineSessionState(geckoState)

        val json = state.toJSON()

        assertEquals(1, json.length())
        assertTrue(json.has("GECKO_STATE"))
        assertEquals("<state>", json.getString("GECKO_STATE"))
    }

    @Test
    fun fromJSON() {
        val json = JSONObject().apply {
            put("GECKO_STATE", "{ 'foo': 'bar' }")
        }

        val state = GeckoEngineSessionState.fromJSON(json)

        assertEquals("""{"foo":"bar"}""", state.actualState.toString())
    }

    @Test
    fun `fromJSON with invalid JSON returns empty State`() {
        val json = JSONObject().apply {
            put("nothing", "helpful")
        }

        val state = GeckoEngineSessionState.fromJSON(json)

        assertNull(state.actualState)
    }
}
