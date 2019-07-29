/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.ext

import android.media.session.PlaybackState
import android.support.v4.media.session.PlaybackStateCompat
import mozilla.components.concept.engine.media.Media
import mozilla.components.feature.media.state.MediaState

/**
 * Gets the list of [Media] associated with this [MediaState].
 */
internal fun MediaState.getMedia(): List<Media> {
    return when (this) {
        is MediaState.Playing -> media
        is MediaState.Paused -> media
        else -> emptyList()
    }
}

/**
 * Turns the [MediaState] into a [PlaybackStateCompat] to be used with a `MediaSession`.
 */
internal fun MediaState.toPlaybackState(): PlaybackStateCompat {
    val state = when (this) {
        is MediaState.Playing -> PlaybackStateCompat.STATE_PLAYING
        is MediaState.Paused -> PlaybackState.STATE_PAUSED
        is MediaState.None -> PlaybackState.STATE_NONE
    }

    return PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE)
        .setState(
            state,
            // Time state not exposed yet:
            // https://github.com/mozilla-mobile/android-components/issues/2458
            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            // Playback speed not exposed yet:
            // https://github.com/mozilla-mobile/android-components/issues/2459
            1.0f)
        .build()
}
