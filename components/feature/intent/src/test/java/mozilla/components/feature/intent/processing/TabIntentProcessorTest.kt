/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.intent.processing

import android.app.SearchManager
import android.content.Intent
import android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.selector.findTabByUrl
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.search.ext.createSearchEngine
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class TabIntentProcessorTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val scope = TestCoroutineScope(testDispatcher)

    private lateinit var middleware: CaptureActionsMiddleware<BrowserState, BrowserAction>

    private lateinit var searchEngine: SearchEngine
    private lateinit var store: BrowserStore
    private lateinit var engine: Engine
    private lateinit var engineSession: EngineSession

    private lateinit var sessionUseCases: SessionUseCases
    private lateinit var tabsUseCases: TabsUseCases
    private lateinit var searchUseCases: SearchUseCases

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        searchEngine = createSearchEngine(
            name = "Test",
            url = "https://localhost/?q={searchTerms}",
            icon = mock()
        )

        engine = mock()
        engineSession = mock()
        doReturn(engineSession).`when`(engine).createSession(anyBoolean(), anyString())

        middleware = CaptureActionsMiddleware()

        store = BrowserStore(
            BrowserState(
                search = SearchState(regionSearchEngines = listOf(searchEngine))
            ),
            middleware = EngineMiddleware.create(
                engine = mock(),
                scope = scope
            ) + listOf(middleware)
        )

        sessionUseCases = SessionUseCases(store)
        tabsUseCases = TabsUseCases(store)
        searchUseCases = SearchUseCases(store, tabsUseCases)
    }

    @Test
    fun `open or select tab on ACTION_VIEW intent`() {
        val useCases = SessionUseCases(store)
        val handler = TabIntentProcessor(TabsUseCases(store), useCases.loadUrl, searchUseCases.newTabSearch)
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_VIEW)
        whenever(intent.dataString).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)

        val tab = store.state.findTabByUrl("https://mozilla.org")
        assertNotNull(tab)

        val otherTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(otherTab, select = true)).joinBlocking()
        assertEquals(2, store.state.tabs.size)
        assertEquals(otherTab, store.state.selectedTab)

        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(2, store.state.tabs.size)
        assertEquals(tab, store.state.selectedTab)
    }

    @Test
    fun `open or select tab on ACTION_MAIN intent`() {
        val useCases = SessionUseCases(store)
        val handler = TabIntentProcessor(TabsUseCases(store), useCases.loadUrl, searchUseCases.newTabSearch)
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_MAIN)
        whenever(intent.dataString).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)

        val tab = store.state.findTabByUrl("https://mozilla.org")
        assertNotNull(tab)

        val otherTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(otherTab, select = true)).joinBlocking()
        assertEquals(2, store.state.tabs.size)
        assertEquals(otherTab, store.state.selectedTab)

        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(2, store.state.tabs.size)
        assertEquals(tab, store.state.selectedTab)
    }

    @Test
    fun `open or select tab on ACTION_NDEF_DISCOVERED intent`() {
        val useCases = SessionUseCases(store)
        val handler = TabIntentProcessor(TabsUseCases(store), useCases.loadUrl, searchUseCases.newTabSearch)
        val intent: Intent = mock()
        whenever(intent.action).thenReturn(ACTION_NDEF_DISCOVERED)
        whenever(intent.dataString).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)

        val tab = store.state.findTabByUrl("https://mozilla.org")
        assertNotNull(tab)

        val otherTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(otherTab, select = true)).joinBlocking()
        assertEquals(2, store.state.tabs.size)
        assertEquals(otherTab, store.state.selectedTab)

        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(2, store.state.tabs.size)
        assertEquals(tab, store.state.selectedTab)
    }

    @Test
    fun `open tab on ACTION_SEND intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("https://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)
        assertEquals("https://mozilla.org", store.state.tabs[0].content.url)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("see https://getpocket.com")
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(2, store.state.tabs.size)
        assertEquals("https://getpocket.com", store.state.tabs[1].content.url)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("see https://firefox.com and https://mozilla.org")
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(3, store.state.tabs.size)
        assertEquals("https://firefox.com", store.state.tabs[2].content.url)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("checkout the Tweet: https://tweets.mozilla.com")
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(4, store.state.tabs.size)
        assertEquals("https://tweets.mozilla.com", store.state.tabs[3].content.url)

        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn("checkout the Tweet: HTTPS://tweets.mozilla.org")
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(5, store.state.tabs.size)
        assertEquals("HTTPS://tweets.mozilla.org", store.state.tabs[4].content.url)
    }

    @Test
    fun `open tab and trigger search on ACTION_SEND if text is not a URL`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val searchTerms = "mozilla android"
        val searchUrl = "https://localhost/?q=mozilla%20android"

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(searchTerms)

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)

        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)
        assertEquals(searchUrl, store.state.tabs[0].content.url)
        assertEquals(searchTerms, store.state.tabs[0].content.searchTerms)
    }

    @Test
    fun `nothing happens on ACTION_SEND if no text is provided`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEND)
        whenever(intent.getStringExtra(Intent.EXTRA_TEXT)).thenReturn(" ")

        val processed = handler.process(intent)
        assertFalse(processed)
    }

    @Test
    fun `nothing happens on ACTION_SEARCH if text is empty`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(" ")

        val processed = handler.process(intent)
        assertFalse(processed)
    }

    @Test
    fun `open tab on ACTION_SEARCH intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn("http://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)

        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)
        assertEquals("http://mozilla.org", store.state.tabs[0].content.url)
        assertEquals("", store.state.tabs[0].content.searchTerms)
    }

    @Test
    fun `open tab and trigger search on ACTION_SEARCH intent if text is not a URL`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val searchTerms = "mozilla android"
        val searchUrl = "https://localhost/?q=mozilla%20android"

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(searchTerms)

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)

        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)
        assertEquals(searchUrl, store.state.tabs[0].content.url)
        assertEquals(searchTerms, store.state.tabs[0].content.searchTerms)
    }

    @Test
    fun `nothing happens on ACTION_WEB_SEARCH if text is empty`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_WEB_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(" ")

        val processed = handler.process(intent)
        assertFalse(processed)
    }

    @Test
    fun `open tab on ACTION_WEB_SEARCH intent`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_WEB_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn("http://mozilla.org")

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)
        assertEquals("http://mozilla.org", store.state.tabs[0].content.url)
        assertEquals("", store.state.tabs[0].content.searchTerms)
    }

    @Test
    fun `open tab and trigger search on ACTION_WEB_SEARCH intent if text is not a URL`() {
        val handler = TabIntentProcessor(TabsUseCases(store), sessionUseCases.loadUrl, searchUseCases.newTabSearch)

        val searchTerms = "mozilla android"
        val searchUrl = "https://localhost/?q=mozilla%20android"

        val intent: Intent = mock()
        whenever(intent.action).thenReturn(Intent.ACTION_SEARCH)
        whenever(intent.getStringExtra(SearchManager.QUERY)).thenReturn(searchTerms)

        assertEquals(0, store.state.tabs.size)
        handler.process(intent)
        store.waitUntilIdle()
        assertEquals(1, store.state.tabs.size)
        assertEquals(searchUrl, store.state.tabs[0].content.url)
        assertEquals(searchTerms, store.state.tabs[0].content.searchTerms)
    }
}
