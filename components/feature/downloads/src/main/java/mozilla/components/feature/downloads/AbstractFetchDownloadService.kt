/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.annotation.TargetApi
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Headers.Names.CONTENT_RANGE
import mozilla.components.concept.fetch.Headers.Names.RANGE
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.feature.downloads.ext.addCompletedDownload
import mozilla.components.feature.downloads.ext.getDownloadExtra
import mozilla.components.feature.downloads.ext.withResponse
import mozilla.components.feature.downloads.facts.emitNotificationResumeFact
import mozilla.components.feature.downloads.facts.emitNotificationPauseFact
import mozilla.components.feature.downloads.facts.emitNotificationCancelFact
import mozilla.components.feature.downloads.facts.emitNotificationTryAgainFact
import mozilla.components.feature.downloads.facts.emitNotificationOpenFact
import mozilla.components.support.utils.DownloadUtils
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
 * To use this service, you must create a subclass in your application and add it to the manifest.
 */
@Suppress("TooManyFunctions", "LargeClass")
abstract class AbstractFetchDownloadService : Service() {

    private val notificationUpdateScope = MainScope()

    protected abstract val httpClient: Client
    @VisibleForTesting
    internal val broadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    @VisibleForTesting
    internal val context: Context get() = this

    internal var downloadJobs = mutableMapOf<Long, DownloadJobState>()

    internal data class DownloadJobState(
        var job: Job? = null,
        @Volatile var state: DownloadState,
        var currentBytesCopied: Long = 0,
        @GuardedBy("context") var status: DownloadJobStatus,
        var foregroundServiceId: Int = 0,
        var downloadDeleted: Boolean = false,
        var notifiedStopped: Boolean = false
    )

    internal fun setDownloadJobStatus(downloadJobState: DownloadJobState, status: DownloadJobStatus) {
        synchronized(context) {
            if (status == DownloadJobStatus.ACTIVE) { downloadJobState.notifiedStopped = false }
            downloadJobState.status = status
        }
    }

    internal fun getDownloadJobStatus(downloadJobState: DownloadJobState): DownloadJobStatus {
        synchronized(context) {
            return downloadJobState.status
        }
    }

    /**
     * Status of an ongoing download
     */
    enum class DownloadJobStatus {
        ACTIVE,
        PAUSED,
        CANCELLED,
        FAILED,
        COMPLETED
    }

