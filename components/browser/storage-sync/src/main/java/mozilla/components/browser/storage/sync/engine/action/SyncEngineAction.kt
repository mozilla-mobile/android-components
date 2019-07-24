package mozilla.components.browser.storage.sync.engine.action

import mozilla.components.concept.sync.engines.SyncAbleEngine
import mozilla.components.lib.state.Action

sealed class SyncEngineAction : Action

data class EnableSyncEngineAction(val engine: SyncAbleEngine) : SyncEngineAction()
data class DisableSyncEngineAction(val engine: SyncAbleEngine) : SyncEngineAction()
object  DisableAllSyncEnginesAction : SyncEngineAction()
