/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.manager

import android.content.Context
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.sync.toSyncEngine
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

internal val declinedEnginesRegistry = ObserverRegistry<DeclinedEnginesObserver>()

internal interface DeclinedEnginesObserver {
    fun onUpdatedDeclinedEngines(declinedEngines: Set<SyncEngine>, isLocalChange: Boolean)
}

/**
 * TODO DOCS
 */
class SyncEngineManager(
    val context: Context
) : DeclinedEnginesObserver, Observable<DeclinedEnginesObserver> by ObserverRegistry() {
    companion object {
        const val SYNC_ENGINES_KEY = "syncEngines"
    }

    init {
        declinedEnginesRegistry.register(this)
    }

    fun getStatus(): Map<SyncEngine, Boolean> {
        val resultMap = mutableMapOf<SyncEngine, Boolean>()
        // TODO think through changes in the Engine list... default value? initial value?

        // this needs to be reversed: go through what we have in local storage, and populate result map based on that.
        // reason: we may have "other" engines.
        // this will be empty if `setStatus` was never called.
        storage().all.forEach {
            if (it.value is Boolean) {
                resultMap[it.key.toSyncEngine()] = it.value as Boolean
            }
        }

        return resultMap
    }

    fun setStatus(engine: SyncEngine, status: Boolean) {
        storage().edit().putBoolean(engine.nativeName, status).apply() // todo consider commit()
    }

    override fun onUpdatedDeclinedEngines(
        declinedEngines: Set<SyncEngine>, isLocalChange: Boolean
    ) {
        // we don't care about isLocalChange...
        declinedEngines.forEach { setStatus(it, false) }
    }

    private fun storage() = context.getSharedPreferences(SYNC_ENGINES_KEY, Context.MODE_PRIVATE)
}