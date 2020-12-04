/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.focus

import android.media.AudioManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.MediaAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.MediaSessionState
import mozilla.components.browser.state.state.MediaState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.feature.media.createMockMediaElement
import mozilla.components.feature.media.service.AbstractMediaSessionService
import mozilla.components.feature.media.service.MediaSessionServiceDelegate
import mozilla.components.support.test.any
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(AndroidJUnit4::class)
class AudioFocusTest {
    private val dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()
    private lateinit var audioManager: AudioManager

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(dispatcher)

    @Before
    fun setUp() {
        audioManager = mock()
    }

    @Test
    fun `Successful request will not change media in state`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(media.id)

        verify(audioManager).requestAudioFocus(any())
        verifyNoMoreInteractions(media.controller)
    }

    @Test
    fun `Failed request will pause media`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(media.id)

        verify(audioManager).requestAudioFocus(any())
        verify(media.controller).pause()
    }

    @Test
    fun `Delayed request will pause media session`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(media.id)

        verify(audioManager).requestAudioFocus(any())
        verify(media.controller).pause()
    }

    @Test
    fun `Will pause and resume on and after transient focus loss`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)

        audioFocus.request(media.id)

        verifyNoMoreInteractions(media.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        verify(media.controller).pause()
        verifyNoMoreInteractions(media.controller)

        store.dispatch(MediaAction.UpdateMediaAggregateAction(
            aggregate = MediaState.Aggregate(
                MediaState.State.PAUSED,
                "test-tab",
                activeMedia = listOf(media.id)
            )
        ))

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        verify(media.controller).play()
        verifyNoMoreInteractions(media.controller)
    }

    @Test
    fun `Will not resume after transient focus loss`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PAUSED,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)

        verifyNoMoreInteractions(media.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        verify(media.controller, never()).pause()
        verifyNoMoreInteractions(media.controller)

        store.dispatch(MediaAction.UpdateMediaAggregateAction(
            aggregate = MediaState.Aggregate(
                MediaState.State.PAUSED,
                "test-tab",
                activeMedia = listOf(media.id)
            )
        ))

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        verify(media.controller, never()).play()
        verifyNoMoreInteractions(media.controller)
    }

    @Test
    fun `Will resume playback when gaining focus after being delayed`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(media.id)

        verify(media.controller).pause()

        store.dispatch(MediaAction.UpdateMediaAggregateAction(
            aggregate = MediaState.Aggregate(
                MediaState.State.PAUSED,
                "test-tab",
                activeMedia = listOf(media.id)
            )
        ))

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        verify(media.controller).play()
    }

    @Test(expected = IllegalStateException::class)
    fun `An unknown media session audio focus response will throw an exception`() {
        doReturn(999)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(media.id)
    }

    @Test
    fun `An unknown media session focus change event will be ignored`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)

        verifyNoMoreInteractions(media.controller)

        store.dispatch(MediaAction.UpdateMediaAggregateAction(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            )
        ))

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        audioFocus.onAudioFocusChange(999)
        verifyNoMoreInteractions(media.controller)
    }

    @Test
    fun `An audio focus loss will pause media session and regain will not resume automatically`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val media = createMockMediaElement()

        val state = MediaState(
            aggregate = MediaState.Aggregate(
                MediaState.State.PLAYING,
                "test-tab",
                activeMedia = listOf(media.id)
            ),
            elements = mapOf(
                "test-tab" to listOf(media)
            )
        )

        val store = BrowserStore(BrowserState(media = state))

        val audioFocus = AudioFocus(audioManager, store)

        audioFocus.request(media.id)

        verifyNoMoreInteractions(media.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        verify(media.controller).pause()

        store.dispatch(MediaAction.UpdateMediaAggregateAction(
            aggregate = MediaState.Aggregate(
                MediaState.State.PAUSED,
                "test-tab",
                activeMedia = listOf(media.id)
            )
        ))

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        verifyNoMoreInteractions(media.controller)
    }

    @Test
    fun `Successful request will not change media session in state`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verifyNoMoreInteractions(mediaSessionState.controller)
    }

    @Test
    fun `Failed request will pause media session`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
                "https://www.mozilla.org",
                mediaSessionState = mediaSessionState
            )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verify(mediaSessionState.controller).pause()
    }

    @Test
    fun `Delayed request will pause media`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verify(mediaSessionState.controller).pause()
    }

    @Test
    fun `Will pause and resume playing media on and after transient focus loss`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        verify(mediaSessionState.controller).pause()
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        verify(mediaSessionState.controller).play()
        verifyNoMoreInteractions(mediaSessionState.controller)
    }

    @Test
    fun `Will not resume paused media after transient focus loss`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PAUSED)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        verify(mediaSessionState.controller).pause()
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        verify(mediaSessionState.controller, never()).play()
        verifyNoMoreInteractions(mediaSessionState.controller)
    }

    @Test
    fun `Will resume media sessio nplayback when gaining focus after being delayed`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_DELAYED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verify(mediaSessionState.controller).pause()

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        verify(mediaSessionState.controller).play()
        verifyNoMoreInteractions(mediaSessionState.controller)
    }

    @Test(expected = IllegalStateException::class)
    fun `An unknown audio focus response will throw an exception`() {
        doReturn(-1).`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)
    }

    @Test
    fun `An unknown focus change event will be ignored`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(999)
        verifyNoMoreInteractions(mediaSessionState.controller)
    }

    @Test
    fun `An audio focus loss will pause media and regain will not resume automatically`() {
        doReturn(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            .`when`(audioManager).requestAudioFocus(any())

        val controller: MediaSession.Controller = mock()
        val mediaSessionState = MediaSessionState(controller,
            playbackState = MediaSession.PlaybackState.PLAYING)
        val tabSession = createTab(
            "https://www.mozilla.org",
            mediaSessionState = mediaSessionState
        )
        val initialState = BrowserState(
            tabs = listOf(tabSession)
        )
        val store = BrowserStore(initialState)
        val service: AbstractMediaSessionService = mock()
        val delegate = MediaSessionServiceDelegate(testContext, service, store)

        delegate.onCreate()

        val audioFocus = AudioFocus(audioManager, store)
        audioFocus.request(tabSession.id)

        verify(audioManager).requestAudioFocus(any())
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        verify(mediaSessionState.controller).pause()
        verifyNoMoreInteractions(mediaSessionState.controller)

        audioFocus.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        verify(mediaSessionState.controller, never()).play()
        verifyNoMoreInteractions(mediaSessionState.controller)
    }
}
