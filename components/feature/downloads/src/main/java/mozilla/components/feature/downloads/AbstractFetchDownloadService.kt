/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.annotation.TargetApi
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
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
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException

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

    private var downloadIsPaused = false

    // TODO: Eventually change this to handle a LIST of download jobs with their streams & bytes copied
    private var listOfDownloadJobs = mutableMapOf<Job, DownloadState>()

    private var currentInStream: InputStream? = null
    private var currentOutStream: OutputStream? = null
    private var currentBytesCopied: Long = 0

    private val broadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                Log.d("Sawyer", "onReceive")
                when (intent?.action) {
                    ACTION_PAUSE -> {
                        downloadIsPaused = true
                        cancelDownload()
                    }

                    ACTION_RESUME -> {
                        Log.d("Sawyer", "ACTION_RESUME")
                        displayOngoingDownloadNotification(currentDownload)

                        downloadJob = CoroutineScope(IO).launch {
                            resumeDownload(currentDownload, currentOutStream)
                        }
                    }

                    ACTION_CANCEL -> {
                        Log.d("Sawyer", "ACTION_CANCEL")
                        if (downloadIsPaused) {
                            // TODO: Kill *just* the notification we want, not all of them
                            NotificationManagerCompat.from(context).cancelAll()
                        } else {
                            cancelDownload()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        // We must start the foreground service immediately in order to stop Android from killing our service
        startForeground(
            NotificationIds.getIdForTag(context, ONGOING_DOWNLOAD_NOTIFICATION_TAG),
            DownloadNotification.createOngoingDownloadNotification(context, "", 0)
        )

        registerForUpdates()

        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        unregisterForUpdates()
        super.onDestroy()
    }

    override suspend fun onStartCommand(intent: Intent?, flags: Int) {
        currentDownload = intent?.getDownloadExtra() ?: return
        val download = intent.getDownloadExtra() ?: return

        // Create a new job and add it, with its downloadState to the map
        val newDownloadJob = CoroutineScope(IO).launch {
            displayOngoingDownloadNotification(download)

            var tag = ""
            val notification = try {
                Log.d("Sawyer", "before download")
                performDownload(download)
                Log.d("Sawyer", "download complete")
                DownloadNotification.createDownloadCompletedNotification(context, download.fileName)
            } catch (e: IOException) {
                if (e.localizedMessage == IO_EXCEPTION_CLOSED) {
                    // We intentionally closed the stream due to it being paused
                    tag = ONGOING_DOWNLOAD_NOTIFICATION_TAG
                    DownloadNotification.createPausedDownloadNotification(context, download.fileName)
                } else {
                    tag = COMPLETED_DOWNLOAD_NOTIFICATION_TAG
                    DownloadNotification.createDownloadFailedNotification(context, download.fileName)
                }
            }

            NotificationManagerCompat.from(context).notify(
                context,
                tag,
                notification
            )

            val downloadID = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
            sendDownloadCompleteBroadcast(downloadID)
        }.also { job ->
            job.invokeOnCompletion { cause ->
                if (cause?.localizedMessage == "Job was cancelled" && downloadIsPaused) {
                    // If it was cancelled that means the user paused
                    // It could ALSO mean the user pressed the cancel button, though :\
                    // Need to distinguish between these two and stopForeground if necessary
                    Log.d("Sawyer", "job was paused")
                } else {
                    // Otherwise the download is complete, so end the service
                    Log.d("Sawyer", "job is done")
                    stopForeground(true)
                }
            }
        }

        downloadJob = newDownloadJob
    }

    private fun registerForUpdates() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_CANCEL)
        }

        context.registerReceiver(broadcastReceiver, filter)
    }

    private fun unregisterForUpdates() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun displayOngoingDownloadNotification(download: DownloadState?) {
        val ongoingDownloadNotification = DownloadNotification.createOngoingDownloadNotification(
            context,
            download?.fileName,
            download?.contentLength
        )

        NotificationManagerCompat.from(context).notify(
            context,
            ONGOING_DOWNLOAD_NOTIFICATION_TAG,
            ongoingDownloadNotification
        )
    }

    private fun cancelDownload() {
        downloadJob?.cancel()

        CoroutineScope(IO).launch {
            // TODO: How do I make this synchronous so as not to cause a nullpointerexception
            // I think the other thread is trying to do a `copyTo` while we're closing which causes a crash
            try {
                currentInStream?.close()
            } catch (e: NullPointerException) {
                Log.d("Sawyer", "Cancel null pointer")
            }
        }
    }

    private fun performDownload(download: DownloadState) {
        val headers = getHeadersFromDownload(download)
        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        // Download stream
        response.body.useStream { inStream ->
            currentInStream = inStream

            val newDownloadState = download.withResponse(response.headers, inStream)
            currentDownload = newDownloadState

            displayOngoingDownloadNotification(currentDownload)

            useFileStream(newDownloadState) { outStream ->
                currentOutStream = outStream
                // Write stream, keep track of the bytes copied
                try {
                    currentBytesCopied = inStream.copyTo(outStream)
                } catch (e: NullPointerException) {
                    // TODO: This means the job was cancelled while we were trying to copy
                    Log.d("Sawyer", "null pointer")
                }
            }
        }
    }

    private suspend fun resumeDownload(downloadState: DownloadState?, outputStream: OutputStream?) = withContext(IO) {
        // Read from inStream starting out outStream's current byte
        val download = downloadState ?: return@withContext
        // val currentOutStream = outputStream ?: return@withContext

        Log.d("Sawy2er2", "outputStream: " + outputStream)

        val headers = getHeadersFromDownload(download)
        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        // TODO: Why is the currentOutStream closed if I never closed it?
        response.body.useStream { inStream ->
            currentInStream = inStream
            val newDownloadState = download.withResponse(response.headers, inStream)
            currentDownload = newDownloadState
            useFileStream(newDownloadState) { outStream ->
                // Resume the stream at the paused position
                currentBytesCopied = inStream.copyTo(outStream)

                // TODO: I want to resume the download where we left off... but the outStream says it's closed
                /*
                inStream.skip(currentBytesCopied)
                currentBytesCopied += inStream.copyTo(outStream)

                 */
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
        private const val IO_EXCEPTION_CLOSED = "closed"
        private const val ONGOING_DOWNLOAD_NOTIFICATION_TAG = "OngoingDownload"
        private const val COMPLETED_DOWNLOAD_NOTIFICATION_TAG = "CompletedDownload"
        const val ACTION_PAUSE = "mozilla.components.feature.downloads.PAUSE"
        const val ACTION_RESUME = "mozilla.components.feature.downloads.RESUME"
        const val ACTION_CANCEL = "mozilla.components.feature.downloads.CANCEL"
    }
}
