/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import mozilla.components.browser.session.Session
import mozilla.components.feature.media.R
import mozilla.components.feature.media.service.MediaService
import mozilla.components.feature.media.state.MediaState

private const val NOTIFICATION_CHANNEL_ID = "Media"

/**
 * Helper to display a notification for web content playing media.
 */
internal class MediaNotification(
    private val context: Context
) {
    /**
     * Creates a new [Notification] for the given [state].
     */
    fun create(state: MediaState, mediaSession: MediaSessionCompat): Notification {
        MediaNotificationChannel.ensureChannelExists(context)

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val data = state.toNotificationData(context)

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(data.icon)
            .setContentTitle(data.title)
            .setContentText(data.description)
            .setContentIntent(pendingIntent)
            .setLargeIcon(data.largeIcon)
            .addAction(data.action)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0))
            .build()
    }
}

@Suppress("LongMethod")
private fun MediaState.toNotificationData(context: Context): NotificationData {
    return when (this) {
        is MediaState.Playing -> NotificationData(
            title = session.titleOrUrl,
            description = session.url,
            icon = R.drawable.mozac_feature_media_playing,
            largeIcon = session.icon,
            action = NotificationCompat.Action.Builder(
                R.drawable.mozac_feature_media_action_pause,
                context.getString(R.string.mozac_feature_media_notification_action_pause),
                PendingIntent.getService(
                    context,
                    0,
                    MediaService.pauseIntent(context),
                    0)
            ).build()
        )
        is MediaState.Paused -> NotificationData(
            title = session.titleOrUrl,
            description = session.url,
            icon = R.drawable.mozac_feature_media_paused,
            largeIcon = session.icon,
            action = NotificationCompat.Action.Builder(
                R.drawable.mozac_feature_media_action_play,
                context.getString(R.string.mozac_feature_media_notification_action_play),
                PendingIntent.getService(
                    context,
                    0,
                    MediaService.playIntent(context),
                    0)
            ).build()
        )
        else -> throw IllegalArgumentException("Cannot create notification for state: $this")
    }
}

private val Session.titleOrUrl
    get() = if (title.isNotEmpty()) title else url

private data class NotificationData(
    val title: String,
    val description: String,
    @DrawableRes val icon: Int,
    val largeIcon: Bitmap? = null,
    val action: NotificationCompat.Action
)
