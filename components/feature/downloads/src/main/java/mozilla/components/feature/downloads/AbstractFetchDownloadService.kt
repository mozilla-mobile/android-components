/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.annotation.TargetApi
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.app.Notification
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import android.webkit.MimeTypeMap
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
import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Headers.Names.CONTENT_RANGE
import mozilla.components.concept.fetch.Headers.Names.RANGE
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus.CANCELLED
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus.COMPLETED
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus.ACTIVE
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus.FAILED
import mozilla.components.feature.downloads.AbstractFetchDownloadService.DownloadJobStatus.PAUSED
import mozilla.components.feature.downloads.DownloadNotification.NOTIFICATION_DOWNLOAD_GROUP_ID
import mozilla.components.feature.downloads.ext.addCompletedDownload
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
import java.lang.IllegalStateException
import kotlin.random.Random

/**
 * Service that performs downloads through a fetch [Client] rather than through the native
 * Android download manager.
 *
 * To use this service, you must create a subclass in your application and add it to the manifest.
 */
@Suppress("TooManyFunctions", "LargeClass")
abstract class AbstractFetchDownloadService : Service() {
    protected abstract val store: BrowserStore

    private val notificationUpdateScope = MainScope()

    protected abstract val httpClient: Client
    @VisibleForTesting
    internal val broadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    @VisibleForTesting
    internal val context: Context get() = this
    @VisibleForTesting
    internal var compatForegroundNotificationId: Int = COMPAT_DEFAULT_FOREGROUND_ID

    internal var downloadJobs = mutableMapOf<Long, DownloadJobState>()

