/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.Manifest.permission.INTERNET
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.content.Intent
import org.junit.Assert.assertFalse
import mozilla.components.browser.session.Download
import mozilla.components.support.test.robolectric.grantPermission
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class DownloadManagerTest {

    private lateinit var download: Download
    private lateinit var downloadManager: DownloadManager

    @Before
    fun setup() {
        download = Download(
            "http://ipv4.download.thinkbroadband.com/5MB.zip",
            "", "application/zip", 5242880,
            "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/8.0 Chrome/69.0.3497.100 Mobile Safari/537.36"
        )
        val context = RuntimeEnvironment.application
        downloadManager = DownloadManager(context)
    }

    @Test(expected = SecurityException::class)
    fun `calling download without the right permission must throw an exception`() {
        downloadManager.download(download)
    }

    @Test
    fun `calling download must download`() {
        var downloadCompleted = false

        downloadManager.onDownloadCompleted = { _, _ ->
            downloadCompleted = true
        }

        grantPermissions()

        val id = downloadManager.download(download)

        notifyDownloadCompleted(id)

        assert(downloadCompleted)
    }

    @Test
    fun `calling registerListener with valid downloadID must call listener after download`() {
        var downloadCompleted = false
        val downloadWithFileName = download.copy(fileName = "5MB.zip")

        grantPermissions()

        val id = downloadManager.download(
            downloadWithFileName,
            cookie = "yummy_cookie=choco",
            refererURL = "https://www.mozilla.org"
        )

        downloadManager.onDownloadCompleted = { _, _ -> downloadCompleted = true }

        notifyDownloadCompleted(id)

        assert(downloadCompleted)

        downloadCompleted = false
        notifyDownloadCompleted(id)

        assertFalse(downloadCompleted)
    }

    private fun notifyDownloadCompleted(id: Long) {
        val application = Shadows.shadowOf(RuntimeEnvironment.application)

        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, id)
        application.sendBroadcast(intent)
    }

    private fun grantPermissions() {
        grantPermission(INTERNET, WRITE_EXTERNAL_STORAGE)
    }
}