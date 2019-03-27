/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.os.SystemClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.NullPointerException

// Declared here, since Kotlin can not declare nested enum classes.
enum class clickKeys(val value: String) {
    objectId("object_id")
}

enum class extraKeys(val value: String) {
    extra1("extra1"),
    extra2("extra2")
}

enum class testNameKeys(val value: String) {
    testName("test_name")
}

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class EventMetricTypeTest {

    @Before
    fun setUp() {
        resetGlean()
    }

    @Test
    fun `The API must define the expected "default" storage`() {
        // Define a 'click' event, which will be stored in "store1"
        val click = EventMetricType<NoExtraKeys>(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("store1")
        )
        assertEquals(listOf("events"), click.defaultStorageDestinations)
    }

    @Test
    fun `The API records to its storage engine`() {

        // Define a 'click' event, which will be stored in "store1"
        val click = EventMetricType<clickKeys>(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("store1")
        )

        // Record two events of the same type, with a little delay.
        click.record(extra = mapOf(clickKeys.objectId to "buttonA"))

        val expectedTimeSinceStart: Long = 37
        SystemClock.sleep(expectedTimeSinceStart)

        click.record(extra = mapOf(clickKeys.objectId to "buttonB"))

        // Check that data was properly recorded.
        val snapshot = click.testGetValue()
        assertTrue(click.testHasValue())
        assertEquals(2, snapshot.size)

        val firstEvent = snapshot.single { e -> e.extra?.get("object_id") == "buttonA" }
        assertEquals("ui", firstEvent.category)
        assertEquals("click", firstEvent.name)

        val secondEvent = snapshot.single { e -> e.extra?.get("object_id") == "buttonB" }
        assertEquals("ui", secondEvent.category)
        assertEquals("click", secondEvent.name)

        assertTrue("The sequence of the events must be preserved",
            firstEvent.timestamp < secondEvent.timestamp)
    }

    @Test
    fun `The API records to its storage engine when category is empty`() {
        // Define a 'click' event, which will be stored in "store1"
        val click = EventMetricType<clickKeys>(
            disabled = false,
            category = "",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("store1")
        )

        // Record two events of the same type, with a little delay.
        click.record(extra = mapOf(clickKeys.objectId to "buttonA"))

        val expectedTimeSinceStart: Long = 37
        SystemClock.sleep(expectedTimeSinceStart)

        click.record(extra = mapOf(clickKeys.objectId to "buttonB"))

        // Check that data was properly recorded.
        val snapshot = click.testGetValue()
        assertTrue(click.testHasValue())
        assertEquals(2, snapshot.size)

        val firstEvent = snapshot.single { e -> e.extra?.get("object_id") == "buttonA" }
        assertEquals("click", firstEvent.name)

        val secondEvent = snapshot.single { e -> e.extra?.get("object_id") == "buttonB" }
        assertEquals("click", secondEvent.name)

        assertTrue("The sequence of the events must be preserved",
            firstEvent.timestamp < secondEvent.timestamp)
    }

    @Test
    fun `disabled events must not record data`() {
        // Define a 'click' event, which will be stored in "store1". It's disabled
        // so it should not record anything.
        val click = EventMetricType<NoExtraKeys>(
            disabled = true,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("store1")
        )

        // Attempt to store the event.
        click.record()

        // Check that nothing was recorded.
        assertFalse("Events must not be recorded if they are disabled",
            click.testHasValue())
    }

    @Test(expected = NullPointerException::class)
    fun `testGetValue() throws NullPointerException if nothing is stored`() {
        val testEvent = EventMetricType<NoExtraKeys>(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "testEvent",
            sendInPings = listOf("store1")
        )
        testEvent.testGetValue()
    }

    @Test
    fun `The API records to secondary pings`() {
        // Define a 'click' event, which will be stored in "store1" and "store2"
        val click = EventMetricType<clickKeys>(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("store1", "store2")
        )

        // Record two events of the same type, with a little delay.
        click.record(extra = mapOf(clickKeys.objectId to "buttonA"))

        val expectedTimeSinceStart: Long = 37
        SystemClock.sleep(expectedTimeSinceStart)

        click.record(extra = mapOf(clickKeys.objectId to "buttonB"))

        // Check that data was properly recorded in the second ping.
        val snapshot = click.testGetValue("store2")
        assertTrue(click.testHasValue("store2"))
        assertEquals(2, snapshot.size)

        val firstEvent = snapshot.single { e -> e.extra?.get("object_id") == "buttonA" }
        assertEquals("ui", firstEvent.category)
        assertEquals("click", firstEvent.name)

        val secondEvent = snapshot.single { e -> e.extra?.get("object_id") == "buttonB" }
        assertEquals("ui", secondEvent.category)
        assertEquals("click", secondEvent.name)

        assertTrue("The sequence of the events must be preserved",
            firstEvent.timestamp < secondEvent.timestamp)
    }

    @Test
    fun `events should not record when upload is disabled`() {
        val eventMetric = EventMetricType<testNameKeys>(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "event_metric",
            sendInPings = listOf("store1")
        )
        assertEquals(true, Glean.getUploadEnabled())
        Glean.setUploadEnabled(true)
        eventMetric.record(mapOf(testNameKeys.testName to "event1"))
        val snapshot1 = eventMetric.testGetValue()
        assertEquals(1, snapshot1.size)
        Glean.setUploadEnabled(false)
        assertEquals(false, Glean.getUploadEnabled())
        eventMetric.record(mapOf(testNameKeys.testName to "event2"))
        val snapshot2 = eventMetric.testGetValue()
        assertEquals(1, snapshot2.size)
        Glean.setUploadEnabled(true)
        eventMetric.record(mapOf(testNameKeys.testName to "event3"))
        val snapshot3 = eventMetric.testGetValue()
        assertEquals(2, snapshot3.size)
    }
}
