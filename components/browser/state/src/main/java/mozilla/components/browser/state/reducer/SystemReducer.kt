/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.SystemAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.EngineState
import mozilla.components.browser.state.state.TabSessionState

internal object SystemReducer {
    /**
     * [SystemAction] Reducer function for modifying [BrowserState].
     */
    fun reduce(state: BrowserState, action: SystemAction): BrowserState {
        return when (action) {
            is SystemAction.LowMemoryAction -> {
                val mapper = { tab: TabSessionState ->
                    if (state.selectedTabId != tab.id) {
                        tab.copy(
                            content = tab.content.copy(thumbnail = null),
                            engineState = if (tab.id in action.states) {
                                EngineState(
                                    engineSession = null,
                                    engineSessionState = action.states[tab.id]
                                )
                            } else {
                                tab.engineState
                            }
                        )
                    } else {
                        tab
                    }
                }

                state.copy(
                    normalTabs = state.normalTabs.map(mapper),
                    privateTabs = state.privateTabs.map(mapper)
                )
            }
        }
    }
}
