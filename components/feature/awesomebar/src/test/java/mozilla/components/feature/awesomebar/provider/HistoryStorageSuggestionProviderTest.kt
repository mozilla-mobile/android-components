/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import kotlinx.coroutines.runBlocking
import mozilla.components.browser.storage.memory.InMemoryHistoryStorage
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.storage.PageVisit
import mozilla.components.concept.storage.RedirectSource
import mozilla.components.concept.storage.SearchResult
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class HistoryStorageSuggestionProviderTest {

    @Test
    fun `Provider returns empty list when text is empty`() = runBlocking {
        val provider = HistoryStorageSuggestionProvider(mock(), mock())

        val suggestions = provider.onInputChanged("")
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `Provider returns suggestions from configured history storage`() = runBlocking {
        val history = InMemoryHistoryStorage()
        val provider = HistoryStorageSuggestionProvider(history, mock())

        history.recordVisit("http://www.mozilla.com", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))

        val suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)
        assertEquals("http://www.mozilla.com", suggestions[0].description)
    }

    @Test
    fun `Provider limits number of returned suggestions to 20 by default`() = runBlocking {
        val history = InMemoryHistoryStorage()
        val provider = HistoryStorageSuggestionProvider(history, mock())

        for (i in 1..100) {
            history.recordVisit("http://www.mozilla.com/$i", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        }

        val suggestions = provider.onInputChanged("moz")
        assertEquals(20, suggestions.size)
    }

    @Test
    fun `Provider allows lowering the number of returned suggestions beneath the default`() = runBlocking {
        val history = InMemoryHistoryStorage()
        val provider = HistoryStorageSuggestionProvider(
            historyStorage = history, loadUrlUseCase = mock(), maxNumberOfSuggestions = 2
        )

        for (i in 1..50) {
            history.recordVisit("http://www.mozilla.com/$i", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        }

        val suggestions = provider.onInputChanged("moz")
        assertEquals(2, suggestions.size)
    }

    @Test
    fun `Provider allows increasing the number of returned suggestions above the default`() = runBlocking {
        val history = InMemoryHistoryStorage()
        val provider = HistoryStorageSuggestionProvider(
            historyStorage = history, loadUrlUseCase = mock(), maxNumberOfSuggestions = 22
        )

        for (i in 1..50) {
            history.recordVisit("http://www.mozilla.com/$i", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        }

        val suggestions = provider.onInputChanged("moz")
        assertEquals(22, suggestions.size)
    }

    @Test
    fun `Provider suggestion should not get cleared when text changes`() {
        val provider = HistoryStorageSuggestionProvider(mock(), mock())
        assertFalse(provider.shouldClearSuggestions)
    }

    @Test
    fun `Provider dedupes suggestions`() = runBlocking {
        val storage: HistoryStorage = mock()

        val provider = HistoryStorageSuggestionProvider(storage, mock())

        val mozSuggestions = listOf(
            SearchResult(id = "http://www.mozilla.com/", url = "http://www.mozilla.com/", score = 1),
            SearchResult(id = "http://www.mozilla.com/", url = "http://www.mozilla.com/", score = 2),
            SearchResult(id = "http://www.mozilla.com/", url = "http://www.mozilla.com/", score = 3)
        )

        val pocketSuggestions = listOf(
            SearchResult(id = "http://www.getpocket.com/", url = "http://www.getpocket.com/", score = 5)
        )

        val exampleSuggestions = listOf(
            SearchResult(id = "http://www.example.com", url = "http://www.example.com/", score = 2)
        )

        `when`(storage.getSuggestions(eq("moz"), eq(DEFAULT_HISTORY_SUGGESTION_LIMIT))).thenReturn(mozSuggestions)
        `when`(storage.getSuggestions(eq("pocket"), eq(DEFAULT_HISTORY_SUGGESTION_LIMIT))).thenReturn(pocketSuggestions)
        `when`(storage.getSuggestions(eq("www"), eq(DEFAULT_HISTORY_SUGGESTION_LIMIT))).thenReturn(pocketSuggestions + mozSuggestions + exampleSuggestions)

        var results = provider.onInputChanged("moz")
        assertEquals(1, results.size)
        assertEquals(3, results[0].score)

        results = provider.onInputChanged("pocket")
        assertEquals(1, results.size)
        assertEquals(5, results[0].score)

        results = provider.onInputChanged("www")
        assertEquals(3, results.size)
        assertEquals(5, results[0].score)
        assertEquals(3, results[1].score)
        assertEquals(2, results[2].score)
    }

    @Test
    fun `provider calls speculative connect for URL of highest scored suggestion`() = runBlocking {
        val history = InMemoryHistoryStorage()
        val engine: Engine = mock()
        val provider = HistoryStorageSuggestionProvider(history, mock(), engine = engine)

        var suggestions = provider.onInputChanged("")
        assertTrue(suggestions.isEmpty())
        verify(engine, never()).speculativeConnect(anyString())

        history.recordVisit("http://www.mozilla.com", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))

        suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)
        assertEquals("http://www.mozilla.com", suggestions[0].description)
        verify(engine, times(1)).speculativeConnect(suggestions[0].description!!)
    }
}
