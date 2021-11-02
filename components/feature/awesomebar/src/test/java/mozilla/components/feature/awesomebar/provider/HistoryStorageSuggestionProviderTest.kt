/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import kotlinx.coroutines.runBlocking
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.*
import mozilla.components.feature.awesomebar.facts.AwesomeBarFacts
import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Action
import mozilla.components.support.base.facts.Fact
import mozilla.components.support.base.facts.FactProcessor
import mozilla.components.support.base.facts.Facts
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.utils.StorageUtils
import mozilla.components.support.utils.segmentAwareDomainMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*

class HistoryStorageSuggestionProviderTest {

    class TestingHistoryStorage : HistoryStorage {

        data class Visit(val timestamp: Long, val type: VisitType)

        val AUTOCOMPLETE_SOURCE_NAME = "memoryHistory"

        var pages: HashMap<String, MutableList<Visit>> = linkedMapOf()
        val pageMeta: HashMap<String, PageObservation> = hashMapOf()

        override suspend fun warmUp() {
            // No-op for an in-memory store
        }

        override suspend fun recordVisit(uri: String, visit: PageVisit) {
            val now = System.currentTimeMillis()
            if (visit.redirectSource != RedirectSource.NOT_A_SOURCE) {
                return
            }

            synchronized(pages) {
                if (!pages.containsKey(uri)) {
                    pages[uri] = mutableListOf(Visit(now, visit.visitType))
                } else {
                    pages[uri]!!.add(Visit(now, visit.visitType))
                }
            }
        }

        override suspend fun recordObservation(uri: String, observation: PageObservation) = synchronized(pageMeta) {
            val existingPageObservation = pageMeta[uri]

            if (existingPageObservation == null ||
                (!observation.title.isNullOrEmpty() && !observation.previewImageUrl.isNullOrEmpty())
            ) {
                pageMeta[uri] = observation
            } else if (!observation.title.isNullOrEmpty()) {
                // Carryover the existing observed previewImageUrl
                pageMeta[uri] = observation.copy(previewImageUrl = existingPageObservation.previewImageUrl)
            } else {
                // Carryover the existing observed title
                pageMeta[uri] = observation.copy(title = existingPageObservation.title)
            }
        }

        override suspend fun getVisited(uris: List<String>): List<Boolean> = synchronized(pages) {
            return uris.map {
                if (pages[it] != null && pages[it]!!.size > 0) {
                    return@map true
                }
                return@map false
            }
        }

        override suspend fun getVisited(): List<String> = synchronized(pages) {
            return pages.keys.toList()
        }

        override suspend fun getVisitsPaginated(offset: Long, count: Long, excludeTypes: List<VisitType>): List<VisitInfo> {
            throw UnsupportedOperationException("Pagination is not yet supported by the in-memory history storage")
        }

        override suspend fun getDetailedVisits(
            start: Long,
            end: Long,
            excludeTypes: List<VisitType>
        ): List<VisitInfo> = synchronized(pages + pageMeta) {
            val visits = mutableListOf<VisitInfo>()

            pages.forEach {
                it.value.forEach { visit ->
                    if (visit.timestamp in start..end && !excludeTypes.contains(visit.type)) {
                        visits.add(
                            VisitInfo(
                                url = it.key,
                                title = pageMeta[it.key]?.title,
                                visitTime = visit.timestamp,
                                visitType = visit.type,
                                previewImageUrl = pageMeta[it.key]?.previewImageUrl
                            )
                        )
                    }
                }
            }

            return visits
        }

        override suspend fun getTopFrecentSites(
            numItems: Int,
            frecencyThreshold: FrecencyThresholdOption
        ): List<TopFrecentSiteInfo> {
            throw UnsupportedOperationException("getTopFrecentSites is not yet supported by the in-memory history storage")
        }

        override fun getSuggestions(query: String, limit: Int): List<SearchResult> = synchronized(pages + pageMeta) {
            data class Hit(val url: String, val score: Int)

            val urlMatches = pages.asSequence().map {
                Hit(it.key, StorageUtils.levenshteinDistance(it.key, query))
            }
            val titleMatches = pageMeta.asSequence().map {
                Hit(it.key, StorageUtils.levenshteinDistance(it.value.title ?: "", query))
            }
            val matchedUrls = mutableMapOf<String, Int>()
            urlMatches.plus(titleMatches).forEach {
                if (matchedUrls.containsKey(it.url) && matchedUrls[it.url]!! < it.score) {
                    matchedUrls[it.url] = it.score
                } else {
                    matchedUrls[it.url] = it.score
                }
            }
            // Calculate maxScore so that we can invert our scoring.
            // Lower Levenshtein distance should produce a higher score.
            val maxScore = urlMatches.maxByOrNull { it.score }?.score ?: return@synchronized listOf()

            matchedUrls.asSequence().sortedBy { it.value }.map {
                SearchResult(id = it.key, score = maxScore - it.value, url = it.key, title = pageMeta[it.key]?.title)
            }.take(limit).toList()
        }

