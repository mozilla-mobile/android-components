/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.search.suggestions

import kotlinx.coroutines.experimental.runBlocking
import mozilla.components.browser.search.SearchEngineParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class SearchSuggestionClientTest {
    companion object {
        val GOOGLE_MOCK_RESPONSE: SearchSuggestionFetcher = { "[\"firefox\",[\"firefox\",\"firefox for mac\",\"firefox quantum\",\"firefox update\",\"firefox esr\",\"firefox focus\",\"firefox addons\",\"firefox extensions\",\"firefox nightly\",\"firefox clear cache\"]]" }
        val QWANT_MOCK_RESPONSE: SearchSuggestionFetcher = { "{\"status\":\"success\",\"data\":{\"items\":[{\"value\":\"firefox (video game)\",\"suggestType\":3},{\"value\":\"firefox addons\",\"suggestType\":12},{\"value\":\"firefox\",\"suggestType\":2},{\"value\":\"firefox quantum\",\"suggestType\":12},{\"value\":\"firefox focus\",\"suggestType\":12}],\"special\":[],\"availableQwick\":[]}}" }
        val SERVER_ERROR_RESPONSE: SearchSuggestionFetcher = { "Server error. Try again later" }
    }

    @Test
    fun `Get a list of results based on the Google search engine`() {
        val searchEngine = SearchEngineParser().load(
                RuntimeEnvironment.application.assets,
                "google", "searchplugins/google-nocodes.xml")

        val client = SearchSuggestionClient(searchEngine, GOOGLE_MOCK_RESPONSE)

        runBlocking {
            val results = client.getSuggestions("firefox")
            val expectedResults = listOf("firefox", "firefox for mac", "firefox quantum", "firefox update", "firefox esr", "firefox focus", "firefox addons", "firefox extensions", "firefox nightly", "firefox clear cache")

            assertEquals(expectedResults, results)
        }
    }

    @Test
    fun `Get a list of results based on a non google search engine`() {
        val searchEngine = SearchEngineParser().load(
                RuntimeEnvironment.application.assets,
                "google", "searchplugins/qwant.xml")

        val client = SearchSuggestionClient(searchEngine, QWANT_MOCK_RESPONSE)

        runBlocking {
            val results = client.getSuggestions("firefox")
            val expectedResults = listOf("firefox (video game)", "firefox addons", "firefox", "firefox quantum", "firefox focus")

            assertEquals(expectedResults, results)
        }
    }

    @Test(expected = SearchSuggestionClient.ResponseParserException::class)
    fun `Check that a bad response will throw a parser exception`() {
        val searchEngine = SearchEngineParser().load(
                RuntimeEnvironment.application.assets,
                "google", "searchplugins/google-nocodes.xml")

        val client = SearchSuggestionClient(searchEngine, SERVER_ERROR_RESPONSE)

        runBlocking {
            client.getSuggestions("firefox")
        }
    }

    @Test(expected = SearchSuggestionClient.FetchException::class)
    fun `Check that an exception in the suggestionFetcher will re-throw an IOException`() {
        val searchEngine = SearchEngineParser().load(
                RuntimeEnvironment.application.assets,
                "google", "searchplugins/google-nocodes.xml")

        val client = SearchSuggestionClient(searchEngine, { throw IOException() })

        runBlocking {
            client.getSuggestions("firefox")
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Check that a search engine without a suggestURI will throw an exception`() {
        val searchEngine = SearchEngineParser().load(
                RuntimeEnvironment.application.assets,
                "drae", "searchplugins/drae.xml")

        val client = SearchSuggestionClient(searchEngine, { "no-op" })
    }
}
