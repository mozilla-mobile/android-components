/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.recentlyclosed

import android.content.Context
import android.util.AttributeSet
import android.util.JsonWriter
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.state.state.ClosedTabSessionState
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.Settings
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.concept.engine.utils.EngineVersion
import mozilla.components.feature.recentlyclosed.db.RecentlyClosedTabEntity
import mozilla.components.feature.recentlyclosed.db.RecentlyClosedTabsDatabase
import mozilla.components.support.ktx.java.io.truncateDirectory
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class RecentlyClosedTabsStorageTest {
    private lateinit var context: Context
    private lateinit var storage: RecentlyClosedTabsStorage
    private lateinit var executor: ExecutorService
    private lateinit var dispatcher: TestCoroutineDispatcher
    private lateinit var engine: Engine

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        executor = Executors.newSingleThreadExecutor()
        context = ApplicationProvider.getApplicationContext()

        val database =
            Room.inMemoryDatabaseBuilder(context, RecentlyClosedTabsDatabase::class.java).build()

        dispatcher = TestCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

        engine = FakeEngine()
        storage = RecentlyClosedTabsStorage(context, engine, scope)
        storage.database = lazy { database }
    }

    @After
    fun tearDown() {
        RecentlyClosedTabEntity.getStateDirectory(context.filesDir).truncateDirectory()

        executor.shutdown()
    }

    @Test
    fun testAddingTabsWithMax() = runBlockingTest {
        // Test tab
        val closedTab = ClosedTabSessionState(
            id = "first-tab",
            title = "Mozilla",
            url = "https://mozilla.org",
            createdAt = System.currentTimeMillis()
        )

        // Test tab
        val secondClosedTab = ClosedTabSessionState(
            id = "second-tab",
            title = "Pocket",
            url = "https://pocket.com",
            createdAt = System.currentTimeMillis()
        )

        storage.addTabsToCollectionWithMax(listOf(closedTab, secondClosedTab), 1)

        dispatcher.advanceUntilIdle()

        val tabs = storage.getTabs().first()

        Assert.assertEquals(1, tabs.size)
        Assert.assertEquals(secondClosedTab.url, tabs[0].url)
        Assert.assertEquals(secondClosedTab.title, tabs[0].title)
        Assert.assertEquals(secondClosedTab.createdAt, tabs[0].createdAt)

        // Test tab
        val thirdClosedTab = ClosedTabSessionState(
            id = "third-tab",
            title = "Firefox",
            url = "https://firefox.com",
            createdAt = System.currentTimeMillis()
        )

        storage.addTabsToCollectionWithMax(listOf(thirdClosedTab), 1)

        dispatcher.advanceUntilIdle()

        val newTabs = storage.getTabs().first()

        Assert.assertEquals(1, newTabs.size)
        Assert.assertEquals(thirdClosedTab.url, newTabs[0].url)
        Assert.assertEquals(thirdClosedTab.title, newTabs[0].title)
        Assert.assertEquals(thirdClosedTab.createdAt, newTabs[0].createdAt)
    }

    @Test
    fun testRemovingAllTabs() = runBlockingTest {
        // Test tab
        val closedTab = ClosedTabSessionState(
            id = "first-tab",
            title = "Mozilla",
            url = "https://mozilla.org",
            createdAt = System.currentTimeMillis()
        )

        // Test tab
        val secondClosedTab = ClosedTabSessionState(
            id = "second-tab",
            title = "Pocket",
            url = "https://pocket.com",
            createdAt = System.currentTimeMillis()
        )

        storage.addTabsToCollectionWithMax(listOf(closedTab, secondClosedTab), 2)

        dispatcher.advanceUntilIdle()

        val tabs = storage.getTabs().first()

        Assert.assertEquals(2, tabs.size)
        Assert.assertEquals(closedTab.url, tabs[0].url)
        Assert.assertEquals(closedTab.title, tabs[0].title)
        Assert.assertEquals(closedTab.createdAt, tabs[0].createdAt)
        Assert.assertEquals(secondClosedTab.url, tabs[1].url)
        Assert.assertEquals(secondClosedTab.title, tabs[1].title)
        Assert.assertEquals(secondClosedTab.createdAt, tabs[1].createdAt)

        storage.removeAllTabs()

        dispatcher.advanceUntilIdle()

        val newTabs = storage.getTabs().first()

        Assert.assertEquals(0, newTabs.size)
    }

    @Test
    fun testRemovingOneTab() = runBlockingTest {
        // Test tab
        val closedTab = ClosedTabSessionState(
            id = "first-tab",
            title = "Mozilla",
            url = "https://mozilla.org",
            createdAt = System.currentTimeMillis()
        )

        // Test tab
        val secondClosedTab = ClosedTabSessionState(
            id = "second-tab",
            title = "Pocket",
            url = "https://pocket.com",
            createdAt = System.currentTimeMillis()
        )

        storage.addTabState(closedTab)
        storage.addTabState(secondClosedTab)

        dispatcher.advanceUntilIdle()

        val tabs = storage.getTabs().first()

        Assert.assertEquals(2, tabs.size)
        Assert.assertEquals(closedTab.url, tabs[0].url)
        Assert.assertEquals(closedTab.title, tabs[0].title)
        Assert.assertEquals(closedTab.createdAt, tabs[0].createdAt)
        Assert.assertEquals(secondClosedTab.url, tabs[1].url)
        Assert.assertEquals(secondClosedTab.title, tabs[1].title)
        Assert.assertEquals(secondClosedTab.createdAt, tabs[1].createdAt)

        storage.removeTab(tabs[0])

        dispatcher.advanceUntilIdle()

        val newTabs = storage.getTabs().first()

        Assert.assertEquals(1, newTabs.size)
        Assert.assertEquals(secondClosedTab.url, newTabs[0].url)
        Assert.assertEquals(secondClosedTab.title, newTabs[0].title)
        Assert.assertEquals(secondClosedTab.createdAt, newTabs[0].createdAt)
    }

    class FakeEngine : Engine {
        override val version: EngineVersion
            get() = throw NotImplementedError("Not needed for test")

        override fun createView(context: Context, attrs: AttributeSet?): EngineView =
            throw UnsupportedOperationException()

        override fun createSession(private: Boolean, contextId: String?): EngineSession =
            throw UnsupportedOperationException()

        override fun createSessionState(json: JSONObject) = FakeEngineSessionState()

        override fun name(): String =
            throw UnsupportedOperationException()

        override fun speculativeConnect(url: String) =
            throw UnsupportedOperationException()

        override val profiler: Profiler?
            get() = throw NotImplementedError("Not needed for test")

        override val settings: Settings = DefaultSettings()
    }

    class FakeEngineSessionState : EngineSessionState {
        override fun writeTo(writer: JsonWriter) {
            writer.beginObject()
            writer.endObject()
        }
        override fun toJSON() = JSONObject()
    }
}
