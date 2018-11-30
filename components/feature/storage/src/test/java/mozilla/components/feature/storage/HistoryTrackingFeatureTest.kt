/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.storage

import kotlinx.coroutines.runBlocking
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryAutocompleteResult
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.SearchResult
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class HistoryTrackingFeatureTest {
    @Test
    fun `feature sets a history delegate on the engine`() {
        val engine: Engine = mock()
        val settings = DefaultSettings()
        `when`(engine.settings).thenReturn(settings)

        assertNull(settings.historyTrackingDelegate)
        HistoryTrackingFeature(engine, mock())
        assertNotNull(settings.historyTrackingDelegate)
    }

    @Test
    fun `history delegate passes through onVisited calls`() = runBlocking {
        val storage: HistoryStorage = mock()
        val delegate = HistoryDelegate(storage)

        delegate.onVisited("http://www.mozilla.org", false)
        verify(storage).recordVisit("http://www.mozilla.org", VisitType.LINK)

        delegate.onVisited("http://www.firefox.com", true)
        verify(storage).recordVisit("http://www.firefox.com", VisitType.RELOAD)
    }

    @Test
    fun `history delegate passes through onTitleChanged calls`() = runBlocking {
        val storage: HistoryStorage = mock()
        val delegate = HistoryDelegate(storage)

        delegate.onTitleChanged("http://www.mozilla.org", "Mozilla")
        verify(storage).recordObservation("http://www.mozilla.org", PageObservation("Mozilla"))
    }

    @Test
    fun `history delegate passes through getVisited calls`() = runBlocking {
        class TestHistoryStorage : HistoryStorage {
            var getVisitedListCalled = false
            var getVisitedPlainCalled = false

            override suspend fun recordVisit(uri: String, visitType: VisitType) {
                fail()
            }

            override suspend fun recordObservation(uri: String, observation: PageObservation) {
                fail()
            }

            override suspend fun getVisited(uris: List<String>): List<Boolean> {
                getVisitedListCalled = true
                assertEquals(listOf("http://www.mozilla.org", "http://www.firefox.com"), uris)
                return emptyList()
            }

            override suspend fun getVisited(): List<String> {
                getVisitedPlainCalled = true
                return emptyList()
            }

            override fun getSuggestions(query: String, limit: Int): List<SearchResult> {
                fail()
                return listOf()
            }

            override fun getAutocompleteSuggestion(query: String): HistoryAutocompleteResult? {
                fail()
                return null
            }

            override fun cleanup() {
                fail()
            }
        }
        val storage = TestHistoryStorage()
        val delegate = HistoryDelegate(storage)

        assertFalse(storage.getVisitedPlainCalled)
        assertFalse(storage.getVisitedListCalled)

        delegate.getVisited()
        assertTrue(storage.getVisitedPlainCalled)
        assertFalse(storage.getVisitedListCalled)

        delegate.getVisited(listOf("http://www.mozilla.org", "http://www.firefox.com"))
        assertTrue(storage.getVisitedListCalled)
    }
}