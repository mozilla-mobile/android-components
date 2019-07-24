/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.private

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.service.glean.error.ErrorRecording
import mozilla.components.service.glean.resetGlean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.NullPointerException

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StringSetMetricTypeTest {

    @Before
    fun setUp() {
        resetGlean()
    }

    @Test
    fun `The API saves to its storage engine by first adding then setting`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1"
        val stringSetMetric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_set_metric",
            sendInPings = listOf("store1")
        )

        // Record two lists using add and set
        stringSetMetric.add("value1")
        stringSetMetric.add("value2")
        stringSetMetric.add("value3")

        // Check that data was properly recorded.
        val snapshot = stringSetMetric.testGetValue()
        assertEquals(3, snapshot.size)
        assertTrue(stringSetMetric.testHasValue())
        assertTrue(snapshot.contains("value1"))
        assertTrue(snapshot.contains("value2"))
        assertTrue(snapshot.contains("value3"))

        // Use set() to see that the first list is replaced by the new list
        stringSetMetric.set(setOf("other1", "other2", "other3"))
        // Check that data was properly recorded.
        val snapshot2 = stringSetMetric.testGetValue()
        assertEquals(3, snapshot2.size)
        assertTrue(stringSetMetric.testHasValue())
        assertTrue(snapshot2.contains("other1"))
        assertTrue(snapshot2.contains("other2"))
        assertTrue(snapshot2.contains("other3"))
    }

    @Test
    fun `The API saves to its storage engine by first setting then adding`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1"
        val stringSetMetric = StringSetMetricType(
                disabled = false,
                category = "telemetry",
                lifetime = Lifetime.Application,
                name = "string_set_metric",
                sendInPings = listOf("store1")
        )

        // Record two lists using set and add
        stringSetMetric.set(setOf("value1", "value2", "value3"))

        // Check that data was properly recorded.
        val snapshot = stringSetMetric.testGetValue()
        assertEquals(3, snapshot.size)
        assertTrue(stringSetMetric.testHasValue())
        assertTrue(snapshot.contains("value1"))
        assertTrue(snapshot.contains("value2"))
        assertTrue(snapshot.contains("value3"))

        // Use set() to see that the first list is replaced by the new list
        stringSetMetric.add("added1")
        // Check that data was properly recorded.
        val snapshot2 = stringSetMetric.testGetValue()
        assertEquals(4, snapshot2.size)
        assertTrue(stringSetMetric.testHasValue())
        assertTrue(snapshot2.contains("value1"))
        assertTrue(snapshot2.contains("value2"))
        assertTrue(snapshot2.contains("value3"))
        assertTrue(snapshot2.contains("added1"))
    }

    @Test
    fun `lists with no lifetime must not record data`() {
        // Define a string list metric which will be stored in "store1".
        // It's lifetime is set to Lifetime.Ping so it should not record anything.
        val stringSetMetric = StringSetMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_set_metric",
            sendInPings = listOf("store1")
        )

        // Attempt to store the string list using set
        stringSetMetric.set(setOf("value1", "value2", "value3"))
        // Check that nothing was recorded.
        assertFalse("StringLists without a lifetime should not record data",
            stringSetMetric.testHasValue())

        // Attempt to store the string list using add.
        stringSetMetric.add("value4")
        // Check that nothing was recorded.
        assertFalse("StringLists without a lifetime should not record data",
            stringSetMetric.testHasValue())
    }

    @Test
    fun `disabled lists must not record data`() {
        // Define a string list metric which will be stored in "store1".
        // It's disabled so it should not record anything.
        val stringSetMetric = StringSetMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_set_metric",
            sendInPings = listOf("store1")
        )

        // Attempt to store the string list using set.
        stringSetMetric.set(setOf("value1", "value2", "value3"))
        // Check that nothing was recorded.
        assertFalse("StringLists must not be recorded if they are disabled",
            stringSetMetric.testHasValue())

        // Attempt to store the string list using add.
        stringSetMetric.add("value4")
        // Check that nothing was recorded.
        assertFalse("StringLists must not be recorded if they are disabled",
            stringSetMetric.testHasValue())
    }

    @Test(expected = NullPointerException::class)
    fun `testGetValue() throws NullPointerException if nothing is stored`() {
        val stringSetMetric = StringSetMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_set_metric",
            sendInPings = listOf("store1")
        )
        stringSetMetric.testGetValue()
    }

    @Test
    fun `The API saves to secondary pings`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1" and "store2"
        val stringSetMetric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_set_metric",
            sendInPings = listOf("store1", "store2")
        )

        // Record two lists using add and set
        stringSetMetric.add("value1")
        stringSetMetric.add("value2")
        stringSetMetric.add("value3")

        // Check that data was properly recorded in the second ping.
        assertTrue(stringSetMetric.testHasValue("store2"))
        val snapshot = stringSetMetric.testGetValue("store2")
        assertEquals(3, snapshot.size)
        assertTrue(snapshot.contains("value1"))
        assertTrue(snapshot.contains("value2"))
        assertTrue(snapshot.contains("value3"))

        // Use set() to see that the first list is replaced by the new list.
        stringSetMetric.set(setOf("other1", "other2", "other3"))
        // Check that data was properly recorded in the second ping.
        assertTrue(stringSetMetric.testHasValue("store2"))
        val snapshot2 = stringSetMetric.testGetValue("store2")
        assertEquals(3, snapshot2.size)
        assertTrue(snapshot2.contains("other1"))
        assertTrue(snapshot2.contains("other2"))
        assertTrue(snapshot2.contains("other3"))
    }

    @Test
    fun `The API discards duplicate strings when added using add`() {
        val stringSetMetric = StringSetMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_set_duplicate",
            sendInPings = listOf("store1")
        )

        // Store a set of non-duplicates.
        stringSetMetric.set(setOf("value1", "value2", "value3"))
        // Then add a duplicate value.
        stringSetMetric.add("value1")

        // Check that data was properly recorded.
        val snapshot = stringSetMetric.testGetValue()
        assertEquals(3, snapshot.size)
        assertTrue(stringSetMetric.testHasValue())
        assertTrue(snapshot.contains("value1"))
        assertTrue(snapshot.contains("value2"))
        assertTrue(snapshot.contains("value3"))
    }
}
