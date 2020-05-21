/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tab.collections.db

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.PagedList
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class TabDaoTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var database: TabCollectionDatabase
    private lateinit var tabCollectionDao: TabCollectionDao
    private lateinit var tabDao: TabDao
    private lateinit var executor: ExecutorService

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, TabCollectionDatabase::class.java).build()
        tabCollectionDao = database.tabCollectionDao()
        tabDao = database.tabDao()
        executor = Executors.newSingleThreadExecutor()
    }

    @Test
    fun testAddingTabsToCollection() = runBlockingTest {
        val collection = TabCollectionEntity(title = "Collection One", createdAt = 10).also {
            it.id = tabCollectionDao.insertTabCollection(it)
        }

        val tab1 = TabEntity(
            title = "Tab One",
            url = "https://www.mozilla.org",
            stateFile = UUID.randomUUID().toString(),
            tabCollectionId = collection.id!!,
            createdAt = 200
        )

        val tab2 = TabEntity(
            title = "Tab Two",
            url = "https://www.firefox.com",
            stateFile = UUID.randomUUID().toString(),
            tabCollectionId = collection.id!!,
            createdAt = 100
        )

        val ids = tabDao.insertTabs(listOf(tab1, tab2))
        tab1.id = ids[0]
        tab2.id = ids[1]

        val dataSource = tabCollectionDao.getTabCollectionsPaged()
            .create()

        val pagedList = PagedList.Builder(dataSource, 10)
            .setNotifyExecutor(executor)
            .setFetchExecutor(executor)
            .build()

        assertEquals(1, pagedList.size)
        assertEquals(2, pagedList[0]!!.tabs.size)
        assertEquals(tab1, pagedList[0]!!.tabs[0])
        assertEquals(tab2, pagedList[0]!!.tabs[1])
    }

    @Test
    fun testRemovingTabFromCollection() = runBlockingTest {
        val collection = TabCollectionEntity(title = "Collection One", createdAt = 10).also {
            it.id = tabCollectionDao.insertTabCollection(it)
        }

        val tab1 = TabEntity(
            title = "Tab One",
            url = "https://www.mozilla.org",
            stateFile = UUID.randomUUID().toString(),
            tabCollectionId = collection.id!!,
            createdAt = 200
        )

        val tab2 = TabEntity(
            title = "Tab Two",
            url = "https://www.firefox.com",
            stateFile = UUID.randomUUID().toString(),
            tabCollectionId = collection.id!!,
            createdAt = 100
        )

        val ids = tabDao.insertTabs(listOf(tab1, tab2))
        tab1.id = ids[0]
        tab2.id = ids[1]

        tabDao.deleteTab(tab1)

        val dataSource = tabCollectionDao.getTabCollectionsPaged()
            .create()

        val pagedList = PagedList.Builder(dataSource, 10)
            .setNotifyExecutor(executor)
            .setFetchExecutor(executor)
            .build()

        assertEquals(1, pagedList.size)
        assertEquals(1, pagedList[0]!!.tabs.size)
        assertEquals(tab2, pagedList[0]!!.tabs[0])
    }

    @After
    fun tearDown() {
        database.close()
        executor.shutdown()
    }
}
