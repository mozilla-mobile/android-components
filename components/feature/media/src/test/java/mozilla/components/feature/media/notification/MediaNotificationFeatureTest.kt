/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.notification

import android.content.Context
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.media.Media
import mozilla.components.feature.media.state.MediaStateMachine
import mozilla.components.feature.media.state.MockMedia
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

class MediaNotificationFeatureTest {
    @Test
    fun `Media playing in Session starts service`() {
        val context: Context = mock()
        val sessionManager = SessionManager(engine = mock())

        val stateMachine = MediaStateMachine(sessionManager)
        stateMachine.start()

        val feature = MediaNotificationFeature(context, stateMachine)
        feature.enable()

        // A session gets added
        val session = Session("https://www.mozilla.org")
        sessionManager.add(session)

        // A media object gets added to the session
        val media = MockMedia(Media.PlaybackState.UNKNOWN)
        session.media = listOf(media)

        media.playbackState = Media.PlaybackState.WAITING

        // So far nothing has happened yet
        verify(context, never()).startService(any())

        // Media starts playing!
        media.playbackState = Media.PlaybackState.PLAYING

        verify(context).startService(any())
    }

    @Test
    fun `Media switching from playing to pause send Intent to service`() {
        val context: Context = mock()
        val media = MockMedia(Media.PlaybackState.PLAYING)

        val sessionManager = SessionManager(engine = mock()).apply {
            add(Session("https://www.mozilla.org").also { it.media = listOf(media) })
        }

        val stateMachine = MediaStateMachine(sessionManager)
        stateMachine.start()

        val feature = MediaNotificationFeature(context, stateMachine)
        feature.enable()

        reset(context)
        verify(context, never()).startService(any())

        media.playbackState = Media.PlaybackState.PAUSE

        verify(context).startService(any())
    }

    @Test
    fun `Media stopping to play with stop service`() {
        val context: Context = mock()
        val media = MockMedia(Media.PlaybackState.UNKNOWN)

        val sessionManager = SessionManager(engine = mock()).apply {
            add(Session("https://www.mozilla.org").also { it.media = listOf(media) })
        }

        val stateMachine = MediaStateMachine(sessionManager)
        stateMachine.start()

        val feature = MediaNotificationFeature(context, stateMachine)
        feature.enable()

        media.playbackState = Media.PlaybackState.PLAYING

        verify(context).startService(any())
        verify(context, never()).stopService(any())

        media.playbackState = Media.PlaybackState.ENDED

        verify(context).stopService(any())
    }
}
