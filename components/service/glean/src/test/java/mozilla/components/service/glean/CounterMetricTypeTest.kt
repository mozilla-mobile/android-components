/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.service.glean.storages.CountersStorageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CounterMetricTypeTest {

    @get:Rule
    val fakeDispatchers = FakeDispatchersInTest()

    @Before
    fun setUp() {
        CountersStorageEngine.applicationContext = ApplicationProvider.getApplicationContext()
        // Clear the stored "user" preferences between tests.
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(CountersStorageEngine.javaClass.simpleName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        CountersStorageEngine.clearAllStores()
    }

    @Test
    fun `The API must define the expected "default" storage`() {
        // Define a 'counterMetric' counter metric, which will be stored in "store1"
        val counterMetric = CounterMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "counter_metric",
            sendInPings = listOf("store1")
        )
        assertEquals(listOf("metrics"), counterMetric.defaultStorageDestinations)
    }

    @Test
    fun `The API saves to its storage engine`() {
        // Define a 'counterMetric' counter metric, which will be stored in "store1"
        val counterMetric = CounterMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "counter_metric",
            sendInPings = listOf("store1")
        )

        // Add to the counter a couple of times with a little delay.  The first call will check
        // calling add() without parameters to test increment by 1.
        counterMetric.add()

        // Check that the count was incremented and properly recorded.
        val snapshot = CountersStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertEquals(1, snapshot!!.size)
        assertEquals(true, snapshot.containsKey("telemetry.counter_metric"))
        assertEquals(1, snapshot["telemetry.counter_metric"])

        counterMetric.add(10)
        // Check that count was incremented and properly recorded.  This second call will check
        // calling add() with 10 to test increment by other amount
        val snapshot2 = CountersStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertEquals(1, snapshot2!!.size)
        assertEquals(true, snapshot2.containsKey("telemetry.counter_metric"))
        assertEquals(11, snapshot2["telemetry.counter_metric"])
    }

    @Test
    fun `counters with no lifetime must not record data`() {
        // Define a 'counterMetric' counter metric, which will be stored in "store1".
        // It's disabled so it should not record anything.
        val counterMetric = CounterMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Ping,
            name = "counter_metric",
            sendInPings = listOf("store1")
        )

        // Attempt to increment the counter
        counterMetric.add(1)
        // Check that nothing was recorded.
        val snapshot = CountersStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertNull("Counters must not be recorded if they are disabled", snapshot)
    }

    @Test
    fun `counters must not increment when passed zero or negative`() {
        // Define a 'counterMetric' counter metric, which will be stored in "store1".
        val counterMetric = CounterMetricType(
                disabled = false,
                category = "telemetry",
                lifetime = Lifetime.Application,
                name = "counter_metric",
                sendInPings = listOf("store1")
        )

        // Attempt to increment the counter with zero
        counterMetric.add(0)
        // Check that nothing was recorded.
        var snapshot = CountersStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertNull("Counters must not be recorded if incremented with zero", snapshot)

        // Attempt to increment the counter with negative
        counterMetric.add(-1)
        // Check that nothing was recorded.
        snapshot = CountersStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertNull("Counters must not be recorded if incremented with negative", snapshot)
    }

    @Test
    fun `disabled counters must not record data`() {
        // Define a 'counterMetric' counter metric, which will be stored in "store1".  It's disabled
        // so it should not record anything.
        val counterMetric = CounterMetricType(
            disabled = true,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "counter_metric",
            sendInPings = listOf("store1")
        )

        // Attempt to store the counter.
        counterMetric.add()
        // Check that nothing was recorded.
        val snapshot = CountersStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertNull("Counters must not be recorded if they are disabled", snapshot)
    }
}
