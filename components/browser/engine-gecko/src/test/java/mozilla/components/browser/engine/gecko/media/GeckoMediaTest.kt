/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.media

import mozilla.components.concept.engine.media.Media
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import mozilla.components.test.ReflectionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.verify
import org.mozilla.geckoview.MediaElement

class GeckoMediaTest {
    @Test
    fun `Playback state is updated from MediaDelegate`() {
        val mediaElement: MediaElement = mock()

        val media = GeckoMedia(mediaElement)

        val captor = argumentCaptor<MediaElement.Delegate>()
        verify(mediaElement).delegate = captor.capture()

        val delegate = captor.value

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_PLAYING)
        assertEquals(Media.PlaybackState.PLAYING, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_SEEKING)
        assertEquals(Media.PlaybackState.SEEKING, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_WAITING)
        assertEquals(Media.PlaybackState.WAITING, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_PAUSE)
        assertEquals(Media.PlaybackState.PAUSE, media.playbackState)
        assertEquals(Media.State.PAUSED, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_PLAY)
        assertEquals(Media.PlaybackState.PLAY, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_SEEKED)
        assertEquals(Media.PlaybackState.SEEKED, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_STALLED)
        assertEquals(Media.PlaybackState.STALLED, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_SUSPEND)
        assertEquals(Media.PlaybackState.SUSPENDED, media.playbackState)
        assertEquals(Media.State.PLAYING, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_ABORT)
        assertEquals(Media.PlaybackState.ABORT, media.playbackState)
        assertEquals(Media.State.STOPPED, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_EMPTIED)
        assertEquals(Media.PlaybackState.EMPTIED, media.playbackState)
        assertEquals(Media.State.STOPPED, media.state)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_ENDED)
        assertEquals(Media.PlaybackState.ENDED, media.playbackState)
        assertEquals(Media.State.STOPPED, media.state)
    }

    @Test
    fun `GeckoMedia exposes GeckoMediaController`() {
        val mediaElement: MediaElement = mock()

        val media = GeckoMedia(mediaElement)

        assertTrue(media.controller is GeckoMediaController)
    }

    @Test
    fun `GeckoMedia observer is notified when its playback state changes`() {
        val mediaElement: MediaElement = mock()

        val media = GeckoMedia(mediaElement)

        val captor = argumentCaptor<MediaElement.Delegate>()
        verify(mediaElement).delegate = captor.capture()
        val delegate = captor.value

        val observer: Media.Observer = mock()
        media.register(observer)

        delegate.onPlaybackStateChange(mediaElement, MediaElement.MEDIA_STATE_PLAYING)
        verify(observer).onPlaybackStateChanged(media, Media.PlaybackState.PLAYING)
    }

    @Test
    fun `GeckoMedia exposes Metadata`() {
        val mediaElement: MediaElement = mock()

        val media = GeckoMedia(mediaElement)

        val captor = argumentCaptor<MediaElement.Delegate>()
        verify(mediaElement).delegate = captor.capture()

        assertEquals(-1.0, media.metadata.duration, 0.0001)

        val delegate = captor.value

        delegate.onMetadataChange(mediaElement, MockedGeckoMetadata(duration = 5.0))
        assertEquals(5.0, media.metadata.duration, 0.0001)

        delegate.onMetadataChange(mediaElement, MockedGeckoMetadata(duration = 572.0))
        assertEquals(572.0, media.metadata.duration, 0.0001)

        delegate.onMetadataChange(mediaElement, MockedGeckoMetadata(duration = 0.0))
        assertEquals(0.0, media.metadata.duration, 0.0001)

        delegate.onMetadataChange(mediaElement, MockedGeckoMetadata(duration = -1.0))
        assertEquals(-1.0, media.metadata.duration, 0.0001)
    }

    @Test
    fun `GeckoMedia exposes Volume`() {
        val mediaElement: MediaElement = mock()

        val media = GeckoMedia(mediaElement)

        val captor = argumentCaptor<MediaElement.Delegate>()
        verify(mediaElement).delegate = captor.capture()

        assertEquals(media.volume.muted, false)

        val delegate = captor.value

        delegate.onVolumeChange(mediaElement, 1.0, true)
        assertEquals(true, media.volume.muted)

        delegate.onVolumeChange(mediaElement, 1.0, false)
        assertEquals(false, media.volume.muted)
    }

    @Test
    fun `GeckoMedia notifies observer when metadata changes`() {
        val media = GeckoMedia(mock())

        val observer: Media.Observer = mock()
        media.register(observer)

        val metadata: Media.Metadata = Media.Metadata(duration = 42.0)
        media.metadata = metadata

        verify(observer).onMetadataChanged(media, metadata)
    }

    @Test
    fun `GeckoMedia notifies observer when volume changes`() {
        val media = GeckoMedia(mock())

        val observer: Media.Observer = mock()
        media.register(observer)

        val volume: Media.Volume = Media.Volume(muted = true)
        media.volume = volume

        verify(observer).onVolumeChanged(media, volume)
    }
}

private class MockedGeckoMetadata(
    duration: Double
) : MediaElement.Metadata() {
    init {
        ReflectionUtils.setField(this, "duration", duration)
    }
}
