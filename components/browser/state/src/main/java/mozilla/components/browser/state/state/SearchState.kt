/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.state

import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine

/**
 * Value type that represents the state of search.
 *
 * @property region The region of the user.
 * @property regionSearchEngines The list of bundled [SearchEngine]s for the "home" region of the user.
 * @property customSearchEngines The list of custom [SearchEngine]s, added by the user.
 * @property hiddenSearchEngines The list of bundled [SearchEngine]s the user has explicitly hidden.
 * @property userSelectedSearchEngineId The ID of the default [SearchEngine] selected by the user. Or
 * `null` if the user hasn't made an explicit choice.
 * @property regionDefaultSearchEngineId The ID of the default [SearchEngine] of the "home" region
 * of the user.
 * @property complete Flag that indicates whether loading the list of search engines has completed.
 * This can be used for waiting for specific values (e.g. the default search engine) to be available.
 */
data class SearchState(
    val region: RegionState? = null,
    val regionSearchEngines: List<SearchEngine> = emptyList(),
    val customSearchEngines: List<SearchEngine> = emptyList(),
    val hiddenSearchEngines: List<SearchEngine> = emptyList(),
    val userSelectedSearchEngineId: String? = null,
    val regionDefaultSearchEngineId: String? = null,
    val complete: Boolean = false
)

/**
 * The list of search engines, bundled and custom.
 */
val SearchState.searchEngines: List<SearchEngine>
    get() = (regionSearchEngines + customSearchEngines)

/**
 * The primary search engine to be used by default for searches. This will either be the user
 * selected search engine, if the user has made an explicit choice, or the default search engine for
 * the user's region.
 */
val SearchState.selectedOrDefaultSearchEngine: SearchEngine?
    get() {
        // Does the user have a default search engine set and is it in the list of available search engines?
        if (userSelectedSearchEngineId != null) {
            searchEngines.find { engine -> userSelectedSearchEngineId == engine.id }?.let { return it }
        }

        // Do we have a default search engine for the region of the user and is it available?
        if (regionDefaultSearchEngineId != null) {
            searchEngines.find { engine -> regionDefaultSearchEngineId == engine.id }?.let { return it }
        }

        // Fallback: Use the first search engine in the list
        if (searchEngines.isNotEmpty()) {
            return searchEngines[0]
        }

        // We couldn't find anything.
        return null
    }
