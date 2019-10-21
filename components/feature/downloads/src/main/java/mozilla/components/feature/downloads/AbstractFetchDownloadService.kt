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
import mozilla.components.support.base.ids.cancel
import mozilla.components.support.base.ids.notify
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

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

    private var listOfDownloadJobs = mutableMapOf<Long, DownloadJobState>()

    // TODO: Can this be simplified/refactored?
    data class DownloadJobState(
            var job: Job? = null,
            var state: DownloadState,
            var currentBytesCopied: Long = 0,
            var isPaused: Boolean = false,
            var isCancelled: Boolean = false,
            var foregroundServiceId: Int = 0
     )

    private val broadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val downloadId =  intent?.extras?.getLong(DownloadNotification.EXTRA_DOWNLOAD_ID) ?: return
                val currentDownloadJobState = listOfDownloadJobs[downloadId] ?: return

                when (intent.action) {
                    ACTION_PAUSE -> {
                        Log.d("Sawyer", "ACTION_PAUSE: " + intent.extras?.getLong(DownloadNotification.EXTRA_DOWNLOAD_ID))
                        currentDownloadJobState.isPaused = true
                        currentDownloadJobState.job?.cancel()
                    }

                    ACTION_RESUME -> {
                        Log.d("Sawyer", "ACTION_RESUME: " + intent.extras?.getLong(DownloadNotification.EXTRA_DOWNLOAD_ID))

                        displayOngoingDownloadNotification(currentDownloadJobState.state)

                        currentDownloadJobState.job = startDownloadJob(currentDownloadJobState.state, true)
                        currentDownloadJobState.isPaused = false
                    }

                    ACTION_CANCEL -> {
                        Log.d("Sawyer", "ACTION_CANCEL: " + intent.extras?.getLong(DownloadNotification.EXTRA_DOWNLOAD_ID))

                        // We must set isCancelled here so `copyInChunks` knows to break out of its loop and cancel the operation
                        currentDownloadJobState.isCancelled = true
                        currentDownloadJobState.job?.cancel()
                        NotificationManagerCompat.from(context).cancel(context, currentDownloadJobState.foregroundServiceId.toString())
                    }
                }
            }
        }
    }

    // TODO: Do I really need to startForeground immediately? This causes double notifs to appear

    override fun onBind(intent: Intent?): IBinder? = null

    override suspend fun onStartCommand(intent: Intent?, flags: Int) {
        val download = intent?.getDownloadExtra() ?: return
        registerForUpdates()

        // TODO: Is there a problem with using a random int for the foregroundServiceId? Could this cause collisions?
        val foregroundServiceId = Random.nextInt()

        // Create a new job and add it, with its downloadState to the map
        listOfDownloadJobs[download.id] = DownloadJobState(
                job = startDownloadJob(download, false),
                state = download,
                foregroundServiceId = foregroundServiceId
        )
    }

    private fun startDownloadJob(download: DownloadState, isResuming: Boolean): Job {
        return CoroutineScope(IO).launch {
            var tag: String
            val notification = try {
                performDownload(download, isResuming)

                if (listOfDownloadJobs[download.id]?.isCancelled == true) { return@launch }

                if (listOfDownloadJobs[download.id]?.isPaused == true) {
                    tag = listOfDownloadJobs[download.id]?.foregroundServiceId?.toString()!!
                    DownloadNotification.createPausedDownloadNotification(context, download)
                } else {
                    tag = listOfDownloadJobs[download.id]?.foregroundServiceId?.toString()!!
                    DownloadNotification.createDownloadCompletedNotification(context, download.fileName)
                }
            } catch (e: IOException) {
                // TODO: Test failed notification download
                tag = listOfDownloadJobs[download.id]?.foregroundServiceId?.toString()!!
                DownloadNotification.createDownloadFailedNotification(context, download.fileName)
            }

            NotificationManagerCompat.from(context).notify(
                    context,
                    tag,
                    notification
            )

            sendDownloadCompleteBroadcast(download.id)
        }
    }

    private fun registerForUpdates() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_CANCEL)
        }

        context.registerReceiver(broadcastReceiver, filter)
    }

    // TODO: Do I need to unregister?
    private fun unregisterForUpdates() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun displayOngoingDownloadNotification(download: DownloadState?) {
        //listOfDownloadJobs[download?.id]?.foregroundServiceId?.toString() ?: return

        val ongoingDownloadNotification = DownloadNotification.createOngoingDownloadNotification(
            context,
            download
        )

        Log.d("Sawyer", "displayOngoing with id: " + download?.id)
        NotificationManagerCompat.from(context).notify(
            context,
            listOfDownloadJobs[download?.id]?.foregroundServiceId?.toString() ?: "",
            ongoingDownloadNotification
        )
    }

    private fun performDownload(download: DownloadState, isResuming: Boolean) {
        val headers = getHeadersFromDownload(download)
        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        Log.d("Sawyer", "performDownload: " + download.id)
        response.body.useStream { inStream ->
            val newDownloadState = download.withResponse(response.headers, inStream)
            listOfDownloadJobs[download.id]?.state = newDownloadState

            displayOngoingDownloadNotification(newDownloadState)

            useFileStream(newDownloadState, isResuming) { outStream ->
                if (isResuming) {
                    inStream.skip(listOfDownloadJobs[download.id]?.currentBytesCopied ?: 0)
                }
                copyInChunks(listOfDownloadJobs[download.id]!!, inStream, outStream)
            }
        }
    }

    // TODO: Can't "copy in chunks" for files without a contentLength
    private fun copyInChunks(downloadJobState: DownloadJobState, inStream: InputStream, outStream: OutputStream) {
        // To ensure that we copy all files (even ones that don't have fileSize, we must NOT check < fileSize

        // TODO: It may have a passed in copy of downloadJobState so this may not work...
        while (!downloadJobState.isPaused && !downloadJobState.isCancelled) {
            val data = ByteArray(chunkSize)
            val bytesRead = inStream.read(data)

            // If bytesRead is -1, there's no data left to read from the stream
            if (bytesRead == -1) { break }

            downloadJobState.currentBytesCopied += bytesRead

            outStream.write(data, 0, bytesRead)
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
        append: Boolean,
        block: (OutputStream) -> Unit
    ) {
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            useFileStreamScopedStorage(download, append, block)
        } else {
            useFileStreamLegacy(download, append, block)
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun useFileStreamScopedStorage(download: DownloadState, append: Boolean, block: (OutputStream) -> Unit) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, download.fileName)
            put(MediaStore.Downloads.MIME_TYPE, download.contentType ?: "*/*")
            put(MediaStore.Downloads.SIZE, download.contentLength)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        // TODO: How do we handle the Q version...?
        Log.d("Sawyer", "in new file stream, append: " + append)

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
    private fun useFileStreamLegacy(download: DownloadState, append: Boolean, block: (OutputStream) -> Unit) {
        val dir = Environment.getExternalStoragePublicDirectory(download.destinationDirectory)
        val file = File(dir, download.fileName!!)
        FileOutputStream(file, append).use(block)

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
        private const val ONGOING_DOWNLOAD_NOTIFICATION_TAG = "OngoingDownload"
        private const val COMPLETED_DOWNLOAD_NOTIFICATION_TAG = "CompletedDownload"
        const val ACTION_PAUSE = "mozilla.components.feature.downloads.PAUSE"
        const val ACTION_RESUME = "mozilla.components.feature.downloads.RESUME"
        const val ACTION_CANCEL = "mozilla.components.feature.downloads.CANCEL"
        const val chunkSize = 4 * 1024
    }
}
