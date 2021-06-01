/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts.ext

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserStoreTest {

    private lateinit var store: BrowserStore

    private val tab = createTab("https://www.firefox.com")
    private val otherTab = createTab("https://getpocket.com")
    private val customTab = createCustomTab("https://www.mozilla.org")
    private val state = BrowserState(
        tabs = listOf(tab, otherTab),
        customTabs = listOf(customTab),
        selectedTabId = tab.id
    )

    var consumeCalled = false
    private val consumeCallback: (PromptRequest) -> Unit = { consumeCalled = true }

    private val tabSessionState: TabSessionState
        get() = store.state.tabs.find { it.id == tab.id }!!

    @Before
    fun setup() {
        store = BrowserStore(state)

        consumeCalled = false
    }

    @Test
    fun `WHEN consumePromptFrom is called THEN invoke the consume callback and dispatch ConsumePromptRequestAction`() {
        val promptRequest: PromptRequest = mock()

        store.dispatch(
            ContentAction.UpdatePromptRequestAction(tab.id, promptRequest)
        ).joinBlocking()

        Assert.assertEquals(promptRequest, tab.content.promptRequest)

        store.consumePromptFrom(consume = consumeCallback)
    }
}
