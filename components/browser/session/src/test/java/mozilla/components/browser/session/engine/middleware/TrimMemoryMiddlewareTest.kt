/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import android.content.ComponentCallbacks2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.SystemAction
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.EngineState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.mock
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class TrimMemoryMiddlewareTest {
    private lateinit var engineSessionReddit: EngineSession
    private lateinit var engineSessionTheVerge: EngineSession
    private lateinit var engineSessionTwitch: EngineSession
    private lateinit var store: BrowserStore
    private val dispatcher = TestCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)

    private lateinit var engineSessionStateReddit: EngineSessionState
    private lateinit var engineSessionStateTheVerge: EngineSessionState
    private lateinit var engineSessionStateTwitch: EngineSessionState

    @Before
    fun setUp() {
        engineSessionTheVerge = mock()
        engineSessionReddit = mock()
        engineSessionTwitch = mock()

        engineSessionStateReddit = mock()
        engineSessionStateTheVerge = mock()
        engineSessionStateTwitch = mock()

        store = BrowserStore(
            middleware = listOf(
                TrimMemoryMiddleware(),
                SuspendMiddleware(scope)
            ),
            initialState = BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "mozilla"),
                    createTab("https://www.theverge.com/", id = "theverge").copy(
                        engineState = EngineState(
                            engineSession = engineSessionTheVerge,
                            engineSessionState = engineSessionStateTheVerge,
                            engineObserver = mock())
                    ),
                    createTab(
                        "https://www.reddit.com/r/firefox/",
                        id = "reddit",
                        private = true
                    ).copy(
                        engineState = EngineState(
                            engineSession = engineSessionReddit,
                            engineSessionState = engineSessionStateReddit,
                            engineObserver = mock()
                        )
                    ),
                    createTab("https://github.com/", id = "github")
                ),
                customTabs = listOf(
                    createCustomTab("https://www.twitch.tv/", id = "twitch").copy(
                        engineState = EngineState(
                            engineSession = engineSessionTwitch,
                            engineSessionState = engineSessionStateTwitch,
                            engineObserver = mock()
                        )
                    ),
                    createCustomTab("https://twitter.com/home", id = "twitter")
                ),
                selectedTabId = "reddit"
            )
        )
    }

    @Test
    fun `TrimMemoryMiddleware - TRIM_MEMORY_UI_HIDDEN`() {
        store.dispatch(SystemAction.LowMemoryAction(
            level = ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        )).joinBlocking()

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        store.state.findTab("theverge")!!.engineState.apply {
            assertNotNull(engineSession)
            assertNotNull(engineObserver)
            assertNotNull(engineSessionState)
        }

        store.state.findTab("reddit")!!.engineState.apply {
            assertNotNull(engineSession)
            assertNotNull(engineObserver)
            assertNotNull(engineSessionState)
        }

        store.state.findCustomTab("twitch")!!.engineState.apply {
            assertNotNull(engineSession)
            assertNotNull(engineObserver)
            assertNotNull(engineSessionState)
        }

        verify(engineSessionTheVerge, never()).close()
        verify(engineSessionReddit, never()).close()
        verify(engineSessionTwitch, never()).close()
    }

    @Test
    fun `TrimMemoryMiddleware - TRIM_MEMORY_RUNNING_CRITICAL`() {
        store.dispatch(SystemAction.LowMemoryAction(
            level = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        )).joinBlocking()

        store.waitUntilIdle()
        dispatcher.advanceUntilIdle()

        store.state.findTab("theverge")!!.engineState.apply {
            assertNull(engineSession)
            assertNull(engineObserver)
            assertNotNull(engineSessionState)
        }

        store.state.findTab("reddit")!!.engineState.apply {
            assertNotNull(engineSession)
            assertNotNull(engineObserver)
            assertNotNull(engineSessionState)
        }

        store.state.findCustomTab("twitch")!!.engineState.apply {
            assertNotNull(engineSession)
            assertNotNull(engineObserver)
            assertNotNull(engineSessionState)
        }

        verify(engineSessionTheVerge).close()
        verify(engineSessionReddit, never()).close()
        verify(engineSessionTwitch, never()).close()
    }
}
