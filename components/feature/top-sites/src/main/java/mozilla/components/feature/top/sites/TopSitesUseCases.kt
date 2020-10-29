/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

/**
 * Contains use cases related to the top sites feature.
 */
class TopSitesUseCases(topSitesStorage: TopSitesStorage) {
    /**
     * Add a pinned site use case.
     */
    class AddPinnedSiteUseCase internal constructor(private val storage: TopSitesStorage) {
        /**
         * Adds a new [PinnedSite].
         *
         * @param title The title string.
         * @param url The URL string.
         */
        operator fun invoke(title: String, url: String, isDefault: Boolean = false) {
            storage.addTopSite(title, url, isDefault)
        }
    }

    /**
     * Remove a top site use case.
     */
    class RemoveTopSiteUseCase internal constructor(private val storage: TopSitesStorage) {
        /**
         * Removes the given [TopSite].
         *
         * @param topSite The top site.
         */
        operator fun invoke(topSite: TopSite) {
            storage.removeTopSite(topSite)
        }
    }

    /**
     * Rename a top site use case.
     */
    class RenameTopSiteUseCase internal constructor(private val storage: TopSitesStorage) {
        /**
         * Renames the given [TopSite].
         *
         * @param topSite The top site.
         * @param title The new title for the top site.
         */
        operator fun invoke(topSite: TopSite, title: String) {
            storage.renameTopSite(topSite, title)
        }
    }

    val addPinnedSites: AddPinnedSiteUseCase by lazy {
        AddPinnedSiteUseCase(topSitesStorage)
    }

    val removeTopSites: RemoveTopSiteUseCase by lazy {
        RemoveTopSiteUseCase(topSitesStorage)
    }

    val renameTopSites: RenameTopSiteUseCase by lazy {
        RenameTopSiteUseCase(topSitesStorage)
    }
}
