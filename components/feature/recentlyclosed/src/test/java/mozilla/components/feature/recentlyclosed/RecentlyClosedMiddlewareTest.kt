/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.recentlyclosed

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.undo.UndoMiddleware
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.action.UndoAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.recover.RecoverableTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.eq
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RecentlyClosedMiddlewareTest {
    lateinit var store: BrowserStore
    lateinit var engine: Engine

    private val dispatcher = TestCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)

    @After
    fun tearDown() {
        dispatcher.cleanupTestCoroutines()
    }

    @Before
    fun setup() {
        store = mock()
        engine = mock()
    }

    // Test tab
    private val closedTab = RecoverableTab(
        id = "tab-id",
        title = "Mozilla",
        url = "https://mozilla.org",
        lastAccess = 1234
    )

    @Test
    fun `closed tab storage stores the provided tab on add tab action`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val store = BrowserStore(
            initialState = BrowserState(),
            middleware = listOf(middleware)
        )

        store.dispatch(RecentlyClosedAction.AddClosedTabsAction(listOf(closedTab))).joinBlocking()
        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        verify(storage).addTabsToCollectionWithMax(
            listOf(closedTab), 5
        )
    }

    @Test
    fun `closed tab storage adds normal tabs removed with TabListAction`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val tab = createTab("https://www.mozilla.org", private = false, id = "1234")
        val tab2 = createTab("https://www.firefox.com", private = false, id = "5678")

        var sessionManager: SessionManager? = null
        val lookup: () -> SessionManager = { sessionManager!! }

        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(tab, tab2)
            ),
            middleware = listOf(UndoMiddleware(lookup, mainScope = scope), middleware)
        )

        sessionManager = SessionManager(engine = mock(), store = store)

        store.dispatch(TabListAction.RemoveTabsAction(listOf("1234", "5678"))).joinBlocking()
        store.dispatch(UndoAction.ClearRecoverableTabs(store.state.undoHistory.tag)).joinBlocking()

        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        val closedTabCaptor = argumentCaptor<List<RecoverableTab>>()
        verify(storage).addTabsToCollectionWithMax(
            closedTabCaptor.capture(),
            eq(5)
        )
        assertEquals(2, closedTabCaptor.value.size)
        assertEquals(tab.content.title, closedTabCaptor.value[0].title)
        assertEquals(tab.content.url, closedTabCaptor.value[0].url)
        assertEquals(tab2.content.title, closedTabCaptor.value[1].title)
        assertEquals(tab2.content.url, closedTabCaptor.value[1].url)
        assertEquals(
            tab.engineState.engineSessionState,
            closedTabCaptor.value[0].state
        )
        assertEquals(
            tab2.engineState.engineSessionState,
            closedTabCaptor.value[1].state
        )
    }

    @Test
    fun `closed tab storage adds a normal tab removed with TabListAction`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val tab = createTab("https://www.mozilla.org", private = false, id = "1234")

        var sessionManager: SessionManager? = null
        val lookup: () -> SessionManager = { sessionManager!! }

        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(tab)
            ),
            middleware = listOf(UndoMiddleware(lookup, mainScope = scope), middleware)
        )

        sessionManager = SessionManager(engine = mock(), store = store)

        store.dispatch(TabListAction.RemoveTabAction("1234")).joinBlocking()
        store.dispatch(UndoAction.ClearRecoverableTabs(store.state.undoHistory.tag)).joinBlocking()

        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        val closedTabCaptor = argumentCaptor<List<RecoverableTab>>()
        verify(storage).addTabsToCollectionWithMax(
            closedTabCaptor.capture(),
            eq(5)
        )
        assertEquals(1, closedTabCaptor.value.size)
        assertEquals(tab.content.title, closedTabCaptor.value[0].title)
        assertEquals(tab.content.url, closedTabCaptor.value[0].url)
        assertEquals(
            tab.engineState.engineSessionState,
            closedTabCaptor.value[0].state
        )
    }

    @Test
    fun `closed tab storage does not add a private tab removed with TabListAction`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val tab = createTab("https://www.mozilla.org", private = true, id = "1234")

        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(tab)
            ),
            middleware = listOf(middleware)
        )

        store.dispatch(TabListAction.RemoveTabAction("1234")).joinBlocking()
        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        verify(storage).getTabs()
        verifyNoMoreInteractions(storage)
    }

    @Test
    fun `closed tab storage adds all normals tab removed with TabListAction RemoveAllNormalTabsAction`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val tab = createTab("https://www.mozilla.org", private = false, id = "1234")
        val tab2 = createTab("https://www.firefox.com", private = true, id = "3456")

        var sessionManager: SessionManager? = null
        val lookup: () -> SessionManager = { sessionManager!! }

        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(tab, tab2)
            ),
            middleware = listOf(UndoMiddleware(lookup, mainScope = scope), middleware)
        )

        sessionManager = SessionManager(engine = mock(), store = store)

        store.dispatch(TabListAction.RemoveAllNormalTabsAction).joinBlocking()
        store.dispatch(UndoAction.ClearRecoverableTabs(store.state.undoHistory.tag)).joinBlocking()

        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        val closedTabCaptor = argumentCaptor<List<RecoverableTab>>()
        verify(storage).addTabsToCollectionWithMax(
            closedTabCaptor.capture(),
            eq(5)
        )
        assertEquals(1, closedTabCaptor.value.size)
        assertEquals(tab.content.title, closedTabCaptor.value[0].title)
        assertEquals(tab.content.url, closedTabCaptor.value[0].url)
        assertEquals(
            tab.engineState.engineSessionState,
            closedTabCaptor.value[0].state
        )
    }

    @Test
    fun `closed tab storage adds all normal tabs and no private tabs removed with TabListAction RemoveAllTabsAction`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val tab = createTab("https://www.mozilla.org", private = false, id = "1234")
        val tab2 = createTab("https://www.firefox.com", private = true, id = "3456")

        var sessionManager: SessionManager? = null
        val lookup: () -> SessionManager = { sessionManager!! }

        val store = BrowserStore(
            initialState = BrowserState(
                tabs = listOf(tab, tab2)
            ),
            middleware = listOf(UndoMiddleware(lookup, mainScope = scope), middleware)
        )

        sessionManager = SessionManager(engine = mock(), store = store)

        store.dispatch(TabListAction.RemoveAllTabsAction).joinBlocking()
        store.dispatch(UndoAction.ClearRecoverableTabs(store.state.undoHistory.tag)).joinBlocking()

        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        val closedTabCaptor = argumentCaptor<List<RecoverableTab>>()
        verify(storage).addTabsToCollectionWithMax(
            closedTabCaptor.capture(),
            eq(5)
        )
        assertEquals(1, closedTabCaptor.value.size)
        assertEquals(tab.content.title, closedTabCaptor.value[0].title)
        assertEquals(tab.content.url, closedTabCaptor.value[0].url)
        assertEquals(
            tab.engineState.engineSessionState,
            closedTabCaptor.value[0].state
        )
    }

    @Test
    fun `fetch the tabs from the recently closed storage and load into browser state on initialize tab state action`() {
        val storage = mockStorage(tabs = listOf(closedTab))

        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)
        val store = BrowserStore(initialState = BrowserState(), middleware = listOf(middleware))

        // Wait for Init action of store to be processed
        store.waitUntilIdle()

        // Now wait for Middleware to process Init action and store to process action from middleware
        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        verify(storage).getTabs()
        assertEquals(closedTab, store.state.closedTabs[0])
    }

    @Test
    fun `recently closed storage removes the provided tab on remove tab action`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)

        val store = BrowserStore(
            initialState = BrowserState(
                closedTabs = listOf(
                    closedTab
                )
            ),
            middleware = listOf(middleware)
        )

        store.dispatch(RecentlyClosedAction.RemoveClosedTabAction(closedTab)).joinBlocking()
        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()
        verify(storage).removeTab(closedTab)
    }

    @Test
    fun `recently closed storage removes all tabs on remove all tabs action`() {
        val storage = mockStorage()
        val middleware = RecentlyClosedMiddleware(testContext, 5, engine, lazy { storage }, scope)
        val store = BrowserStore(
            initialState = BrowserState(
                closedTabs = listOf(
                    closedTab
                )
            ),
            middleware = listOf(middleware)
        )

        store.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction).joinBlocking()
        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()

        dispatcher.advanceUntilIdle()
        store.waitUntilIdle()
        verify(storage).removeAllTabs()
    }
}

private fun mockStorage(
    tabs: List<RecoverableTab> = emptyList()
): RecentlyClosedMiddleware.Storage {
    val storage: RecentlyClosedMiddleware.Storage = mock()

    whenever(storage.getTabs()).thenReturn(
        flow {
            emit(tabs)
        }
    )

    return storage
}
