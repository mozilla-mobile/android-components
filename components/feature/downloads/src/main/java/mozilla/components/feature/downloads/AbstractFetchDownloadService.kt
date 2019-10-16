/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.annotation.TargetApi
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Header
import mozilla.components.concept.fetch.Headers.Names.CONTENT_LENGTH
import mozilla.components.concept.fetch.Headers.Names.CONTENT_TYPE
import mozilla.components.concept.fetch.Headers.Names.REFERRER
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.toMutableHeaders
import mozilla.components.feature.downloads.ext.addCompletedDownload
import mozilla.components.feature.downloads.ext.getDownloadExtra
import mozilla.components.feature.downloads.ext.withResponse
import mozilla.components.support.base.ids.NotificationIds
import mozilla.components.support.base.ids.notify
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Service that performs downloads through a fetch [Client] rather than through the native
 * Android download manager.
 *
 * To use this service, you must create a subclass in your application and it to the manifest.
 */
abstract class AbstractFetchDownloadService : CoroutineService() {

    protected abstract val httpClient: Client
    @VisibleForTesting
    internal val broadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    @VisibleForTesting
    internal val context: Context get() = this

    private var currentDownload: DownloadState? = null

    private var downloadJob: Job? = null

    private var currentOutStream: OutputStream? = null
    private var currentBytesCopied: Long = 0

    private val broadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PAUSE -> {
                        displayPauseNotification(currentDownload)
                        pauseDownload()
                    }
                    ACTION_RESUME -> {
                        displayOngoingDownloadNotification(currentDownload)
                        downloadJob = CoroutineScope(IO).launch {
                            resumeDownload(currentDownload, currentOutStream)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("Sawyer", "binding")
        registerForUpdates()
        return null
    }

    override fun onDestroy() {
        Log.d("Sawyer", "destroying")
        super.onDestroy()
        unregisterForUpdates()
    }

    override suspend fun onStartCommand(intent: Intent?, flags: Int) {
        //currentDownload = intent?.getDownloadExtra() ?: return
        val download = intent?.getDownloadExtra() ?: return
        val pauseIntent = createPendingIntent(ACTION_PAUSE, 0)

        Log.d("Sawyer", "onStartCommand")

        // TODO: Register for updates somewhere else?
        registerForUpdates()
        startForeground(
                NotificationIds.getIdForTag(context, ONGOING_DOWNLOAD_NOTIFICATION_TAG),
                DownloadNotification.createOngoingDownloadNotification(context, download.fileName, download.contentLength, pauseIntent)
        )

        /*
        downloadJob = CoroutineScope(IO).launch {
            Log.d("Sawyer", "starting download job... is null? " + (currentDownload == null))
            currentDownload?.let {
                val notification = try {
                    performDownload(it)
                    DownloadNotification.createDownloadCompletedNotification(context, it.fileName)
                } catch (e: IOException) {
                    DownloadNotification.createDownloadFailedNotification(context, it.fileName)
                }

                NotificationManagerCompat.from(context).notify(
                        context,
                        COMPLETED_DOWNLOAD_NOTIFICATION_TAG,
                        notification
                )

                val downloadID = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                sendDownloadCompleteBroadcast(downloadID)
            }
        }

         */

        //downloadJob?.start()
    }

    private fun createPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(action)
        intent.setPackage(applicationContext.packageName)
        return PendingIntent.getBroadcast(applicationContext, requestCode, intent, 0)
    }

