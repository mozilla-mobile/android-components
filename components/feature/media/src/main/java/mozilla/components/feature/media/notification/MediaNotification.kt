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
import mozilla.components.feature.media.MediaFeature
import mozilla.components.feature.media.R
import mozilla.components.feature.media.ext.getTitleOrUrl
import mozilla.components.feature.media.ext.nonPrivateIcon
import mozilla.components.feature.media.ext.nonPrivateUrl
import mozilla.components.feature.media.service.MediaService
import mozilla.components.feature.media.state.MediaState
import mozilla.components.support.base.ids.SharedIdsHelper

/**
 * Helper to display a notification for web content playing media.
 */
internal class MediaNotification(
    private val context: Context
) {
    /**
     * Creates a new [Notification] for the given [state].
     */
    @Suppress("LongMethod")
    fun create(state: MediaState, mediaSession: MediaSessionCompat): Notification {
        val channel = MediaNotificationChannel.ensureChannelExists(context)

        val data = state.toNotificationData(context)

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(data.icon)
            .setContentTitle(data.title)
            .setContentText(data.description)
            .setLargeIcon(data.largeIcon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)

        if (data.action != null) {
            builder.addAction(data.action)

            style.setShowActionsInCompactView(0)
        }

        builder.setStyle(style)

        if (!state.isForExternalApp()) {
            // We only set a content intent if this media notification is not for an "external app"
            // like a custom tab. Currently we can't route the user to that particular activity:
            // https://github.com/mozilla-mobile/android-components/issues/3986
            builder.setContentIntent(data.contentIntent)
        }

        return builder.build()
    }
}

private fun MediaState.toNotificationData(context: Context): NotificationData {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.also {
        it.action = MediaFeature.ACTION_SWITCH_TAB
    }

    return when (this) {
        is MediaState.Playing -> NotificationData(
            title = session.getTitleOrUrl(context),
            description = session.nonPrivateUrl,
            icon = R.drawable.mozac_feature_media_playing,
            largeIcon = session.nonPrivateIcon,
            action = NotificationCompat.Action.Builder(
                R.drawable.mozac_feature_media_action_pause,
                context.getString(R.string.mozac_feature_media_notification_action_pause),
                PendingIntent.getService(
                    context,
                    0,
                    MediaService.pauseIntent(context),
                    0)
            ).build(),
            contentIntent = PendingIntent.getActivity(context,
                SharedIdsHelper.getIdForTag(context, MediaFeature.PENDING_INTENT_TAG),
                intent?.apply { putExtra(MediaFeature.EXTRA_TAB_ID, session.id) }, 0)
        )
        is MediaState.Paused -> NotificationData(
            title = session.getTitleOrUrl(context),
            description = session.nonPrivateUrl,
            icon = R.drawable.mozac_feature_media_paused,
            largeIcon = session.nonPrivateIcon,
            action = NotificationCompat.Action.Builder(
                R.drawable.mozac_feature_media_action_play,
                context.getString(R.string.mozac_feature_media_notification_action_play),
                PendingIntent.getService(
                    context,
                    0,
                    MediaService.playIntent(context),
                    0)
            ).build(),
            contentIntent = PendingIntent.getActivity(context,
                SharedIdsHelper.getIdForTag(context, MediaFeature.PENDING_INTENT_TAG),
                intent?.apply { putExtra(MediaFeature.EXTRA_TAB_ID, session.id) }, 0)
        )
        // Dummy notification that is only used to satisfy the requirement to ALWAYS call
        // startForeground with a notification.
        else -> NotificationData()
    }
}

private data class NotificationData(
    val title: String = "",
    val description: String = "",
    @DrawableRes val icon: Int = R.drawable.mozac_feature_media_playing,
    val largeIcon: Bitmap? = null,
    val action: NotificationCompat.Action? = null,
    val contentIntent: PendingIntent? = null
)

private fun MediaState.isForExternalApp(): Boolean {
    return when (this) {
        is MediaState.Playing -> session.isCustomTabSession()
        is MediaState.Paused -> session.isCustomTabSession()
        is MediaState.None -> false
    }
}
