/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import mozilla.components.service.glean.net.HttpPingUploader
import mozilla.components.service.glean.storages.EventsStorageEngine
import mozilla.components.service.glean.storages.ExperimentsStorageEngine
import mozilla.components.service.glean.storages.StringsStorageEngine
import mozilla.components.service.glean.metrics.Baseline
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GleanTest {

    @get:Rule
    val fakeDispatchers = FakeDispatchersInTest()

    @Before
    fun setup() {
        Glean.initialized = false
        Glean.initialize(ApplicationProvider.getApplicationContext())
    }

    @After
    fun resetGlobalState() {
        Glean.setMetricsEnabled(true)
        Glean.clearExperiments()
    }

    @Test
    fun `disabling metrics should record nothing`() {
        val stringMetric = StringMetricType(
                disabled = false,
                category = "telemetry",
                lifetime = Lifetime.Application,
                name = "string_metric",
                sendInPings = listOf("store1")
        )
        StringsStorageEngine.clearAllStores()
        Glean.setMetricsEnabled(false)
        assertEquals(false, Glean.getMetricsEnabled())
        stringMetric.set("foo")
        assertNull(
                "Metrics should not be recorded if glean is disabled",
                StringsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        )
    }

    @Test
    fun `disabling event metrics should record only when enabled`() {
        val eventMetric = EventMetricType(
                disabled = false,
                category = "ui",
                lifetime = Lifetime.Ping,
                name = "event_metric",
                sendInPings = listOf("store1"),
                objects = listOf("buttonA")
        )
        EventsStorageEngine.clearAllStores()
        Glean.setMetricsEnabled(true)
        assertEquals(true, Glean.getMetricsEnabled())
        eventMetric.record("buttonA", "event1")
        val snapshot1 = EventsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertEquals(1, snapshot1!!.size)
        Glean.setMetricsEnabled(false)
        assertEquals(false, Glean.getMetricsEnabled())
        eventMetric.record("buttonA", "event2")
        val snapshot2 = EventsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertEquals(1, snapshot2!!.size)
        Glean.setMetricsEnabled(true)
        eventMetric.record("buttonA", "event3")
        val snapshot3 = EventsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        assertEquals(2, snapshot3!!.size)
    }

    @Test
    fun `test path generation`() {
        val uuid = UUID.randomUUID()
        val path = Glean.makePath("test", uuid)
        val applicationId = ApplicationProvider.getApplicationContext<Context>().packageName
        // Make sure that the default applicationId matches the package name.
        assertEquals(applicationId, Glean.applicationId)
        assertEquals(path, "/submit/$applicationId/test/${Glean.SCHEMA_VERSION}/$uuid")
    }

    @Test
    fun `test sending of default pings`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("OK"))

        StringsStorageEngine.clearAllStores()
        ExperimentsStorageEngine.clearAllStores()
        val stringMetric = StringMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_metric",
            sendInPings = listOf("default")
        )

        val realClient = Glean.httpPingUploader
        val testConfig = Glean.configuration.copy(
            serverEndpoint = "http://" + server.hostName + ":" + server.port
        )
        Glean.httpPingUploader = HttpPingUploader(testConfig)

        try {
            stringMetric.set("foo")

            Glean.setExperimentActive(
                "experiment1", "branch_a"
            )
            Glean.setExperimentActive(
                "experiment2", "branch_b",
                    mapOf("key" to "value")
            )
            Glean.setExperimentInactive("experiment1")

            Glean.handleEvent(Glean.PingEvent.Default)

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            val metricsJsonData = request.body.readUtf8()
            val metricsJson = JSONObject(metricsJsonData)
            checkPingSchema(metricsJson)
            assertEquals(
                "foo",
                metricsJson.getJSONObject("metrics")
                    .getJSONObject("string")
                    .getString("telemetry.string_metric")
            )
            assertNull(metricsJson.opt("events"))
            assertNotNull(metricsJson.opt("ping_info"))
            assertNotNull(metricsJson.getJSONObject("ping_info").opt("experiments"))
            val applicationId = ApplicationProvider.getApplicationContext<Context>().packageName
            assert(
                request.path.startsWith("/submit/$applicationId/metrics/${Glean.SCHEMA_VERSION}/")
            )
        } finally {
            Glean.httpPingUploader = realClient
            server.shutdown()
        }
    }

    @Test
    fun `test sending of background pings`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("OK"))

        EventsStorageEngine.clearAllStores()
        val click = EventMetricType(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("default"),
            objects = listOf("buttonA")
        )

        val realClient = Glean.httpPingUploader
        val testConfig = Glean.configuration.copy(
            serverEndpoint = "http://" + server.hostName + ":" + server.port
        )
        Glean.httpPingUploader = HttpPingUploader(testConfig)

        try {
            click.record("buttonA")
            Baseline.sessions.add()

            Glean.handleEvent(Glean.PingEvent.Background)

            val requests: MutableMap<String, String> = mutableMapOf()
            for (i in 0..1) {
                val request = server.takeRequest()
                val docType = request.path.split("/")[3]
                requests.set(docType, request.body.readUtf8())
            }

            val eventsJson = JSONObject(requests["events"])
            checkPingSchema(eventsJson)
            assertEquals(1, eventsJson.getJSONArray("events")!!.length())

            val baselineJson = JSONObject(requests["baseline"])
            checkPingSchema(baselineJson)

            val expectedBaselineStringMetrics = arrayOf(
                "baseline.os",
                "baseline.os_version",
                "baseline.device",
                "baseline.architecture"
            )
            val baselineStringMetrics = baselineJson.getJSONObject("metrics")!!.getJSONObject("string")!!
            assertEquals(expectedBaselineStringMetrics.size, baselineStringMetrics.length())
            for (metric in expectedBaselineStringMetrics) {
                assertNotNull(baselineStringMetrics.get(metric))
            }

            val expectedBaselineCounterMetrics = arrayOf(
                "baseline.sessions"
            )
            val baselineCounterMetrics = baselineJson.getJSONObject("metrics")!!.getJSONObject("counter")!!
            assertEquals(expectedBaselineCounterMetrics.size, baselineCounterMetrics.length())
            for (metric in expectedBaselineCounterMetrics) {
                assertNotNull(baselineCounterMetrics.get(metric))
            }
        } finally {
            Glean.httpPingUploader = realClient
            server.shutdown()
        }
    }

    @Test
    fun `initialize() must not crash the app if Glean's data dir is messed up`() {
        // Remove the Glean's data directory.
        val gleanDir = File(
            ApplicationProvider.getApplicationContext<Context>().applicationInfo.dataDir,
            Glean.GLEAN_DATA_DIR
        )
        assertTrue(gleanDir.deleteRecursively())

        // Create a file in its place.
        assertTrue(gleanDir.createNewFile())

        Glean.initialized = false

        // Try to init Glean: it should not crash.
        Glean.initialize(applicationContext = ApplicationProvider.getApplicationContext())

        // Clean up after this, so that other tests don't fail.
        assertTrue(gleanDir.delete())
    }

    @Test
    fun `Don't send metrics if not initialized`() {
        val stringMetric = StringMetricType(
            disabled = false,
            category = "telemetry",
            lifetime = Lifetime.Application,
            name = "string_metric",
            sendInPings = listOf("store1")
        )
        StringsStorageEngine.clearAllStores()
        Glean.initialized = false
        stringMetric.set("foo")
        assertNull(
            "Metrics should not be recorded if glean is not initialized",
            StringsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        )

        Glean.initialized = true
    }

    @Test(expected = IllegalStateException::class)
    fun `Don't initialize twice`() {
        Glean.initialize(applicationContext = ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `Don't handle events when uninitialized`() {
        val gleanSpy = spy<GleanInternalAPI>(GleanInternalAPI::class.java)

        doThrow(IllegalStateException("Shouldn't send ping")).`when`(gleanSpy).sendPing(anyString(), anyString())
        gleanSpy.initialized = false
        gleanSpy.handleEvent(Glean.PingEvent.Default)
    }
}
