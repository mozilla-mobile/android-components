/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.script.SlowScriptRequest
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.mock
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify

class SlowLoadingFeatureTest {
    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    private lateinit var store: BrowserStore
    private val sessionId = "1"
    private val showNotification = mock<(SlowScriptRequest) -> Unit>()
    private val dismissNotification = mock<() -> Unit>()
    private lateinit var slowLoadingFeature: SlowLoadingFeature

    @Before
    fun setup() {
        store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "1"),
                ),
                selectedTabId = sessionId,
            ),
        )

        slowLoadingFeature =
            SlowLoadingFeature(sessionId, store, showNotification, dismissNotification)
    }

    @Test
    fun `WHEN the slow script feature is started THEN the notification is dismissed`() {
        slowLoadingFeature.start()

        verify(dismissNotification).invoke()
    }

    @Test
    fun `GIVEN the session has a slow script WHEN the slow script notification callback is removed from the session THEN dismiss the notification`() {
        slowLoadingFeature.start()
        store.dispatch(ContentAction.AddSlowScriptRequest(sessionId, mock())).joinBlocking()

        store.dispatch(ContentAction.RemoveSlowScriptRequest(sessionId)).joinBlocking()

        verify(dismissNotification, Mockito.times(2)).invoke()
    }

    @Test
    fun `WHEN the session has a slow script THEN show the notification`() {
        slowLoadingFeature.start()
        val callback = mock<SlowScriptRequest>()

        store.dispatch(ContentAction.AddSlowScriptRequest(sessionId, callback)).joinBlocking()
        store.waitUntilIdle()

        verify(showNotification).invoke(callback)
    }

    @Test
    fun `WHEN the session is stopped THEN dismiss the notification`() {
        slowLoadingFeature.stop()

        verify(dismissNotification).invoke()
    }
}
