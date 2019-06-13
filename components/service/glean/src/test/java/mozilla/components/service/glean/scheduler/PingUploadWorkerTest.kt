package mozilla.components.service.glean.scheduler

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import mozilla.components.service.glean.config.Configuration
import mozilla.components.service.glean.resetGlean
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations.initMocks

@RunWith(AndroidJUnit4::class)
class PingUploadWorkerTest {

    @Mock
    lateinit var workerParams: WorkerParameters

    lateinit var pingUploadWorker: PingUploadWorker

    @Before
    fun setUp() {
        initMocks(this)

        resetGlean(testContext, config = Configuration().copy(logPings = true))

        pingUploadWorker = PingUploadWorker(testContext, workerParams)
    }

    @Test
    fun testPingConfiguration() {
        // Set the constraints around which the worker can be run, in this case it
        // only requires that any network connection be available.
        val workRequest = PingUploadWorker.buildWorkRequest()
        val workSpec = workRequest.workSpec

        // verify constraints
        assertEquals(NetworkType.CONNECTED, workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, workSpec.backoffPolicy)
        assertTrue(workRequest.tags.contains(PingUploadWorker.PING_WORKER_TAG))
    }

    @Test
    fun testDoWorkSuccess() {
        val result = pingUploadWorker.doWork()
        assertTrue(result.toString().contains("Success"))
    }
}
