/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.syncedtabs

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.RemoteTabsStorage
import mozilla.components.browser.storage.sync.Tab
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.concept.sync.Device
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.manager.ext.withConstellation
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * A feature that listens to the [BrowserStore] changes to update the local remote tabs state
 * in [RemoteTabsStorage].
 */
@ExperimentalCoroutinesApi
class SyncedTabsFeature(
    private val accountManager: FxaAccountManager,
    private val store: BrowserStore,
    private val tabsStorage: RemoteTabsStorage = RemoteTabsStorage()
) {
    private var scope: CoroutineScope? = null

    /**
     * Start listening to browser store changes.
     */
    fun start() {
        scope = store.flowScoped { flow ->
            flow.ifChanged { it.tabs }.map { state ->
                // TO-DO: https://github.com/mozilla-mobile/android-components/issues/5178
                val lastUsed = 0L
                // TO-DO: https://github.com/mozilla-mobile/android-components/issues/5179
                val iconUrl = null
                state.tabs.filter { !it.content.private }.map { tab ->
                    // TO-DO: https://github.com/mozilla-mobile/android-components/issues/1340
                    val history = listOf(TabEntry(tab.content.title, tab.content.url, iconUrl))
                    Tab(history, 0, lastUsed)
                }
            }.collect { tabs ->
                tabsStorage.store(tabs)
            }
        }
    }

    /**
     * Stop listening to browser store changes.
     */
    fun stop() {
        scope?.cancel()
    }

    /**
     * Get the list of remote tabs.
     */
    suspend fun getSyncedTabs(): Map<Device, List<Tab>> {
        val otherDevices = syncClients() ?: return emptyMap()
        return tabsStorage.getAll().mapNotNull { (client, tabs) ->
            val fxaDevice = otherDevices.find { it.id == client.id }
            fxaDevice?.let { fxaDevice to tabs }
        }.toMap()
    }

    /**
     * List of synced devices.
     */
    @VisibleForTesting
    internal fun syncClients(): List<Device>? {
        accountManager.withConstellation { constellation ->
            return constellation.state()?.otherDevices
        }
        return null
    }
}
