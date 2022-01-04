/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import mozilla.components.browser.state.action.TopSiteAction
import mozilla.components.browser.state.state.TopSite
import mozilla.components.browser.state.state.TopSite.Type.DEFAULT
import mozilla.components.browser.state.state.TopSite.Type.PINNED
import mozilla.components.browser.state.store.BrowserStore

/**
 * Contains use cases related to the top sites feature.
 */
class TopSitesUseCases(store: BrowserStore) {
    /**
     * Add a pinned site use case.
     */
    class AddPinnedSiteUseCase internal constructor(private val store: BrowserStore) {
        /**
         * Adds a new [PinnedSite].
         *
         * @param title The title string.
         * @param url The URL string.
         */
        operator fun invoke(title: String, url: String, isDefault: Boolean = false) {
            val type = if (isDefault) DEFAULT else PINNED
            store.dispatch(
                TopSiteAction.AddTopSiteAction(
                    TopSite(
                        title = title,
                        url = url,
                        type = type
                    )
                )
            )
        }
    }

    /**
     * Remove a top site use case.
     */
    class RemoveTopSiteUseCase internal constructor(private val store: BrowserStore) {
        /**
         * Removes the given [TopSite].
         *
         * @param topSite The top site.
         */
        operator fun invoke(topSite: TopSite) {
            store.dispatch(TopSiteAction.RemoveTopSiteAction(topSite))
        }
    }

    /**
     * Update a top site use case.
     */
    class UpdateTopSiteUseCase internal constructor(private val store: BrowserStore) {
        /**
         * Updates the given [TopSite].
         *
         * @param topSite The top site.
         * @param title The new title for the top site.
         * @param url The new url for the top site.
         */
        operator fun invoke(topSite: TopSite, title: String, url: String) {
            store.dispatch(
                TopSiteAction.UpdateTopSiteAction(
                    topSite = topSite,
                    title = title,
                    url = url
                )
            )
        }
    }

    val addPinnedSites: AddPinnedSiteUseCase by lazy {
        AddPinnedSiteUseCase(store)
    }

    val removeTopSites: RemoveTopSiteUseCase by lazy {
        RemoveTopSiteUseCase(store)
    }

    val updateTopSites: UpdateTopSiteUseCase by lazy {
        UpdateTopSiteUseCase(store)
    }
}
