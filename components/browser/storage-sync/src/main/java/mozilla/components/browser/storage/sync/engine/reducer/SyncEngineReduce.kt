/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.sync.engines.reducer

import mozilla.components.concept.sync.engines.SyncEngineState
import mozilla.components.browser.storage.sync.engine.action.EnableSyncEngineAction
import mozilla.components.browser.storage.sync.engine.action.DisableAllSyncEnginesAction
import mozilla.components.browser.storage.sync.engine.action.DisableSyncEngineAction
import mozilla.components.browser.storage.sync.engine.action.SyncEngineAction

internal object SyncEngineReduce {

    fun reduce(state: SyncEngineState, action: SyncEngineAction): SyncEngineState {
        return when (action) {
            //TODO: validate for duplicate enabledEngines.
            is EnableSyncEngineAction -> state.copy(enabledEngines = state.enabledEngines + action.engine)
            is DisableSyncEngineAction -> state.copy(enabledEngines = state.enabledEngines - action.engine)
            is DisableAllSyncEnginesAction -> state.copy(enabledEngines = emptyList())
        }
    }
}