/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.CrashAction
import mozilla.components.browser.state.state.BrowserState

internal object CrashReducer {
    /**
     * [CrashAction] Reducer function for modifying [BrowserState].
     */
    fun reduce(state: BrowserState, action: CrashAction): BrowserState = when (action) {
        is CrashAction.SessionCrashedAction -> state.updateTabState(action.tabId) { tab ->
            tab.createCopy(crashed = true)
        }
        is CrashAction.RestoreCrashedSessionAction -> state.updateTabState(action.tabId) { tab ->
            // We only update the flag in the reducer. A middleware is responsible for actually
            // performing a restoring side effect.
            tab.createCopy(crashed = false)
        }
    }
}
