/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.search.provider

import kotlinx.coroutines.experimental.runBlocking
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.provider.filter.SearchEngineFilter
import mozilla.components.browser.search.provider.localization.SearchLocalizationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AssetsSearchEngineProviderTest {
    @Test
    fun `Load search engines for en-US from assets`() = runBlocking {
        val localizationProvider = object : SearchLocalizationProvider() {
            override val country: String = "US"
            override val language = "en"
            override val region: String? = null
        }

        val searchEngineProvider = AssetsSearchEngineProvider(localizationProvider)

        val searchEngines = searchEngineProvider.loadSearchEngines(RuntimeEnvironment.application)

        assertEquals(6, searchEngines.size)
    }

    @Test
    fun `Load search engines for en-US with with filter`() = runBlocking {
        val localizationProvider = object : SearchLocalizationProvider() {
            override val country: String = "US"
            override val language = "en"
            override val region: String? = null
        }

        val filter = object : SearchEngineFilter {
            private val exclude = listOf("yahoo", "bing", "ddg")

            override fun filter(searchEngine: SearchEngine): Boolean {
                return !exclude.contains(searchEngine.identifier)
            }
        }

        val searchEngineProvider = AssetsSearchEngineProvider(localizationProvider, listOf(filter))

        val searchEngines = searchEngineProvider.loadSearchEngines(RuntimeEnvironment.application)

        assertEquals(4, searchEngines.size)
    }

    @Test
    fun `Load search engines for de-DE with US region override`() = runBlocking {
        run {
            val localizationProviderWithoutRegion = object : SearchLocalizationProvider() {
                override val country: String = "DE"
                override val language = "de"
                override val region: String? = null
            }

            val searchEngineProvider = AssetsSearchEngineProvider(localizationProviderWithoutRegion)
            val searchEngines = searchEngineProvider.loadSearchEngines(RuntimeEnvironment.application)

            assertEquals(7, searchEngines.size)
            assertContainsSearchEngine("google", searchEngines)
            assertContainsNotSearchEngine("google-2018", searchEngines)
        }

        run {
            val localizationProviderWithRegion = object : SearchLocalizationProvider() {
                override val country: String = "DE"
                override val language = "de"
                override val region: String? = "US"
            }

            val searchEngineProvider = AssetsSearchEngineProvider(localizationProviderWithRegion)
            val searchEngines = searchEngineProvider.loadSearchEngines(RuntimeEnvironment.application)

            assertEquals(7, searchEngines.size)
            assertContainsSearchEngine("google-2018", searchEngines)
            assertContainsNotSearchEngine("google", searchEngines)
        }
    }

    @Test
    fun `load search engines for locale not in configuration`() = runBlocking {
        val provider = object : SearchLocalizationProvider() {
            override val country: String = "XX"
            override val language = "xx"
            override val region: String? = null
        }

        val searchEngineProvider = AssetsSearchEngineProvider(provider)
        val searchEngines = searchEngineProvider.loadSearchEngines(RuntimeEnvironment.application)

        assertEquals(6, searchEngines.size)
    }

    private fun assertContainsSearchEngine(identifier: String, searchEngines: List<SearchEngine>) {
        searchEngines.forEach {
            if (identifier == it.identifier) {
                return
            }
        }
        throw AssertionError("Search engine $identifier not in list")
    }

    private fun assertContainsNotSearchEngine(identifier: String, searchEngines: List<SearchEngine>) {
        searchEngines.forEach {
            if (identifier == it.identifier) {
                throw AssertionError("Search engine $identifier in list")
            }
        }
    }
}
