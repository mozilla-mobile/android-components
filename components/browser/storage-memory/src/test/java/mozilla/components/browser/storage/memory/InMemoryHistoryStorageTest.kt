/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.memory

import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.reset

class InMemoryHistoryStorageTest {
    @Test
    fun `store can be used to track visit information`() {
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
    fun `store can be used to record and retrieve history via webview-style callbacks`() {
        val history = InMemoryHistoryStorage()

        val callback = mock<(List<String>) -> Unit>()

        // Empty.
        history.getVisited(callback)
        verify(callback).invoke(listOf())
        reset(callback)
        history.getVisited(callback)
        verify(callback).invoke(listOf())
        reset(callback)

        // Regular visits are tracked.
        history.recordVisit("https://www.mozilla.org", VisitType.LINK)
        history.getVisited(callback)
        verify(callback).invoke(listOf("https://www.mozilla.org"))
        reset(callback)

        // Multiple visits can be tracked, results ordered by "URL's first seen first".
        history.recordVisit("https://www.firefox.com", VisitType.LINK)
        history.getVisited(callback)
        verify(callback).invoke(listOf("https://www.mozilla.org", "https://www.firefox.com"))
        reset(callback)

        // Visits marked as reloads can be tracked.
        history.recordVisit("https://www.firefox.com", VisitType.RELOAD)
        history.getVisited(callback)
        verify(callback).invoke(listOf("https://www.mozilla.org", "https://www.firefox.com"))
        reset(callback)

        // Visited urls are certainly a set.
        history.recordVisit("https://www.firefox.com", VisitType.LINK)
        history.recordVisit("https://www.mozilla.org", VisitType.LINK)
        history.recordVisit("https://www.wikipedia.org", VisitType.LINK)
        history.getVisited(callback)
        verify(callback).invoke(listOf("https://www.mozilla.org", "https://www.firefox.com", "https://www.wikipedia.org"))
    }

    @Test
    fun `store can be used to record and retrieve history via gecko-style callbacks`() {
        val history = InMemoryHistoryStorage()

        // Empty.
        val callback = mock<(List<Boolean>) -> Unit>()

        history.getVisited(listOf(), callback)
        verify(callback).invoke(listOf())
        reset(callback)

        // Regular visits are tracked
        history.recordVisit("https://www.mozilla.org", VisitType.LINK)
        history.getVisited(listOf("https://www.mozilla.org"), callback)
        verify(callback).invoke(listOf(true))
        reset(callback)

        // Duplicate requests are handled.
        history.getVisited(listOf("https://www.mozilla.org", "https://www.mozilla.org"), callback)
        verify(callback).invoke(listOf(true, true))
        reset(callback)

        // Visit map is returned in correct order.
        history.getVisited(listOf("https://www.mozilla.org", "https://www.unknown.com"), callback)
        verify(callback).invoke(listOf(true, false))
        reset(callback)

        history.getVisited(listOf("https://www.unknown.com", "https://www.mozilla.org"), callback)
        verify(callback).invoke(listOf(false, true))
        reset(callback)

        // Multiple visits can be tracked. Reloads can be tracked.
        history.recordVisit("https://www.firefox.com", VisitType.LINK)
        history.recordVisit("https://www.mozilla.org", VisitType.RELOAD)
        history.recordVisit("https://www.wikipedia.org", VisitType.LINK)
        history.getVisited(listOf("https://www.firefox.com", "https://www.wikipedia.org", "https://www.unknown.com", "https://www.mozilla.org"), callback)
        verify(callback).invoke(listOf(true, true, false, true))
    }

    @Test
    fun `store can be used to track page meta information - title changes`() {
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
}