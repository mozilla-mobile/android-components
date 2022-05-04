/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import mozilla.components.concept.storage.FrecencyThresholdOption

/**
 * Top sites configuration to specify the number of top sites to display and
 * whether or not to include top frecent sites in the top sites feature.
 *
 * @property totalSites A total number of sites that will be displayed.
 * @property frecencyConfig If [frecencyConfig] is specified, only visited sites with a frecency
 * score above the given threshold will be returned. Otherwise, frecent top site results are
 * not included.
 * @property providerConfig An instance of [TopSitesProviderConfig] that specifies whether or
 * not to fetch top sites from the [TopSitesProvider].
 */
data class TopSitesConfig(
    val totalSites: Int,
    val frecencyConfig: FrecencyThresholdOption? = null,
    val providerConfig: TopSitesProviderConfig? = null
)

/**
 * Top sites provider configuration to specify whether or not to fetch top sites from the provider.
 *
 * @property showProviderTopSites Whether or not to display the top sites from the provider.
 * @property maxThreshold Only fetch the top sites from the provider if the number of top sites are
 * below the maximum threshold.
 * @property providerFilter Optional function used to filter the top sites from the provider.
 */
data class TopSitesProviderConfig(
    val showProviderTopSites: Boolean,
    val maxThreshold: Int = Int.MAX_VALUE,
    val providerFilter: ((TopSite) -> Boolean)? = null
)
