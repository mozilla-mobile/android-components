/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites.db

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DefaultSiteDaoTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var database: TopSiteDatabase
    private lateinit var defaultSiteDao: DefaultSiteDao
    private lateinit var executor: ExecutorService

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, TopSiteDatabase::class.java).build()
        defaultSiteDao = database.defaultSiteDao()
        executor = Executors.newSingleThreadExecutor()
    }

    @After
    fun tearDown() {
        database.close()
        executor.shutdown()
    }

    @Test
    fun insertDefaultSite() {
        val defaultSite = DefaultSiteEntity(
            region = "XX",
            language = "XX",
            title = "Mozilla",
            url = "https://www.mozilla.org"
        ).also {
            it.id = defaultSiteDao.insertDefaultSite(it)
        }

        val defaultSites = defaultSiteDao.getDefaultSites("XX", "XX")

        assertEquals(1, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(defaultSite, this)
            assertEquals(defaultSite.id, id)
            assertEquals(defaultSite.region, region)
            assertEquals(defaultSite.language, language)
            assertEquals(defaultSite.title, title)
            assertEquals(defaultSite.url, url)
        }
    }

    @Test
    fun insertAllDefaultSites() {
        val defaultSite1 = DefaultSiteEntity(
            region = "XX",
            language = "XX",
            title = "Mozilla",
            url = "https://www.mozilla.org"
        )
        val defaultSite2 = DefaultSiteEntity(
            region = "XX",
            language = "XX",
            title = "Wikipedia",
            url = "https://www.wikipedia.org"
        )

        defaultSiteDao.insertAllDefaultSites(listOf(
            defaultSite1,
            defaultSite2
        ))

        val defaultSites = defaultSiteDao.getDefaultSites("XX", "XX")

        assertEquals(2, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(defaultSite1, this)
            assertEquals(defaultSite1.id, id)
            assertEquals(defaultSite1.region, region)
            assertEquals(defaultSite1.language, language)
            assertEquals(defaultSite1.title, title)
            assertEquals(defaultSite1.url, url)
        }
        with(defaultSites[1]) {
            assertEquals(defaultSite2, this)
            assertEquals(defaultSite2.id, id)
            assertEquals(defaultSite2.region, region)
            assertEquals(defaultSite2.language, language)
            assertEquals(defaultSite2.title, title)
            assertEquals(defaultSite2.url, url)
        }
    }

    @Test
    fun getDefaultSites() {
        var defaultSite = DefaultSiteEntity(
            region = "US",
            language = "en",
            title = "Mozilla",
            url = "https://www.mozilla.org"
        ).also {
            it.id = defaultSiteDao.insertDefaultSite(it)
        }

        var defaultSites = defaultSiteDao.getDefaultSites("US", "en")

        assertEquals(1, defaultSites.size)
        assertEquals(defaultSite, defaultSites[0])

        val defaultSite2 = DefaultSiteEntity(
            region = "US",
            language = "en",
            title = "Wikipedia",
            url = "https://www.wikipedia.org"
        ).also {
            it.id = defaultSiteDao.insertDefaultSite(it)
        }

        defaultSites = defaultSiteDao.getDefaultSites("US", "en")

        assertEquals(2, defaultSites.size)
        assertEquals(defaultSite, defaultSites[0])
        assertEquals(defaultSite2, defaultSites[1])

        defaultSite = DefaultSiteEntity(
            region = "US",
            language = "XX",
            title = "Mozilla",
            url = "https://www.mozilla.org"
        ).also {
            it.id = defaultSiteDao.insertDefaultSite(it)
        }

        defaultSites = defaultSiteDao.getDefaultSites("US", "XX")

        assertEquals(1, defaultSites.size)
        assertEquals(defaultSite, defaultSites[0])

        defaultSite = DefaultSiteEntity(
            region = "XX",
            language = "en",
            title = "Mozilla",
            url = "https://www.mozilla.org"
        ).also {
            it.id = defaultSiteDao.insertDefaultSite(it)
        }

        defaultSites = defaultSiteDao.getDefaultSites("XX", "en")

        assertEquals(1, defaultSites.size)
        assertEquals(defaultSite, defaultSites[0])
    }
}
