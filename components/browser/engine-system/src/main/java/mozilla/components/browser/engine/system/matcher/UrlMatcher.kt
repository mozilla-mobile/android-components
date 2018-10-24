/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system.matcher

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets.UTF_8
import java.util.LinkedList

/**
 * Provides functionality to process categorized URL black/white lists and match
 * URLs against these lists.
 */
class UrlMatcher {
    private val categories: MutableMap<String, Trie>
    internal val enabledCategories = HashSet<String>()

    private val whiteList: WhiteList?
    private val previouslyMatched = HashSet<String>()
    private val previouslyUnmatched = HashSet<String>()
    private var blockWebfonts = true

    constructor(patterns: Array<String>) {
        categories = HashMap()
        whiteList = null

        val defaultCategory = Trie.createRootNode()
        patterns.forEach { defaultCategory.put(it.reverse()) }
        categories[DEFAULT] = defaultCategory
        enabledCategories.add(DEFAULT)
    }

    internal constructor(
        enabledCategories: Set<String>,
        supportedCategories: Set<String>,
        categoryMap: MutableMap<String, Trie>,
        whiteList: WhiteList? = null
    ) {
        this.whiteList = whiteList
        this.categories = categoryMap

        for ((key) in categoryMap) {
            if (!supportedCategories.contains(key)) {
                throw IllegalArgumentException("categoryMap contains undeclared category")
            }
        }

        enabledCategories.forEach { setCategoryEnabled(it, true) }
    }

    /**
     * Enables the provided categories.
     *
     * @param categories set of categories to enable.
     */
    fun setCategoriesEnabled(categories: Set<String>) {
        if (enabledCategories != categories) {
            enabledCategories.removeAll { it != DEFAULT }
            categories.forEach { setCategoryEnabled(it, true) }
        }
    }

    internal fun setCategoryEnabled(category: String, enabled: Boolean) {
        if (WEBFONTS == category) {
            blockWebfonts = enabled
            return
        }

        if (enabled) {
            if (enabledCategories.contains(category)) {
                return
            } else {
                enabledCategories.add(category)
                previouslyUnmatched.clear()
            }
        } else {
            if (!enabledCategories.contains(category)) {
                return
            } else {
                enabledCategories.remove(category)
                previouslyMatched.clear()
            }
        }
    }

    /**
     * Checks if the given page URI is blacklisted for the given resource URI.
     * Returns true if the site (page URI) is allowed to access
     * the resource URI, otherwise false.
     *
     * @param resourceURI URI of a resource to be loaded by the page
     * @param pageURI URI of the page
     */
    fun matches(resourceURI: String, pageURI: String): Boolean {
        return matches(Uri.parse(resourceURI), Uri.parse(pageURI))
    }

    /**
     * Checks if the given page URI is blacklisted for the given resource URI.
     * Returns true if the site (page URI) is allowed to access
     * the resource URI, otherwise false.
     *
     * @param resourceURI URI of a resource to be loaded by the page
     * @param pageURI URI of the page
     */
    @Suppress("ReturnCount", "ComplexMethod")
    fun matches(resourceURI: Uri, pageURI: Uri): Boolean {
        val resourceURLString = resourceURI.toString()
        val resourceHost = resourceURI.host
        val pageHost = pageURI.host

        if (blockWebfonts && isWebFont(resourceURI)) {
            return true
        }

        if (previouslyUnmatched.contains(resourceURLString)) {
            return false
        }

        if (whiteList?.contains(pageURI, resourceURI) == true) {
            return false
        }

        if (pageHost != null && pageHost == resourceHost) {
            return false
        }

        if (previouslyMatched.contains(resourceURLString)) {
            return true
        }

        for ((key, value) in categories) {
            if (enabledCategories.contains(key) && value.findNode(resourceHost.reverse()) != null) {
                previouslyMatched.add(resourceURLString)
                return true
            }
        }

        previouslyUnmatched.add(resourceURLString)
        return false
    }

