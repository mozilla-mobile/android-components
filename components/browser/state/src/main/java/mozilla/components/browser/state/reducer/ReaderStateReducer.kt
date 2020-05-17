/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.ReaderAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.TabSessionState

internal object ReaderStateReducer {

    /**
     * [EngineAction] Reducer function for modifying a specific [ReaderState]
     * of a [BrowserState].
     */
    fun reduce(state: BrowserState, action: ReaderAction): BrowserState = when (action) {
        is ReaderAction.UpdateReaderableAction -> state.copyWithReaderState(action.tabId) {
            it.copy(readerable = action.readerable)
        }
        is ReaderAction.UpdateReaderActiveAction -> state.copyWithReaderState(action.tabId) {
            it.copy(active = action.active)
        }
        is ReaderAction.UpdateReaderableCheckRequiredAction -> state.copyWithReaderState(action.tabId) {
            it.copy(checkRequired = action.checkRequired)
        }
        is ReaderAction.UpdateReaderConnectRequiredAction -> state.copyWithReaderState(action.tabId) {
            it.copy(connectRequired = action.connectRequired)
        }
        is ReaderAction.UpdateReaderBaseUrlAction -> state.copyWithReaderState(action.tabId) {
            it.copy(baseUrl = action.baseUrl)
        }
        is ReaderAction.UpdateReaderActiveUrlAction -> state.copyWithReaderState(action.tabId) {
            it.copy(activeUrl = action.activeUrl)
        }
        is ReaderAction.ClearReaderActiveUrlAction -> state.copyWithReaderState(action.tabId) {
            it.copy(activeUrl = null)
        }
    }
}

private fun BrowserState.copyWithReaderState(
    tabId: String,
    update: (ReaderState) -> ReaderState
): BrowserState {
    val updater = { current: TabSessionState ->
        current.copy(readerState = update.invoke(current.readerState))
    }

    val newNormalTabs = normalTabs.updateTabs(tabId, updater)
    if (newNormalTabs != null) return copy(normalTabs = newNormalTabs)

    val newPrivateTabs = privateTabs.updateTabs(tabId, updater)
    if (newPrivateTabs != null) return copy(privateTabs = newPrivateTabs)

    return this
}
