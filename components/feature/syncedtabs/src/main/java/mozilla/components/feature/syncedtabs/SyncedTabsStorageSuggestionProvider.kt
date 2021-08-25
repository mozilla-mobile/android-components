/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package mozilla.components.feature.syncedtabs

import android.graphics.drawable.Drawable
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.awesomebar.AwesomeBar.Suggestion.Flag
import mozilla.components.concept.sync.DeviceType
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.syncedtabs.facts.emitSyncedTabSuggestionClickedFact
import mozilla.components.feature.syncedtabs.storage.SyncedTabsStorage
import mozilla.components.support.base.log.logger.Logger
import java.util.UUID

/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides suggestions for remote tabs
 * based on [SyncedTabsStorage].
 */
class SyncedTabsStorageSuggestionProvider(
    private val syncedTabs: SyncedTabsStorage,
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
    private val icons: BrowserIcons? = null,
    private val deviceIndicators: DeviceIndicators = DeviceIndicators()
) : AwesomeBar.SuggestionProvider {

    override val id: String = UUID.randomUUID().toString()
    private val logger = Logger("SyncedTabsStorageSuggestionProvider")

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        logger.debug("onInputChanged(${text.length} characters) called")

        if (text.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<ClientTabPair>()
        for ((client, tabs) in syncedTabs.getSyncedDeviceTabs()) {
            for (tab in tabs) {
                val activeTabEntry = tab.active()
                // This is a fairly naive match implementation, but this is what we do on Desktop 🤷.
                if (activeTabEntry.url.contains(text, ignoreCase = true) ||
                    activeTabEntry.title.contains(text, ignoreCase = true)
                ) {
                    results.add(
                        ClientTabPair(
                            clientName = client.displayName,
                            tab = activeTabEntry,
                            lastUsed = tab.lastUsed,
                            deviceType = client.deviceType
                        )
                    )
                }
            }
        }
        return results.sortedByDescending { it.lastUsed }.into().also {
            logger.debug("\tonInputChanged: ${it.size} suggestions returned")
        }
    }

    /**
     * Expects list of BookmarkNode to be specifically of bookmarks (e.g. nodes with a url).
     */
    private suspend fun List<ClientTabPair>.into(): List<AwesomeBar.Suggestion> {
        val iconRequests = this.map { client ->
            client.tab.iconUrl?.let { iconUrl -> icons?.loadIcon(IconRequest(iconUrl)) }
        }

        return this.zip(iconRequests) { result, icon ->
            val now = System.currentTimeMillis()

            AwesomeBar.Suggestion(
                provider = this@SyncedTabsStorageSuggestionProvider,
                icon = icon?.await()?.bitmap,
                indicatorIcon = when (result.deviceType) {
                    DeviceType.DESKTOP -> deviceIndicators.desktop
                    DeviceType.MOBILE -> deviceIndicators.mobile
                    DeviceType.TABLET -> deviceIndicators.tablet
                    else -> null
                },
                flags = setOf(Flag.SYNC_TAB),
                title = result.tab.title,
                description = result.clientName,
                onSuggestionClicked = {
                    loadUrlUseCase.invoke(result.tab.url)
                    emitSyncedTabSuggestionClickedFact()
                }
            ).also {
                logger.debug("\tinto() mapped this suggestion after ${System.currentTimeMillis() - now} millis")
            }
        }
    }
}

/**
 * AwesomeBar suggestion indicators data class for desktop, mobile, tablet device types.
 */
data class DeviceIndicators(
    val desktop: Drawable? = null,
    val mobile: Drawable? = null,
    val tablet: Drawable? = null
)

private data class ClientTabPair(
    val clientName: String,
    val tab: TabEntry,
    val lastUsed: Long,
    val deviceType: DeviceType
)