    companion object {
        const val ADVERTISING = "Advertising"
        const val ANALYTICS = "Analytics"
        const val CONTENT = "Content"
        const val DISCONNECT = "Disconnect"
        const val SOCIAL = "Social"
        const val WEBFONTS = "Webfonts"
        const val DEFAULT = "default"

        private val ignoredCategories = setOf("Legacy Disconnect", "Legacy Content")
        private val disconnectMoved = setOf("Facebook", "Twitter")
        private val webfontExceptions = arrayOf(".woff2", ".woff", ".eot", ".ttf", ".otf")
        private val supportedCategories = setOf(ADVERTISING, ANALYTICS, SOCIAL, CONTENT, WEBFONTS)

        /**
         * Creates a new matcher instance for the provided URL lists.
         *
         * @param blackListFile resource ID to a JSON file containing the black list
         * @param overrides array of resource ID to JSON files containing black list overrides
         * @param whiteListFile resource ID to a JSON file containing the white list
         */
        fun createMatcher(
            context: Context,
            blackListFile: Int,
            overrides: IntArray?,
            whiteListFile: Int,
            enabledCategories: Set<String> = supportedCategories
        ): UrlMatcher {
            val blackListReader = InputStreamReader(context.resources.openRawResource(blackListFile), UTF_8)
            val whiteListReader = InputStreamReader(context.resources.openRawResource(whiteListFile), UTF_8)
            val overrideReaders = overrides?.map { InputStreamReader(context.resources.openRawResource(it), UTF_8) }
            return createMatcher(blackListReader, overrideReaders, whiteListReader, enabledCategories)
        }

        /**
         * Creates a new matcher instance for the provided URL lists.
         *
         * @param black reader containing the black list
         * @param overrides array of resource ID to JSON files containing black list overrides
         * @param white resource ID to a JSON file containing the white list
         */
        fun createMatcher(
            black: Reader,
            overrides: List<Reader>?,
            white: Reader,
            enabledCategories: Set<String> = supportedCategories
        ): UrlMatcher {
            val categoryMap = HashMap<String, Trie>()

            JsonReader(black).use {
                jsonReader -> loadCategories(jsonReader, categoryMap)
            }

            overrides?.forEach {
                JsonReader(it).use {
                    jsonReader -> loadCategories(jsonReader, categoryMap, true)
                }
            }

            var whiteList: WhiteList? = null
            JsonReader(white).use { jsonReader -> whiteList = WhiteList.fromJson(jsonReader) }
            return UrlMatcher(enabledCategories, supportedCategories, categoryMap, whiteList)
        }

        /**
         * Checks if the given URI points to a Web font.
         * @param uri the URI to check.
         *
         * @return true if the URI is a Web font, otherwise false.
         */
        fun isWebFont(uri: Uri): Boolean {
            val path = uri.path ?: return false
            return webfontExceptions.find { path.endsWith(it) } != null
        }

        private fun loadCategories(
            reader: JsonReader,
            categoryMap: MutableMap<String, Trie>,
            override: Boolean = false
        ): Map<String, Trie> {
            reader.beginObject()

            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "categories") {
                    extractCategories(reader, categoryMap, override)
                } else {
                    reader.skipValue()
                }
            }

            reader.endObject()
            return categoryMap
        }

        @Suppress("ThrowsCount", "ComplexMethod", "NestedBlockDepth")
        private fun extractCategories(reader: JsonReader, categoryMap: MutableMap<String, Trie>, override: Boolean) {
            reader.beginObject()

            val socialOverrides = LinkedList<String>()
            while (reader.hasNext()) {
                val categoryName = reader.nextName()
                when {
                    ignoredCategories.contains(categoryName) -> reader.skipValue()
                    categoryName == DISCONNECT -> {
                        extractCategory(reader) { url, owner ->
                            if (disconnectMoved.contains(owner)) socialOverrides.add(url)
                        }
                    }
                    else -> {
                        val categoryTrie: Trie?
                        if (!override) {
                            if (categoryMap.containsKey(categoryName)) {
                                throw IllegalStateException("Cannot insert already loaded category")
                            }
                            categoryTrie = Trie.createRootNode()
                            categoryMap[categoryName] = categoryTrie
                        } else {
                            categoryTrie = categoryMap[categoryName]
                            if (categoryTrie == null) {
                                throw IllegalStateException("Cannot add override items to nonexistent category")
                            }
                        }
                        extractCategory(reader) { url, _ -> categoryTrie.put(url.reverse()) }
                    }
                }
            }

            val socialTrie = categoryMap[SOCIAL]
            if (socialTrie == null && !override) {
                throw IllegalStateException("Expected social list to exist")
            }
            socialOverrides.forEach { socialTrie?.put(it.reverse()) }
            reader.endObject()
        }

        private fun extractCategory(reader: JsonReader, callback: (String, String) -> Unit) {
            reader.beginArray()
            while (reader.hasNext()) {
                extractSite(reader, callback)
            }
            reader.endArray()
        }

        private fun extractSite(reader: JsonReader, callback: (String, String) -> Unit) {
            reader.beginObject()
            val siteOwner = reader.nextName()

            reader.beginObject()
            while (reader.hasNext()) {
                reader.skipValue()
                val nextToken = reader.peek()
                if (nextToken.name == "STRING") {
                    // Sometimes there's a "dnt" entry, with unspecified purpose.
                    reader.skipValue()
                } else {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        callback(reader.nextString(), siteOwner)
                    }
                    reader.endArray()
                }
            }
            reader.endObject()

            reader.endObject()
        }
    }
}
