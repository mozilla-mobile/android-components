/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import mozilla.components.feature.media.ext.toPlaybackState
import mozilla.components.feature.media.notification.MediaNotification
import mozilla.components.feature.media.notification.MediaNotificationFeature
import mozilla.components.feature.media.session.MediaSessionCallback
import mozilla.components.feature.media.state.MediaState
import mozilla.components.support.base.ids.NotificationIds
import mozilla.components.support.base.log.logger.Logger

private const val NOTIFICATION_TAG = "mozac.feature.media.foreground-service"

/**
 * A foreground service that will keep the process alive while we are playing media (with the app possibly in the
 * background) and shows an ongoing notification
 */
internal class MediaService : Service() {
    private val logger = Logger("MediaService")
    private val notification = MediaNotification(this)
    private val mediaSession by lazy { MediaSessionCompat(this, "MozacMedia") }

    override fun onCreate() {
        super.onCreate()

        logger.debug("Service created")

        mediaSession.setCallback(MediaSessionCallback())

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("Command received")

        processCurrentState()

        return START_NOT_STICKY
    }

    private fun processCurrentState() {
        // The current state is currently hold by the notification feature. We should either turn
        // this into a generic media feature of keep the state somewhere else.
        val state = MediaNotificationFeature.getState()
        if (state == MediaState.None) {
            shutdown()
            return
        }

        if (state is MediaState.Playing) {
            requestAudioFocus(this)
        }

        updateMediaSession()
        updateNotification(state)
    }

    private fun updateMediaSession() {
        mediaSession.setPlaybackState(MediaNotificationFeature
            .getState()
            .toPlaybackState())
        mediaSession.isActive = true
    }

    private fun updateNotification(state: MediaState) {
        val notificationId = NotificationIds.getIdForTag(this, NOTIFICATION_TAG)
        startForeground(notificationId, notification.create(state, mediaSession))
    }

    private fun shutdown() {
        mediaSession.release()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private fun requestAudioFocus(context: Context) {
    // This implementation currently only request focus and does not manage audio focus correctly.
    // See: https://github.com/mozilla-mobile/android-components/issues/3935

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        @Suppress("DEPRECATION")
        audioManager.requestAudioFocus({}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    } else {
        audioManager.requestAudioFocus(
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build())
    }
}
