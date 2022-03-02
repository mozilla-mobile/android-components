/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import kotlin.coroutines.CoroutineContext

interface TopSitesState : State {
    val topSites: List<TopSite>
}

data class AppState(
    val inactiveTabsExpanded: Boolean = false,
    override val topSites: List<TopSite> = emptyList()
) : TopSitesState

sealed class TopSitesAction : Action

object InitAction : TopSitesAction()

sealed class AppAction : TopSitesAction() {
    data class UpdateInactiveExpanded(val expanded: Boolean) : AppAction()
}

internal object AppStoreReducer {
    fun reduce(state: AppState, action: AppAction): AppState {
        return when (action) {
            is AppAction.UpdateInactiveExpanded -> state.copy(inactiveTabsExpanded = action.expanded)
            else -> state.copy(topSites = TopSitesReducer.reduce(state, action).topSites)
        }
        // if (action is AppAction.UpdateInactiveExpanded) {
        //     return state.copy(inactiveTabsExpanded = action.expanded)
        // }
        //
        // return state.copy(topSites = TopSitesReducer.reduce(state, action).topSites)
    }
}

internal object TopSitesReducer {
    fun reduce(state: TopSitesState, action: TopSitesAction): TopSitesState {
        return when (action) {
            is InitAction -> state
            else -> state
        }
    }
}

class AppStore(
    initialState: AppState = AppState(),
    middlewares: List<Middleware<AppState, AppAction>> = emptyList()
) : Store<AppState, AppAction>(initialState, AppStoreReducer::reduce, middlewares)

class TopSitesMiddleware(
    private val storage: Storage,
    private val config: () -> TopSitesConfig,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : Middleware<TopSitesState, TopSitesAction> {

    private var scope = CoroutineScope(coroutineContext)

    override fun invoke(
        context: MiddlewareContext<TopSitesState, TopSitesAction>,
        next: (TopSitesAction) -> Unit,
        action: TopSitesAction
    ) {
        TODO("Not yet implemented")
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
         * @param providerConfig An instance of [TopSitesProviderConfig] that specifies whether or
         * not to fetch top sites from the [TopSitesProvider].
         */
        suspend fun getTopSites(
            totalSites: Int,
            frecencyConfig: FrecencyThresholdOption? = null,
            providerConfig: TopSitesProviderConfig? = null
        ): List<TopSite>
    }
}