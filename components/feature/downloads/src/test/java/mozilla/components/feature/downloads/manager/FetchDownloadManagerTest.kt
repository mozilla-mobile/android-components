/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads.manager

import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.INTERNET
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.AbstractFetchDownloadService.Companion.EXTRA_DOWNLOAD
import mozilla.components.feature.downloads.AbstractFetchDownloadService.Companion.EXTRA_DOWNLOAD_STATUS
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.grantPermission
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.mockito.Mockito.times

@RunWith(AndroidJUnit4::class)
class FetchDownloadManagerTest {

    private lateinit var broadcastManager: LocalBroadcastManager
    private lateinit var service: MockDownloadService
    private lateinit var download: DownloadState
    private lateinit var downloadManager: FetchDownloadManager<MockDownloadService>

    @Before
    fun setup() {
        broadcastManager = LocalBroadcastManager.getInstance(testContext)
        service = MockDownloadService()
        download = DownloadState(
            "http://ipv4.download.thinkbroadband.com/5MB.zip",
            "", "application/zip", 5242880,
            "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/8.0 Chrome/69.0.3497.100 Mobile Safari/537.36"
        )
        downloadManager = FetchDownloadManager(testContext, MockDownloadService::class, broadcastManager)
    }

    @Test(expected = SecurityException::class)
    fun `calling download without the right permission must throw an exception`() {
        downloadManager.download(download)
    }

    @Test
    fun `calling download must download the file`() {
        val context: Context = mock()
        downloadManager = FetchDownloadManager(context, MockDownloadService::class, broadcastManager)
        var downloadCompleted = false

        downloadManager.onDownloadStopped = { _, _, _ -> downloadCompleted = true }

        grantPermissions()

        val id = downloadManager.download(download)!!

        verify(context).startService(any())

        notifyDownloadCompleted(id)

        assertTrue(downloadCompleted)
    }

    @Test
    fun `calling tryAgain starts the download again`() {
        val context: Context = mock()
        downloadManager = FetchDownloadManager(context, MockDownloadService::class, broadcastManager)
        var downloadCompleted = false

        downloadManager.onDownloadStopped = { _, _, _ -> downloadCompleted = true }

        grantPermissions()

        val id = downloadManager.download(download)!!

        verify(context).startService(any())
        notifyDownloadCompleted(id)
        assertTrue(downloadCompleted)

        downloadCompleted = false

        downloadManager.tryAgain(id)

        verify(context, times(2)).startService(any())
        notifyDownloadCompleted(id)
        assertTrue(downloadCompleted)
    }

    @Test
    fun `try again should not crash when download does not exist`() {
        val context: Context = mock()
        downloadManager = FetchDownloadManager(context, MockDownloadService::class, broadcastManager)
        var downloadCompleted = false

        downloadManager.onDownloadStopped = { _, _, _ -> downloadCompleted = true }

        grantPermissions()

        val id = downloadManager.download(download)!!

        verify(context).startService(any())
        notifyDownloadCompleted(id)
        assertTrue(downloadCompleted)

        downloadCompleted = false
        downloadManager.tryAgain(id + 1)
        assertFalse(downloadCompleted)
        verify(context, times(1)).startService(any())
    }

    @Test
    fun `trying to download a file with invalid protocol must NOT triggered a download`() {

        val invalidDownload = download.copy(url = "ftp://ipv4.download.thinkbroadband.com/5MB.zip")

        grantPermissions()

        val id = downloadManager.download(invalidDownload)

        assertNull(id)
    }

    @Test
    fun `trying to download a file with a blob scheme should trigger a download`() {
        val validBlobDownload = download.copy(url = "blob:https://ipv4.download.thinkbroadband.com/5MB.zip")
        grantPermissions()

        val id = downloadManager.download(validBlobDownload)!!
        assertNotNull(id)
    }

    @Test
    fun `sendBroadcast with valid downloadID must call onDownloadStopped after download`() {
        var downloadCompleted = false
        var downloadStatus: DownloadJobStatus? = null
        val downloadWithFileName = download.copy(fileName = "5MB.zip")

        grantPermissions()

        downloadManager.onDownloadStopped = { _, _, status ->
            downloadStatus = status
            downloadCompleted = true
        }

        val id = downloadManager.download(
            downloadWithFileName,
            cookie = "yummy_cookie=choco"
        )!!

        notifyDownloadCompleted(id)

        assertTrue(downloadCompleted)

        assertEquals(DownloadJobStatus.COMPLETED, downloadStatus)
    }

    @Test
    fun `onReceive properly gets download object form sendBroadcast`() {
        var downloadCompleted = false
        var downloadStatus: DownloadJobStatus? = null
        var downloadName = ""
        var downloadSize = 0L
        val downloadWithFileName = download.copy(fileName = "5MB.zip", contentLength = 5L)

        grantPermissions()

        downloadManager.onDownloadStopped = { download, _, status ->
            downloadStatus = status
            downloadCompleted = true
            downloadName = download.fileName ?: ""
            downloadSize = download.contentLength ?: 0
        }

        val id = downloadManager.download(downloadWithFileName)!!

        notifyDownloadCompleted(id, downloadWithFileName)

        assertTrue(downloadCompleted)
        assertEquals("5MB.zip", downloadName)
        assertEquals(5L, downloadSize)
        assertEquals(DownloadJobStatus.COMPLETED, downloadStatus)
    }

    private fun notifyDownloadCompleted(id: Long, download: DownloadState = DownloadState(url = "")) {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_DOWNLOAD_ID, id)
        intent.putExtra(EXTRA_DOWNLOAD_STATUS, DownloadJobStatus.COMPLETED)
        intent.putExtra(EXTRA_DOWNLOAD, download)

        broadcastManager.sendBroadcast(intent)
    }

    private fun grantPermissions() {
        grantPermission(INTERNET, WRITE_EXTERNAL_STORAGE, FOREGROUND_SERVICE)
    }

    class MockDownloadService : AbstractFetchDownloadService() {
        override val httpClient: Client = mock()
    }
}
