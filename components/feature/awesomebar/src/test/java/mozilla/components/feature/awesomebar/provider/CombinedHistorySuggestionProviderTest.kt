/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.components.concept.storage.*
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import mozilla.components.support.utils.StorageUtils
import mozilla.components.support.utils.segmentAwareDomainMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doReturn

@RunWith(AndroidJUnit4::class)
class CombinedHistorySuggestionProviderTest {

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


    private val historyEntry = HistoryMetadata(
        key = HistoryMetadataKey("http://www.mozilla.com", null, null),
        title = "mozilla",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        totalViewTime = 10,
        documentType = DocumentType.Regular,
        previewImageUrl = null
    )

    @Test
    fun `GIVEN history items exists WHEN onInputChanged is called with empty text THEN return empty suggestions list`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        whenever(storage.queryHistoryMetadata("moz", DEFAULT_METADATA_SUGGESTION_LIMIT)).thenReturn(listOf(historyEntry))
        val history = TestingHistoryStorage()
        history.recordVisit("http://www.mozilla.com", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        val provider = CombinedHistorySuggestionProvider(history, storage, mock())

        assertTrue(provider.onInputChanged("").isEmpty())
        assertTrue(provider.onInputChanged("  ").isEmpty())
    }

    @Test
    fun `GIVEN more suggestions asked than metadata items exist WHEN user changes input THEN return a combined list of suggestions`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        doReturn(listOf(historyEntry)).`when`(storage).queryHistoryMetadata(eq("moz"), anyInt())
        val history = TestingHistoryStorage()
        history.recordVisit("http://www.mozilla.com/firefox", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        val provider = CombinedHistorySuggestionProvider(history, storage, mock())

        val result = provider.onInputChanged("moz")

        assertEquals(2, result.size)
        assertEquals("http://www.mozilla.com", result[0].description)
        assertEquals("http://www.mozilla.com/firefox", result[1].description)
    }

    @Test
    fun `GIVEN fewer suggestions asked than metadata items exist WHEN user changes input THEN return suggestions only based on metadata items`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        doReturn(listOf(historyEntry)).`when`(storage).queryHistoryMetadata(eq("moz"), anyInt())
        val history = TestingHistoryStorage()
        history.recordVisit("http://www.mozilla.com/firefox", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        val provider = CombinedHistorySuggestionProvider(history, storage, mock(), maxNumberOfSuggestions = 1)

        val result = provider.onInputChanged("moz")

        assertEquals(1, result.size)
        assertEquals("http://www.mozilla.com", result[0].description)
    }

    @Test
    fun `GIVEN only storage history items exist WHEN user changes input THEN return suggestions only based on storage items`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        doReturn(emptyList<HistoryMetadata>()).`when`(storage).queryHistoryMetadata(eq("moz"), anyInt())
        val history = TestingHistoryStorage()
        history.recordVisit("http://www.mozilla.com/firefox", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        val provider = CombinedHistorySuggestionProvider(history, storage, mock(), maxNumberOfSuggestions = 1)

        val result = provider.onInputChanged("moz")

        assertEquals(1, result.size)
        assertEquals("http://www.mozilla.com/firefox", result[0].description)
    }

    @Test
    fun `GIVEN duplicated metadata and storage entries WHEN user changes input THEN return distinct suggestions`() = runBlocking {
        val storage: HistoryMetadataStorage = mock()
        doReturn(listOf(historyEntry)).`when`(storage).queryHistoryMetadata(eq("moz"), anyInt())
        val history = TestingHistoryStorage()
        history.recordVisit(
            "http://www.mozilla.com",
            PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE)
        )
        val provider = CombinedHistorySuggestionProvider(history, storage, mock())

        val result = provider.onInputChanged("moz")

        assertEquals(1, result.size)
        assertEquals("http://www.mozilla.com", result[0].description)
    }

    @Test
    fun `GIVEN a combined list of suggestions WHEN history results exist THEN urls are deduped and scores are adjusted`() = runBlocking {
        val metadataEntry1 = HistoryMetadata(
            key = HistoryMetadataKey("https://www.mozilla.com", null, null),
            title = "mozilla",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            totalViewTime = 10,
            documentType = DocumentType.Regular,
            previewImageUrl = null
        )

        val metadataEntry2 = HistoryMetadata(
            key = HistoryMetadataKey("https://www.mozilla.com/firefox", null, null),
            title = "firefox",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            totalViewTime = 20,
            documentType = DocumentType.Regular,
            previewImageUrl = null
        )

        val searchResult1 = SearchResult(
            id = "1",
            url = "https://www.mozilla.com",
            title = "mozilla",
            score = 1
        )

        val searchResult2 = SearchResult(
            id = "2",
            url = "https://www.mozilla.com/pocket",
            title = "pocket",
            score = 2
        )

        val metadataStorage: HistoryMetadataStorage = mock()
        val historyStorage: TestingHistoryStorage = mock()
        doReturn(listOf(metadataEntry2, metadataEntry1)).`when`(metadataStorage).queryHistoryMetadata(eq("moz"), anyInt())
        doReturn(listOf(searchResult1, searchResult2)).`when`(historyStorage).getSuggestions(eq("moz"), anyInt())

        val provider = CombinedHistorySuggestionProvider(historyStorage, metadataStorage, mock())

        val result = provider.onInputChanged("moz")

        assertEquals(3, result.size)
        assertEquals("https://www.mozilla.com/firefox", result[0].description)
        assertEquals(4, result[0].score)

        assertEquals("https://www.mozilla.com", result[1].description)
        assertEquals(3, result[1].score)

        assertEquals("https://www.mozilla.com/pocket", result[2].description)
        assertEquals(2, result[2].score)
    }
}
