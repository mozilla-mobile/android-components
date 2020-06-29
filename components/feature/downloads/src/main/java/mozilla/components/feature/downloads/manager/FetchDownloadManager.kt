/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads.manager

import android.Manifest.permission.FOREGROUND_SERVICE
import android.Manifest.permission.INTERNET
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.downloads.AbstractFetchDownloadService
import mozilla.components.feature.downloads.AbstractFetchDownloadService.Companion.EXTRA_DOWNLOAD_STATUS
import mozilla.components.feature.downloads.ext.isScheme
import kotlin.reflect.KClass

/**
 * Handles the interactions with [AbstractFetchDownloadService].
 *
 * @property applicationContext a reference to [Context] applicationContext.
 * @property service The subclass of [AbstractFetchDownloadService] to use.
 */
class FetchDownloadManager<T : AbstractFetchDownloadService>(
    private val applicationContext: Context,
    private val store: BrowserStore,
    private val service: KClass<T>,
    private val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(applicationContext),
    override var onDownloadStopped: onDownloadStopped = noop
) : BroadcastReceiver(), DownloadManager {

    private var isSubscribedReceiver = false

    override val permissions = if (SDK_INT >= P) {
        arrayOf(INTERNET, WRITE_EXTERNAL_STORAGE, FOREGROUND_SERVICE)
    } else {
        arrayOf(INTERNET, WRITE_EXTERNAL_STORAGE)
    }

    /**
     * Schedules a download through the [AbstractFetchDownloadService].
     * @param download metadata related to the download.
     * @param cookie any additional cookie to add as part of the download request.
     * @return the id reference of the scheduled download.
     */
    override fun download(download: DownloadState, cookie: String): Long? {
        if (!download.isScheme(listOf("http", "https", "data", "blob"))) {
            return null
        }
        validatePermissionGranted(applicationContext)

        // The middleware will notify the service to start the download
        // once this action is processed.
        store.dispatch(DownloadAction.QueueDownloadAction(download))

        registerBroadcastReceiver()
        return download.id
    }

    override fun tryAgain(downloadId: Long) {
        val download = store.state.queuedDownloads[downloadId] ?: return

        val intent = Intent(applicationContext, service.java)
        intent.putExtra(EXTRA_DOWNLOAD_ID, download.id)
        applicationContext.startService(intent)

        registerBroadcastReceiver()
    }

    /**
     * Remove all the listeners.
     */
    override fun unregisterListeners() {
        if (isSubscribedReceiver) {
            broadcastManager.unregisterReceiver(this)
            isSubscribedReceiver = false
            store.dispatch(DownloadAction.RemoveAllQueuedDownloadsAction)
        }
    }

    private fun registerBroadcastReceiver() {
        if (!isSubscribedReceiver) {
            val filter = IntentFilter(ACTION_DOWNLOAD_COMPLETE)
            broadcastManager.registerReceiver(this, filter)
            isSubscribedReceiver = true
        }
    }

    /**
     * Invoked when a download is complete. Notifies [onDownloadStopped] and removes the queued
     * download if it's complete.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val downloadID = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
        val download = store.state.queuedDownloads[downloadID]
        val downloadStatus = intent.getSerializableExtra(EXTRA_DOWNLOAD_STATUS)
            as AbstractFetchDownloadService.DownloadJobStatus

        if (downloadStatus == AbstractFetchDownloadService.DownloadJobStatus.COMPLETED) {
            store.dispatch(DownloadAction.RemoveQueuedDownloadAction(downloadID))
        }
        if (download != null) {
            onDownloadStopped(download, downloadID, downloadStatus)
        }
    }
}
