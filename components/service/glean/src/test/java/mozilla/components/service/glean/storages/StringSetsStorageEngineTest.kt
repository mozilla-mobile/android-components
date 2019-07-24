/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.storages

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import mozilla.components.service.glean.error.ErrorRecording.ErrorType
import mozilla.components.service.glean.error.ErrorRecording.testGetNumRecordedErrors
import mozilla.components.service.glean.private.Lifetime
import mozilla.components.service.glean.private.StringSetMetricType
import mozilla.components.service.glean.resetGlean
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StringSetsStorageEngineTest {

    @Before
    fun setUp() {
        resetGlean()
    }

    @Test
    fun `set() properly sets the value in all stores`() {
        val storeNames = listOf("store1", "store2")

        val metric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_list_metric",
            sendInPings = storeNames
        )

        val set = setOf("First", "Second")

        StringSetsStorageEngine.set(
            metricData = metric,
            value = set
        )

        // Check that the data was correctly set in each store.
        for (storeName in storeNames) {
            val snapshot = StringSetsStorageEngine.getSnapshot(
                storeName = storeName,
                clearStore = false
            )
            assertEquals(1, snapshot!!.size)
            assertTrue(snapshot["telemetry.string_list_metric"]!!.contains("First"))
            assertTrue(snapshot["telemetry.string_list_metric"]!!.contains("Second"))
        }
    }

    @Test
    fun `add() properly adds the value in all stores`() {
        val storeNames = listOf("store1", "store2")

        val metric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_list_metric",
            sendInPings = storeNames
        )

        StringSetsStorageEngine.add(
            metricData = metric,
            value = "First"
        )

        // Check that the data was correctly added in each store.
        for (storeName in storeNames) {
            val snapshot = StringSetsStorageEngine.getSnapshot(
                storeName = storeName,
                clearStore = false)
            assertTrue(snapshot!!["telemetry.string_list_metric"]!!.contains("First"))
        }
    }

    @Test
    fun `add() won't allow adding beyond the max set length in a single accumulation`() {
        val metric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_list_metric",
            sendInPings = listOf("store1")
        )

        for (i in 1..StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE + 1) {
            StringSetsStorageEngine.add(
                metricData = metric,
                value = "value$i"
            )
        }

        // Check that list was truncated.
        val snapshot = StringSetsStorageEngine.getSnapshot(
            storeName = "store1",
            clearStore = false)
        assertEquals(1, snapshot!!.size)
        assertEquals(true, snapshot.containsKey("telemetry.string_list_metric"))
        assertEquals(
            StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE,
            snapshot["telemetry.string_list_metric"]?.count()
        )

        assertEquals(1, testGetNumRecordedErrors(metric, ErrorType.InvalidValue))
    }

    @Test
    fun `add() won't allow adding beyond the max set length over multiple accumulations`() {
        val metric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_list_metric",
            sendInPings = listOf("store1")
        )

        // Add values up to half capacity
        for (i in 1..StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE / 2) {
            StringSetsStorageEngine.add(
                metricData = metric,
                value = "value$i"
            )
        }

        // Check that list was added
        val snapshot = StringSetsStorageEngine.getSnapshot(
            storeName = "store1",
            clearStore = false)
        assertEquals(1, snapshot!!.size)
        assertEquals(true, snapshot.containsKey("telemetry.string_list_metric"))
        assertEquals(
            StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE / 2,
            snapshot["telemetry.string_list_metric"]?.count()
        )

        // Add values that would exceed capacity
        for (i in 1..StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE) {
            StringSetsStorageEngine.add(
                metricData = metric,
                value = "otherValue$i"
            )
        }

        // Check that the list was truncated to the list capacity
        val snapshot2 = StringSetsStorageEngine.getSnapshot(
            storeName = "store1",
            clearStore = false)
        assertEquals(1, snapshot2!!.size)
        assertEquals(true, snapshot2.containsKey("telemetry.string_list_metric"))
        assertEquals(
            StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE,
            snapshot2["telemetry.string_list_metric"]?.count()
        )

        assertEquals(StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE / 2,
            testGetNumRecordedErrors(metric, ErrorType.InvalidValue))
    }

    @Test
    fun `set() won't allow adding a set longer than the max list length`() {
        val metric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_list_metric",
            sendInPings = listOf("store1")
        )

        val stringSet: MutableSet<String> = mutableSetOf()
        for (i in 1..StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE + 1) {
            stringSet.add("value$i")
        }

        StringSetsStorageEngine.set(metricData = metric, value = stringSet)

        // Check that list was truncated.
        val snapshot = StringSetsStorageEngine.getSnapshot(
            storeName = "store1",
            clearStore = false)
        assertEquals(1, snapshot!!.size)
        assertEquals(true, snapshot.containsKey("telemetry.string_list_metric"))
        assertEquals(
            StringSetsStorageEngineImplementation.MAX_SET_SIZE_VALUE,
            snapshot["telemetry.string_list_metric"]?.count()
        )

        assertEquals(1, testGetNumRecordedErrors(metric, ErrorType.InvalidValue))
    }

    @Test
    fun `string set deserializer should correctly parse string sets`() {
        val persistedSample = mapOf(
            "store1#telemetry.invalid_string" to "invalid_string",
            "store1#telemetry.invalid_bool" to false,
            "store1#telemetry.null" to null,
            "store1#telemetry.invalid_int" to -1,
            "store1#telemetry.invalid_set" to listOf("1", "2", "3"),
            "store1#telemetry.invalid_int_set" to "[1,2,3]",
            "store1#telemetry.valid" to "[\"a\",\"b\",\"c\"]"
        )

        val storageEngine = StringSetsStorageEngineImplementation()

        // Create a fake application context that will be used to load our data.
        val context = Mockito.mock(Context::class.java)
        val sharedPreferences = Mockito.mock(SharedPreferences::class.java)
        Mockito.`when`(sharedPreferences.all).thenAnswer { persistedSample }
        Mockito.`when`(context.getSharedPreferences(
            ArgumentMatchers.eq(storageEngine::class.java.canonicalName),
            ArgumentMatchers.eq(Context.MODE_PRIVATE)
        )).thenReturn(sharedPreferences)
        Mockito.`when`(context.getSharedPreferences(
            ArgumentMatchers.eq("${storageEngine::class.java.canonicalName}.PingLifetime"),
            ArgumentMatchers.eq(Context.MODE_PRIVATE)
        )).thenReturn(ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("${storageEngine::class.java.canonicalName}.PingLifetime",
                Context.MODE_PRIVATE))

        storageEngine.applicationContext = context
        val snapshot = storageEngine.getSnapshot(storeName = "store1", clearStore = true)
        // Because JSONArray constructor will deserialize with or without the escaped quotes, it
        // treat the invalid_int_set above the same as the valid list, so we assertEquals 2
        assertEquals(2, snapshot!!.size)
        assertEquals(setOf("a", "b", "c"), snapshot["telemetry.valid"])
    }

    @Test
    fun `string set serializer should correctly serialize sets`() {
        run {
            val storageEngine = StringSetsStorageEngineImplementation()
            storageEngine.applicationContext = ApplicationProvider.getApplicationContext()

            val storeNames = listOf("store1", "store2")

            val metric = StringSetMetricType(
                    disabled = false,
                    category = "telemetry",
                    lifetime = Lifetime.User,
                    name = "string_list_metric",
                    sendInPings = storeNames
            )

            val set = setOf("First", "Second")

            storageEngine.set(
                    metricData = metric,
                    value = set
            )

            // Get snapshot from store1
            val json = storageEngine.getSnapshotAsJSON("store1", true)
            // Check for correct JSON serialization
            assertEquals(
                    "{\"telemetry.string_list_metric\":[\"First\",\"Second\"]}",
                    json.toString()
            )
        }

        // Create a new instance of storage engine to verify serialization to storage rather than
        // to the cache
        run {
            val storageEngine = StringSetsStorageEngineImplementation()
            storageEngine.applicationContext = ApplicationProvider.getApplicationContext()

            // Get snapshot from store1
            val json = storageEngine.getSnapshotAsJSON("store1", true)
            // Check for correct JSON serialization
            assertEquals(
                    "{\"telemetry.string_list_metric\":[\"First\",\"Second\"]}",
                    json.toString()
            )
        }
    }

    @Test
    fun `test JSON output`() {
        val storeNames = listOf("store1", "store2")

        val metric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_list_metric",
            sendInPings = storeNames
        )

        val set = setOf("First", "Second")

        StringSetsStorageEngine.set(
            metricData = metric,
            value = set
        )

        // Get snapshot from store1 and clear it
        val json = StringSetsStorageEngine.getSnapshotAsJSON("store1", true)
        // Check that getting a new snapshot for "store1" returns an empty store.
        Assert.assertNull("The engine must report 'null' on empty stores",
                StringSetsStorageEngine.getSnapshotAsJSON(storeName = "store1", clearStore = false))
        // Check for correct JSON serialization
        assertEquals(
            "{\"telemetry.string_list_metric\":[\"First\",\"Second\"]}",
            json.toString()
        )
    }

    @Test
    fun `The API truncates long string values`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1"
        val stringSetMetric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_set_metric",
            sendInPings = listOf("store1")
        )

        val longString = "a".repeat(StringSetsStorageEngineImplementation.MAX_STRING_LENGTH + 10)

        // Check that data was truncated via add() method.
        StringSetsStorageEngine.add(stringSetMetric, longString)
        var snapshot = StringSetsStorageEngine.getSnapshotAsJSON("store1", true) as JSONObject
        var stringSet = snapshot["telemetry.string_set_metric"] as JSONArray
        assertEquals(longString.take(StringSetsStorageEngineImplementation.MAX_STRING_LENGTH),
            stringSet[0])

        // Check that data was truncated via set() method.
        StringSetsStorageEngine.set(stringSetMetric, setOf(longString))
        snapshot = StringSetsStorageEngine.getSnapshotAsJSON("store1", true) as JSONObject
        stringSet = snapshot["telemetry.string_set_metric"] as JSONArray
        assertEquals(1, stringSet.length())
        assertTrue(stringSetMetric.testHasValue())
        assertEquals(longString.take(StringSetsStorageEngineImplementation.MAX_STRING_LENGTH),
            stringSet[0])

        assertEquals(2, testGetNumRecordedErrors(stringSetMetric, ErrorType.InvalidValue))
    }
}