    // TODO Move this to browser store and make immutable:
    // https://github.com/mozilla-mobile/android-components/issues/7050
    internal data class DownloadJobState(
        var job: Job? = null,
        @Volatile var state: DownloadState,
        var currentBytesCopied: Long = 0,
        @GuardedBy("context") var status: DownloadJobStatus,
        var foregroundServiceId: Int = 0,
        var downloadDeleted: Boolean = false,
        var notifiedStopped: Boolean = false,
        var lastNotificationUpdate: Long = 0L,
        var createdTime: Long = System.currentTimeMillis()
    ) {
        internal fun canUpdateNotification(): Boolean {
            return isUnderNotificationUpdateLimit() && !notifiedStopped
        }

        /**
         * Android imposes a limit on of how often we can send updates for a notification.
         * The limit is one second per update.
         * See https://developer.android.com/training/notify-user/build-notification.html#Updating
         * This function indicates if we are under that limit.
         */
        internal fun isUnderNotificationUpdateLimit(): Boolean {
            return getSecondsSinceTheLastNotificationUpdate() >= 1
        }

        @Suppress("MagicNumber")
        internal fun getSecondsSinceTheLastNotificationUpdate(): Long {
            return (System.currentTimeMillis() - lastNotificationUpdate) / 1000
        }
    }

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
                        setDownloadJobStatus(currentDownloadJobState, PAUSED)
                        currentDownloadJobState.job?.cancel()
                        emitNotificationPauseFact()
                    }

                    ACTION_RESUME -> {
                        setDownloadJobStatus(currentDownloadJobState, ACTIVE)

                        currentDownloadJobState.job = CoroutineScope(IO).launch {
                            startDownloadJob(currentDownloadJobState)
                        }

                        emitNotificationResumeFact()
                    }

                    ACTION_CANCEL -> {
                        removeNotification(context, currentDownloadJobState)
                        currentDownloadJobState.lastNotificationUpdate = System.currentTimeMillis()
                        setDownloadJobStatus(currentDownloadJobState, CANCELLED)
                        currentDownloadJobState.job?.cancel()

                        currentDownloadJobState.job = CoroutineScope(IO).launch {
                            deleteDownloadingFile(currentDownloadJobState.state)
                            currentDownloadJobState.downloadDeleted = true
                        }

                        removeDownloadJob(currentDownloadJobState)
                        emitNotificationCancelFact()
                    }

                    ACTION_TRY_AGAIN -> {
                        removeNotification(context, currentDownloadJobState)
                        currentDownloadJobState.lastNotificationUpdate = System.currentTimeMillis()
                        setDownloadJobStatus(currentDownloadJobState, ACTIVE)

                        currentDownloadJobState.job = CoroutineScope(IO).launch {
                            startDownloadJob(currentDownloadJobState)
                        }

                        emitNotificationTryAgainFact()
                    }

                    ACTION_DISMISS -> removeDownloadJob(currentDownloadJobState)

                    ACTION_OPEN -> {
                        if (!openFile(
                                context = context,
                                filePath = currentDownloadJobState.state.filePath,
                                contentType = currentDownloadJobState.state.contentType
                            )) {
                            val fileExt = MimeTypeMap.getFileExtensionFromUrl(
                                    currentDownloadJobState.state.filePath.toString()
                            )
                            val errorMessage = applicationContext.getString(
                                    R.string.mozac_feature_downloads_open_not_supported, fileExt
                            )

                            Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                        }

                        emitNotificationOpenFact()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerNotificationActionsReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val download = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1)?.let {
            store.state.queuedDownloads[it]
        } ?: return START_REDELIVER_INTENT

        // If the job already exists, then don't create a new ID. This can happen when calling tryAgain
        val foregroundServiceId = downloadJobs[download.id]?.foregroundServiceId ?: Random.nextInt()

        // Create a new job and add it, with its downloadState to the map
        val downloadJobState = DownloadJobState(
            state = download,
            foregroundServiceId = foregroundServiceId,
            status = ACTIVE
        )

        downloadJobState.job = CoroutineScope(IO).launch {
            startDownloadJob(downloadJobState)
        }

        downloadJobs[download.id] = downloadJobState

        setForegroundNotification(downloadJobState)

        notificationUpdateScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL)
                updateDownloadNotification()
                if (downloadJobs.isEmpty()) cancel()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Android rate limits notifications being sent, so we must send them on a delay so that
     * notifications are not dropped
     */
    @Suppress("ComplexMethod")
    private fun updateDownloadNotification() {
        for (download in downloadJobs.values) {
            if (!download.canUpdateNotification()) { continue }
            /*
            * We want to keep a consistent state in the UI, download.status can be changed from
            * another thread while we are posting updates to the UI, causing inconsistent UIs.
            * For this reason, we ONLY use the latest status during an UI update, new changes
            * will be posted in subsequent updates.
            */
            val uiStatus = getDownloadJobStatus(download)

            updateForegroundNotificationIfNeeded(download)

            // Dispatch the corresponding notification based on the current status
            updateDownloadNotification(uiStatus, download)

            if (uiStatus != ACTIVE) {
                sendDownloadStopped(download)
            }
        }
    }

    /**
     * Updates the notification state with the passed [download] data.
     * Be aware that you need to pass [latestUIStatus] as [DownloadJobState.status] can be modified
     * from another thread, causing inconsistencies in the ui.
     */
    private fun updateDownloadNotification(latestUIStatus: DownloadJobStatus, download: DownloadJobState) {
        val notification = when (latestUIStatus) {
            ACTIVE -> DownloadNotification.createOngoingDownloadNotification(context, download)
            PAUSED -> DownloadNotification.createPausedDownloadNotification(context, download)
            COMPLETED -> DownloadNotification.createDownloadCompletedNotification(context, download)
            FAILED -> DownloadNotification.createDownloadFailedNotification(context, download)
            CANCELLED -> {
                removeNotification(context, download)
                download.lastNotificationUpdate = System.currentTimeMillis()
                null
            }
        }

        notification?.let {
            NotificationManagerCompat.from(context).notify(download.foregroundServiceId, it)
            download.lastNotificationUpdate = System.currentTimeMillis()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()

        clearAllDownloadsNotificationsAndJobs()
        unregisterNotificationActionsReceiver()
    }

    // Cancels all running jobs and remove all notifications.
    // Also cleans any resources that we were holding like broadcastReceivers
    internal fun clearAllDownloadsNotificationsAndJobs() {
        val notificationManager = NotificationManagerCompat.from(context)

        stopForeground(true)
        compatForegroundNotificationId = COMPAT_DEFAULT_FOREGROUND_ID

        // Before doing any cleaning, we have to stop the notification updater scope.
        // To ensure we are not recreating the notifications.
        notificationUpdateScope.cancel()

        downloadJobs.values.forEach { state ->
            notificationManager.cancel(state.foregroundServiceId)
            state.job?.cancel()
        }
    }

    internal fun startDownloadJob(currentDownloadJobState: DownloadJobState) {
        try {
            performDownload(currentDownloadJobState)
        } catch (e: IOException) {
            setDownloadJobStatus(currentDownloadJobState, FAILED)
        }
    }

    internal fun deleteDownloadingFile(downloadState: DownloadState) {
        val downloadedFile = File(downloadState.filePath)
        downloadedFile.delete()
    }

    @VisibleForTesting
    internal fun registerNotificationActionsReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_CANCEL)
            addAction(ACTION_DISMISS)
            addAction(ACTION_TRY_AGAIN)
            addAction(ACTION_OPEN)
        }

        context.registerReceiver(broadcastReceiver, filter)
    }

    @VisibleForTesting
    internal fun unregisterNotificationActionsReceiver() {
        context.unregisterReceiver(broadcastReceiver)
    }

    @VisibleForTesting
    internal fun removeDownloadJob(downloadJobState: DownloadJobState) {
        downloadJobs.remove(downloadJobState.state.id)
        store.dispatch(DownloadAction.RemoveQueuedDownloadAction(downloadJobState.state.id))
        if (downloadJobs.isEmpty()) {
            stopSelf()
        } else {
            updateForegroundNotificationIfNeeded(downloadJobState)
            removeNotification(context, downloadJobState)
        }
    }

    @VisibleForTesting
    internal fun removeNotification(context: Context, currentDownloadJobState: DownloadJobState) {
        NotificationManagerCompat.from(context).cancel(currentDownloadJobState.foregroundServiceId)
    }

    /**
     * Refresh the notification group content only for devices that support it,
     * otherwise nothing will happen.
     */
    @VisibleForTesting
    internal fun updateNotificationGroup(): Notification? {
        return if (SDK_INT >= Build.VERSION_CODES.N) {
            val downloadList = downloadJobs.values.toList()
            val notificationGroup = DownloadNotification.createDownloadGroupNotification(context, downloadList)
            NotificationManagerCompat.from(context).apply {
                notify(NOTIFICATION_DOWNLOAD_GROUP_ID, notificationGroup)
            }
            notificationGroup
        } else {
            null
        }
    }

    internal fun createCompactForegroundNotification(downloadJobState: DownloadJobState): Notification {
        val notification = DownloadNotification.createOngoingDownloadNotification(context, downloadJobState)
        compatForegroundNotificationId = downloadJobState.foregroundServiceId
        NotificationManagerCompat.from(context).apply {
            notify(compatForegroundNotificationId, notification)
            downloadJobState.lastNotificationUpdate = System.currentTimeMillis()
        }
        return notification
    }

    @VisibleForTesting
    internal fun getForegroundId(): Int {
        return if (SDK_INT >= Build.VERSION_CODES.N) {
            NOTIFICATION_DOWNLOAD_GROUP_ID
        } else {
            compatForegroundNotificationId
        }
    }

    /**
     * We have two different behaviours as notification groups are not supported in all devices.
     * For devices that support it, we create a separate notification which will be the foreground
     * notification, it will be always present until we don't have more active downloads.
     * For devices that doesn't support notification groups, we set the latest active notification as
     * the foreground notification and we keep changing it to the latest active download.
     */
    @VisibleForTesting
    internal fun setForegroundNotification(downloadJobState: DownloadJobState) {
        var previousDownload: DownloadJobState? = null

        val (notificationId, notification) = if (SDK_INT >= Build.VERSION_CODES.N) {
            NOTIFICATION_DOWNLOAD_GROUP_ID to updateNotificationGroup()
        } else {
            previousDownload = downloadJobs.values.firstOrNull {
                it.foregroundServiceId == compatForegroundNotificationId
            }
            downloadJobState.foregroundServiceId to createCompactForegroundNotification(downloadJobState)
        }

        startForeground(notificationId, notification)
        /**
         * In devices that doesn't use notification groups, every new download becomes the new foreground one,
         * unfortunately, when we call startForeground it removes the previous foreground notification
         * when it's not an ongoing one, for this reason, we have to recreate the deleted notification.
         * By the way ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
         * doesn't work neither calling stopForeground(false) and then calling startForeground,
         * it always deletes the previous notification :(
         */
        previousDownload?.let {
            updateDownloadNotification(previousDownload.status, it)
        }
    }

    /**
     * Indicates the status of a download has changed and maybe the foreground notification needs,
     * to be updated. For devices that support group notifications, we update the overview
     * notification. For devices that don't support group notifications, we try to find a new
     * active download and selected it as the new foreground notification.
     */
    internal fun updateForegroundNotificationIfNeeded(download: DownloadJobState) {
        if (SDK_INT < Build.VERSION_CODES.N) {
            /**
             * For devices that don't support notification groups, we have to keep updating
             * the foreground notification id, when the previous one gets a state that
             * is likely to be dismissed.
             */
            val status = download.status
            val foregroundId = download.foregroundServiceId
            val isSelectedForegroundId = compatForegroundNotificationId == foregroundId
            val needNewForegroundNotification = when (status) {
                COMPLETED, FAILED, CANCELLED -> true
                else -> false
            }

            if (isSelectedForegroundId && needNewForegroundNotification) {
                // We need to deselect the actual foreground notification, because while it is
                // selected the user will not be able to dismiss it.
                stopForeground(false)

                // Now we need to find a new foreground notification, if needed.
                val newSelectedForegroundDownload = downloadJobs.values.firstOrNull { it.status == ACTIVE }
                newSelectedForegroundDownload?.let {
                    setForegroundNotification(it)
                }
            }
        } else {
            // This device supports notification groups, we just need to update the summary notification
            updateNotificationGroup()
        }
    }

    @Suppress("ComplexCondition")
    internal fun performDownload(currentDownloadJobState: DownloadJobState) {
        val download = currentDownloadJobState.state
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
            setDownloadJobStatus(currentDownloadJobState, FAILED)
            return
        }

        response.body.useStream { inStream ->
            val newDownloadState = download.withResponse(response.headers, inStream)
            currentDownloadJobState.state = newDownloadState

            useFileStream(newDownloadState, isResumingDownload) { outStream ->
                copyInChunks(currentDownloadJobState, inStream, outStream)
            }

            verifyDownload(currentDownloadJobState)
        }
    }

    /**
     * Updates the status of an ACTIVE download to completed or failed based on bytes copied
     */
    internal fun verifyDownload(download: DownloadJobState) {
        if (getDownloadJobStatus(download) == DownloadJobStatus.ACTIVE &&
            download.currentBytesCopied < download.state.contentLength ?: 0) {
            setDownloadJobStatus(download, FAILED)
        } else if (getDownloadJobStatus(download) == DownloadJobStatus.ACTIVE) {
            setDownloadJobStatus(download, COMPLETED)
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
    private fun sendDownloadStopped(downloadState: DownloadJobState) {
        downloadState.notifiedStopped = true

        val intent = Intent(ACTION_DOWNLOAD_COMPLETE)
        intent.putExtra(EXTRA_DOWNLOAD_STATUS, getDownloadJobStatus(downloadState))
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadState.state.id)

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

        // Query if we have a pending download with the same name. This can happen
        // if a download was interrupted, failed or cancelled before the file was
        // written to disk. Our logic above will have generated a unique file name
        // based on existing files on the device, but we might already have a row
        // for the download in the content resolver.
        var downloadUri: Uri? = null
        resolver.query(
            MediaStore.setIncludePending(collection),
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf("${download.fileName}"),
            null
        )?.use {
            if (it.count > 0) {
                val idColumnIndex = it.getColumnIndex(MediaStore.Downloads._ID)
                it.moveToFirst()
                downloadUri = ContentUris.withAppendedId(collection, it.getLong(idColumnIndex))
            }
        }

        if (downloadUri == null) {
            downloadUri = resolver.insert(collection, values)
        }

        downloadUri?.let {
            val pfd = resolver.openFileDescriptor(it, "w")
            ParcelFileDescriptor.AutoCloseOutputStream(pfd).use(block)

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, values, null, null)
        } ?: throw IOException("Failed to register download with content resolver")
    }

    @TargetApi(Build.VERSION_CODES.P)
    @Suppress("Deprecation")
    private fun useFileStreamLegacy(download: DownloadState, append: Boolean, block: (OutputStream) -> Unit) {
        val fileName = download.fileName ?: throw IllegalStateException("A fileName for a download is required")
        val dir = Environment.getExternalStoragePublicDirectory(download.destinationDirectory)
        val file = File(dir, fileName)

        FileOutputStream(file, append).use(block)

        val downloadJobState = downloadJobs[download.id] ?: return
        if (getDownloadJobStatus(downloadJobState) != COMPLETED) { return }

        addCompletedDownload(
            title = fileName,
            description = fileName,
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
        internal const val PROGRESS_UPDATE_INTERVAL = 750L

        const val EXTRA_DOWNLOAD_STATUS = "mozilla.components.feature.downloads.extras.DOWNLOAD_STATUS"
        const val ACTION_OPEN = "mozilla.components.feature.downloads.OPEN"
        const val ACTION_PAUSE = "mozilla.components.feature.downloads.PAUSE"
        const val ACTION_RESUME = "mozilla.components.feature.downloads.RESUME"
        const val ACTION_CANCEL = "mozilla.components.feature.downloads.CANCEL"
        const val ACTION_DISMISS = "mozilla.components.feature.downloads.DISMISS"
        const val ACTION_TRY_AGAIN = "mozilla.components.feature.downloads.TRY_AGAIN"
        const val COMPAT_DEFAULT_FOREGROUND_ID = -1
    }
}