        override fun getAutocompleteSuggestion(query: String): HistoryAutocompleteResult? = synchronized(pages) {
            return segmentAwareDomainMatch(query, pages.keys)?.let { urlMatch ->
                HistoryAutocompleteResult(
                    query, urlMatch.matchedSegment, urlMatch.url, AUTOCOMPLETE_SOURCE_NAME, pages.size
                )
            }
        }

        override suspend fun deleteEverything() = synchronized(pages + pageMeta) {
            pages.clear()
            pageMeta.clear()
        }

        override suspend fun deleteVisitsSince(since: Long) = synchronized(pages) {
            pages.entries.forEach {
                it.setValue(it.value.filterNot { visit -> visit.timestamp >= since }.toMutableList())
            }
            pages = pages.filter { it.value.isNotEmpty() } as HashMap<String, MutableList<Visit>>
        }

        override suspend fun deleteVisitsBetween(startTime: Long, endTime: Long) = synchronized(pages) {
            pages.entries.forEach {
                it.setValue(
                    it.value.filterNot { visit ->
                        visit.timestamp >= startTime && visit.timestamp <= endTime
                    }.toMutableList()
                )
            }
            pages = pages.filter { it.value.isNotEmpty() } as HashMap<String, MutableList<Visit>>
        }

        override suspend fun deleteVisitsFor(url: String) = synchronized(pages + pageMeta) {
            pages.remove(url)
            pageMeta.remove(url)
            Unit
        }

        override suspend fun deleteVisit(url: String, timestamp: Long) = synchronized(pages) {
            if (pages.containsKey(url)) {
                pages[url] = pages[url]!!.filter { it.timestamp != timestamp }.toMutableList()
            }
        }

        override suspend fun prune() {
            // Not applicable.
        }

        override suspend fun runMaintenance() {
            // Not applicable.
        }

        override fun cleanup() {
            // GC will take care of our internal data structures, so there's nothing to do here.
        }
    }

    @Before
    fun setup() {
        Facts.clearProcessors()
    }

    @Test
    fun `Provider returns empty list when text is empty`() = runBlocking {
        val provider = HistoryStorageSuggestionProvider(mock(), mock())

        val suggestions = provider.onInputChanged("")
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `Provider returns suggestions from configured history storage`() = runBlocking {
        val history = TestingHistoryStorage()
        val provider = HistoryStorageSuggestionProvider(history, mock())

        history.recordVisit("http://www.mozilla.com", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))

        val suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)
        assertEquals("http://www.mozilla.com", suggestions[0].description)
    }

    @Test
    fun `Provider limits number of returned suggestions to 20 by default`() = runBlocking {
        val history = TestingHistoryStorage()
        val provider = HistoryStorageSuggestionProvider(history, mock())

        for (i in 1..100) {
            history.recordVisit("http://www.mozilla.com/$i", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        }

        val suggestions = provider.onInputChanged("moz")
        assertEquals(20, suggestions.size)
    }

    @Test
    fun `Provider allows lowering the number of returned suggestions beneath the default`() = runBlocking {
        val history = TestingHistoryStorage()
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
        val history = TestingHistoryStorage()
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
        val history = TestingHistoryStorage()
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

    @Test
    fun `fact is emitted when suggestion is clicked`() = runBlocking {
        val history = TestingHistoryStorage()
        val engine: Engine = mock()
        val provider = HistoryStorageSuggestionProvider(history, mock(), engine = engine)

        var suggestions = provider.onInputChanged("")
        assertTrue(suggestions.isEmpty())
        verify(engine, never()).speculativeConnect(anyString())

        history.recordVisit("http://www.mozilla.com", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))

        suggestions = provider.onInputChanged("moz")
        assertEquals(1, suggestions.size)

        val emittedFacts = mutableListOf<Fact>()
        Facts.registerProcessor(object : FactProcessor {
            override fun process(fact: Fact) {
                emittedFacts.add(fact)
            }
        })

        suggestions[0].onSuggestionClicked?.invoke()
        assertTrue(emittedFacts.isNotEmpty())
        assertEquals(
            Fact(
                Component.FEATURE_AWESOMEBAR,
                Action.INTERACTION,
                AwesomeBarFacts.Items.HISTORY_SUGGESTION_CLICKED
            ),
            emittedFacts.first()
        )
    }
}
