/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import mozilla.components.feature.media.MediaFeature.Companion.NOTIFICATION_TAG
import mozilla.components.feature.media.ext.getSession
import mozilla.components.feature.media.ext.getTitleOrUrl
import mozilla.components.feature.media.ext.isForCustomTabSession
import mozilla.components.feature.media.ext.pauseIfPlaying
import mozilla.components.feature.media.ext.playIfPaused
import mozilla.components.feature.media.ext.toPlaybackState
import mozilla.components.feature.media.facts.emitNotificationPauseFact
import mozilla.components.feature.media.facts.emitNotificationPlayFact
import mozilla.components.feature.media.focus.AudioFocus
import mozilla.components.feature.media.notification.MediaNotification
import mozilla.components.feature.media.session.MediaSessionCallback
import mozilla.components.feature.media.state.MediaState
import mozilla.components.feature.media.state.MediaStateMachine
import mozilla.components.support.base.ids.SharedIdsHelper
import mozilla.components.support.base.log.logger.Logger

private const val ACTION_UPDATE_STATE = "mozac.feature.media.service.UPDATE_STATE"
private const val ACTION_PLAY = "mozac.feature.media.service.PLAY"
private const val ACTION_PAUSE = "mozac.feature.media.service.PAUSE"

/**
 * A foreground service that will keep the process alive while we are playing media (with the app possibly in the
 * background) and shows an ongoing notification
 */
internal class MediaService : Service() {
    private val logger = Logger("MediaService")
    private val notification = MediaNotification(this)
    private val mediaSession by lazy { MediaSessionCompat(this, "MozacMedia") }
    private val audioFocus by lazy { AudioFocus(getSystemService(Context.AUDIO_SERVICE) as AudioManager) }

    override fun onCreate() {
        super.onCreate()

        logger.debug("Service created")

        mediaSession.setCallback(MediaSessionCallback())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("Command received: ${intent?.action}")

        when (intent?.action) {
            ACTION_UPDATE_STATE -> processCurrentState()
            ACTION_PLAY -> {
                MediaStateMachine.state.playIfPaused()
                emitNotificationPlayFact()
            }
            ACTION_PAUSE -> {
                MediaStateMachine.state.pauseIfPlaying()
                emitNotificationPauseFact()
            }
            else -> logger.debug("Can't process action: ${intent?.action}")
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val state = MediaStateMachine.state

        if (state.isForCustomTabSession()) {
            // Custom Tabs have their own lifetime management (bound to the liftime of the activity)
            // and do not need to be handled here.
            return
        }

        MediaStateMachine.reset()

        state.pauseIfPlaying()

        shutdown()
    }

    private fun processCurrentState() {
        val state = MediaStateMachine.state

        if (state == MediaState.None) {
            updateNotification(state)
            shutdown()
            return
        }

        if (state is MediaState.Playing) {
            audioFocus.request(state)
        }

        updateMediaSession(state)
        updateNotification(state)
    }

    private fun updateMediaSession(state: MediaState) {
        mediaSession.setPlaybackState(state.toPlaybackState())
        mediaSession.isActive = true
        mediaSession.setMetadata(MediaMetadataCompat.Builder()
            .putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                state.getSession().getTitleOrUrl(this))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, -1)
            .build())
    }

    private fun updateNotification(state: MediaState) {
        val notificationId = SharedIdsHelper.getIdForTag(this, NOTIFICATION_TAG)

        val notification = notification.create(state, mediaSession)

        // Android wants us to always, always, ALWAYS call startForeground() if
        // startForegroundService() was invoked. Even if we already did that for this service or
        // if we are stopping foreground or stopping the whole service. No matter what. Always
        // call startForeground().
        startForeground(notificationId, notification)

        if (state !is MediaState.Playing) {
            stopForeground(false)

            NotificationManagerCompat.from(this)
                .notify(notificationId, notification)
        }

        if (state is MediaState.None) {
            stopForeground(true)
        }
    }

    private fun shutdown() {
        audioFocus.abandon()
        mediaSession.release()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun updateState(context: Context) {
            ContextCompat.startForegroundService(context, updateStateIntent(context))
        }

        fun updateStateIntent(context: Context) = Intent(ACTION_UPDATE_STATE).apply {
            component = ComponentName(context, MediaService::class.java)
        }

        fun playIntent(context: Context): Intent = Intent(ACTION_PLAY).apply {
            component = ComponentName(context, MediaService::class.java)
        }

        fun pauseIntent(context: Context): Intent = Intent(ACTION_PAUSE).apply {
            component = ComponentName(context, MediaService::class.java)
        }
    }
}
