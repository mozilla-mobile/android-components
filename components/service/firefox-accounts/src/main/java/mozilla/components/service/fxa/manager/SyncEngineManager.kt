/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.manager

import android.content.Context
import mozilla.components.service.fxa.Engine
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

internal val declinedEnginesRegistry = ObserverRegistry<DeclinedEnginesObserver>()

internal interface DeclinedEnginesObserver {
    fun onUpdatedDeclinedEngines(declinedEngines: Set<Engine>, isLocalChange: Boolean)
}

/**
 * Handles the state related to which engines should be syncable.
 *
 * //TODO: define if this class should be internal
 */
class SyncEngineManager(
    val context: Context
) : DeclinedEnginesObserver, Observable<DeclinedEnginesObserver> by ObserverRegistry() {
    companion object {
         private const val SYNC_ENGINES_KEY = "syncEngines"
    }

    init {
        declinedEnginesRegistry.register(this)
    }

    /**
     * Returns the status of each engine.
     * @return A [Map] of [Engine] and [Boolean] that indicates when a given [Engine] is active
     * to sync. True indicates is active otherwise false.
     */
    fun getEnginesStatus(): Map<Engine, Boolean?> {
        val resultMap = mutableMapOf<Engine, Boolean?>()
        Engine.values().forEach {
            // TODO think through changes in the Engine list... default value? initial value?

            val keyExists = storage().contains(it.name)

            resultMap[it] = if (keyExists) {
                storage().getBoolean(it.name, false)
            } else {
                null
            }

        }
        return resultMap
    }

    //TODO Should this be public as this could cause that any consumer of this object
    // can update the status of the engines an alternative could be
    // that we provide this information through out [FxaAccountManager]
    fun setStatus(engine: Engine, status: Boolean) {
        storage().edit().putBoolean(engine.name, status).apply() // todo consider commit()
    }

    fun clear(){
        storage().edit().clear().apply()
    }

    override fun onUpdatedDeclinedEngines(
        declinedEngines: Set<Engine>, isLocalChange: Boolean
    ) {
        // we don't care about isLocalChange...
        declinedEngines.forEach { setStatus(it, false) }
    }

    private fun storage() = context.getSharedPreferences(SYNC_ENGINES_KEY, Context.MODE_PRIVATE)
}