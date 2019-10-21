/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.app.DownloadManager.ACTION_VIEW_DOWNLOADS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_NONE
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.feature.downloads.AbstractFetchDownloadService.Companion.ACTION_CANCEL
import mozilla.components.feature.downloads.AbstractFetchDownloadService.Companion.ACTION_PAUSE
import mozilla.components.feature.downloads.AbstractFetchDownloadService.Companion.ACTION_RESUME
import kotlin.random.Random

internal object DownloadNotification {

    private const val NOTIFICATION_CHANNEL_ID = "Downloads"
    internal const val EXTRA_DOWNLOAD_ID = "downloadId"

    /**
     * Build the notification to be displayed while the download service is active.
     */
    fun createOngoingDownloadNotification(context: Context, downloadState: DownloadState?): NotificationCompat.Builder {
        val channelId = ensureChannelExists(context)

        val fileSizeText = (downloadState?.contentLength?.toMegabyteString() ?: "")

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mozac_feature_download_ic_ongoing_download)
            .setContentTitle(downloadState?.fileName)
            .setContentText(fileSizeText)
            .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(1, 0, false)
            .setOngoing(true)
            .addAction(getPauseAction(context, downloadState?.id ?: -1))
            .addAction(getCancelAction(context, downloadState?.id ?: -1))
    }

    /**
     * Build the notification to be displayed while the download service is active.
     */
    fun createPausedDownloadNotification(context: Context, downloadState: DownloadState?): Notification {
        val channelId = ensureChannelExists(context)

        return NotificationCompat.Builder(context, channelId)
                // TODO: Set the drawable to be a pause icon
                .setSmallIcon(R.drawable.mozac_feature_download_ic_download)
                .setContentTitle(downloadState?.fileName)
                .setContentText(context.getString(R.string.mozac_feature_downloads_paused_notification_text))
                .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOngoing(true)
                // TODO: What's the best way to handle a null ID here...?
                .addAction(getResumeAction(context, downloadState?.id ?: -1))
                .addAction(getCancelAction(context, downloadState?.id ?: -1))
                .build()
    }

    /**
     * Build the notification to be displayed when a download finishes.
     */
    fun createDownloadCompletedNotification(context: Context, fileName: String?): Notification {
        // TODO: Pass downloadState in here?
        val channelId = ensureChannelExists(context)
        val intent = Intent(ACTION_VIEW_DOWNLOADS).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mozac_feature_download_ic_download)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.mozac_feature_downloads_completed_notification_text2))
            .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
            .build()
    }

    /**
     * Build the notification to be displayed when a download fails to finish.
     */
    fun createDownloadFailedNotification(context: Context, fileName: String?): Notification {
        // TODO: Pass downloadState in here?
        val channelId = ensureChannelExists(context)

        // TODO: Add the try again button?
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.mozac_feature_download_ic_download)
            .setContentTitle(fileName)
            .setContentText(context.getString(R.string.mozac_feature_downloads_failed_notification_text2))
            .setColor(ContextCompat.getColor(context, R.color.mozac_feature_downloads_notification))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
    }

    /**
     * Check if notifications from the download channel are enabled.
     * Verifies that app notifications, channel notifications, and group notifications are enabled.
     */
    fun isChannelEnabled(context: Context): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = context.getSystemService()!!
            if (!notificationManager.areNotificationsEnabled()) return false

            val channelId = ensureChannelExists(context)
            val channel = notificationManager.getNotificationChannel(channelId)
            if (channel.importance == IMPORTANCE_NONE) return false

            if (SDK_INT >= Build.VERSION_CODES.P) {
                val group = notificationManager.getNotificationChannelGroup(channel.group)
                group?.isBlocked != true
            } else {
                true
            }
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Make sure a notification channel for download notification exists.
     *
     * Returns the channel id to be used for download notifications.
     */
    private fun ensureChannelExists(context: Context): String {
        if (SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = context.getSystemService()!!

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.mozac_feature_downloads_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(channel)
        }

        return NOTIFICATION_CHANNEL_ID
    }

    private fun getPauseAction(context: Context, downloadStateId: Long): NotificationCompat.Action {
        val pauseIntent = createPendingIntent(context, ACTION_PAUSE, downloadStateId)

        // Tell us which download id to pause
        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.mozac_feature_downloads_button_pause),
            pauseIntent
        ).build()
    }

    private fun getResumeAction(context: Context, downloadStateId: Long): NotificationCompat.Action {
        val resumeIntent = createPendingIntent(context, ACTION_RESUME, downloadStateId)

        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.mozac_feature_downloads_button_resume),
            resumeIntent
        ).build()
    }

    private fun getCancelAction(context: Context, downloadStateId: Long): NotificationCompat.Action {
        val cancelIntent = createPendingIntent(context, ACTION_CANCEL, downloadStateId)

        return NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.mozac_feature_downloads_button_cancel),
            cancelIntent
        ).build()
    }

    private fun createPendingIntent(context: Context, action: String, downloadStateId: Long): PendingIntent {
        val intent = Intent(action)
        intent.setPackage(context.applicationContext.packageName)

        val bundleExtra = Bundle()
        bundleExtra.putLong(EXTRA_DOWNLOAD_ID, downloadStateId)
        intent.putExtras(bundleExtra)

        // Need distinct PendingIntent objects: https://developer.android.com/reference/android/app/PendingIntent.html
        // TODO: Might need to "save" the bundle data somehow when the notification is created... it only seems to persist the most recent intent's bundle data
        return PendingIntent.getBroadcast(context.applicationContext, Random.nextInt(), intent, 0)
    }
}
