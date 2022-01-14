/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.tabstray

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabPartition
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.support.test.ext.joinBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(AndroidJUnit4::class)
class TabsTrayPresenterTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `initial set of sessions will be passed to tabs tray`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b")
                ),
                selectedTabId = "a"
            )
        )

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            closeTabsTray = {},
            tabPartitionsFilter = { null },
            tabsFilter = { true }
        )

        verifyNoMoreInteractions(tabsTray)

        presenter.start()

        testDispatcher.advanceUntilIdle()

        assertNotNull(tabsTray.updateTabs)

        assertEquals("a", tabsTray.selectedTabId!!)
        assertEquals(2, tabsTray.updateTabs!!.size)
        assertEquals("https://www.mozilla.org", tabsTray.updateTabs!![0].content.url)
        assertEquals("https://getpocket.com", tabsTray.updateTabs!![1].content.url)

        presenter.stop()
    }

    @Test
    fun `tab tray will get updated if session gets added`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b")
                ),
                selectedTabId = "a"
            )
        )

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            closeTabsTray = {},
            tabPartitionsFilter = { null },
            tabsFilter = { true }
        )

        presenter.start()

        testDispatcher.advanceUntilIdle()

        assertEquals(2, tabsTray.updateTabs!!.size)

        store.dispatch(
            TabListAction.AddTabAction(
                createTab("https://developer.mozilla.org/")
            )
        ).joinBlocking()

        assertEquals(3, tabsTray.updateTabs!!.size)

        presenter.stop()
    }

    @Test
    fun `tabs tray will get updated if session gets removed`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b")
                ),
                selectedTabId = "a"
            )
        )

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            closeTabsTray = {},
            tabPartitionsFilter = { null },
            tabsFilter = { true }
        )

        presenter.start()

        testDispatcher.advanceUntilIdle()

        assertEquals(2, tabsTray.updateTabs!!.size)

        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()
        testDispatcher.advanceUntilIdle()

        assertEquals(1, tabsTray.updateTabs!!.size)

        store.dispatch(TabListAction.RemoveTabAction("b")).joinBlocking()
        testDispatcher.advanceUntilIdle()

        assertEquals(0, tabsTray.updateTabs!!.size)

        presenter.stop()
    }

    @Test
    fun `tabs tray will get updated if all sessions get removed`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b")
                ),
                selectedTabId = "a"
            )
        )

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            closeTabsTray = {},
            tabPartitionsFilter = { null },
            tabsFilter = { true }
        )

        presenter.start()

        testDispatcher.advanceUntilIdle()

        assertEquals(2, tabsTray.updateTabs!!.size)

        store.dispatch(TabListAction.RemoveAllTabsAction()).joinBlocking()
        testDispatcher.advanceUntilIdle()

        assertEquals(0, tabsTray.updateTabs!!.size)

        presenter.stop()
    }

    @Test
    fun `tabs tray will get updated if selection changes`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b"),
                    createTab("https://developer.mozilla.org", id = "c"),
                    createTab("https://www.firefox.com", id = "d"),
                    createTab("https://www.google.com", id = "e")
                ),
                selectedTabId = "a"
            )
        )

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            closeTabsTray = {},
            tabPartitionsFilter = { null },
            tabsFilter = { true }
        )

        presenter.start()
        testDispatcher.advanceUntilIdle()

        assertEquals(5, tabsTray.updateTabs!!.size)
        assertEquals("a", tabsTray.selectedTabId)

        store.dispatch(TabListAction.SelectTabAction("d")).joinBlocking()
        testDispatcher.advanceUntilIdle()

        println("Selection: " + store.state.selectedTabId)
        assertEquals("d", tabsTray.selectedTabId)
    }

    @Test
    fun `presenter invokes session filtering`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b", private = true)
                ),
                selectedTabId = "a"
            )
        )

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            closeTabsTray = {},
            tabPartitionsFilter = { null },
            tabsFilter = { it.content.private }
        )

        presenter.start()
        testDispatcher.advanceUntilIdle()

        assertTrue(tabsTray.updateTabs?.size == 1)
    }

    @Test
    fun `presenter will close tabs tray when all sessions get removed`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b"),
                    createTab("https://developer.mozilla.org", id = "c"),
                    createTab("https://www.firefox.com", id = "d"),
                    createTab("https://www.google.com", id = "e")
                ),
                selectedTabId = "a"
            )
        )

        var closed = false

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            tabPartitionsFilter = { null },
            tabsFilter = { true },
            closeTabsTray = { closed = true }
        )

        presenter.start()
        testDispatcher.advanceUntilIdle()

        Assert.assertFalse(closed)

        store.dispatch(TabListAction.RemoveAllTabsAction()).joinBlocking()
        testDispatcher.advanceUntilIdle()

        assertTrue(closed)

        presenter.stop()
    }

    @Test
    fun `presenter will close tabs tray when last session gets removed`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab("https://www.mozilla.org", id = "a"),
                    createTab("https://getpocket.com", id = "b")
                ),
                selectedTabId = "a"
            )
        )

        var closed = false

        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            tabPartitionsFilter = { null },
            tabsFilter = { true },
            closeTabsTray = { closed = true }
        )

        presenter.start()
        testDispatcher.advanceUntilIdle()

        Assert.assertFalse(closed)

        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()
        testDispatcher.advanceUntilIdle()

        Assert.assertFalse(closed)

        store.dispatch(TabListAction.RemoveTabAction("b")).joinBlocking()
        testDispatcher.advanceUntilIdle()

        assertTrue(closed)

        presenter.stop()
    }

    @Test
    fun `tabs tray should not invoke the close callback on start`() {
        val store = BrowserStore(
            BrowserState(
                tabs = emptyList(),
                selectedTabId = null
            )
        )

        var invoked = false
        val tabsTray: MockedTabsTray = spy(MockedTabsTray())
        val presenter = TabsTrayPresenter(
            tabsTray,
            store,
            tabPartitionsFilter = { null },
            tabsFilter = { it.content.private },
            closeTabsTray = { invoked = true }
        )

        presenter.start()
        testDispatcher.advanceUntilIdle()

        assertFalse(invoked)
    }
}

private class MockedTabsTray : TabsTray {
    var updateTabs: List<TabSessionState>? = null
    var selectedTabId: String? = null

    override fun updateTabs(tabs: List<TabSessionState>, tabPartition: TabPartition?, selectedTabId: String?) {
        updateTabs = tabs
        this.selectedTabId = selectedTabId
    }
}
