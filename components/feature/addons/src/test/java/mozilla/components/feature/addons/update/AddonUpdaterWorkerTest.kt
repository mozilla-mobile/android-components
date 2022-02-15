/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.await
import androidx.work.testing.TestListenableWorkerBuilder
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.webextension.WebExtensionException
import mozilla.components.feature.addons.AddonManager
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import mozilla.components.support.webextensions.WebExtensionSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AddonUpdaterWorkerTest {

    @Before
    fun setUp() {
        GlobalAddonDependencyProvider.addonManager = null

        initWebExtensionSupport()
    }

    private fun initWebExtensionSupport() {
        val store = spy(BrowserStore())
        val engine: Engine = mock()
        val extension: WebExtension = mock()
        whenever(extension.id).thenReturn("addonId")
        val callbackCaptor = argumentCaptor<((List<WebExtension>) -> Unit)>()
        whenever(engine.listInstalledWebExtensions(callbackCaptor.capture(), any())).thenAnswer {
            callbackCaptor.value.invoke(listOf(extension))
        }
        WebExtensionSupport.initialize(engine, store)
    }

    @After
    fun after() {
        GlobalAddonDependencyProvider.addonManager = null
    }

    @Test
    fun `doWork - will return Result_success when SuccessfullyUpdated`() {
        val updateAttemptStorage = mock<DefaultAddonUpdater.UpdateAttemptStorage>()
        val addonId = "addonId"
        val onFinishCaptor = argumentCaptor<((AddonUpdater.Status) -> Unit)>()
        val addonManager = mock<AddonManager>()
        val worker = spy(
            TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
                .setInputData(AddonUpdaterWorker.createWorkerData(addonId))
                .build()
        )

        doReturn(updateAttemptStorage).`when`((worker as AddonUpdaterWorker)).updateAttemptStorage
        GlobalAddonDependencyProvider.initialize(addonManager, mock())

        whenever(addonManager.updateAddon(anyString(), onFinishCaptor.capture())).then {
            onFinishCaptor.value.invoke(AddonUpdater.Status.SuccessfullyUpdated)
        }

        runTest {
            doReturn(this).`when`(worker).attemptScope

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            verify(worker).saveUpdateAttempt(addonId, AddonUpdater.Status.SuccessfullyUpdated)
        }
    }

    @Test
    fun `doWork - will return Result_success when NoUpdateAvailable`() {
        val addonId = "addonId"
        val onFinishCaptor = argumentCaptor<((AddonUpdater.Status) -> Unit)>()
        val addonManager = mock<AddonManager>()
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .setInputData(AddonUpdaterWorker.createWorkerData(addonId))
            .build()

        GlobalAddonDependencyProvider.initialize(addonManager, mock())

        whenever(addonManager.updateAddon(anyString(), onFinishCaptor.capture())).then {
            onFinishCaptor.value.invoke(AddonUpdater.Status.NoUpdateAvailable)
        }

        runTest {
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun `doWork - will return Result_failure when NotInstalled`() {
        val addonId = "addonId"
        val onFinishCaptor = argumentCaptor<((AddonUpdater.Status) -> Unit)>()
        val addonManager = mock<AddonManager>()
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .setInputData(AddonUpdaterWorker.createWorkerData(addonId))
            .build()

        GlobalAddonDependencyProvider.initialize(addonManager, mock())

        whenever(addonManager.updateAddon(anyString(), onFinishCaptor.capture())).then {
            onFinishCaptor.value.invoke(AddonUpdater.Status.NotInstalled)
        }

        runTest {
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }
    }

    @Test
    fun `doWork - will return Result_retry when an Error happens and is recoverable`() {
        val updateAttemptStorage = mock<DefaultAddonUpdater.UpdateAttemptStorage>()
        val addonId = "addonId"
        val onFinishCaptor = argumentCaptor<((AddonUpdater.Status) -> Unit)>()
        val addonManager = mock<AddonManager>()
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .setInputData(AddonUpdaterWorker.createWorkerData(addonId))
            .build()
        val recoverableException = WebExtensionException(Exception(), isRecoverable = true)
        worker.updateAttemptStorage = updateAttemptStorage

        GlobalAddonDependencyProvider.initialize(addonManager, mock())

        whenever(addonManager.updateAddon(anyString(), onFinishCaptor.capture())).then {
            onFinishCaptor.value.invoke(AddonUpdater.Status.Error("error", recoverableException))
        }

        runTest {
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            updateAttemptStorage.saveOrUpdate(any())
        }
    }

    @Test
    fun `doWork - will return Result_success when an Error happens and is unrecoverable`() {
        val updateAttemptStorage = mock<DefaultAddonUpdater.UpdateAttemptStorage>()
        val addonId = "addonId"
        val onFinishCaptor = argumentCaptor<((AddonUpdater.Status) -> Unit)>()
        val addonManager = mock<AddonManager>()
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .setInputData(AddonUpdaterWorker.createWorkerData(addonId))
            .build()
        val unrecoverableException = WebExtensionException(Exception(), isRecoverable = false)
        worker.updateAttemptStorage = updateAttemptStorage

        GlobalAddonDependencyProvider.initialize(addonManager, mock())

        whenever(addonManager.updateAddon(anyString(), onFinishCaptor.capture())).then {
            onFinishCaptor.value.invoke(AddonUpdater.Status.Error("error", unrecoverableException))
        }

        runTest {
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            updateAttemptStorage.saveOrUpdate(any())
        }
    }

    @Test
    fun `doWork - will try pass any exceptions to the crashReporter`() {
        val addonId = "addonId"
        val onFinishCaptor = argumentCaptor<((AddonUpdater.Status) -> Unit)>()
        val addonManager = mock<AddonManager>()
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .setInputData(AddonUpdaterWorker.createWorkerData(addonId))
            .build()
        var crashWasReported = false
        val crashReporter: ((Throwable) -> Unit) = { _ ->
            crashWasReported = true
        }

        GlobalAddonDependencyProvider.initialize(addonManager, mock(), crashReporter)
        GlobalAddonDependencyProvider.addonManager = null

        whenever(addonManager.updateAddon(anyString(), onFinishCaptor.capture())).then {
            onFinishCaptor.value.invoke(AddonUpdater.Status.Error("error", Exception()))
        }

        runTest {
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertTrue(crashWasReported)
        }
    }

    @Test
    fun `retryIfRecoverable must return retry for recoverable exception`() {
        val recoverableException = WebExtensionException(Exception(), isRecoverable = true)
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .build()

        assertEquals(ListenableWorker.Result.retry(), worker.retryIfRecoverable(recoverableException))
    }

    @Test
    fun `retryIfRecoverable must return success for unrecoverable exception`() {
        val unrecoverableException = WebExtensionException(Exception(), isRecoverable = false)
        val worker = TestListenableWorkerBuilder<AddonUpdaterWorker>(testContext)
            .build()

        assertEquals(ListenableWorker.Result.success(), worker.retryIfRecoverable(unrecoverableException))
    }
}
