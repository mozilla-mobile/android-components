/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.storages

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StorageEngineManagerTest {
    @Test
    fun `collect() must return an empty object for empty or unknown stores`() {
        val manager = StorageEngineManager(applicationContext = RuntimeEnvironment.application)
        val data = manager.collect("thisStoreNameDoesNotExist")
        assertNotNull(data)
        assertEquals("{}", data.toString())
    }

    @Test
    fun `collect() must report data from all the stores`() {
        val manager = StorageEngineManager(storageEngines = mapOf(
            "engine1" to MockStorageEngine(JSONObject(mapOf("test" to "val"))),
            "engine2" to MockStorageEngine(JSONArray(listOf("a", "b", "c")))
        ), applicationContext = RuntimeEnvironment.application)

        val data = manager.collect("test")
        assertEquals("{\"engine1\":{\"test\":\"val\"},\"engine2\":[\"a\",\"b\",\"c\"]}",
            data.toString())
    }
}
