/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.DefaultSearchEngineProvider
import mozilla.components.browser.search.SearchEngine
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchActionProviderTest {
    @Test
    fun `provider returns no suggestion for empty text`() {
        val provider = SearchActionProvider(mock(), mock())
        val suggestions = runBlocking { provider.onInputChanged("") }

        assertEquals(0, suggestions.size)
    }

    @Test
    fun `provider returns no suggestion for blank text`() {
        val provider = SearchActionProvider(mock(), mock())
        val suggestions = runBlocking { provider.onInputChanged("     ") }

        assertEquals(0, suggestions.size)
    }

    @Test
    fun `provider returns suggestion matching input`() {
        val provider = SearchActionProvider(
            defaultSearchEngineProvider = object : DefaultSearchEngineProvider {
                override fun getDefaultSearchEngine(): SearchEngine? = mock()
                override suspend fun retrieveDefaultSearchEngine(): SearchEngine? = mock()
            },
            searchUseCase = mock()
        )
        val suggestions = runBlocking { provider.onInputChanged("firefox") }

        assertEquals(1, suggestions.size)

        val suggestion = suggestions[0]

        assertEquals("firefox", suggestion.title)
    }
}
