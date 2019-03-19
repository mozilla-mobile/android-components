/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.NullPointerException
import java.lang.Thread.sleep

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class StringMetricTypeTest {

    @get:Rule
    val fakeDispatchers = FakeDispatchersInTest()

    @Before
    fun setUp() {
        resetGlean()
    }

    @Test
    fun `The API must define the expected "default" storage`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1"
        val stringMetric = StringMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "string_metric",
            sendInPings = listOf("store1")
        )
        assertEquals(listOf("metrics"), stringMetric.defaultStorageDestinations)
    }

    @Test
    fun `The API saves to its storage engine`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1"
        val stringMetric = StringMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_metric",
            sendInPings = listOf("store1")
        )

        // Record two strings of the same type, with a little delay.
        stringMetric.set("value")

        // Check that data was properly recorded.
        assertTrue(stringMetric.testHasValue())
        assertEquals("value", stringMetric.testGetValue())

        stringMetric.set("overriddenValue")
        // Check that data was properly recorded.
        assertTrue(stringMetric.testHasValue())
        assertEquals("overriddenValue", stringMetric.testGetValue())
    }

    @Test
    fun `strings with no lifetime must not record data`() {
        // Define a 'stringMetric' string metric, which will be stored in
        // "store1". It's disabled so it should not record anything.
        val stringMetric = StringMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "stringMetric",
            sendInPings = listOf("store1")
        )

        // Attempt to store the string.
        stringMetric.set("value")
        // Check that nothing was recorded.
        assertFalse("Strings must not be recorded if they have no lifetime",
            stringMetric.testHasValue())
    }

    @Test
    fun `disabled strings must not record data`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1". It's disabled
        // so it should not record anything.
        val stringMetric = StringMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "stringMetric",
            sendInPings = listOf("store1")
        )

        // Attempt to store the string.
        stringMetric.set("value")
        // Check that nothing was recorded.
        assertFalse("Strings must not be recorded if they are disabled",
            stringMetric.testHasValue())
    }

    @Test(expected = NullPointerException::class)
    fun `testGetValue() throws NullPointerException if nothing is stored`() {
        val stringMetric = StringMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "stringMetric",
            sendInPings = listOf("store1")
        )
        stringMetric.testGetValue()
    }

    @Test
    fun `testHasValue() should not cause deadlocks`() {
        val stringMetric = StringMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "stringMetric",
            sendInPings = listOf("store1")
        )

        var stopWrites = true
        val stringWriter = GlobalScope.async {
            do {
                stringMetric.set("should_not_be_recorded")
            } while (!stopWrites)
        }

        // Deadlocky?
        val stringConsumer = GlobalScope.async {
            for (i in 1..100) {
                stringMetric.testHasValue()
            }
        }
        sleep(10000)


        // Stop recording to the test metric and wait for the async stuff
        // to complete.
        runBlocking {
            stopWrites = true
            stringConsumer.await()
            stringWriter.await()
        }

    }

    @Test
    fun `The API saves to secondary pings`() {
        // Define a 'stringMetric' string metric, which will be stored in "store1" and "store2"
        val stringMetric = StringMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_metric",
            sendInPings = listOf("store1", "store2")
        )

        // Record two strings of the same type, with a little delay.
        stringMetric.set("value")

        // Check that data was properly recorded in the second ping.
        assertTrue(stringMetric.testHasValue("store2"))
        assertEquals("value", stringMetric.testGetValue("store2"))

        stringMetric.set("overriddenValue")
        // Check that data was properly recorded in the second ping.
        assertTrue(stringMetric.testHasValue("store2"))
        assertEquals("overriddenValue", stringMetric.testGetValue("store2"))
    }
}