    internal val broadcastReceiver by lazy {
        object : BroadcastReceiver() {
            @Suppress("LongMethod")
            override fun onReceive(context: Context, intent: Intent?) {
                val downloadId =
                    intent?.extras?.getLong(DownloadNotification.EXTRA_DOWNLOAD_ID) ?: return
                val currentDownloadJobState = downloadJobs[downloadId] ?: return

                when (intent.action) {
                    ACTION_PAUSE -> {
                        setDownloadJobStatus(currentDownloadJobState, DownloadJobStatus.PAUSED)
                        currentDownloadJobState.job?.cancel()
                        emitNotificationPauseFact()
                    }

                    ACTION_RESUME -> {
                        setDownloadJobStatus(currentDownloadJobState, DownloadJobStatus.ACTIVE)

                        currentDownloadJobState.job = CoroutineScope(IO).launch {
                            startDownloadJob(downloadId)
                        }

                        emitNotificationResumeFact()
                    }

                    ACTION_CANCEL -> {
                        NotificationManagerCompat.from(context).cancel(
                            currentDownloadJobState.foregroundServiceId
                        )

                        setDownloadJobStatus(currentDownloadJobState, DownloadJobStatus.CANCELLED)
                        currentDownloadJobState.job?.cancel()

                        currentDownloadJobState.job = CoroutineScope(IO).launch {
                            deleteDownloadingFile(currentDownloadJobState.state)
                            currentDownloadJobState.downloadDeleted = true
                        }

                        emitNotificationCancelFact()
                    }

                    ACTION_TRY_AGAIN -> {
                        NotificationManagerCompat.from(context).cancel(
                            currentDownloadJobState.foregroundServiceId
                        )

                        setDownloadJobStatus(currentDownloadJobState, DownloadJobStatus.ACTIVE)

                        currentDownloadJobState.job = CoroutineScope(IO).launch {
                            startDownloadJob(downloadId)
                        }

                        emitNotificationTryAgainFact()
                    }

                    ACTION_OPEN -> {
                        if (!openFile(
                                context = context,
                                filePath = currentDownloadJobState.state.filePath,
                                contentType = currentDownloadJobState.state.contentType
                            )) {
                            Toast.makeText(applicationContext,
                                applicationContext.getString(R.string.mozac_feature_downloads_could_not_open_file),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        emitNotificationOpenFact()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val download = intent?.getDownloadExtra() ?: return START_REDELIVER_INTENT
        registerForUpdates()

        // If the job already exists, then don't create a new ID. This can happen when calling tryAgain
        val foregroundServiceId = downloadJobs[download.id]?.foregroundServiceId ?: Random.nextInt()

        // Create a new job and add it, with its downloadState to the map
        downloadJobs[download.id] = DownloadJobState(
            state = download,
            foregroundServiceId = foregroundServiceId,
            status = DownloadJobStatus.ACTIVE
        )

        downloadJobs[download.id]?.job = CoroutineScope(IO).launch {
            startDownloadJob(download.id)
        }

        notificationUpdateScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL)
                updateDownloadNotification()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /***
     * Android rate limits notifications being sent, so we must send them on a delay so that
     * notifications are not dropped
     */
    @Suppress("ComplexMethod")
    private fun updateDownloadNotification() {
        for (download in downloadJobs.values) {
            if (download.notifiedStopped) { continue }

            // Dispatch the corresponding notification based on the current status
            val notification = when (getDownloadJobStatus(download)) {
                DownloadJobStatus.ACTIVE -> {
                    DownloadNotification.createOngoingDownloadNotification(
                        context,
                        download.state,
                        download.currentBytesCopied
                    )
                }

                DownloadJobStatus.PAUSED -> {
                    DownloadNotification.createPausedDownloadNotification(context, download.state)
                }

                DownloadJobStatus.COMPLETED -> {
                    DownloadNotification.createDownloadCompletedNotification(context, download.state)
                }

                DownloadJobStatus.FAILED -> {
                    DownloadNotification.createDownloadFailedNotification(context, download.state)
                }

                DownloadJobStatus.CANCELLED -> { null }
            }

            notification?.let {
                NotificationManagerCompat.from(context).notify(
                    download.foregroundServiceId,
                    it
                )
            }

            if (getDownloadJobStatus(download) != DownloadJobStatus.ACTIVE) {
                sendDownloadStopped(download.state.id)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If the task gets removed (app gets swiped away in the task switcher) our process and this
        // service gets killed. In this situation we remove the notification since the download will
        // stop and cannot be controlled via the notification anymore (until we persist enough data
        // to resume downloads from a new process).

        val notificationManager = NotificationManagerCompat.from(context)

        downloadJobs.values.forEach { state ->
            notificationManager.cancel(state.foregroundServiceId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        downloadJobs.values.forEach {
            it.job?.cancel()
        }

        notificationUpdateScope.cancel()
    }

    internal fun startDownloadJob(downloadId: Long) {
        val currentDownloadJobState = downloadJobs[downloadId] ?: return

        try {
            performDownload(currentDownloadJobState.state)
        } catch (e: IOException) {
            setDownloadJobStatus(currentDownloadJobState, DownloadJobStatus.FAILED)
        }
    }

    internal fun deleteDownloadingFile(downloadState: DownloadState) {
        val downloadedFile = File(downloadState.filePath)
        downloadedFile.delete()
    }

    private fun registerForUpdates() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_CANCEL)
            addAction(ACTION_TRY_AGAIN)
            addAction(ACTION_OPEN)
        }

        context.registerReceiver(broadcastReceiver, filter)
    }

    @Suppress("ComplexCondition")
    internal fun performDownload(download: DownloadState) {
        val currentDownloadJobState = downloadJobs[download.id] ?: return
        val isResumingDownload = currentDownloadJobState.currentBytesCopied > 0L
        val headers = MutableHeaders()

        if (isResumingDownload) {
            headers.append(RANGE, "bytes=${currentDownloadJobState.currentBytesCopied}-")
        }

        val request = Request(download.url, headers = headers)
        val response = httpClient.fetch(request)

        // If we are resuming a download and the response does not contain a CONTENT_RANGE
        // we cannot be sure that the request will properly be handled
        if (response.status != PARTIAL_CONTENT_STATUS && response.status != OK_STATUS ||
            (isResumingDownload && !response.headers.contains(CONTENT_RANGE))) {
            // We experienced a problem trying to fetch the file, send a failure notification
            currentDownloadJobState.currentBytesCopied = 0
            setDownloadJobStatus(currentDownloadJobState, DownloadJobStatus.FAILED)
            return
        }

        response.body.useStream { inStream ->
            val newDownloadState = download.withResponse(response.headers, inStream)
            downloadJobs[download.id]?.state = newDownloadState

            useFileStream(newDownloadState, isResumingDownload) { outStream ->
                copyInChunks(downloadJobs[download.id]!!, inStream, outStream)
            }

            verifyDownload(downloadJobs[download.id]!!)
        }
    }

    /**
     * Updates the status of an ACTIVE download to completed or failed based on bytes copied
     */
    internal fun verifyDownload(download: DownloadJobState) {
        if (getDownloadJobStatus(download) == DownloadJobStatus.ACTIVE &&
            download.currentBytesCopied < download.state.contentLength ?: 0) {
            setDownloadJobStatus(download, DownloadJobStatus.FAILED)
        } else if (getDownloadJobStatus(download) == DownloadJobStatus.ACTIVE) {
            setDownloadJobStatus(download, DownloadJobStatus.COMPLETED)
        }
    }

    private fun copyInChunks(downloadJobState: DownloadJobState, inStream: InputStream, outStream: OutputStream) {
        val data = ByteArray(CHUNK_SIZE)

        // To ensure that we copy all files (even ones that don't have fileSize, we must NOT check < fileSize
        while (getDownloadJobStatus(downloadJobState) == DownloadJobStatus.ACTIVE) {
            val bytesRead = inStream.read(data)

            // If bytesRead is -1, there's no data left to read from the stream
            if (bytesRead == -1) { break }

            downloadJobState.currentBytesCopied += bytesRead

            outStream.write(data, 0, bytesRead)
        }
    }

    /**
     * Informs [mozilla.components.feature.downloads.manager.FetchDownloadManager] that a download
     * is no longer in progress due to being paused, completed, or failed
     */
    private fun sendDownloadStopped(downloadID: Long) {
        val downloadJob = downloadJobs[downloadID] ?: return
        downloadJob.notifiedStopped = true

        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_DOWNLOAD_STATUS, getDownloadJobStatus(downloadJob))
        intent.putExtra(EXTRA_DOWNLOAD, downloadJob.state)
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
        val downloadWithUniqueFileName = makeUniqueFileNameIfNecessary(download, append)
        downloadJobs[download.id]?.state = downloadWithUniqueFileName

        if (SDK_INT >= Build.VERSION_CODES.Q) {
            useFileStreamScopedStorage(downloadWithUniqueFileName, block)
        } else {
            useFileStreamLegacy(downloadWithUniqueFileName, append, block)
        }
    }

    /**
     * Returns an updated [DownloadState] with a unique fileName if the file is not being appended
     */
    @Suppress("Deprecation")
    internal fun makeUniqueFileNameIfNecessary(
        download: DownloadState,
        append: Boolean
    ): DownloadState {
        if (append) { return download }

        return download.fileName?.let {
            download.copy(fileName = DownloadUtils.uniqueFileName(
                Environment.getExternalStoragePublicDirectory(download.destinationDirectory),
                it
            ))
        } ?: download
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
    private fun useFileStreamLegacy(download: DownloadState, append: Boolean, block: (OutputStream) -> Unit) {
        val dir = Environment.getExternalStoragePublicDirectory(download.destinationDirectory)
        val file = File(dir, download.fileName!!)

        FileOutputStream(file, append).use(block)

        val downloadJobState = downloadJobs[download.id] ?: return
        if (getDownloadJobStatus(downloadJobState) != DownloadJobStatus.COMPLETED) { return }

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
        /**
         * Launches an intent to open the given file, returns whether or not the file could be opened
         */
        fun openFile(context: Context, filePath: String, contentType: String?): Boolean {
            // Create a new file with the location of the saved file to extract the correct path
            // `file` has the wrong path, so we must construct it based on the `fileName` and `dir.path`s
            val fileLocation = File(filePath)
            val constructedFilePath = FileProvider.getUriForFile(
                context,
                context.packageName + FILE_PROVIDER_EXTENSION,
                fileLocation
            )

            val newIntent = Intent(ACTION_VIEW).apply {
                setDataAndType(constructedFilePath, contentType ?: "*/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            return try {
                context.startActivity(newIntent)
                true
            } catch (error: ActivityNotFoundException) {
                false
            }
        }

        private const val FILE_PROVIDER_EXTENSION = ".feature.downloads.fileprovider"
        private const val CHUNK_SIZE = 4 * 1024
        private const val PARTIAL_CONTENT_STATUS = 206
        private const val OK_STATUS = 200
        /**
         * This interval was decided on by balancing the limit of the system (200ms) and allowing
         * users to press buttons on the notification. If a new notification is presented while a
         * user is tapping a button, their press will be cancelled.
         */
        private const val PROGRESS_UPDATE_INTERVAL = 750L

        const val EXTRA_DOWNLOAD = "mozilla.components.feature.downloads.extras.DOWNLOAD"
        const val EXTRA_DOWNLOAD_STATUS = "mozilla.components.feature.downloads.extras.DOWNLOAD_STATUS"
        const val ACTION_OPEN = "mozilla.components.feature.downloads.OPEN"
        const val ACTION_PAUSE = "mozilla.components.feature.downloads.PAUSE"
        const val ACTION_RESUME = "mozilla.components.feature.downloads.RESUME"
        const val ACTION_CANCEL = "mozilla.components.feature.downloads.CANCEL"
        const val ACTION_TRY_AGAIN = "mozilla.components.feature.downloads.TRY_AGAIN"
    }
}
