/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import kotlinx.coroutines.runBlocking
import mozilla.components.feature.top.sites.db.PinnedSiteDao
import mozilla.components.feature.top.sites.db.PinnedSiteEntity
import mozilla.components.feature.top.sites.db.TopSiteDatabase
import mozilla.components.feature.top.sites.TopSite.Type.DEFAULT
import mozilla.components.feature.top.sites.TopSite.Type.PINNED
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PinnedSitesStorageTest {

    @Test
    fun addAllDefaultSites() = runBlocking {
        val storage = PinnedSiteStorage(mock())
        val dao = mockDao(storage)

        storage.currentTimeMillis = { 42 }

        storage.addAllPinnedSites(listOf(
            Pair("Mozilla", "https://www.mozilla.org"),
            Pair("Firefox", "https://www.firefox.com"),
            Pair("Wikipedia", "https://www.wikipedia.com"),
            Pair("Pocket", "https://www.getpocket.com")
        ), isDefault = true)

        verify(dao).insertAllPinnedSites(listOf(
            PinnedSiteEntity(title = "Mozilla", url = "https://www.mozilla.org", isDefault = true, createdAt = 42),
            PinnedSiteEntity(title = "Firefox", url = "https://www.firefox.com", isDefault = true, createdAt = 42),
            PinnedSiteEntity(title = "Wikipedia", url = "https://www.wikipedia.com", isDefault = true, createdAt = 42),
            PinnedSiteEntity(title = "Pocket", url = "https://www.getpocket.com", isDefault = true, createdAt = 42)
        ))

        Unit
    }

    @Test
    fun addPinnedSite() = runBlocking {
        val storage = PinnedSiteStorage(mock())
        val dao = mockDao(storage)

        storage.currentTimeMillis = { 3 }

        storage.addPinnedSite("Mozilla", "https://www.mozilla.org")
        storage.addPinnedSite("Firefox", "https://www.firefox.com", isDefault = true)

        // PinnedSiteDao.insertPinnedSite is actually called with "id = null", but due to an
        // extraneous assignment ("entity.id = ") in PinnedSiteStorage.addPinnedSite we can for now
        // only verify the call with "id = 0". See issue #9708.
        verify(dao).insertPinnedSite(PinnedSiteEntity(id = 0, title = "Mozilla", url = "https://www.mozilla.org", isDefault = false, createdAt = 3))
        verify(dao).insertPinnedSite(PinnedSiteEntity(id = 0, title = "Firefox", url = "https://www.firefox.com", isDefault = true, createdAt = 3))

        Unit
    }

    @Test
    fun removePinnedSite() = runBlocking {
        val storage = PinnedSiteStorage(mock())
        val dao = mockDao(storage)

        storage.removePinnedSite(TopSite(1, "Mozilla", "https://www.mozilla.org", 1, PINNED))
        storage.removePinnedSite(TopSite(2, "Firefox", "https://www.firefox.com", 1, DEFAULT))

        verify(dao).deletePinnedSite(PinnedSiteEntity(1, "Mozilla", "https://www.mozilla.org", false, 1))
        verify(dao).deletePinnedSite(PinnedSiteEntity(2, "Firefox", "https://www.firefox.com", true, 1))
    }

    @Test
    fun getPinnedSites() = runBlocking {
        val storage = PinnedSiteStorage(mock())
        val dao = mockDao(storage)

        `when`(dao.getPinnedSites()).thenReturn(listOf(
            PinnedSiteEntity(1, "Mozilla", "https://www.mozilla.org", false, 10),
            PinnedSiteEntity(2, "Firefox", "https://www.firefox.com", true, 10)
        ))
        `when`(dao.getPinnedSitesCount()).thenReturn(2)

        val topSites = storage.getPinnedSites()
        val topSitesCount = storage.getPinnedSitesCount()

        assertNotNull(topSites)
        assertEquals(2, topSites.size)
        assertEquals(2, topSitesCount)

        with(topSites[0]) {
            assertEquals(1L, id)
            assertEquals("Mozilla", title)
            assertEquals("https://www.mozilla.org", url)
            assertEquals(PINNED, type)
            assertEquals(10L, createdAt)
        }

        with(topSites[1]) {
            assertEquals(2L, id)
            assertEquals("Firefox", title)
            assertEquals("https://www.firefox.com", url)
            assertEquals(DEFAULT, type)
            assertEquals(10L, createdAt)
        }
    }

    @Test
    fun updatePinnedSite() = runBlocking {
        val storage = PinnedSiteStorage(mock())
        val dao = mockDao(storage)

        val site = TopSite(1, "Mozilla", "https://www.mozilla.org", 1, PINNED)
        storage.updatePinnedSite(site, "Mozilla (IT)", "https://www.mozilla.org/it")

        verify(dao).updatePinnedSite(PinnedSiteEntity(1, "Mozilla (IT)", "https://www.mozilla.org/it", false, 1))
    }

    private fun mockDao(storage: PinnedSiteStorage): PinnedSiteDao {
        val db = mock<TopSiteDatabase>()
        storage.database = lazy { db }
        val dao = mock<PinnedSiteDao>()
        `when`(db.pinnedSiteDao()).thenReturn(dao)
        return dao
    }
}
