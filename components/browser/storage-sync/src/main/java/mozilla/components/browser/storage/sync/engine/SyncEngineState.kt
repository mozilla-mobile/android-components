/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package mozilla.components.browser.storage.sync.engine

import mozilla.components.concept.sync.engines.SyncAbleEngine
import mozilla.components.lib.state.State

data class SyncEngineState(
    val enabledEngines: List<SyncAbleEngine> = emptyList()
) : State