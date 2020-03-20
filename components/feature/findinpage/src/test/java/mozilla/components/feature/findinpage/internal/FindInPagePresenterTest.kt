/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.findinpage.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.content.FindResultState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.findinpage.view.FindInPageView
import mozilla.components.support.test.any
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class FindInPagePresenterTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var store: BrowserStore

    @Before
    @ExperimentalCoroutinesApi
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = BrowserStore(BrowserState(
            tabs = listOf(
                createTab("https://www.mozilla.org", id = "test-tab")
            ),
            selectedTabId = "test-tab"
        ))
    }

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `view is updated to display latest find result`() {
        val view: FindInPageView = mock()
        val presenter = FindInPagePresenter(store, view)
        presenter.start()

        val result = FindResultState(0, 2, false)
        store.dispatch(ContentAction.AddFindResultAction("test-tab", result)).joinBlocking()
        testDispatcher.advanceUntilIdle()
        verify(view, never()).displayResult(result)

        presenter.bind(store.state.selectedTab!!)
        store.dispatch(ContentAction.AddFindResultAction("test-tab", result)).joinBlocking()
        testDispatcher.advanceUntilIdle()
        verify(view).displayResult(result)

        val result2 = FindResultState(1, 2, true)
        store.dispatch(ContentAction.AddFindResultAction("test-tab", result2)).joinBlocking()
        testDispatcher.advanceUntilIdle()
        verify(view).displayResult(result2)
    }

    @Test
    fun `no find results are observed after stop has been called`() {
        val view: FindInPageView = mock()
        val presenter = FindInPagePresenter(store, view)
        presenter.start()

        presenter.bind(store.state.selectedTab!!)
        store.dispatch(ContentAction.AddFindResultAction("test-tab", mock())).joinBlocking()
        testDispatcher.advanceUntilIdle()
        verify(view, times(1)).displayResult(any())

        presenter.stop()
        store.dispatch(ContentAction.AddFindResultAction("test-tab", mock())).joinBlocking()
        testDispatcher.advanceUntilIdle()
        verify(view, times(1)).displayResult(any())
    }

    @Test
    fun `bind updates session and focuses view`() {
        val view: FindInPageView = mock()

        val presenter = FindInPagePresenter(mock(), view)
        val session: SessionState = mock()
        presenter.bind(session)

        assertEquals(presenter.session, session)
        verify(view).focus()
    }

    @Test
    fun `unbind clears session and view`() {
        val view: FindInPageView = mock()

        val presenter = FindInPagePresenter(mock(), view)
        val session: SessionState = mock()
        presenter.bind(session)
        presenter.unbind()

        assertNull(presenter.session)
        verify(view).clear()
    }
}
