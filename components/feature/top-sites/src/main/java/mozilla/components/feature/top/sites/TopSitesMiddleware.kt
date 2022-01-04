/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.InitAction
import mozilla.components.browser.state.action.TopSiteAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TopSite
import mozilla.components.browser.state.state.TopSite.Type.DEFAULT
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.Store
import kotlin.coroutines.CoroutineContext

/**
 * [Middleware] implementation for handling [TopSiteAction] and syncing the top sites in
 * [BrowserState.topSites] with the [DefaultTopSitesStorage].
 */
class TopSitesMiddleware(
    private val storage: Storage,
    private val config: () -> TopSitesConfig,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Middleware<BrowserState, BrowserAction> {

    private var scope = CoroutineScope(coroutineContext)

    override fun invoke(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        when (action) {
            is InitAction -> initializeTopSites(context.store)
            is TopSiteAction.AddTopSiteAction -> addTopSite(action)
            is TopSiteAction.RemoveTopSiteAction -> removeTopSite(action)
            else -> next(action)
        }
    }

    private fun initializeTopSites(
        store: Store<BrowserState, BrowserAction>
    ) = scope.launch {
        val (totalSites, frecencyConfig) = config.invoke()
        val topSites = storage.getTopSites(
            totalSites = totalSites,
            frecencyConfig = frecencyConfig
        )
        store.dispatch(TopSiteAction.AddTopSitesAction(topSites))
    }

    private fun addTopSite(action: TopSiteAction.AddTopSiteAction) = scope.launch {
        storage.addTopSite(
            title = action.topSite.title,
            url = action.topSite.url,
            isDefault = action.topSite.type === DEFAULT
        )
    }

    private fun removeTopSite(action: TopSiteAction.RemoveTopSiteAction) = scope.launch {
        val (totalSites, frecencyConfig) = config.invoke()
        val topSites = storage.getTopSites(
            totalSites = totalSites,
            frecencyConfig = frecencyConfig
        )

        // Find the matching top site in storage since top sites added from [AddPinnedSiteUseCase]
        // does not contain the matching id between the Store and storage.
        topSites.find {
            it.url == action.topSite.url &&
                it.title == action.topSite.title &&
                it.type == action.topSite.type
        }?.let {
            storage.removeTopSite(it)
        }
    }

    /**
     * Interface for a storage to be passed to the middleware.
     */
    interface Storage {
        /**
         * Adds a new top site.
         *
         * @param title The title string.
         * @param url The URL string.
         * @param isDefault Whether or not the pinned site added should be a default pinned site. This
         * is used to identify pinned sites that are added by the application.
         */
        fun addTopSite(title: String, url: String, isDefault: Boolean = false)

        /**
         * Removes the given [TopSite].
         *
         * @param topSite The top site.
         */
        fun removeTopSite(topSite: TopSite)

        /**
         * Updates the given [TopSite].
         *
         * @param topSite The top site.
         * @param title The new title for the top site.
         * @param url The new url for the top site.
         */
        fun updateTopSite(topSite: TopSite, title: String, url: String)

        /**
         * Return a unified list of top sites based on the given number of sites desired.
         * If `frecencyConfig` is specified, fill in any missing top sites with frecent top site results.
         *
         * @param totalSites A total number of sites that will be retrieve if possible.
         * @param frecencyConfig If [frecencyConfig] is specified, only visited sites with a frecency
         * score above the given threshold will be returned. Otherwise, frecent top site results are
         * not included.
         */
        suspend fun getTopSites(
            totalSites: Int,
            frecencyConfig: FrecencyThresholdOption?
        ): List<TopSite>
    }
}