    private fun registerForUpdates() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
        }

        context.registerReceiver(broadcastReceiver, filter)
    }

    private fun unregisterForUpdates() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun displayOngoingDownloadNotification(download: DownloadState?) {
        // TODO: If foreground service... just notify!
        val pauseIntent = createPendingIntent(ACTION_PAUSE, 0)

        val ongoingDownloadNotification =
                DownloadNotification.createOngoingDownloadNotification(context, download?.fileName, download?.contentLength, pauseIntent)

        NotificationManagerCompat.from(context).notify(
                context,
                ONGOING_DOWNLOAD_NOTIFICATION_TAG,
                ongoingDownloadNotification
        )
    }

    private fun displayPauseNotification(download: DownloadState?) {
        val resumeIntent = createPendingIntent(ACTION_RESUME, 0)
        val pauseNotification =
                DownloadNotification.createPausedDownloadNotification(context, download?.fileName, resumeIntent)

        NotificationManagerCompat.from(context).notify(
                context,
                ONGOING_DOWNLOAD_NOTIFICATION_TAG,
                pauseNotification
        )
    }

    private fun pauseDownload()  {
        downloadJob?.cancel()
    }

    private suspend fun performDownload(download: DownloadState) = withContext(IO) {
        val headers = getHeadersFromDownload(download)
        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        // Download stream
        response.body.useStream { inStream ->
            val newDownloadState = download.withResponse(response.headers, inStream)
            currentDownload = newDownloadState

            // TODO: Create the notification, hide the size if <= 0L
            useFileStream(newDownloadState) { outStream ->
                // Write stream, keep track of the bytes copied
                currentBytesCopied = inStream.copyTo(outStream)
            }
        }
    }

    private suspend fun resumeDownload(downloadState: DownloadState?, outputStream: OutputStream?) = withContext(IO) {
        // Read from inStream starting out outStream's current byte
        val download = downloadState ?: return@withContext
        val currentOutStream = outputStream ?: return@withContext

        val headers = getHeadersFromDownload(download)
        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        response.body.useStream { inStream ->
            val newDownloadState = download.withResponse(response.headers, inStream)
            currentDownload = newDownloadState
            useFileStream(newDownloadState) {
                // Resume the stream at the paused position
                inStream.skip(currentBytesCopied)
                currentBytesCopied += inStream.copyTo(currentOutStream)
            }
        }
    }

    private fun getHeadersFromDownload(download: DownloadState): MutableHeaders {
        return listOf(
                CONTENT_TYPE to download.contentType,
                CONTENT_LENGTH to download.contentLength?.toString(),
                REFERRER to download.referrerUrl
        ).mapNotNull { (name, value) ->
            if (value.isNullOrBlank()) null else Header(name, value)
        }.toMutableHeaders()
    }

    /**
     * Informs [mozilla.components.feature.downloads.manager.FetchDownloadManager] that a download
     * has been completed.
     */
    private fun sendDownloadCompleteBroadcast(downloadID: Long) {
        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadID)
        broadcastManager.sendBroadcast(intent)
    }

    /**
     * Creates an output stream on the local filesystem, then informs the system that a download
     * is complete after [block] is run.
     *
     * Encapsulates different behaviour depending on the SDK version.
     */
    internal fun useFileStream(
        download: DownloadState,
        block: (OutputStream) -> Unit
    ) {
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            useFileStreamScopedStorage(download, block)
        } else {
            useFileStreamLegacy(download, block)
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun useFileStreamScopedStorage(download: DownloadState, block: (OutputStream) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, download.fileName)
            put(MediaStore.Downloads.MIME_TYPE, download.contentType ?: "*/*")
            put(MediaStore.Downloads.SIZE, download.contentLength)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = applicationContext.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = resolver.insert(collection, values)

        val pfd = resolver.openFileDescriptor(item!!, "w")
        ParcelFileDescriptor.AutoCloseOutputStream(pfd).use(block)

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(item, values, null, null)
    }

    @TargetApi(Build.VERSION_CODES.P)
    @Suppress("Deprecation")
    private fun useFileStreamLegacy(download: DownloadState, block: (OutputStream) -> Unit) {
        val dir = Environment.getExternalStoragePublicDirectory(download.destinationDirectory)
        val file = File(dir, download.fileName!!)
        FileOutputStream(file).use(block)

        addCompletedDownload(
            title = download.fileName!!,
            description = download.fileName!!,
            isMediaScannerScannable = true,
            mimeType = download.contentType ?: "*/*",
            path = file.absolutePath,
            length = download.contentLength ?: file.length(),
            // Only show notifications if our channel is blocked
            showNotification = !DownloadNotification.isChannelEnabled(context),
            uri = download.url.toUri(),
            referer = download.referrerUrl?.toUri()
        )
    }

    companion object {
        private const val PAUSED_DOWNLOAD_NOTIFICATION_TAG = "PausedDownload"
        private const val ONGOING_DOWNLOAD_NOTIFICATION_TAG = "OngoingDownload"
        private const val COMPLETED_DOWNLOAD_NOTIFICATION_TAG = "CompletedDownload"
        private const val ACTION_PAUSE = "mozilla.components.feature.downloads.PAUSE"
        private const val ACTION_RESUME = "mozilla.components.feature.downloads.RESUME"
    }
}
