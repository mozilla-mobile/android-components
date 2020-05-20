/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.search.provider.custom

import android.content.Context
import android.content.SharedPreferences
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.search.SearchEngineParser
import mozilla.components.browser.search.provider.SearchEngineList
import mozilla.components.browser.search.provider.SearchEngineProvider
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.stringSetPreference

private const val PREF_KEY_CUSTOM_SEARCH_ENGINES = "pref_custom_search_engines"
private const val PREF_FILE_SEARCH_ENGINES = "custom-search-engines"

/**
 * SearchEngineProvider implementation to load user entered custom search engines.
 */
class CustomSearchEngineProvider : SearchEngineProvider {
    /**
     * Exception type for errors caught while building custom search engine XML
     */
    class SearchEngineParserFailure : Exception("There was a problem with parsing custom search engine")

    internal val searchEngineWriter = CustomSearchEngineWriter()

    override suspend fun loadSearchEngines(context: Context): SearchEngineList {
        val storage = SearchEngineStorage(context)
        val parser = SearchEngineParser()
        val searchEngines = storage.customSearchEngineIds.mapNotNull {
            val engineXml = storage[it] ?: return@mapNotNull null
            val engineInputStream = engineXml.byteInputStream().buffered()
            parser.load(it, engineInputStream)
        }

        return SearchEngineList(searchEngines, null)
    }

    /**
     * Add a search engine to the CustomSearchEngineProvider.
     * @param context [Context] used for [SearchEngineStorage].
     * @param searchEngineName The name of the search engine.
     * @param searchQuery The templated search string for the search engine.
     * @param icons [BrowserIcons] to load icon bitmap.
     * @throws SearchEngineParserFailure if there was any issues while building custom search engine XML.
     */
    suspend fun addSearchEngine(
        context: Context,
        searchEngineName: String,
        searchQuery: String,
        icons: BrowserIcons
    ) {
        val storage = SearchEngineStorage(context)
        if (storage.customSearchEngineIds.contains(searchEngineName)) { return }

        val icon = icons.loadIcon(IconRequest(searchQuery)).await()
        val searchEngineXml = searchEngineWriter.buildSearchEngineXML(searchEngineName, searchQuery, icon.bitmap)
            ?: throw SearchEngineParserFailure()

        val engines = storage.customSearchEngineIds.toMutableSet()
        engines.add(searchEngineName)
        storage.customSearchEngineIds = engines
        storage[searchEngineName] = searchEngineXml
    }

    /**
     * Updates an existing search engine.
     * To prevent duplicate search engines we want to remove the old engine before adding the new one.
     * @param context [Context] used for [SearchEngineStorage]
     * @param oldSearchEngineName the name of the engine you want to replace
     * @param newSearchEngineName the name of the engine you want to save
     * @param searchQuery The templated search string for the search engine
     * @param icons [BrowserIcons] to load icon bitmap.
     */
    suspend fun replaceSearchEngine(
        context: Context,
        oldSearchEngineName: String,
        newSearchEngineName: String,
        searchQuery: String,
        icons: BrowserIcons
    ) {
        removeSearchEngine(context, oldSearchEngineName)
        addSearchEngine(context, newSearchEngineName, searchQuery, icons)
    }

    /**
     * Removes a search engine from the store.
     * @param context [Context] used for [SearchEngineStorage].
     * @param searchEngineId the id of the engine you want to remove.
     */
    fun removeSearchEngine(context: Context, searchEngineId: String) {
        val storage = SearchEngineStorage(context)
        val customEngines = storage.customSearchEngineIds
        storage.customSearchEngineIds = customEngines.filterNot { it == searchEngineId }.toSet()
        storage[searchEngineId] = null
    }

    /**
     * Checks the store to see if it contains a search engine.
     * @param context [Context] used for [SearchEngineStorage].
     * @param searchEngineId The name of the search engine check for contain.
     */
    fun isCustomSearchEngine(context: Context, searchEngineId: String): Boolean {
        val storage = SearchEngineStorage(context)
        return storage.customSearchEngineIds.contains(searchEngineId)
    }

    /**
     * Helper class to help interact with [SharedPreferences]
     * @param context [Context] used for SharedPreferences.
     */
    class SearchEngineStorage(private val context: Context) : PreferencesHolder {
        override val preferences: SharedPreferences =
            context.getSharedPreferences(PREF_FILE_SEARCH_ENGINES, Context.MODE_PRIVATE)

        var customSearchEngineIds by stringSetPreference(PREF_KEY_CUSTOM_SEARCH_ENGINES, emptySet())

        /**
         * Get custom search engine from the storage given id.
         */
        operator fun get(searchEngineId: String): String? {
            return preferences.getString(searchEngineId, null)
        }

        /**
         * Set custom search engine from the storage given id.
         */
        operator fun set(searchEngineId: String, value: String?) {
            preferences.edit().putString(searchEngineId, value).apply()
        }
    }
}
