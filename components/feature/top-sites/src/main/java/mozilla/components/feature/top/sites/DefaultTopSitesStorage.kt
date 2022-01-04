/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.TopSite
import mozilla.components.browser.state.state.TopSite.Type.FRECENT
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.top.sites.ext.hasUrl
import mozilla.components.feature.top.sites.ext.toTopSite
import mozilla.components.feature.top.sites.facts.emitTopSitesCountFact
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of [TopSitesMiddleware.Storage].
 *
 * @param pinnedSitesStorage An instance of [PinnedSiteStorage], used for storing pinned sites.
 * @param historyStorage An instance of [PlacesHistoryStorage], used for retrieving top frecent
 * sites from history.
 * @param defaultTopSites A list containing a title to url pair of default top sites to be added
 * to the [PinnedSiteStorage].
 */
class DefaultTopSitesStorage(
    private val pinnedSitesStorage: PinnedSiteStorage,
    private val historyStorage: PlacesHistoryStorage,
    private val defaultTopSites: List<Pair<String, String>> = listOf(),
    coroutineContext: CoroutineContext = Dispatchers.IO
) : TopSitesMiddleware.Storage {

    private var scope = CoroutineScope(coroutineContext)

    init {
        if (defaultTopSites.isNotEmpty()) {
            scope.launch {
                pinnedSitesStorage.addAllPinnedSites(defaultTopSites, isDefault = true)
            }
        }
    }

    override fun addTopSite(title: String, url: String, isDefault: Boolean) {
        scope.launch {
            pinnedSitesStorage.addPinnedSite(title, url, isDefault)
        }
    }

    override fun removeTopSite(topSite: TopSite) {
        scope.launch {
            if (topSite.type != FRECENT) {
                pinnedSitesStorage.removePinnedSite(topSite)
            }

            // Remove the top site from both history and pinned sites storage to avoid having it
            // show up as a frecent site if it is a pinned site.
            historyStorage.deleteVisitsFor(topSite.url)
        }
    }

    override fun updateTopSite(topSite: TopSite, title: String, url: String) {
        scope.launch {
            if (topSite.type != FRECENT) {
                pinnedSitesStorage.updatePinnedSite(topSite, title, url)
            }
        }
    }

    override suspend fun getTopSites(
        totalSites: Int,
        frecencyConfig: FrecencyThresholdOption?
    ): List<TopSite> {
        val topSites = ArrayList<TopSite>()
        val pinnedSites = pinnedSitesStorage.getPinnedSites().take(totalSites)
        val numSitesRequired = totalSites - pinnedSites.size

        topSites.addAll(pinnedSites)

        if (frecencyConfig != null && numSitesRequired > 0) {
            // Get 'totalSites' sites for duplicate entries with
            // existing pinned sites
            val frecentSites = historyStorage
                .getTopFrecentSites(totalSites, frecencyConfig)
                .map { it.toTopSite() }
                .filter { !pinnedSites.hasUrl(it.url) }
                .take(numSitesRequired)

            topSites.addAll(frecentSites)
        }

        emitTopSitesCountFact(pinnedSites.size)

        return topSites
    }
}
