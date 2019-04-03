/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import mozilla.components.service.glean.GleanMetrics.GleanInternalMetrics
import mozilla.components.service.glean.config.Configuration
import mozilla.components.service.glean.metrics.DatetimeMetricType
import mozilla.components.service.glean.metrics.EventMetricType
import mozilla.components.service.glean.metrics.Lifetime
import mozilla.components.service.glean.metrics.NoExtraKeys
import mozilla.components.service.glean.metrics.StringMetricType
import mozilla.components.service.glean.metrics.TimeUnit as GleanTimeUnit
import mozilla.components.service.glean.metrics.UuidMetricType
import mozilla.components.service.glean.storages.StringsStorageEngine
import mozilla.components.service.glean.scheduler.GleanLifecycleObserver
import mozilla.components.service.glean.scheduler.PingUploadWorker
import mozilla.components.service.glean.storages.StorageEngineManager
import mozilla.components.service.glean.utils.getLanguageFromLocale
import mozilla.components.service.glean.utils.getLocaleTag
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GleanTest {

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext())

        resetGlean()
    }

    @After
    fun resetGlobalState() {
        Glean.setUploadEnabled(true)
    }

    @Test
    fun `disabling upload should disable metrics recording`() {
        val stringMetric = StringMetricType(
                disabled = false,
                category = "telemetry",
                lifetime = Lifetime.Application,
                name = "string_metric",
                sendInPings = listOf("store1")
        )
        Glean.setUploadEnabled(false)
        assertEquals(false, Glean.getUploadEnabled())
        stringMetric.set("foo")
        assertNull(
                "Metrics should not be recorded if glean is disabled",
                StringsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        )
    }

    @Test
    fun `test path generation`() {
        val uuid = UUID.randomUUID()
        val path = Glean.makePath("test", uuid)
        val applicationId = "mozilla-components-service-glean"
        // Make sure that the default applicationId matches the package name.
        assertEquals(applicationId, Glean.applicationId)
        assertEquals(path, "/submit/$applicationId/test/${Glean.SCHEMA_VERSION}/$uuid")
    }

    @Test
    fun `test experiments recording`() {
        Glean.setExperimentActive(
            "experiment_test", "branch_a"
        )
        Glean.setExperimentActive(
            "experiment_api", "branch_b",
            mapOf("test_key" to "value")
        )
        assertTrue(Glean.testIsExperimentActive("experiment_api"))
        assertTrue(Glean.testIsExperimentActive("experiment_test"))

        Glean.setExperimentInactive("experiment_test")

        assertTrue(Glean.testIsExperimentActive("experiment_api"))
        assertFalse(Glean.testIsExperimentActive("experiment_test"))

        val storedData = Glean.testGetExperimentData("experiment_api")
        assertEquals("branch_b", storedData.branch)
        assertEquals(1, storedData.extra?.size)
        assertEquals("value", storedData.extra?.getValue("test_key"))
    }

    @Test
    fun `test sending of background pings`() {
        val server = MockWebServer()

        // It's important to note here that we expect to receive two pings back, the baseline and
        // the events ping so we need to enqueue a response back for EACH of them.
        server.enqueue(MockResponse().setBody("OK"))
        server.enqueue(MockResponse().setBody("OK"))

        val click = EventMetricType<NoExtraKeys>(
            disabled = false,
            category = "ui",
            lifetime = Lifetime.Ping,
            name = "click",
            sendInPings = listOf("default")
        )

        resetGlean(getContextWithMockedInfo(), Glean.configuration.copy(
            serverEndpoint = "http://" + server.hostName + ":" + server.port,
            logPings = true
        ))

        // Fake calling the lifecycle observer.
        val lifecycleRegistry = LifecycleRegistry(mock(LifecycleOwner::class.java))
        val gleanLifecycleObserver = GleanLifecycleObserver()
        lifecycleRegistry.addObserver(gleanLifecycleObserver)

        try {
            // Simulate the first foreground event after the application starts.
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            click.record()

            // Simulate going to background.
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

            // Trigger worker task to upload the pings in the background
            triggerWorkManager()

            val requests: MutableMap<String, String> = mutableMapOf()
            for (i in 0..1) {
                val request = server.takeRequest(20L, TimeUnit.SECONDS)
                val docType = request.path.split("/")[3]
                requests.set(docType, request.body.readUtf8())
            }

            val eventsJson = JSONObject(requests["events"])
            checkPingSchema(eventsJson)
            assertEquals("events", eventsJson.getJSONObject("ping_info")["ping_type"])
            assertEquals(1, eventsJson.getJSONArray("events")!!.length())

            val baselineJson = JSONObject(requests["baseline"])
            assertEquals("baseline", baselineJson.getJSONObject("ping_info")["ping_type"])
            checkPingSchema(baselineJson)

            val baselineMetricsObject = baselineJson.getJSONObject("metrics")!!
            val baselineStringMetrics = baselineMetricsObject.getJSONObject("string")!!
            assertEquals(1, baselineStringMetrics.length())
            assertNotNull(baselineStringMetrics.get("glean.baseline.locale"))

            val baselineTimespanMetrics = baselineMetricsObject.getJSONObject("timespan")!!
            assertEquals(1, baselineTimespanMetrics.length())
            assertNotNull(baselineTimespanMetrics.get("glean.baseline.duration"))
        } finally {
            server.shutdown()
            lifecycleRegistry.removeObserver(gleanLifecycleObserver)
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

        resetGlean()

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
        Glean.initialized = false
        stringMetric.set("foo")
        assertNull(
            "Metrics should not be recorded if glean is not initialized",
            StringsStorageEngine.getSnapshot(storeName = "store1", clearStore = false)
        )

        Glean.initialized = true
    }

    @Test
    fun `Initializing twice is a no-op`() {
        val beforeConfig = Glean.configuration

        Glean.initialize(ApplicationProvider.getApplicationContext())

        val afterConfig = Glean.configuration

        assertSame(beforeConfig, afterConfig)
    }

    @Test
    fun `Don't handle events when uninitialized`() {
        val gleanSpy = spy<GleanInternalAPI>(GleanInternalAPI::class.java)

        gleanSpy.initialized = false
        runBlocking {
            gleanSpy.handleBackgroundEvent()
        }
        assertFalse(isWorkScheduled(PingUploadWorker.PING_WORKER_TAG))
    }

    @Test
    fun `Don't schedule pings if metrics disabled`() {
        Glean.setUploadEnabled(false)

        runBlocking {
            Glean.handleBackgroundEvent()
        }
        assertFalse(isWorkScheduled(PingUploadWorker.PING_WORKER_TAG))
    }

    @Test
    fun `Don't schedule pings if there is no ping content`() {
        resetGlean(getContextWithMockedInfo())

        runBlocking {
            Glean.handleBackgroundEvent()
        }

        // We should only have a baseline ping and no events or metrics pings since nothing was
        // recorded
        val files = Glean.pingStorageEngine.storageDirectory.listFiles()

        // Make sure only the baseline ping is present and no events or metrics pings
        assertEquals(1, files.count())
        val file = files.first()
        BufferedReader(FileReader(file)).use {
            val lines = it.readLines()
            assert(lines[0].contains("baseline"))
        }
    }

    @Test
    fun `Application id sanitizer must correctly filter undesired characters`() {
        assertEquals(
            "org-mozilla-test-app",
            Glean.sanitizeApplicationId("org.mozilla.test-app")
        )

        assertEquals(
            "org-mozilla-test-app",
            Glean.sanitizeApplicationId("org.mozilla..test---app")
        )

        assertEquals(
            "org-mozilla-test-app",
            Glean.sanitizeApplicationId("org-mozilla-test-app")
        )
    }

    @Test
    fun `The appChannel must be correctly set, if requested`() {
        // No appChannel must be set if nothing was provided through the config
        // options.
        resetGlean(getContextWithMockedInfo(), Configuration())
        assertFalse(GleanInternalMetrics.appChannel.testHasValue())

        // The appChannel must be correctly reported if a channel value
        // was provided.
        val testChannelName = "my-test-channel"
        resetGlean(getContextWithMockedInfo(), Configuration(channel = testChannelName))
        assertTrue(GleanInternalMetrics.appChannel.testHasValue())
        assertEquals(testChannelName, GleanInternalMetrics.appChannel.testGetValue())
    }

    @Test
    fun `client_id and first_run_date metrics should be copied from the old location`() {
        // 1539480 BACKWARD COMPATIBILITY HACK

        // The resetGlean called right before this function will add client_id
        // and first_run_date to the new location in glean_client_info.  We
        // need to clear those out again so we can test what happens when they
        // are missing.
        StorageEngineManager(
            applicationContext = ApplicationProvider.getApplicationContext()
        ).clearAllStores()

        val clientIdMetric = UuidMetricType(
            disabled = false,
            category = "",
            name = "client_id",
            lifetime = Lifetime.User,
            sendInPings = listOf("glean_ping_info")
        )
        val clientIdValue = clientIdMetric.generateAndSet()

        val firstRunDateMetric = DatetimeMetricType(
            disabled = false,
            category = "",
            name = "first_run_date",
            lifetime = Lifetime.User,
            sendInPings = listOf("glean_ping_info"),
            timeUnit = GleanTimeUnit.Day
        )
        firstRunDateMetric.set()

        assertFalse(GleanInternalMetrics.clientId.testHasValue())
        assertFalse(GleanInternalMetrics.firstRunDate.testHasValue())

        // This should copy the values to their new locations
        Glean.initialized = false
        Glean.initialize(ApplicationProvider.getApplicationContext())

        assertEquals(clientIdValue, GleanInternalMetrics.clientId.testGetValue())
        assertTrue(GleanInternalMetrics.firstRunDate.testHasValue())
    }

    @Test
    fun `client_id and first_run_date metrics should not override new location`() {
        // 1539480 BACKWARD COMPATIBILITY HACK

        // The resetGlean called right before this function will add client_id
        // and first_run_date to the new location in glean_client_info.
        // In this case we want to keep those and confirm that any old values
        // won't override the new ones.

        val clientIdMetric = UuidMetricType(
            disabled = false,
            category = "",
            name = "client_id",
            lifetime = Lifetime.User,
            sendInPings = listOf("glean_ping_info")
        )
        val clientIdValue = clientIdMetric.generateAndSet()

        val firstRunDateMetric = DatetimeMetricType(
            disabled = false,
            category = "",
            name = "first_run_date",
            lifetime = Lifetime.User,
            sendInPings = listOf("glean_ping_info"),
            timeUnit = GleanTimeUnit.Day
        )
        firstRunDateMetric.set(Date(2200, 1, 1))

        assertTrue(GleanInternalMetrics.clientId.testHasValue())
        assertTrue(GleanInternalMetrics.firstRunDate.testHasValue())

        // This should copy the values to their new locations
        Glean.initialized = false
        Glean.initialize(ApplicationProvider.getApplicationContext())

        assertNotEquals(clientIdValue, GleanInternalMetrics.clientId.testGetValue())
        assertNotEquals(firstRunDateMetric.testGetValue(), GleanInternalMetrics.firstRunDate.testGetValue())
    }

    @Test
    fun `getLanguageTag() reports the tag for the default locale`() {
        val defaultLanguageTag = getLocaleTag()

        assertNotNull(defaultLanguageTag)
        assertFalse(defaultLanguageTag.isEmpty())
        assertEquals("en-US", defaultLanguageTag)
    }

    @Test
    fun `getLanguageTag reports the correct tag for a non-default language`() {
        val defaultLocale = Locale.getDefault()

        try {
            Locale.setDefault(Locale("fy", "NL"))

            val languageTag = getLocaleTag()

            assertNotNull(languageTag)
            assertFalse(languageTag.isEmpty())
            assertEquals("fy-NL", languageTag)
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun `getLanguage reports the modern translation for some languages`() {
        assertEquals("he", getLanguageFromLocale(Locale("iw", "IL")))
        assertEquals("id", getLanguageFromLocale(Locale("in", "ID")))
        assertEquals("yi", getLanguageFromLocale(Locale("ji", "ID")))
    }
}
