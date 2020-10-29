/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.search.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import mozilla.components.feature.search.middleware.SearchMiddleware

private const val PREFERENCE_FILE = "mozac_feature_search_metadata"

private const val PREFERENCE_KEY_USER_SELECTED_SEARCH_ENGINE_ID = "user_selected_search_engine"
private const val PREFERENCE_KEY_HIDDEN_SEARCH_ENGINES = "hidden_search_engines"

/**
 * Storage for saving additional search related metadata.
 */
internal class SearchMetadataStorage(
    context: Context,
    @VisibleForTesting private val preferences: Lazy<SharedPreferences> = lazy {
        context.getSharedPreferences(
            PREFERENCE_FILE,
            Context.MODE_PRIVATE
        )
    }
) : SearchMiddleware.MetadataStorage {
    /**
     * Gets the ID of the default search engine the user has picked. Returns `null` if the user
     * has not made a choice.
     */
    override suspend fun getUserSelectedSearchEngineId(): String? {
        return preferences.value.getString(PREFERENCE_KEY_USER_SELECTED_SEARCH_ENGINE_ID, null)
    }

    /**
     * Sets the ID of the default search engine the user has picked.
     */
    override suspend fun setUserSelectedSearchEngineId(id: String) {
        preferences.value.edit()
            .putString(PREFERENCE_KEY_USER_SELECTED_SEARCH_ENGINE_ID, id)
            .apply()
    }

    /**
     * Sets the list of IDs of hidden search engines.
     */
    override suspend fun setHiddenSearchEngines(ids: List<String>) {
        preferences.value.edit()
            .putStringSet(PREFERENCE_KEY_HIDDEN_SEARCH_ENGINES, ids.toSet())
            .apply()
    }

    /**
     * Gets the list of IDs of hidden search engines.
     */
    override suspend fun getHiddenSearchEngines(): List<String> {
        return preferences.value
            .getStringSet(PREFERENCE_KEY_HIDDEN_SEARCH_ENGINES, emptySet())!!
            .toList()
    }
}
