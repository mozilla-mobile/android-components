/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagedList
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.feature.downloads.db.DownloadsDatabase
import mozilla.components.feature.downloads.db.Migrations
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val MIGRATION_TEST_DB = "migration-test"

@ExperimentalCoroutinesApi
class OnDeviceDownloadStorageTest {
    private lateinit var context: Context
    private lateinit var storage: DownloadStorage
    private lateinit var executor: ExecutorService
    private lateinit var database: DownloadsDatabase

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DownloadsDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        executor = Executors.newSingleThreadExecutor()

        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, DownloadsDatabase::class.java).build()

        storage = DownloadStorage(context)
        storage.database = lazy { database }
    }

    @After
    fun tearDown() {
        executor.shutdown()
        database.close()
    }

    @Test
    fun migrate1to2() {
        helper.createDatabase(MIGRATION_TEST_DB, 1).apply {
            query("SELECT * FROM downloads").use { cursor ->
                assertEquals(-1, cursor.columnNames.indexOf("is_private"))
            }
            execSQL(
                    "INSERT INTO " +
                            "downloads " +
                            "(id, url, file_name, content_type,content_length,status,destination_directory,created_at) " +
                            "VALUES " +
                            "(1,'url','file_name','content_type',1,1,'destination_directory',1)"
            )
        }

        val dbVersion2 = helper.runMigrationsAndValidate(MIGRATION_TEST_DB, 2, true, Migrations.migration_1_2)

        dbVersion2.query("SELECT * FROM downloads").use { cursor ->
            assertTrue(cursor.columnNames.contains("is_private"))

            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_private")))
        }
    }

    @Test
    fun testAddingDownload() = runBlockingTest {
        val download1 = createMockDownload("1", "url1")
        val download2 = createMockDownload("2", "url2")
        val download3 = createMockDownload("3", "url3")

        storage.add(download1)
        storage.add(download2)
        storage.add(download3)

        val downloads = getDownloadsPagedList()

        assertEquals(3, downloads.size)

        assertTrue(DownloadStorage.isSameDownload(download1, downloads.first()))
        assertTrue(DownloadStorage.isSameDownload(download2, downloads[1]!!))
        assertTrue(DownloadStorage.isSameDownload(download3, downloads[2]!!))
    }

    @Test
    fun testRemovingDownload() = runBlockingTest {
        val download1 = createMockDownload("1", "url1")
        val download2 = createMockDownload("2", "url2")

        storage.add(download1)
        storage.add(download2)

        assertEquals(2, getDownloadsPagedList().size)

        storage.remove(download1)

        val downloads = getDownloadsPagedList()
        val downloadFromDB = downloads.first()

        assertEquals(1, downloads.size)
        assertTrue(DownloadStorage.isSameDownload(download2, downloadFromDB))
    }

    @Test
    fun testGettingDownloads() = runBlockingTest {
        val download1 = createMockDownload("1", "url1")
        val download2 = createMockDownload("2", "url2")

        storage.add(download1)
        storage.add(download2)

        val downloads = getDownloadsPagedList()

        assertEquals(2, downloads.size)

        assertTrue(DownloadStorage.isSameDownload(download1, downloads.first()))
        assertTrue(DownloadStorage.isSameDownload(download2, downloads[1]!!))
    }

    @Test
    fun testRemovingDownloads() = runBlocking {
        for (index in 1..2) {
            storage.add(createMockDownload(index.toString(), "url1"))
        }

        var pagedList = getDownloadsPagedList()

        assertEquals(2, pagedList.size)

        pagedList.forEach {
            storage.remove(it)
        }

        pagedList = getDownloadsPagedList()

        assertTrue(pagedList.isEmpty())
    }

    private fun createMockDownload(id: String, url: String): DownloadState {
        return DownloadState(
                id = id,
                url = url, contentType = "application/zip", contentLength = 5242880,
                userAgent = "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/8.0 Chrome/69.0.3497.100 Mobile Safari/537.36"
        )
    }

    private fun getDownloadsPagedList(): PagedList<DownloadState> {
        val dataSource = storage.getDownloadsPaged().create()
        return PagedList.Builder(dataSource, 10)
                .setNotifyExecutor(executor)
                .setFetchExecutor(executor)
                .build()
    }
}
