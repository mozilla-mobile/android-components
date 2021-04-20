/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.BAIDU_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.CN_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.EN_LANGUAGE
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.GOOGLE_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.JD_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.POCKET_TRENDING_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.RU_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.WIKIPEDIA_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.XX_LANGUAGE
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.XX_REGION
import mozilla.components.feature.top.sites.TopSite.Type.DEFAULT
import mozilla.components.feature.top.sites.TopSite.Type.PINNED
import mozilla.components.feature.top.sites.db.DefaultSite
import mozilla.components.feature.top.sites.db.Migrations
import mozilla.components.feature.top.sites.db.TopSiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val MIGRATION_TEST_DB = "migration-test"

@Suppress("LargeClass")
class OnDevicePinnedSitesStorageTest {
    private lateinit var context: Context
    private lateinit var storage: PinnedSiteStorage
    private lateinit var executor: ExecutorService

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TopSiteDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
        executor = Executors.newSingleThreadExecutor()

        context = ApplicationProvider.getApplicationContext()
        val database = Room.inMemoryDatabaseBuilder(context, TopSiteDatabase::class.java).build()

        storage = PinnedSiteStorage(context)
        storage.database = lazy { database }
    }

    @After
    fun tearDown() {
        executor.shutdown()
    }

    @Test
    fun testAddingAllPinnedSitesAsDefault() = runBlocking {
        val defaultTopSites = listOf(
            Pair("Mozilla", "https://www.mozilla.org"),
            Pair("Firefox", "https://www.firefox.com"),
            Pair("Wikipedia", "https://www.wikipedia.com"),
            Pair("Pocket", "https://www.getpocket.com")
        )

        storage.addAllPinnedSites(defaultTopSites, isDefault = true)

        val topSites = storage.getPinnedSites()

        assertEquals(4, topSites.size)
        assertEquals(4, storage.getPinnedSitesCount())

        assertEquals("Mozilla", topSites[0].title)
        assertEquals("https://www.mozilla.org", topSites[0].url)
        assertEquals(DEFAULT, topSites[0].type)
        assertEquals("Firefox", topSites[1].title)
        assertEquals("https://www.firefox.com", topSites[1].url)
        assertEquals(DEFAULT, topSites[2].type)
        assertEquals("Wikipedia", topSites[2].title)
        assertEquals("https://www.wikipedia.com", topSites[2].url)
        assertEquals(DEFAULT, topSites[2].type)
        assertEquals("Pocket", topSites[3].title)
        assertEquals("https://www.getpocket.com", topSites[3].url)
        assertEquals(DEFAULT, topSites[3].type)
    }

    @Test
    fun testAddingPinnedSite() = runBlocking {
        storage.addPinnedSite("Mozilla", "https://www.mozilla.org")
        storage.addPinnedSite("Firefox", "https://www.firefox.com", isDefault = true)

        val topSites = storage.getPinnedSites()

        assertEquals(2, topSites.size)
        assertEquals(2, storage.getPinnedSitesCount())

        assertEquals("Mozilla", topSites[0].title)
        assertEquals("https://www.mozilla.org", topSites[0].url)
        assertEquals(PINNED, topSites[0].type)
        assertEquals("Firefox", topSites[1].title)
        assertEquals("https://www.firefox.com", topSites[1].url)
        assertEquals(DEFAULT, topSites[1].type)
    }

    @Test
    fun testRemovingPinnedSites() = runBlocking {
        storage.addPinnedSite("Mozilla", "https://www.mozilla.org")
        storage.addPinnedSite("Firefox", "https://www.firefox.com")

        storage.getPinnedSites().let { topSites ->
            assertEquals(2, topSites.size)
            assertEquals(2, storage.getPinnedSitesCount())

            storage.removePinnedSite(topSites[0])
        }

        storage.getPinnedSites().let { topSites ->
            assertEquals(1, topSites.size)
            assertEquals(1, storage.getPinnedSitesCount())

            assertEquals("Firefox", topSites[0].title)
            assertEquals("https://www.firefox.com", topSites[0].url)
        }
    }

    @Test
    fun testGettingPinnedSites() = runBlocking {
        storage.addPinnedSite("Mozilla", "https://www.mozilla.org")
        storage.addPinnedSite("Firefox", "https://www.firefox.com", isDefault = true)

        val topSites = storage.getPinnedSites()

        assertNotNull(topSites)
        assertEquals(2, topSites.size)
        assertEquals(2, storage.getPinnedSitesCount())

        with(topSites[0]) {
            assertEquals("Mozilla", title)
            assertEquals("https://www.mozilla.org", url)
            assertEquals(PINNED, type)
        }

        with(topSites[1]) {
            assertEquals("Firefox", title)
            assertEquals("https://www.firefox.com", url)
            assertEquals(DEFAULT, type)
        }
    }

    @Test
    fun testUpdatingPinnedSites() = runBlocking {
        storage.addPinnedSite("Mozilla", "https://www.mozilla.org")
        var pinnedSites = storage.getPinnedSites()

        assertEquals(1, pinnedSites.size)
        assertEquals(1, storage.getPinnedSitesCount())
        assertEquals("https://www.mozilla.org", pinnedSites[0].url)
        assertEquals("Mozilla", pinnedSites[0].title)

        storage.updatePinnedSite(pinnedSites[0], "", "")

        pinnedSites = storage.getPinnedSites()
        assertEquals(1, pinnedSites.size)
        assertEquals(1, storage.getPinnedSitesCount())
        assertEquals("", pinnedSites[0].url)
        assertEquals("", pinnedSites[0].title)

        storage.updatePinnedSite(pinnedSites[0], "Mozilla Firefox", "https://www.firefox.com")

        pinnedSites = storage.getPinnedSites()
        assertEquals(1, pinnedSites.size)
        assertEquals(1, storage.getPinnedSitesCount())
        assertEquals("https://www.firefox.com", pinnedSites[0].url)
        assertEquals("Mozilla Firefox", pinnedSites[0].title)
    }

    @Test
    fun testGettingDefaultSites() = runBlocking {
        val defaultTopSites = listOf(
            DefaultSite(
                CN_REGION,
                XX_LANGUAGE,
                context.getString(R.string.default_top_site_baidu),
                BAIDU_URL
            ),
            DefaultSite(
                CN_REGION,
                XX_LANGUAGE,
                context.getString(R.string.default_top_site_jd),
                JD_URL
            ),
            DefaultSite(
                RU_REGION,
                EN_LANGUAGE,
                context.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                RU_REGION,
                XX_LANGUAGE,
                context.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            ),
            DefaultSite(
                XX_REGION,
                XX_LANGUAGE,
                context.getString(R.string.default_top_site_google),
                GOOGLE_URL
            ),
            DefaultSite(
                XX_REGION,
                EN_LANGUAGE,
                context.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                XX_REGION,
                XX_LANGUAGE,
                context.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )

        storage.addAllDefaultSites(defaultTopSites)

        var defaultSites = storage.getDefaultSites(CN_REGION, XX_LANGUAGE)

        assertEquals(2, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(context.getString(R.string.default_top_site_baidu), title)
            assertEquals(BAIDU_URL, url)
            assertEquals(CN_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }
        with(defaultSites[1]) {
            assertEquals(context.getString(R.string.default_top_site_jd), title)
            assertEquals(JD_URL, url)
            assertEquals(CN_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }

        defaultSites = storage.getDefaultSites(RU_REGION, EN_LANGUAGE)

        assertEquals(2, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(context.getString(R.string.pocket_pinned_top_articles), title)
            assertEquals(POCKET_TRENDING_URL, url)
            assertEquals(RU_REGION, region)
            assertEquals(EN_LANGUAGE, language)
        }
        with(defaultSites[1]) {
            assertEquals(context.getString(R.string.default_top_site_wikipedia), title)
            assertEquals(WIKIPEDIA_URL, url)
            assertEquals(RU_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }

        defaultSites = storage.getDefaultSites(RU_REGION, XX_LANGUAGE)

        assertEquals(1, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(context.getString(R.string.default_top_site_wikipedia), title)
            assertEquals(WIKIPEDIA_URL, url)
            assertEquals(RU_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }

        defaultSites = storage.getDefaultSites(XX_REGION, EN_LANGUAGE)

        assertEquals(3, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(context.getString(R.string.default_top_site_google), title)
            assertEquals(GOOGLE_URL, url)
            assertEquals(XX_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }
        with(defaultSites[1]) {
            assertEquals(context.getString(R.string.pocket_pinned_top_articles), title)
            assertEquals(POCKET_TRENDING_URL, url)
            assertEquals(XX_REGION, region)
            assertEquals(EN_LANGUAGE, language)
        }
        with(defaultSites[2]) {
            assertEquals(context.getString(R.string.default_top_site_wikipedia), title)
            assertEquals(WIKIPEDIA_URL, url)
            assertEquals(XX_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }

        defaultSites = storage.getDefaultSites(XX_REGION, XX_LANGUAGE)

        assertEquals(2, defaultSites.size)
        with(defaultSites[0]) {
            assertEquals(context.getString(R.string.default_top_site_google), title)
            assertEquals(GOOGLE_URL, url)
            assertEquals(XX_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }
        with(defaultSites[1]) {
            assertEquals(context.getString(R.string.default_top_site_wikipedia), title)
            assertEquals(WIKIPEDIA_URL, url)
            assertEquals(XX_REGION, region)
            assertEquals(XX_LANGUAGE, language)
        }
    }

    @Test
    fun migrate1to2() {
        val dbVersion1 = helper.createDatabase(MIGRATION_TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO " +
                    "top_sites " +
                    "(title, url, created_at) " +
                    "VALUES " +
                    "('Mozilla','mozilla.org',1)," +
                    "('Top Articles','https://getpocket.com/fenix-top-articles',2)," +
                    "('Wikipedia','https://www.wikipedia.org/',3)," +
                    "('YouTube','https://www.youtube.com/',4)"
            )
        }

        dbVersion1.query("SELECT * FROM top_sites").use { cursor ->
            assertEquals(4, cursor.columnCount)
        }

        val dbVersion2 = helper.runMigrationsAndValidate(
            MIGRATION_TEST_DB, 2, true, Migrations.migration_1_2
        ).apply {
            execSQL(
                "INSERT INTO " +
                    "top_sites " +
                    "(title, url, is_default, created_at) " +
                    "VALUES " +
                    "('Firefox','firefox.com',1,5)," +
                    "('Monitor','https://monitor.firefox.com/',0,5)"
            )
        }

        dbVersion2.query("SELECT * FROM top_sites").use { cursor ->
            assertEquals(5, cursor.columnCount)

            // Check is_default for Mozilla
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

            // Check is_default for Top Articles
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

            // Check is_default for Wikipedia
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

            // Check is_default for YouTube
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

            // Check is_default for Firefox
            cursor.moveToNext()
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))

            // Check is_default for Monitor
            cursor.moveToNext()
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
        }
    }

    @Test
    fun migrate2to3() {
        val dbVersion2 = helper.createDatabase(MIGRATION_TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO " +
                    "top_sites " +
                    "(title, url, is_default, created_at) " +
                    "VALUES " +
                    "('Mozilla','mozilla.org',0,1)," +
                    "('Top Articles','https://getpocket.com/fenix-top-articles',0,2)," +
                    "('Wikipedia','https://www.wikipedia.org/',0,3)," +
                    "('YouTube','https://www.youtube.com/',0,4)"
            )
        }

        dbVersion2.query("SELECT * FROM top_sites").use { cursor ->
            assertEquals(5, cursor.columnCount)
        }

        val dbVersion3 = helper.runMigrationsAndValidate(
            MIGRATION_TEST_DB, 3, true, Migrations.migration_2_3
        )

        dbVersion3.query("SELECT * FROM top_sites").use { cursor ->
            assertEquals(5, cursor.columnCount)
            assertEquals(4, cursor.count)

            // Check isDefault for Mozilla
            cursor.moveToFirst()
            assertEquals("Mozilla", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("mozilla.org", cursor.getString(cursor.getColumnIndexOrThrow("url")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))

            // Check isDefault for Top Articles
            cursor.moveToNext()
            assertEquals("Top Articles", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://getpocket.com/fenix-top-articles",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))

            // Check isDefault for Wikipedia
            cursor.moveToNext()
            assertEquals("Wikipedia", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://www.wikipedia.org/",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))

            // Check isDefault for YouTube
            cursor.moveToNext()
            assertEquals("YouTube", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://www.youtube.com/",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(4, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))
        }
    }

    @Test
    fun migrate3to4() = runBlocking {
        val dbVersion3 = helper.createDatabase(MIGRATION_TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO " +
                    "top_sites " +
                    "(title, url, is_default, created_at) " +
                    "VALUES " +
                    "('Google','https://www.google.com/',1,1)," +
                    "('Top Articles','https://getpocket.com/fenix-top-articles',1,2)," +
                    "('Wikipedia','https://www.wikipedia.org/',1,3)"
            )
        }

        dbVersion3.query("SELECT * FROM top_sites").use { cursor ->
            assertEquals(5, cursor.columnCount)
        }

        val dbVersion4 = helper.runMigrationsAndValidate(
            MIGRATION_TEST_DB, 4, true, Migrations.migration_3_4
        ).apply {
            // Insert test data into the new default_top_sites table.
            execSQL(
                "INSERT INTO " +
                    "default_top_sites " +
                    "(region, language, title, url) " +
                    "VALUES " +
                    "('XX','XX','Firefox','https://www.firefox.com')," +
                    "('US','en','Monitor','https://monitor.firefox.com/')"
            )
        }

        // Inspect that no changes occurred to the top_sites table.
        dbVersion4.query("SELECT * FROM top_sites").use { cursor ->
            assertEquals(5, cursor.columnCount)
            assertEquals(3, cursor.count)

            cursor.moveToFirst()
            assertEquals("Google", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://www.google.com/",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))

            cursor.moveToNext()
            assertEquals("Top Articles", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://getpocket.com/fenix-top-articles",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))

            cursor.moveToNext()
            assertEquals("Wikipedia", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://www.wikipedia.org/",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_default")))
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("created_at")))
        }

        // Query that new records were able to be inserted into the new default_top_sites table.
        dbVersion4.query("SELECT * FROM default_top_sites").use { cursor ->
            assertEquals(5, cursor.columnCount)
            assertEquals(2, cursor.count)

            cursor.moveToFirst()
            assertEquals("XX", cursor.getString(cursor.getColumnIndexOrThrow("region")))
            assertEquals("XX", cursor.getString(cursor.getColumnIndexOrThrow("language")))
            assertEquals("Firefox", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://www.firefox.com",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )

            cursor.moveToNext()
            assertEquals("US", cursor.getString(cursor.getColumnIndexOrThrow("region")))
            assertEquals("en", cursor.getString(cursor.getColumnIndexOrThrow("language")))
            assertEquals("Monitor", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(
                "https://monitor.firefox.com/",
                cursor.getString(cursor.getColumnIndexOrThrow("url"))
            )
        }
    }
}
