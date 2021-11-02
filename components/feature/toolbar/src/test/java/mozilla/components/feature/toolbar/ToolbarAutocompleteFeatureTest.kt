/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.toolbar

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.domains.Domain
import mozilla.components.browser.domains.autocomplete.BaseDomainAutocompleteProvider
import mozilla.components.browser.domains.autocomplete.DomainList
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.*
import mozilla.components.concept.toolbar.AutocompleteDelegate
import mozilla.components.concept.toolbar.AutocompleteResult
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.utils.StorageUtils.levenshteinDistance
import mozilla.components.support.utils.segmentAwareDomainMatch
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class ToolbarAutocompleteFeatureTest {

    class TestToolbar : Toolbar {
        override var highlight: Toolbar.Highlight = Toolbar.Highlight.NONE
        override var siteTrackingProtection: Toolbar.SiteTrackingProtection =
            Toolbar.SiteTrackingProtection.OFF_GLOBALLY
        override var title: String = ""
        override var url: CharSequence = ""
        override var siteSecure: Toolbar.SiteSecurity = Toolbar.SiteSecurity.INSECURE
        override var private: Boolean = false

        var autocompleteFilter: (suspend (String, AutocompleteDelegate) -> Unit)? = null

        override fun setSearchTerms(searchTerms: String) {
            fail()
        }

        override fun displayProgress(progress: Int) {
            fail()
        }

        override fun onBackPressed(): Boolean {
            fail()
            return false
        }

        override fun onStop() {
            fail()
        }

        override fun setOnUrlCommitListener(listener: (String) -> Boolean) {
            fail()
        }

        override fun setAutocompleteListener(filter: suspend (String, AutocompleteDelegate) -> Unit) {
            autocompleteFilter = filter
        }

        override fun addBrowserAction(action: Toolbar.Action) {
            fail()
        }

        override fun removeBrowserAction(action: Toolbar.Action) {
            fail()
        }

        override fun removePageAction(action: Toolbar.Action) {
            fail()
        }

        override fun addPageAction(action: Toolbar.Action) {
            fail()
        }

        override fun addNavigationAction(action: Toolbar.Action) {
            fail()
        }

        override fun setOnEditListener(listener: Toolbar.OnEditListener) {
            fail()
        }

        override fun displayMode() {
            fail()
        }

        override fun editMode() {
            fail()
        }

        override fun addEditAction(action: Toolbar.Action) {
            fail()
        }

        override fun invalidateActions() {
            fail()
        }

        override fun dismissMenu() {
            fail()
        }

        override fun enableScrolling() {
            fail()
        }

        override fun disableScrolling() {
            fail()
        }

        override fun collapse() {
            fail()
        }

        override fun expand() {
            fail()
        }
    }

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
                Hit(it.key, levenshteinDistance(it.key, query))
            }
            val titleMatches = pageMeta.asSequence().map {
                Hit(it.key, levenshteinDistance(it.value.title ?: "", query))
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

    @Test
    fun `feature can be used without providers`() {
        val toolbar = TestToolbar()

        ToolbarAutocompleteFeature(toolbar)

        assertNotNull(toolbar.autocompleteFilter)

        val autocompleteDelegate: AutocompleteDelegate = mock()
        runBlocking {
            toolbar.autocompleteFilter!!("moz", autocompleteDelegate)
        }
        verify(autocompleteDelegate, never()).applyAutocompleteResult(any(), any())
        verify(autocompleteDelegate, times(1)).noAutocompleteResult("moz")
    }

    @Test
    fun `feature can be configured with providers`() {
        val toolbar = TestToolbar()
        var feature = ToolbarAutocompleteFeature(toolbar)
        val autocompleteDelegate: AutocompleteDelegate = mock()

        var history: HistoryStorage = TestingHistoryStorage()
        val domains = object : BaseDomainAutocompleteProvider(DomainList.CUSTOM, { emptyList() }) {
            fun testDomains(list: List<Domain>) {
                domains = list
            }
        }

        // Can autocomplete with just an empty history provider.
        feature.addHistoryStorageProvider(history)
        verifyNoAutocompleteResult(toolbar, autocompleteDelegate, "hi")

        // Can autocomplete with a non-empty history provider.
        runBlocking {
            history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        }

        verifyNoAutocompleteResult(toolbar, autocompleteDelegate, "hi")
        verifyAutocompleteResult(
            toolbar, autocompleteDelegate, "mo",
            AutocompleteResult(
                input = "mo",
                text = "mozilla.org",
                url = "https://www.mozilla.org",
                source = "memoryHistory", totalItems = 1
            )
        )

        // Can autocomplete with just an empty domain provider.
        feature = ToolbarAutocompleteFeature(toolbar)
        feature.addDomainProvider(domains)

        verifyNoAutocompleteResult(toolbar, autocompleteDelegate, "hi")

        // Can autocomplete with a non-empty domain provider.
        domains.testDomains(
            listOf(
                Domain.create("https://www.mozilla.org")
            )
        )

        verifyNoAutocompleteResult(toolbar, autocompleteDelegate, "hi")
        verifyAutocompleteResult(
            toolbar, autocompleteDelegate, "mo",
            AutocompleteResult(
                input = "mo",
                text = "mozilla.org",
                url = "https://www.mozilla.org",
                source = "custom",
                totalItems = 1
            )
        )

        // Can autocomplete with empty history and domain providers.
        history = TestingHistoryStorage()
        domains.testDomains(listOf())
        feature.addHistoryStorageProvider(history)

        verifyNoAutocompleteResult(toolbar, autocompleteDelegate, "hi")

        // Can autocomplete with both domains providing data; test that history is prioritized,
        // falling back to domains.
        domains.testDomains(
            listOf(
                Domain.create("https://www.mozilla.org"),
                Domain.create("https://moscow.ru")
            )
        )

        verifyAutocompleteResult(
            toolbar, autocompleteDelegate, "mo",
            AutocompleteResult(
                input = "mo",
                text = "mozilla.org",
                url = "https://www.mozilla.org",
                source = "custom",
                totalItems = 2
            )
        )

        runBlocking {
            history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        }

        verifyAutocompleteResult(
            toolbar, autocompleteDelegate, "mo",
            AutocompleteResult(
                input = "mo",
                text = "mozilla.org",
                url = "https://www.mozilla.org",
                source = "memoryHistory",
                totalItems = 1
            )
        )

        verifyAutocompleteResult(
            toolbar, autocompleteDelegate, "mos",
            AutocompleteResult(
                input = "mos",
                text = "moscow.ru",
                url = "https://moscow.ru",
                source = "custom",
                totalItems = 2
            )
        )
    }

    @Test
    fun `feature triggers speculative connect for results if engine provided`() {
        val toolbar = TestToolbar()
        val engine: Engine = mock()
        var feature = ToolbarAutocompleteFeature(toolbar, engine)
        val autocompleteDelegate: AutocompleteDelegate = mock()

        val domains = object : BaseDomainAutocompleteProvider(DomainList.CUSTOM, { emptyList() }) {
            fun testDomains(list: List<Domain>) {
                domains = list
            }
        }
        domains.testDomains(listOf(Domain.create("https://www.mozilla.org")))
        feature.addDomainProvider(domains)

        runBlocking {
            toolbar.autocompleteFilter!!.invoke("mo", autocompleteDelegate)
        }

        val callbackCaptor = argumentCaptor<() -> Unit>()
        verify(autocompleteDelegate, times(1)).applyAutocompleteResult(any(), callbackCaptor.capture())
        verify(engine, never()).speculativeConnect("https://www.mozilla.org")
        callbackCaptor.value.invoke()
        verify(engine).speculativeConnect("https://www.mozilla.org")
    }

    @Suppress("SameParameterValue")
    private fun verifyNoAutocompleteResult(toolbar: TestToolbar, autocompleteDelegate: AutocompleteDelegate, query: String) {
        runBlocking {
            toolbar.autocompleteFilter!!(query, autocompleteDelegate)
        }
        verify(autocompleteDelegate, never()).applyAutocompleteResult(any(), any())
        verify(autocompleteDelegate, times(1)).noAutocompleteResult(query)
        reset(autocompleteDelegate)
    }

    private fun verifyAutocompleteResult(toolbar: TestToolbar, autocompleteDelegate: AutocompleteDelegate, query: String, result: AutocompleteResult) {
        runBlocking {
            toolbar.autocompleteFilter!!.invoke(query, autocompleteDelegate)
        }
        verify(autocompleteDelegate, times(1)).applyAutocompleteResult(eq(result), any())
        verify(autocompleteDelegate, never()).noAutocompleteResult(query)
        reset(autocompleteDelegate)
    }
}
