/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads.manager

import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.INTERNET
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.support.test.any
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.grantPermission
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class FetchDownloadManagerTest {

    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var service: MockDownloadService
    private lateinit var download: DownloadState
    private lateinit var downloadManager: FetchDownloadManager<MockDownloadService>
    private lateinit var browserStore: BrowserStore

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        browserStore = BrowserStore()
        service = MockDownloadService(browserStore)
        download = DownloadState(
            "http://ipv4.download.thinkbroadband.com/5MB.zip",
            "", "application/zip", 5242880,
            "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/8.0 Chrome/69.0.3497.100 Mobile Safari/537.36"
        )
        downloadManager = FetchDownloadManager(testContext, browserStore, MockDownloadService::class)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test(expected = SecurityException::class)
    fun `calling download without the right permission must throw an exception`() {
        downloadManager.download(download)
    }

    @Test
    fun `calling download must download the file`() {
        val context: Context = mock()
        downloadManager = FetchDownloadManager(context, browserStore, MockDownloadService::class)
        var downloadCompleted = false

        downloadManager.onDownloadCompleted = { _, _ -> downloadCompleted = true }

        grantPermissions()

        val id = downloadManager.download(download)!!

        verify(context).startService(any())

        notifyDownloadCompleted(id)

        assertTrue(downloadCompleted)
    }

    @Test
    fun `trying to download a file with invalid protocol must NOT triggered a download`() {

        val invalidDownload = download.copy(url = "ftp://ipv4.download.thinkbroadband.com/5MB.zip")

        grantPermissions()

        val id = downloadManager.download(invalidDownload)

        assertNull(id)
    }

    @Test
    fun `calling registerListener with valid downloadID must call listener after download`() {
        var downloadCompleted = false
        val downloadWithFileName = download.copy(fileName = "5MB.zip")

        grantPermissions()

        downloadManager.onDownloadCompleted = { _, _ -> downloadCompleted = true }

        val id = downloadManager.download(
            downloadWithFileName,
            cookie = "yummy_cookie=choco"
        )!!

        notifyDownloadCompleted(id)

        assertTrue(downloadCompleted)

        downloadCompleted = false
        notifyDownloadCompleted(id)

        assertFalse(downloadCompleted)
    }

    private fun notifyDownloadCompleted(id: Long) {
        browserStore.dispatch(DownloadAction.DownloadCompletedAction(id)).joinBlocking()
    }

    private fun grantPermissions() {
        grantPermission(INTERNET, WRITE_EXTERNAL_STORAGE, FOREGROUND_SERVICE)
    }

    class MockDownloadService(override val browserStore: BrowserStore) : AbstractFetchDownloadService() {
        override val httpClient: Client = mock()
    }
}
