/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.reflect.KVisibility

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PlacesStorageMaintenanceWorkerTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @After
    fun tearDown() {
        GlobalPlacesDependencyProvider.placesStorage = null
    }

    @Test
    fun `PlacesStorage's runMaintenance is called when worker's startWork is called`() {
        runTestOnMain {
            val placesStorage = mock<PlacesStorage>()
            GlobalPlacesDependencyProvider.initialize(placesStorage)
            val worker = TestListenableWorkerBuilder<PlacesStorageMaintenanceWorker>(testContext).build()

            worker.doWork()
            verify(placesStorage).runMaintenance(PlacesStorageMaintenanceWorker.DB_SIZE_LIMIT_IN_BYTES.toUInt())
        }
    }

    @Test
    fun `PlacesStorage's runMaintenance operation is successful, successful result returned by the worker`() {
        runTestOnMain {
            val placesStorage = mock<PlacesStorage>()
            GlobalPlacesDependencyProvider.initialize(placesStorage)
            val worker = TestListenableWorkerBuilder<PlacesStorageMaintenanceWorker>(testContext).build()

            val result = worker.doWork()
            assertEquals(Result.success(), result)
        }
    }

    @Test
    fun `PlacesStorage's runMaintenance is called, exception is thrown and failure result is returned`() {
        runTestOnMain {
            val placesStorage = mock<PlacesStorage>()
            `when`(placesStorage.runMaintenance(PlacesStorageMaintenanceWorker.DB_SIZE_LIMIT_IN_BYTES.toUInt()))
                .thenThrow(CancellationException())
            GlobalPlacesDependencyProvider.initialize(placesStorage)
            val worker = TestListenableWorkerBuilder<PlacesStorageMaintenanceWorker>(testContext).build()

            val result = worker.doWork()
            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun `PlacesStorage's runMaintenance is called, exception is thrown and active write operations are cancelled`() {
        runTestOnMain {
            val placesStorage = mock<PlacesStorage>()
            `when`(placesStorage.runMaintenance(PlacesStorageMaintenanceWorker.DB_SIZE_LIMIT_IN_BYTES.toUInt()))
                .thenThrow(CancellationException())
            GlobalPlacesDependencyProvider.initialize(placesStorage)
            val worker = TestListenableWorkerBuilder<PlacesStorageMaintenanceWorker>(testContext).build()

            worker.doWork()
            verify(placesStorage).cancelWrites()
        }
    }

    @Test
    fun `StorageMaintenanceWorker's visibility is internal`() {
        assertEquals(PlacesStorageMaintenanceWorker::class.visibility, KVisibility.INTERNAL)
    }
}
