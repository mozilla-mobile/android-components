/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system

import android.os.Bundle
import android.util.JsonWriter
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class SystemEngineSessionStateTest {

    @Test
    @Suppress("DEPRECATION")
    fun toJSON() {
        val state = SystemEngineSessionState(Bundle().apply {
            putString("k0", "v0")
            putInt("k1", 1)
            putBoolean("k2", true)
            putStringArrayList("k3", ArrayList<String>(listOf("Hello", "World")))
            putDouble("k4", 5.0)
            putFloat("k5", 1.0f)
        })

        val json = state.toJSON()

        assertEquals(5, json.length())

        assertTrue(json.has("k0"))
        assertTrue(json.has("k1"))
        assertTrue(json.has("k2"))
        assertTrue(json.has("k4"))
        assertTrue(json.has("k5"))

        assertEquals("v0", json.get("k0"))
        assertEquals(1, json.get("k1"))
        assertEquals(true, json.get("k2"))
        assertEquals(5.0, json.get("k4"))
        assertEquals(1.0f, json.get("k5"))
    }

    @Test
    fun fromJSON() {
        val json = JSONObject().apply {
            put("k0", "v0")
            put("k1", 1)
            put("k2", true)
            put("k3", 5.0)
            put("k4", 1.0f)
            put("k5", JSONArray(listOf(1, 2, 3)))
        }

        val state = SystemEngineSessionState.fromJSON(json)
        val bundle = state.bundle!!

        assertEquals(5, bundle.size())

        assertTrue(bundle.containsKey("k0"))
        assertTrue(bundle.containsKey("k1"))
        assertTrue(bundle.containsKey("k2"))
        assertTrue(bundle.containsKey("k3"))
        assertTrue(bundle.containsKey("k4"))

        assertEquals("v0", bundle.get("k0"))
        assertEquals(1, bundle.get("k1"))
        assertEquals(true, bundle.get("k2"))
        assertEquals(5.0, bundle.get("k3"))
        assertEquals(1.0f, bundle.get("k4"))
    }

    @Test
    @Suppress("DEPRECATION")
    fun toJSONAndfromJSON() {
        val raw = SystemEngineSessionState(Bundle().apply {
            putString("k0", "v0")
            putInt("k1", 1)
            putBoolean("k2", true)
            putStringArrayList("k3", ArrayList<String>(listOf("Hello", "World")))
            putDouble("k4", 5.0)
            putFloat("k5", 1.0f)
            putFloat("k6", 42.25f)
            putDouble("k7", 23.23)
        }).toJSON().toString()

        val bundle = SystemEngineSessionState.fromJSON(JSONObject(raw)).bundle!!

        assertEquals(7, bundle.size())

        assertTrue(bundle.containsKey("k0"))
        assertTrue(bundle.containsKey("k1"))
        assertTrue(bundle.containsKey("k2"))
        assertFalse(bundle.containsKey("k3"))
        assertTrue(bundle.containsKey("k4"))
        assertTrue(bundle.containsKey("k5"))
        assertTrue(bundle.containsKey("k6"))
        assertTrue(bundle.containsKey("k7"))

        assertEquals("v0", bundle.get("k0"))
        assertEquals(1, bundle.get("k1"))
        assertEquals(true, bundle.get("k2"))
        assertEquals(5, bundle.get("k4")) // JSONObject converts to Int
        assertEquals(1, bundle.get("k5")) // JSONObject converts to Int
        assertEquals(42.25, bundle.get("k6")) // Implicit conversion to Double
        assertEquals(23.23, bundle.get("k7"))
    }

    @Test
    fun writeToAndFromJSON() {
        val state = SystemEngineSessionState(Bundle().apply {
            putString("k0", "v0")
            putInt("k1", 1)
            putBoolean("k2", true)
            putStringArrayList("k3", ArrayList<String>(listOf("Hello", "World")))
            putDouble("k4", 5.0)
            putFloat("k5", 1.0f)
            putFloat("k6", 42.25f)
            putDouble("k7", 23.23)
        })

        val outputStream = ByteArrayOutputStream()
        state.writeTo(JsonWriter(outputStream.writer()))

        val bundle = SystemEngineSessionState.fromJSON(
            JSONObject(outputStream.toString())
        ).bundle

        assertNotNull(bundle!!)

        assertEquals(7, bundle.size())

        assertTrue(bundle.containsKey("k0"))
        assertTrue(bundle.containsKey("k1"))
        assertTrue(bundle.containsKey("k2"))
        assertFalse(bundle.containsKey("k3"))
        assertTrue(bundle.containsKey("k4"))
        assertTrue(bundle.containsKey("k5"))
        assertTrue(bundle.containsKey("k6"))
        assertTrue(bundle.containsKey("k7"))

        assertEquals("v0", bundle.get("k0"))
        assertEquals(1, bundle.get("k1"))
        assertEquals(true, bundle.get("k2"))
        assertEquals(5.0, bundle.get("k4"))
        assertEquals(1.0, bundle.get("k5")) // Implicit conversion to Double
        assertEquals(42.25, bundle.get("k6")) // Implicit conversion to Double
        assertEquals(23.23, bundle.get("k7"))
    }
}
