/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.support.base.observer.Observable

/**
 * Abstraction layer above the [PinnedSiteStorage] and [PlacesHistoryStorage] storages.
 */
interface TopSitesStorage : Observable<TopSitesStorage.Observer> {
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

    /**
     * Return a count of top sites.
     */
    suspend fun getTopSitesCount(): Int

    /**
     * Interface to be implemented by classes that want to observe the top site storage.
     */
    interface Observer {
        /**
         * Notify the observer when changes are made to the storage.
         */
        fun onStorageUpdated()
    }
}
