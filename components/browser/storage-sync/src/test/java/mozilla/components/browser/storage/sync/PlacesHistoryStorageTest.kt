/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.appservices.places.InternalPanic
import mozilla.appservices.places.PlacesException
import mozilla.appservices.places.PlacesReaderConnection
import mozilla.appservices.places.PlacesWriterConnection
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.PageVisit
import mozilla.components.concept.storage.RedirectSource
import mozilla.components.concept.storage.VisitType
import mozilla.components.concept.sync.SyncAuthInfo
import mozilla.components.concept.sync.SyncStatus
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlacesHistoryStorageTest {
    private lateinit var history: PlacesHistoryStorage

    @Before
    fun setup() = runBlocking {
        history = PlacesHistoryStorage(testContext)
        // There's a database on disk which needs to be cleaned up between tests.
        history.deleteEverything()
    }

    @After
    fun cleanup() = runBlocking {
        history.cleanup()
    }

    @Test
    fun `storage allows recording and querying visits of different types`() = runBlocking {
        history.recordVisit("http://www.firefox.com/1", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/2", PageVisit(VisitType.RELOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/3", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/4", PageVisit(VisitType.REDIRECT_TEMPORARY, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/5", PageVisit(VisitType.REDIRECT_PERMANENT, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/6", PageVisit(VisitType.FRAMED_LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/7", PageVisit(VisitType.EMBED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/8", PageVisit(VisitType.BOOKMARK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com/9", PageVisit(VisitType.DOWNLOAD, RedirectSource.NOT_A_SOURCE))

        val recordedVisits = history.getDetailedVisits(0)
        assertEquals(9, recordedVisits.size)
        assertEquals("http://www.firefox.com/1", recordedVisits[0].url)
        assertEquals(VisitType.LINK, recordedVisits[0].visitType)
        assertEquals("http://www.firefox.com/2", recordedVisits[1].url)
        assertEquals(VisitType.RELOAD, recordedVisits[1].visitType)
        assertEquals("http://www.firefox.com/3", recordedVisits[2].url)
        assertEquals(VisitType.TYPED, recordedVisits[2].visitType)
        assertEquals("http://www.firefox.com/4", recordedVisits[3].url)
        assertEquals(VisitType.REDIRECT_TEMPORARY, recordedVisits[3].visitType)
        assertEquals("http://www.firefox.com/5", recordedVisits[4].url)
        assertEquals(VisitType.REDIRECT_PERMANENT, recordedVisits[4].visitType)
        assertEquals("http://www.firefox.com/6", recordedVisits[5].url)
        assertEquals(VisitType.FRAMED_LINK, recordedVisits[5].visitType)
        assertEquals("http://www.firefox.com/7", recordedVisits[6].url)
        assertEquals(VisitType.EMBED, recordedVisits[6].visitType)
        assertEquals("http://www.firefox.com/8", recordedVisits[7].url)
        assertEquals(VisitType.BOOKMARK, recordedVisits[7].visitType)
        assertEquals("http://www.firefox.com/9", recordedVisits[8].url)
        assertEquals(VisitType.DOWNLOAD, recordedVisits[8].visitType)

        // Can use WebView-style getVisited API.
        assertEquals(listOf(
                "http://www.firefox.com/1", "http://www.firefox.com/2", "http://www.firefox.com/3",
                "http://www.firefox.com/4", "http://www.firefox.com/5", "http://www.firefox.com/6",
                "http://www.firefox.com/7", "http://www.firefox.com/8", "http://www.firefox.com/9"
        ), history.getVisited())

        // Can use GeckoView-style getVisited API.
        assertEquals(
                listOf(false, true, true, true, true, true, true, false, true, true, true),
                history.getVisited(listOf(
                        "http://www.mozilla.com",
                        "http://www.firefox.com/1", "http://www.firefox.com/2", "http://www.firefox.com/3",
                        "http://www.firefox.com/4", "http://www.firefox.com/5", "http://www.firefox.com/6",
                        "http://www.firefox.com/oops",
                        "http://www.firefox.com/7", "http://www.firefox.com/8", "http://www.firefox.com/9"
                ))
        )

        // Can query using pagination.
        val page1 = history.getVisitsPaginated(0, 3)
        assertEquals(3, page1.size)
        assertEquals("http://www.firefox.com/9", page1[0].url)
        assertEquals("http://www.firefox.com/8", page1[1].url)
        assertEquals("http://www.firefox.com/7", page1[2].url)

        // Can exclude visit types during pagination.
        val page1Limited = history.getVisitsPaginated(0, 10, listOf(VisitType.REDIRECT_PERMANENT, VisitType.REDIRECT_TEMPORARY))
        assertEquals(7, page1Limited.size)
        assertEquals("http://www.firefox.com/9", page1Limited[0].url)
        assertEquals("http://www.firefox.com/8", page1Limited[1].url)
        assertEquals("http://www.firefox.com/7", page1Limited[2].url)
        assertEquals("http://www.firefox.com/6", page1Limited[3].url)
        assertEquals("http://www.firefox.com/3", page1Limited[4].url)
        assertEquals("http://www.firefox.com/2", page1Limited[5].url)
        assertEquals("http://www.firefox.com/1", page1Limited[6].url)

        val page2 = history.getVisitsPaginated(3, 3)
        assertEquals(3, page2.size)
        assertEquals("http://www.firefox.com/6", page2[0].url)
        assertEquals("http://www.firefox.com/5", page2[1].url)
        assertEquals("http://www.firefox.com/4", page2[2].url)

        val page3 = history.getVisitsPaginated(6, 10)
        assertEquals(3, page3.size)
        assertEquals("http://www.firefox.com/3", page3[0].url)
        assertEquals("http://www.firefox.com/2", page3[1].url)
        assertEquals("http://www.firefox.com/1", page3[2].url)
    }

    @Test
    fun `storage passes through recordObservation calls`() = runBlocking {
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("http://www.mozilla.org", PageObservation(title = "Mozilla"))

        val recordedVisits = history.getDetailedVisits(0)
        assertEquals(1, recordedVisits.size)
        assertEquals("Mozilla", recordedVisits[0].title)
    }

    @Test
    fun `store can be used to query top frecent site information`() = runBlocking {
        val toAdd = listOf(
            "https://www.example.com/123",
            "https://www.example.com/123",
            "https://www.example.com/12345",
            "https://www.mozilla.com/foo/bar/baz",
            "https://www.mozilla.com/foo/bar/baz",
            "https://mozilla.com/a1/b2/c3",
            "https://news.ycombinator.com/",
            "https://www.mozilla.com/foo/bar/baz"
        )

        for (url in toAdd) {
            history.recordVisit(url, PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        }

        var infos = history.getTopFrecentSites(0, frecencyThreshold = FrecencyThresholdOption.NONE)
        assertEquals(0, infos.size)

        infos = history.getTopFrecentSites(0, frecencyThreshold = FrecencyThresholdOption.SKIP_ONE_TIME_PAGES)
        assertEquals(0, infos.size)

        infos = history.getTopFrecentSites(3, frecencyThreshold = FrecencyThresholdOption.NONE)
        assertEquals(3, infos.size)
        assertEquals("https://www.mozilla.com/foo/bar/baz", infos[0].url)
        assertEquals("https://www.example.com/123", infos[1].url)
        assertEquals("https://news.ycombinator.com/", infos[2].url)

        infos = history.getTopFrecentSites(3, frecencyThreshold = FrecencyThresholdOption.SKIP_ONE_TIME_PAGES)
        assertEquals(2, infos.size)
        assertEquals("https://www.mozilla.com/foo/bar/baz", infos[0].url)
        assertEquals("https://www.example.com/123", infos[1].url)

        infos = history.getTopFrecentSites(5, frecencyThreshold = FrecencyThresholdOption.NONE)
        assertEquals(5, infos.size)
        assertEquals("https://www.mozilla.com/foo/bar/baz", infos[0].url)
        assertEquals("https://www.example.com/123", infos[1].url)
        assertEquals("https://news.ycombinator.com/", infos[2].url)
        assertEquals("https://mozilla.com/a1/b2/c3", infos[3].url)
        assertEquals("https://www.example.com/12345", infos[4].url)

        infos = history.getTopFrecentSites(5, frecencyThreshold = FrecencyThresholdOption.SKIP_ONE_TIME_PAGES)
        assertEquals(2, infos.size)
        assertEquals("https://www.mozilla.com/foo/bar/baz", infos[0].url)
        assertEquals("https://www.example.com/123", infos[1].url)

        infos = history.getTopFrecentSites(100, frecencyThreshold = FrecencyThresholdOption.NONE)
        assertEquals(5, infos.size)
        assertEquals("https://www.mozilla.com/foo/bar/baz", infos[0].url)
        assertEquals("https://www.example.com/123", infos[1].url)
        assertEquals("https://news.ycombinator.com/", infos[2].url)
        assertEquals("https://mozilla.com/a1/b2/c3", infos[3].url)
        assertEquals("https://www.example.com/12345", infos[4].url)

        infos = history.getTopFrecentSites(100, frecencyThreshold = FrecencyThresholdOption.SKIP_ONE_TIME_PAGES)
        assertEquals(2, infos.size)
        assertEquals("https://www.mozilla.com/foo/bar/baz", infos[0].url)
        assertEquals("https://www.example.com/123", infos[1].url)

        infos = history.getTopFrecentSites(-4, frecencyThreshold = FrecencyThresholdOption.SKIP_ONE_TIME_PAGES)
        assertEquals(0, infos.size)
    }

    @Test
    fun `store can be used to query detailed visit information`() = runBlocking {
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.RELOAD, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("http://www.mozilla.org", PageObservation("Mozilla"))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))

        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.REDIRECT_TEMPORARY, RedirectSource.NOT_A_SOURCE))

        val visits = history.getDetailedVisits(0, excludeTypes = listOf(VisitType.REDIRECT_TEMPORARY))
        assertEquals(3, visits.size)
        assertEquals("http://www.mozilla.org/", visits[0].url)
        assertEquals("Mozilla", visits[0].title)
        assertEquals(VisitType.LINK, visits[0].visitType)

        assertEquals("http://www.mozilla.org/", visits[1].url)
        assertEquals("Mozilla", visits[1].title)
        assertEquals(VisitType.RELOAD, visits[1].visitType)

        assertEquals("http://www.firefox.com/", visits[2].url)
        assertEquals("", visits[2].title)
        assertEquals(VisitType.LINK, visits[2].visitType)

        val visitsAll = history.getDetailedVisits(0)
        assertEquals(4, visitsAll.size)
    }

    @Test
    fun `store can be used to record and retrieve history via webview-style callbacks`() = runBlocking {
        // Empty.
        assertEquals(0, history.getVisited().size)

        // Regular visits are tracked.
        history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        assertEquals(listOf("https://www.mozilla.org/"), history.getVisited())

        // Multiple visits can be tracked, results ordered by "URL's first seen first".
        history.recordVisit("https://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        assertEquals(listOf("https://www.mozilla.org/", "https://www.firefox.com/"), history.getVisited())

        // Visits marked as reloads can be tracked.
        history.recordVisit("https://www.firefox.com", PageVisit(VisitType.RELOAD, RedirectSource.NOT_A_SOURCE))
        assertEquals(listOf("https://www.mozilla.org/", "https://www.firefox.com/"), history.getVisited())

        // Visited urls are certainly a set.
        history.recordVisit("https://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("https://www.wikipedia.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        assertEquals(
                listOf("https://www.mozilla.org/", "https://www.firefox.com/", "https://www.wikipedia.org/"),
                history.getVisited()
        )
    }

    @Test
    fun `store can be used to record and retrieve history via gecko-style callbacks`() = runBlocking {
        assertEquals(0, history.getVisited(listOf()).size)

        // Regular visits are tracked
        history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        assertEquals(listOf(true), history.getVisited(listOf("https://www.mozilla.org")))

        // Duplicate requests are handled.
        assertEquals(listOf(true, true), history.getVisited(listOf("https://www.mozilla.org", "https://www.mozilla.org")))

        // Visit map is returned in correct order.
        assertEquals(listOf(true, false), history.getVisited(listOf("https://www.mozilla.org", "https://www.unknown.com")))

        assertEquals(listOf(false, true), history.getVisited(listOf("https://www.unknown.com", "https://www.mozilla.org")))

        // Multiple visits can be tracked. Reloads can be tracked.
        history.recordVisit("https://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.RELOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("https://www.wikipedia.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        assertEquals(listOf(true, true, false, true), history.getVisited(listOf("https://www.firefox.com", "https://www.wikipedia.org", "https://www.unknown.com", "https://www.mozilla.org")))
    }

    @Test
    fun `store can be used to track page meta information - title changes`() = runBlocking {
        // Title changes are recorded.
        history.recordVisit("https://www.wikipedia.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("https://www.wikipedia.org", PageObservation("Wikipedia"))
        var recorded = history.getDetailedVisits(0)
        assertEquals(1, recorded.size)
        assertEquals("Wikipedia", recorded[0].title)

        history.recordObservation("https://www.wikipedia.org", PageObservation("Википедия"))
        recorded = history.getDetailedVisits(0)
        assertEquals(1, recorded.size)
        assertEquals("Википедия", recorded[0].title)

        // Titles for different pages are recorded.
        history.recordVisit("https://www.firefox.com", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("https://www.firefox.com", PageObservation("Firefox"))
        history.recordVisit("https://www.mozilla.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("https://www.mozilla.org", PageObservation("Мозилла"))
        recorded = history.getDetailedVisits(0)
        assertEquals(3, recorded.size)
        assertEquals("Википедия", recorded[0].title)
        assertEquals("Firefox", recorded[1].title)
        assertEquals("Мозилла", recorded[2].title)
    }

    @Test
    fun `store can provide suggestions`() = runBlocking {
        assertEquals(0, history.getSuggestions("Mozilla", 100).size)

        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        val search = history.getSuggestions("Mozilla", 100)
        assertEquals(0, search.size)

        history.recordVisit("http://www.wikipedia.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.moscow.ru", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("http://www.mozilla.org", PageObservation("Mozilla"))
        history.recordObservation("http://www.firefox.com", PageObservation("Mozilla Firefox"))
        history.recordObservation("http://www.moscow.ru", PageObservation("Moscow City"))
        history.recordObservation("http://www.moscow.ru/notitle", PageObservation(""))

        // Empty search.
        assertEquals(4, history.getSuggestions("", 100).size)

        val search2 = history.getSuggestions("Mozilla", 100).sortedByDescending { it.url }
        assertEquals(2, search2.size)
        assertEquals("http://www.mozilla.org/", search2[0].id)
        assertEquals("http://www.mozilla.org/", search2[0].url)
        assertEquals("Mozilla", search2[0].title)
        assertEquals("http://www.firefox.com/", search2[1].id)
        assertEquals("http://www.firefox.com/", search2[1].url)
        assertEquals("Mozilla Firefox", search2[1].title)

        val search3 = history.getSuggestions("Mo", 100).sortedByDescending { it.url }
        assertEquals(3, search3.size)

        assertEquals("http://www.mozilla.org/", search3[0].id)
        assertEquals("http://www.mozilla.org/", search3[0].url)
        assertEquals("Mozilla", search3[0].title)

        assertEquals("http://www.moscow.ru/", search3[1].id)
        assertEquals("http://www.moscow.ru/", search3[1].url)
        assertEquals("Moscow City", search3[1].title)

        assertEquals("http://www.firefox.com/", search3[2].id)
        assertEquals("http://www.firefox.com/", search3[2].url)
        assertEquals("Mozilla Firefox", search3[2].title)

        // Respects the limit
        val search4 = history.getSuggestions("Mo", 1)
        assertEquals("http://www.moscow.ru/", search4[0].id)
        assertEquals("http://www.moscow.ru/", search4[0].url)
        assertEquals("Moscow City", search4[0].title)
    }

    @Test
    fun `store can provide autocomplete suggestions`() = runBlocking {
        assertNull(history.getAutocompleteSuggestion("moz"))

        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        var res = history.getAutocompleteSuggestion("moz")!!
        assertEquals("mozilla.org/", res.text)
        assertEquals("http://www.mozilla.org/", res.url)
        assertEquals("placesHistory", res.source)
        assertEquals(1, res.totalItems)

        history.recordVisit("http://firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        res = history.getAutocompleteSuggestion("firefox")!!
        assertEquals("firefox.com/", res.text)
        assertEquals("http://firefox.com/", res.url)
        assertEquals("placesHistory", res.source)
        assertEquals(1, res.totalItems)

        history.recordVisit("https://en.wikipedia.org/wiki/Mozilla", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        res = history.getAutocompleteSuggestion("en")!!
        assertEquals("en.wikipedia.org/", res.text)
        assertEquals("https://en.wikipedia.org/", res.url)
        assertEquals("placesHistory", res.source)
        assertEquals(1, res.totalItems)

        res = history.getAutocompleteSuggestion("en.wikipedia.org/wi")!!
        assertEquals("en.wikipedia.org/wiki/", res.text)
        assertEquals("https://en.wikipedia.org/wiki/", res.url)
        assertEquals("placesHistory", res.source)
        assertEquals(1, res.totalItems)

        assertNull(history.getAutocompleteSuggestion("hello"))
    }

    @Test
    fun `store ignores url parse exceptions during record operations`() = runBlocking {
        // These aren't valid URIs, and if we're not explicitly ignoring exceptions from the underlying
        // storage layer, these calls will throw.
        history.recordVisit("mozilla.org", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))
        history.recordObservation("mozilla.org", PageObservation("mozilla"))
    }

    @Test
    fun `store can delete everything`() = runBlocking {
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.DOWNLOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.BOOKMARK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.RELOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.EMBED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.REDIRECT_PERMANENT, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.REDIRECT_TEMPORARY, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))

        history.recordObservation("http://www.firefox.com", PageObservation("Firefox"))

        assertEquals(2, history.getVisited().size)

        history.deleteEverything()

        assertEquals(0, history.getVisited().size)
    }

    @Test
    fun `store can delete by url`() = runBlocking {
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.DOWNLOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.BOOKMARK, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.RELOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.EMBED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.REDIRECT_PERMANENT, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.REDIRECT_TEMPORARY, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.firefox.com", PageVisit(VisitType.LINK, RedirectSource.NOT_A_SOURCE))

        history.recordObservation("http://www.firefox.com", PageObservation("Firefox"))

        assertEquals(2, history.getVisited().size)

        history.deleteVisitsFor("http://www.mozilla.org")

        assertEquals(1, history.getVisited().size)
        assertEquals("http://www.firefox.com/", history.getVisited()[0])

        history.deleteVisitsFor("http://www.firefox.com")
        assertEquals(0, history.getVisited().size)
    }

    @Test
    fun `store can delete by 'since'`() = runBlocking {
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.DOWNLOAD, RedirectSource.NOT_A_SOURCE))
        history.recordVisit("http://www.mozilla.org", PageVisit(VisitType.BOOKMARK, RedirectSource.NOT_A_SOURCE))

        history.deleteVisitsSince(0)
        val visits = history.getVisited()
        assertEquals(0, visits.size)
    }

    @Test
    fun `store can delete by 'range'`() {
        runBlocking {
            history.recordVisit("http://www.mozilla.org/1", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
            Thread.sleep(10)
            history.recordVisit("http://www.mozilla.org/2", PageVisit(VisitType.DOWNLOAD, RedirectSource.NOT_A_SOURCE))
            Thread.sleep(10)
            history.recordVisit("http://www.mozilla.org/3", PageVisit(VisitType.BOOKMARK, RedirectSource.NOT_A_SOURCE))
        }

        val ts = runBlocking {
            val visits = history.getDetailedVisits(0, Long.MAX_VALUE)

            assertEquals(3, visits.size)
            visits[1].visitTime
        }

        runBlocking {
            history.deleteVisitsBetween(ts - 1, ts + 1)
        }
        val visits = runBlocking {
            history.getDetailedVisits(0, Long.MAX_VALUE)
        }
        assertEquals(2, visits.size)

        assertEquals("http://www.mozilla.org/1", visits[0].url)
        assertEquals("http://www.mozilla.org/3", visits[1].url)
    }

    @Test
    fun `store can delete visit by 'url' and 'timestamp'`() {
        runBlocking {
            history.recordVisit("http://www.mozilla.org/1", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
            Thread.sleep(10)
            history.recordVisit("http://www.mozilla.org/2", PageVisit(VisitType.DOWNLOAD, RedirectSource.NOT_A_SOURCE))
            Thread.sleep(10)
            history.recordVisit("http://www.mozilla.org/3", PageVisit(VisitType.BOOKMARK, RedirectSource.NOT_A_SOURCE))
        }

        val ts = runBlocking {
            val visits = history.getDetailedVisits(0, Long.MAX_VALUE)

            assertEquals(3, visits.size)
            visits[1].visitTime
        }

        runBlocking {
            history.deleteVisit("http://www.mozilla.org/4", 111)
            // There are no visits for this url, delete is a no-op.
            assertEquals(3, history.getDetailedVisits(0, Long.MAX_VALUE).size)
        }

        runBlocking {
            history.deleteVisit("http://www.mozilla.org/1", ts)
            // There is no such visit for this url, delete is a no-op.
            assertEquals(3, history.getDetailedVisits(0, Long.MAX_VALUE).size)
        }

        runBlocking {
            history.deleteVisit("http://www.mozilla.org/2", ts)
        }

        val visits = runBlocking {
            history.getDetailedVisits(0, Long.MAX_VALUE)
        }
        assertEquals(2, visits.size)

        assertEquals("http://www.mozilla.org/1", visits[0].url)
        assertEquals("http://www.mozilla.org/3", visits[1].url)
    }

    @Test
    fun `can run maintanence on the store`() = runBlocking {
        history.runMaintenance()
    }

    @Test
    fun `can run prune on the store`() = runBlocking {
        // Empty.
        history.prune()
        history.recordVisit("http://www.mozilla.org/1", PageVisit(VisitType.TYPED, RedirectSource.NOT_A_SOURCE))
        // Non-empty.
        history.prune()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `storage validates calls to getSuggestion`() {
        history.getSuggestions("Hello!", -1)
    }

    // We can't test 'sync' stuff yet, since that exercises the network and we can't mock that out currently.
    // Instead, we test that our wrappers act correctly.
    internal class MockingPlacesHistoryStorage(override val places: Connection) : PlacesHistoryStorage(testContext)

    @Test
    fun `storage passes through sync calls`() = runBlocking {
        var passedAuthInfo: SyncAuthInfo? = null
        val conn = object : Connection {
            override fun reader(): PlacesReaderConnection {
                fail()
                return mock()
            }

            override fun writer(): PlacesWriterConnection {
                fail()
                return mock()
            }

            override fun syncHistory(syncInfo: SyncAuthInfo) {
                assertNull(passedAuthInfo)
                passedAuthInfo = syncInfo
            }

            override fun syncBookmarks(syncInfo: SyncAuthInfo) {
                fail()
            }

            override fun getHandle(): Long {
                fail()
                return 0L
            }

            override fun importVisitsFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun importBookmarksFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun readPinnedSitesFromFennec(dbPath: String): List<BookmarkNode> {
                fail()
                return emptyList()
            }

            override fun close() {
                fail()
            }
        }
        val storage = MockingPlacesHistoryStorage(conn)

        storage.sync(SyncAuthInfo("kid", "token", 123L, "key", "serverUrl"))

        assertEquals("kid", passedAuthInfo!!.kid)
        assertEquals("serverUrl", passedAuthInfo!!.tokenServerUrl)
        assertEquals("token", passedAuthInfo!!.fxaAccessToken)
        assertEquals(123L, passedAuthInfo!!.fxaAccessTokenExpiresAt)
        assertEquals("key", passedAuthInfo!!.syncKey)
    }

    @Test
    fun `storage passes through sync OK results`() = runBlocking {
        val conn = object : Connection {
            override fun reader(): PlacesReaderConnection {
                fail()
                return mock()
            }

            override fun writer(): PlacesWriterConnection {
                fail()
                return mock()
            }

            override fun syncHistory(syncInfo: SyncAuthInfo) {}

            override fun syncBookmarks(syncInfo: SyncAuthInfo) {}

            override fun getHandle(): Long {
                fail()
                return 0L
            }

            override fun importVisitsFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun importBookmarksFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun readPinnedSitesFromFennec(dbPath: String): List<BookmarkNode> {
                fail()
                return emptyList()
            }

            override fun close() {
                fail()
            }
        }
        val storage = MockingPlacesHistoryStorage(conn)

        val result = storage.sync(SyncAuthInfo("kid", "token", 123L, "key", "serverUrl"))
        assertEquals(SyncStatus.Ok, result)
    }

    @Test
    fun `storage passes through sync exceptions`() = runBlocking {
        val exception = PlacesException("test error")
        val conn = object : Connection {
            override fun reader(): PlacesReaderConnection {
                fail()
                return mock()
            }

            override fun writer(): PlacesWriterConnection {
                fail()
                return mock()
            }

            override fun syncHistory(syncInfo: SyncAuthInfo) {
                throw exception
            }

            override fun syncBookmarks(syncInfo: SyncAuthInfo) {
                fail()
            }

            override fun getHandle(): Long {
                fail()
                return 0L
            }

            override fun importVisitsFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun importBookmarksFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun readPinnedSitesFromFennec(dbPath: String): List<BookmarkNode> {
                fail()
                return emptyList()
            }

            override fun close() {
                fail()
            }
        }
        val storage = MockingPlacesHistoryStorage(conn)

        val result = storage.sync(SyncAuthInfo("kid", "token", 123L, "key", "serverUrl"))

        assertTrue(result is SyncStatus.Error)

        val error = result as SyncStatus.Error
        assertEquals("test error", error.exception.message)
    }

    @Test(expected = InternalPanic::class)
    fun `storage re-throws sync panics`() = runBlocking {
        val exception = InternalPanic("test panic")
        val conn = object : Connection {
            override fun reader(): PlacesReaderConnection {
                fail()
                return mock()
            }

            override fun writer(): PlacesWriterConnection {
                fail()
                return mock()
            }

            override fun syncHistory(syncInfo: SyncAuthInfo) {
                throw exception
            }

            override fun syncBookmarks(syncInfo: SyncAuthInfo) {
                fail()
            }

            override fun getHandle(): Long {
                fail()
                return 0L
            }

            override fun importVisitsFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun importBookmarksFromFennec(dbPath: String): JSONObject {
                fail()
                return JSONObject()
            }

            override fun readPinnedSitesFromFennec(dbPath: String): List<BookmarkNode> {
                fail()
                return emptyList()
            }

            override fun close() {
                fail()
            }
        }
        val storage = MockingPlacesHistoryStorage(conn)
        storage.sync(SyncAuthInfo("kid", "token", 123L, "key", "serverUrl"))
        fail()
    }

    @Test
    fun `history import v0 empty`() {
        // Doesn't have a schema or a set user_version pragma.
        val path = getTestPath("databases/empty-v0.db").absolutePath
        try {
            history.importFromFennec(path)
            fail("Expected v0 database to be unsupported")
        } catch (e: PlacesException) {
            // This is a little brittle, but the places library doesn't have a proper error type for this.
            assertEquals("Database version 0 is not supported", e.message)
        }
    }

    @Test
    fun `history import v23 populated`() {
        // Fennec v38 schema populated with data.
        val path = getTestPath("databases/bookmarks-v23.db").absolutePath
        try {
            history.importFromFennec(path)
            fail("Expected v23 database to be unsupported")
        } catch (e: PlacesException) {
            // This is a little brittle, but the places library doesn't have a proper error type for this.
            assertEquals("Database version 23 is not supported", e.message)
        }
    }

    @Test
    fun `history import v38 populated`() = runBlocking {
        val path = getTestPath("databases/populated-v38.db").absolutePath
        var visits = history.getDetailedVisits(0, Long.MAX_VALUE)
        assertEquals(0, visits.size)
        history.importFromFennec(path)

        visits = history.getDetailedVisits(0, Long.MAX_VALUE)
        assertEquals(152, visits.size)

        assertEquals(listOf(false, false, true, true, false), history.reader.getVisited(listOf(
            "files:///",
            "https://news.ycombinator.com/",
            "https://www.theguardian.com/film/2017/jul/24/stranger-things-thor-ragnarok-comic-con-2017",
            "http://www.bbc.com/news/world-us-canada-40662772",
            "https://mobile.reuters.com/"
        )))

        with(visits[0]) {
            assertEquals("Apple", this.title)
            assertEquals("http://www.apple.com/", this.url)
            assertEquals(1472685165382, this.visitTime)
            assertEquals(VisitType.REDIRECT_PERMANENT, this.visitType)
        }
    }

    @Test
    fun `history import v39 populated`() = runBlocking {
        val path = getTestPath("databases/populated-v39.db").absolutePath
        var visits = history.getDetailedVisits(0, Long.MAX_VALUE)
        assertEquals(0, visits.size)
        history.importFromFennec(path)

        visits = history.getDetailedVisits(0, Long.MAX_VALUE)
        assertEquals(6, visits.size)

        assertEquals(listOf(false, true, true, true, true, true, true), history.reader.getVisited(listOf(
            "files:///",
            "https://news.ycombinator.com/",
            "https://news.ycombinator.com/item?id=21224209",
            "https://mobile.twitter.com/random_walker/status/1182635589604171776",
            "https://www.mozilla.org/en-US/",
            "https://www.mozilla.org/en-US/firefox/accounts/",
            "https://mobile.reuters.com/"
        )))

        with(visits[0]) {
            assertEquals("Hacker News", this.title)
            assertEquals("https://news.ycombinator.com/", this.url)
            assertEquals(1570822280639, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[1]) {
            assertEquals("Why Enterprise Software Sucks | Hacker News", this.title)
            assertEquals("https://news.ycombinator.com/item?id=21224209", this.url)
            assertEquals(1570822283117, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[2]) {
            assertEquals("Arvind Narayanan on Twitter: \"My university just announced that it’s dumping Blackboard, and there was much rejoicing. Why is Blackboard universally reviled? There’s a standard story of why \"enterprise software\" sucks. If you’ll bear with me, I think this is best appreciated by talking about… baby clothes!\" / Twitter", this.title)
            assertEquals("https://mobile.twitter.com/random_walker/status/1182635589604171776", this.url)
            assertEquals(1570822287349, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[3]) {
            assertEquals("Internet for people, not profit — Mozilla", this.title)
            assertEquals("https://www.mozilla.org/en-US/", this.url)
            assertEquals(1570830201733, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[4]) {
            assertEquals("There is a way to protect your privacy. Join Firefox.", this.title)
            assertEquals("https://www.mozilla.org/en-US/firefox/accounts/", this.url)
            assertEquals(1570830207742, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[5]) {
            assertEquals("", this.title)
            assertEquals("https://mobile.reuters.com/", this.url)
            assertEquals(1570830217562, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
    }

    @Test
    fun `history import v34 populated`() = runBlocking {
        val path = getTestPath("databases/history-v34.db").absolutePath
        var visits = history.getDetailedVisits(0, Long.MAX_VALUE)
        assertEquals(0, visits.size)
        history.importFromFennec(path)

        visits = history.getDetailedVisits(0, Long.MAX_VALUE)
        assertEquals(6, visits.size)

        assertEquals(listOf(true, true, true, true, true), history.reader.getVisited(listOf(
            "https://www.newegg.com/",
            "https://news.ycombinator.com/",
            "https://terrytao.wordpress.com/2020/04/12/john-conway/",
            "https://news.ycombinator.com/item?id=22862053",
            "https://malleable.systems/"
        )))

        with(visits[0]) {
            assertEquals("Computer Parts, PC Components, Laptop Computers, LED LCD TV, Digital Cameras and more - Newegg.com", this.title)
            assertEquals("https://www.newegg.com/", this.url)
            assertEquals(1586838104188, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[1]) {
            assertEquals("Hacker News", this.title)
            assertEquals("https://news.ycombinator.com/", this.url)
            assertEquals(1586838109506, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[2]) {
            assertEquals("https://terrytao.wordpress.com/2020/04/12/john-conway/", this.title)
            assertEquals("https://terrytao.wordpress.com/2020/04/12/john-conway/", this.url)
            assertEquals(1586838113212, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[3]) {
            assertEquals("John Conway | Hacker News", this.title)
            assertEquals("https://news.ycombinator.com/item?id=22862053", this.url)
            assertEquals(1586838123314, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[4]) {
            assertEquals("John Conway | Hacker News", this.title)
            assertEquals("https://news.ycombinator.com/item?id=22862053", this.url)
            assertEquals(1586838126671, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
        with(visits[5]) {
            assertEquals("https://malleable.systems/", this.title)
            assertEquals("https://malleable.systems/", this.url)
            assertEquals(1586838164613, this.visitTime)
            assertEquals(VisitType.LINK, this.visitType)
        }
    }

    @Test
    fun `add and get latest history metadata by url`() = runBlocking {
        assertNull(history.getLatestHistoryMetadataForUrl("http://www.mozilla.org"))

        val currentTime = System.currentTimeMillis()

        val meta1 = HistoryMetadata(
            url = "https://doc.rust-lang.org/std/macro.assert_eq.html",
            title = "std::assert_eq - Rust",
            createdAt = currentTime,
            updatedAt = currentTime + 5000,
            totalViewTime = 2000,
            searchTerm = "rust assert_eq",
            isMedia = false,
            parentUrl = "http://www.google.com"
        )
        history.addHistoryMetadata(meta1)

        assertHistoryMetadataRecord(meta1.copy(parentUrl = "www.google.com"), history.getLatestHistoryMetadataForUrl("https://doc.rust-lang.org/std/macro.assert_eq.html")!!)

        // now let's add another one, but newer:
        val meta2 = HistoryMetadata(
            url = "https://doc.rust-lang.org/std/macro.assert_eq.html",
            title = "std::assert_eq - Rust 2000", // This changed in the newer record
            createdAt = currentTime + 10000,
            updatedAt = currentTime + 50000,
            totalViewTime = 2000,
            searchTerm = "rust assert_eq",
            isMedia = true, // and it's playing music now, too!
            parentUrl = "http://www.shmoogle.com" // finally, a private search engine that works
        )
        history.addHistoryMetadata(meta2)

        // NB: title change isn't respected atm.
        assertHistoryMetadataRecord(meta2.copy(title = "std::assert_eq - Rust", parentUrl = "www.shmoogle.com"), history.getLatestHistoryMetadataForUrl("https://doc.rust-lang.org/std/macro.assert_eq.html")!!)

        // now let's add another one, but with all optional fields as null
        val meta3 = HistoryMetadata(
            url = "https://doc.rust-lang.org/std/macro.assert_eq.html",
            title = null, // This changed in the newer record
            createdAt = currentTime + 10001,
            updatedAt = currentTime + 50001,
            totalViewTime = 2000,
            searchTerm = null,
            isMedia = true, // and it's playing music now, too!
            parentUrl = null // finally, a private search engine that works
        )
        history.addHistoryMetadata(meta3)

        // NB: again, title remains as-is on the first write. The rest of the changes are reflected though.
        assertHistoryMetadataRecord(meta3.copy(title = "std::assert_eq - Rust"), history.getLatestHistoryMetadataForUrl("https://doc.rust-lang.org/std/macro.assert_eq.html")!!)
    }

    @Test
    fun `get history query`() = runBlocking {
        val currentTime = System.currentTimeMillis()

        assertEquals(0, history.queryHistoryMetadata("keystore", 1).size)

        val meta1 = HistoryMetadata(
            url = "https://sql.telemetry.mozilla.org/dashboard/android-keystore-reliability-experiment",
            title = "Android Keystore Reliability Experiment",
            createdAt = currentTime,
            updatedAt = currentTime,
            totalViewTime = 20000,
            searchTerm = "keystore reliability",
            isMedia = false,
            parentUrl = "http://self.mozilla.com"
        )

        val meta2 = HistoryMetadata(
            url = "https://www.youtube.com/watch?v=F7PQdCDiE44",
            title = "Are we ready for the next crisis? | DW Documentary - YouTube",
            createdAt = currentTime + 1200,
            updatedAt = currentTime + 1200,
            totalViewTime = 30000,
            searchTerm = "crisis",
            isMedia = true,
            parentUrl = "https://www.google.com/search?client=firefox-b-d&q=dw+crisis"
        )

        val meta3 = HistoryMetadata(
            url = "https://www.cbc.ca/news/canada/toronto/covid-19-ontario-april-16-2021-new-restrictions-modelling-1.5990092",
            title = "Ford announces new restrictions as cases threaten to remain high all summer | CBC News",
            createdAt = currentTime + 5000,
            updatedAt = currentTime + 6000,
            totalViewTime = 20000,
            searchTerm = "ford covid19",
            isMedia = false,
            parentUrl = "https://duckduckgo.com/?q=ford+covid19&t=hc&va=u&ia=web"
        )

        val meta4 = HistoryMetadata(
            url = "https://www.youtube.com/watch?v=TfXbzbJQHuw",
            title = "New York City rich and poor — the inequality crisis - YouTube",
            createdAt = currentTime + 10000,
            updatedAt = currentTime + 12000,
            totalViewTime = 20000,
            searchTerm = "dw nyc rich",
            isMedia = true,
            parentUrl = "https://duckduckgo.com/?q=dw+nyc+rich&t=hc&va=u&ia=web"
        )

        history.addHistoryMetadata(meta1)
        history.addHistoryMetadata(meta2)
        history.addHistoryMetadata(meta3)
        history.addHistoryMetadata(meta4)

        assertEquals(0, history.queryHistoryMetadata("keystore", 0).size)
        // query by url
        with(history.queryHistoryMetadata("dashboard", 10)) {
            assertEquals(1, this.size)
            assertHistoryMetadataRecord(meta1.copy(parentUrl = "self.mozilla.com"), this[0])
        }

        // query by title
        with(history.queryHistoryMetadata("next crisis", 10)) {
            assertEquals(1, this.size)
            assertHistoryMetadataRecord(meta2.copy(parentUrl = "www.google.com"), this[0])
        }

        // query by search term
        with(history.queryHistoryMetadata("covid19", 10)) {
            assertEquals(1, this.size)
            assertHistoryMetadataRecord(meta3.copy(parentUrl = "duckduckgo.com"), this[0])
        }

        // multiple results, mixed search targets
        with(history.queryHistoryMetadata("dw", 10)) {
            assertEquals(2, this.size)
            assertHistoryMetadataRecord(meta2.copy(parentUrl = "www.google.com"), this[0])
            assertHistoryMetadataRecord(meta4.copy(parentUrl = "duckduckgo.com"), this[1])
        }

        // limit is respected
        with(history.queryHistoryMetadata("dw", 1)) {
            assertEquals(1, this.size)
            assertHistoryMetadataRecord(meta2.copy(parentUrl = "www.google.com"), this[0])
        }
    }

    @Test
    fun `get history metadata between`() = runBlocking {
        assertEquals(0, history.getHistoryMetadataBetween(-1, 0).size)
        assertEquals(0, history.getHistoryMetadataBetween(0, Long.MAX_VALUE).size)
        assertEquals(0, history.getHistoryMetadataBetween(Long.MAX_VALUE, Long.MIN_VALUE).size)
        assertEquals(0, history.getHistoryMetadataBetween(Long.MIN_VALUE, Long.MAX_VALUE).size)

        val currentTime = System.currentTimeMillis()

        val meta1 = HistoryMetadata(
            url = "https://www.youtube.com/watch?v=lNeRQuiKBd4",
            title = "Pixels and Painting: Artist Talk with Kristoffer Zetterstrand - YouTube",
            createdAt = currentTime,
            updatedAt = currentTime,
            totalViewTime = 20000,
            searchTerm = null,
            isMedia = true,
            parentUrl = "http://www.twitter.com"
        )
        history.addHistoryMetadata(meta1)

        val meta2 = HistoryMetadata(
            url = "https://www.youtube.com/watch?v=Cs1b5qvCZ54",
            title = "Тайна валдайской дачи Путина - YouTube",
            createdAt = currentTime + 1500,
            updatedAt = currentTime + 1700,
            totalViewTime = 200,
            searchTerm = "путин валдай",
            isMedia = true,
            parentUrl = "http://www.yandex.ru"
        )
        history.addHistoryMetadata(meta2)

        val meta3 = HistoryMetadata(
            url = "https://www.ifixit.com/News/35377/which-wireless-earbuds-are-the-least-evil",
            title = "Are All Wireless Earbuds As Evil As AirPods? - iFixit",
            createdAt = currentTime + 1000,
            updatedAt = currentTime + 2000,
            totalViewTime = 2000,
            searchTerm = "repairable wireless headset",
            isMedia = false,
            parentUrl = "http://www.google.com"
        )
        history.addHistoryMetadata(meta3)

        assertEquals(3, history.getHistoryMetadataBetween(0, Long.MAX_VALUE).size)
        assertEquals(0, history.getHistoryMetadataBetween(Long.MAX_VALUE, 0).size)
        assertEquals(0, history.getHistoryMetadataBetween(Long.MIN_VALUE, 0).size)

        with(history.getHistoryMetadataBetween(currentTime, currentTime + 1700)) {
            assertEquals(2, this.size)
            assertEquals("https://www.youtube.com/watch?v=Cs1b5qvCZ54", this[0].url)
            assertEquals("https://www.youtube.com/watch?v=lNeRQuiKBd4", this[1].url)
        }
        with(history.getHistoryMetadataBetween(currentTime, currentTime + 1699)) {
            assertEquals(1, this.size)
            assertEquals("https://www.youtube.com/watch?v=lNeRQuiKBd4", this[0].url)
        }
        with(history.getHistoryMetadataBetween(currentTime + 1, currentTime + 1700)) {
            assertEquals(1, this.size)
            assertEquals("https://www.youtube.com/watch?v=Cs1b5qvCZ54", this[0].url)
        }
    }

    @Test
    fun `get history metadata since`() = runBlocking {
        val currentTime = System.currentTimeMillis()

        assertEquals(0, history.getHistoryMetadataSince(-1).size)
        assertEquals(0, history.getHistoryMetadataSince(0).size)
        assertEquals(0, history.getHistoryMetadataSince(Long.MIN_VALUE).size)
        assertEquals(0, history.getHistoryMetadataSince(Long.MAX_VALUE).size)

        assertEquals(0, history.getHistoryMetadataSince(currentTime).size)

        val meta1 = HistoryMetadata(
            url = "https://www.youtube.com/watch?v=lNeRQuiKBd4",
            title = "Pixels and Painting: Artist Talk with Kristoffer Zetterstrand - YouTube",
            createdAt = currentTime,
            updatedAt = currentTime,
            totalViewTime = 20000,
            searchTerm = null,
            isMedia = true,
            parentUrl = "http://www.twitter.com"
        )
        history.addHistoryMetadata(meta1)

        val meta2 = HistoryMetadata(
            url = "https://www.ifixit.com/News/35377/which-wireless-earbuds-are-the-least-evil",
            title = "Are All Wireless Earbuds As Evil As AirPods? - iFixit",
            createdAt = currentTime + 1000,
            updatedAt = currentTime + 2000,
            totalViewTime = 2000,
            searchTerm = "repairable wireless headset",
            isMedia = false,
            parentUrl = "http://www.google.com"
        )
        history.addHistoryMetadata(meta2)

        val meta3 = HistoryMetadata(
            url = "https://www.youtube.com/watch?v=Cs1b5qvCZ54",
            title = "Тайна валдайской дачи Путина - YouTube",
            createdAt = currentTime + 1500,
            updatedAt = currentTime + 1700,
            totalViewTime = 200,
            searchTerm = "путин валдай",
            isMedia = true,
            parentUrl = "http://www.yandex.ru"
        )
        history.addHistoryMetadata(meta3)

        // order is by updatedAt
        with(history.getHistoryMetadataSince(currentTime)) {
            assertEquals(3, this.size)
            assertHistoryMetadataRecord(meta2.copy(parentUrl = "www.google.com"), this[0])
            assertHistoryMetadataRecord(meta3.copy(parentUrl = "www.yandex.ru"), this[1])
            assertHistoryMetadataRecord(meta1.copy(parentUrl = "www.twitter.com"), this[2])
        }

        // search is inclusive of time
        with(history.getHistoryMetadataSince(currentTime + 1700)) {
            assertEquals(2, this.size)
            assertHistoryMetadataRecord(meta2.copy(parentUrl = "www.google.com"), this[0])
            assertHistoryMetadataRecord(meta3.copy(parentUrl = "www.yandex.ru"), this[1])
        }

        with(history.getHistoryMetadataSince(currentTime + 1900)) {
            assertEquals(1, this.size)
            assertHistoryMetadataRecord(meta2.copy(parentUrl = "www.google.com"), this[0])
        }
    }

    private fun assertHistoryMetadataRecord(local_meta: HistoryMetadata, db_meta: HistoryMetadata) {
        assertEquals(local_meta.url, db_meta.url)
        assertEquals(local_meta.title, db_meta.title)
        assertEquals(local_meta.createdAt, db_meta.createdAt)
        assertEquals(local_meta.updatedAt, db_meta.updatedAt)
        assertEquals(local_meta.totalViewTime, db_meta.totalViewTime)
        assertEquals(local_meta.searchTerm, db_meta.searchTerm)
        assertEquals(local_meta.isMedia, db_meta.isMedia)
        assertEquals(local_meta.parentUrl, db_meta.parentUrl)
    }
}

fun getTestPath(path: String): File {
    return PlacesHistoryStorage::class.java.classLoader!!
        .getResource(path).file
        .let { File(it) }
}
