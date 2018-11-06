/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.memory

import kotlinx.coroutines.experimental.runBlocking
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.VisitType
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryHistoryStorageTest {
    @Test
    fun `store can be used to track visit information`() = runBlocking {
        val history = InMemoryHistoryStorage()

        assertEquals(0, history.pages.size)

        history.recordVisit("http://www.mozilla.org", VisitType.LINK)
        assertEquals(1, history.pages.size)
        assertEquals(1, history.pages["http://www.mozilla.org"]!!.size)
        assertEquals(VisitType.LINK, history.pages["http://www.mozilla.org"]!![0].type)

        // Reloads are recorded.
        history.recordVisit("http://www.mozilla.org", VisitType.RELOAD)
        assertEquals(1, history.pages.size)
        assertEquals(2, history.pages["http://www.mozilla.org"]!!.size)
        assertEquals(VisitType.LINK, history.pages["http://www.mozilla.org"]!![0].type)
        assertEquals(VisitType.RELOAD, history.pages["http://www.mozilla.org"]!![1].type)

        // Visits for multiple pages are tracked.
        history.recordVisit("http://www.firefox.com", VisitType.LINK)
        assertEquals(2, history.pages.size)
        assertEquals(2, history.pages["http://www.mozilla.org"]!!.size)
        assertEquals(VisitType.LINK, history.pages["http://www.mozilla.org"]!![0].type)
        assertEquals(VisitType.RELOAD, history.pages["http://www.mozilla.org"]!![1].type)
        assertEquals(1, history.pages["http://www.firefox.com"]!!.size)
        assertEquals(VisitType.LINK, history.pages["http://www.firefox.com"]!![0].type)
    }

    @Test
    fun `store can be used to record and retrieve history via webview-style callbacks`() = runBlocking {
        val history = InMemoryHistoryStorage()

        // Empty.
        assertEquals(0, history.getVisited().await().size)

        // Regular visits are tracked.
        history.recordVisit("https://www.mozilla.org", VisitType.LINK)
        assertEquals(listOf("https://www.mozilla.org"), history.getVisited().await())

        // Multiple visits can be tracked, results ordered by "URL's first seen first".
        history.recordVisit("https://www.firefox.com", VisitType.LINK)
        assertEquals(listOf("https://www.mozilla.org", "https://www.firefox.com"), history.getVisited().await())

        // Visits marked as reloads can be tracked.
        history.recordVisit("https://www.firefox.com", VisitType.RELOAD)
        assertEquals(listOf("https://www.mozilla.org", "https://www.firefox.com"), history.getVisited().await())

        // Visited urls are certainly a set.
        history.recordVisit("https://www.firefox.com", VisitType.LINK)
        history.recordVisit("https://www.mozilla.org", VisitType.LINK)
        history.recordVisit("https://www.wikipedia.org", VisitType.LINK)
        assertEquals(
            listOf("https://www.mozilla.org", "https://www.firefox.com", "https://www.wikipedia.org"),
            history.getVisited().await()
        )
    }

    @Test
    fun `store can be used to record and retrieve history via gecko-style callbacks`() = runBlocking {
        val history = InMemoryHistoryStorage()

        assertEquals(0, history.getVisited(listOf()).await().size)

        // Regular visits are tracked
        history.recordVisit("https://www.mozilla.org", VisitType.LINK)
        assertEquals(listOf(true), history.getVisited(listOf("https://www.mozilla.org")).await())

        // Duplicate requests are handled.
        assertEquals(listOf(true, true), history.getVisited(listOf("https://www.mozilla.org", "https://www.mozilla.org")).await())

        // Visit map is returned in correct order.
        assertEquals(listOf(true, false), history.getVisited(listOf("https://www.mozilla.org", "https://www.unknown.com")).await())

        assertEquals(listOf(false, true), history.getVisited(listOf("https://www.unknown.com", "https://www.mozilla.org")).await())

        // Multiple visits can be tracked. Reloads can be tracked.
        history.recordVisit("https://www.firefox.com", VisitType.LINK)
        history.recordVisit("https://www.mozilla.org", VisitType.RELOAD)
        history.recordVisit("https://www.wikipedia.org", VisitType.LINK)
        assertEquals(listOf(true, true, false, true), history.getVisited(listOf("https://www.firefox.com", "https://www.wikipedia.org", "https://www.unknown.com", "https://www.mozilla.org")).await())
    }

    @Test
    fun `store can be used to track page meta information - title changes`() = runBlocking {
        val history = InMemoryHistoryStorage()
        assertEquals(0, history.pageMeta.size)

        // Title changes are recorded.
        history.recordObservation("https://www.wikipedia.org", PageObservation("Wikipedia"))
        assertEquals(1, history.pageMeta.size)
        assertEquals(PageObservation("Wikipedia"), history.pageMeta["https://www.wikipedia.org"])

        history.recordObservation("https://www.wikipedia.org", PageObservation("Википедия"))
        assertEquals(1, history.pageMeta.size)
        assertEquals(PageObservation("Википедия"), history.pageMeta["https://www.wikipedia.org"])

        // Titles for different pages are recorded.
        history.recordObservation("https://www.firefox.com", PageObservation("Firefox"))
        history.recordObservation("https://www.mozilla.org", PageObservation("Мозилла"))
        assertEquals(3, history.pageMeta.size)
        assertEquals(PageObservation("Википедия"), history.pageMeta["https://www.wikipedia.org"])
        assertEquals(PageObservation("Firefox"), history.pageMeta["https://www.firefox.com"])
        assertEquals(PageObservation("Мозилла"), history.pageMeta["https://www.mozilla.org"])
    }

    @Test
    fun `store can provide suggestions`() {
        val history = InMemoryHistoryStorage()
        assertEquals(0, history.getSuggestions("Mozilla").size)

        history.recordVisit("http://www.firefox.com", VisitType.LINK)
        val search = history.getSuggestions("Mozilla")
        assertEquals(1, search.size)
        assertEquals("http://www.firefox.com", search[0].url)

        history.recordVisit("http://www.wikipedia.org", VisitType.LINK)
        history.recordVisit("http://www.mozilla.org", VisitType.LINK)
        history.recordVisit("http://www.moscow.ru", VisitType.LINK)
        history.recordObservation("http://www.mozilla.org", PageObservation("Mozilla"))
        history.recordObservation("http://www.firefox.com", PageObservation("Mozilla Firefox"))
        history.recordObservation("http://www.moscow.ru", PageObservation("Moscow City"))

        // Empty search.
        assertEquals(4, history.getSuggestions("").size)

        val search2 = history.getSuggestions("Mozilla")
        assertEquals(4, search2.size)
        assertEquals("http://www.mozilla.org", search2[0].id)
        assertEquals("http://www.mozilla.org", search2[0].url)
        assertEquals("Mozilla", search2[0].title)

        assertEquals("http://www.firefox.com", search2[1].id)
        assertEquals("http://www.firefox.com", search2[1].url)
        assertEquals("Mozilla Firefox", search2[1].title)

        assertEquals("http://www.moscow.ru", search2[2].id)
        assertEquals("http://www.moscow.ru", search2[2].url)
        assertEquals("Moscow City", search2[2].title)

        assertEquals("http://www.wikipedia.org", search2[3].id)
        assertEquals("http://www.wikipedia.org", search2[3].url)
        assertEquals(null, search2[3].title)
    }
}