/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.top.sites.presenter.DefaultTopSitesPresenter
import mozilla.components.feature.top.sites.presenter.TopSitesPresenter
import mozilla.components.feature.top.sites.view.TopSitesView
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * View-bound feature that updates the UI when the [TopSitesStorage] is updated.
 *
 * @param view An implementor of [TopSitesView] that will be notified of changes to the storage.
 * @param storage The top sites storage that stores pinned and frecent sites.
 * @param config Lambda expression that returns [TopSitesConfig] which species the number of top
 * sites to return and whether or not to include frequently visited sites.
 * @param browserStore Browser store used to verify selected search engine changes
 */
class TopSitesFeature(
    private val view: TopSitesView,
    val storage: TopSitesStorage,
    val config: () -> TopSitesConfig,
    private val presenter: TopSitesPresenter = DefaultTopSitesPresenter(
        view,
        storage,
        config
    ),
    private val browserStore: BrowserStore
) : LifecycleAwareFeature {

    private var scope: CoroutineScope? = null

    override fun start() {
        presenter.start()
        scope = browserStore.flowScoped { flow ->
            flow.map { state -> state.search.selectedOrDefaultSearchEngine }
                .ifChanged()
                .collect { searchEngine ->
                    searchEngine?.let {
                        when(it.name){
                            "Amazon.com" -> storage.updateSponsoredShortcutFilter(SponsoredShortcutFilter.AMAZON)
                            "eBay" -> storage.updateSponsoredShortcutFilter(SponsoredShortcutFilter.EBAY)
                            else -> storage.updateSponsoredShortcutFilter(null)
                        }
                    }
                }
        }
    }

    override fun stop() {
        presenter.stop()
        scope?.cancel()
    }
}
