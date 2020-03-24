/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.toolbar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.action.TrackingProtectionAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.SecurityInfoState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.TrackingProtectionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.toolbar.internal.URLRenderer
import mozilla.components.support.test.any
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class ToolbarPresenterTest {
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    @ExperimentalCoroutinesApi
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
    fun `start with no custom tab id registers on store and renders selected tab`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(createTab("https://www.mozilla.org", id = "tab1")),
                customTabs = listOf(createCustomTab("https://www.example.org", id = "ct")),
                selectedTabId = "tab1"))
        )
        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))

        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(store).observeManually(any())

        verify(toolbarPresenter).render(any())

        verify(toolbarPresenter.renderer).post("https://www.mozilla.org")
        verify(toolbar).setSearchTerms("")
        verify(toolbar).displayProgress(0)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.INSECURE
    }

    @Test
    fun `start with custom tab id registers on store and renders custom tab`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(createTab("https://www.mozilla.org", id = "tab1")),
                customTabs = listOf(createCustomTab("https://www.example.org", id = "ct")),
                selectedTabId = "tab1"))
        )
        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store, customTabId = "ct"))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(store).observeManually(any())
        verify(toolbarPresenter).render(any())

        verify(toolbarPresenter.renderer).post("https://www.example.org")
        verify(toolbar).setSearchTerms("")
        verify(toolbar).displayProgress(0)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.INSECURE
    }

    @Test
    fun `SecurityInfoState change updates toolbar`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(createTab("https://www.mozilla.org", id = "tab1")),
                customTabs = listOf(createCustomTab("https://www.example.org", id = "ct")),
                selectedTabId = "tab1"))
        )

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(toolbar, never()).siteSecure = Toolbar.SiteSecurity.SECURE

        store.dispatch(ContentAction.UpdateSecurityInfoAction("tab1", SecurityInfoState(
            secure = true,
            host = "mozilla.org",
            issuer = "Mozilla"
        ))).joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).siteSecure = Toolbar.SiteSecurity.SECURE
    }

    @Test
    fun `Toolbar gets cleared when all tabs are removed`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(TabSessionState(
                    id = "tab1",
                    content = ContentState(
                        url = "https://www.mozilla.org",
                        securityInfo = SecurityInfoState(true, "mozilla.org", "Mozilla"),
                        searchTerms = "Hello World",
                        progress = 60
                    )
                )),
                selectedTabId = "tab1"))
        )

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(toolbarPresenter.renderer).start()
        verify(toolbarPresenter.renderer).post("https://www.mozilla.org")
        verify(toolbar).setSearchTerms("Hello World")
        verify(toolbar).displayProgress(60)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.SECURE
        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.OFF_GLOBALLY
        verifyNoMoreInteractions(toolbarPresenter.renderer)
        verifyNoMoreInteractions(toolbar)

        store.dispatch(TabListAction.RemoveTabAction("tab1")).joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbarPresenter.renderer).post("")
        verify(toolbar).setSearchTerms("")
        verify(toolbar).displayProgress(0)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.INSECURE
    }

    @Test
    fun `Search terms changes updates toolbar`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(createTab("https://www.mozilla.org", id = "tab1")),
                customTabs = listOf(createCustomTab("https://www.example.org", id = "ct")),
                selectedTabId = "tab1"))
        )

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(toolbar, never()).setSearchTerms("Hello World")

        store.dispatch(ContentAction.UpdateSearchTermsAction(
            sessionId = "tab1",
            searchTerms = "Hello World")
        ).joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).setSearchTerms("Hello World")
    }

    @Test
    fun `Progress changes updates toolbar`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(createTab("https://www.mozilla.org", id = "tab1")),
                customTabs = listOf(createCustomTab("https://www.example.org", id = "ct")),
                selectedTabId = "tab1"))
        )

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(toolbar, never()).displayProgress(75)

        store.dispatch(
            ContentAction.UpdateProgressAction("tab1", 75)
        ).joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).displayProgress(75)

        verify(toolbar, never()).displayProgress(90)

        store.dispatch(
            ContentAction.UpdateProgressAction("tab1", 90)
        ).joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).displayProgress(90)
    }

    @Test
    fun `Toolbar does not get cleared if a background tab gets removed`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(
                    TabSessionState(
                        id = "tab1",
                        content = ContentState(
                            url = "https://www.mozilla.org",
                            securityInfo = SecurityInfoState(true, "mozilla.org", "Mozilla"),
                            searchTerms = "Hello World",
                            progress = 60
                        )
                    ),
                    createTab(id = "tab2", url = "https://www.example.org")),
                selectedTabId = "tab1"))
        )

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        store.dispatch(TabListAction.RemoveTabAction("tab2")).joinBlocking()

        verify(toolbarPresenter.renderer).start()
        verify(toolbarPresenter.renderer).post("https://www.mozilla.org")
        verify(toolbar).setSearchTerms("Hello World")
        verify(toolbar).displayProgress(60)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.SECURE
        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.OFF_GLOBALLY
        verifyNoMoreInteractions(toolbarPresenter.renderer)
        verifyNoMoreInteractions(toolbar)
    }

    @Test
    fun `Toolbar is updated when selected tab changes`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(
                    TabSessionState(
                        id = "tab1",
                        content = ContentState(
                            url = "https://www.mozilla.org",
                            securityInfo = SecurityInfoState(true, "mozilla.org", "Mozilla"),
                            searchTerms = "Hello World",
                            progress = 60
                        )
                    ),
                    TabSessionState(
                        id = "tab2",
                        content = ContentState(
                            url = "https://www.example.org",
                            securityInfo = SecurityInfoState(false, "example.org", "Example"),
                            searchTerms = "Example",
                            progress = 90
                        ),
                        trackingProtection = TrackingProtectionState(enabled = true)
                    )),
                selectedTabId = "tab1"))
        )

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(toolbarPresenter.renderer).start()
        verify(toolbarPresenter.renderer).post("https://www.mozilla.org")
        verify(toolbar).setSearchTerms("Hello World")
        verify(toolbar).displayProgress(60)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.SECURE
        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.OFF_GLOBALLY
        verifyNoMoreInteractions(toolbarPresenter.renderer)
        verifyNoMoreInteractions(toolbar)

        store.dispatch(TabListAction.SelectTabAction("tab2")).joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbarPresenter.renderer).post("https://www.example.org")
        verify(toolbar).setSearchTerms("Example")
        verify(toolbar).displayProgress(90)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.INSECURE
        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED
        verifyNoMoreInteractions(toolbarPresenter.renderer)
        verifyNoMoreInteractions(toolbar)
    }

    @Test
    fun `displaying different tracking protection states`() {
        val toolbar: Toolbar = mock()
        val store = spy(BrowserStore(
            BrowserState(
                tabs = listOf(
                    TabSessionState(
                        id = "tab",
                        content = ContentState(
                            url = "https://www.mozilla.org",
                            securityInfo = SecurityInfoState(true, "mozilla.org", "Mozilla"),
                            searchTerms = "Hello World",
                            progress = 60
                        )
                    )),
                selectedTabId = "tab")
        ))

        val toolbarPresenter = spy(ToolbarPresenter(toolbar, store))
        toolbarPresenter.renderer = mock()

        toolbarPresenter.start()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.OFF_GLOBALLY

        store.dispatch(TrackingProtectionAction.ToggleAction("tab", true))
            .joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED

        store.dispatch(TrackingProtectionAction.TrackerBlockedAction("tab", mock()))
            .joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.ON_TRACKERS_BLOCKED

        store.dispatch(TrackingProtectionAction.ToggleExclusionListAction("tab", true))
            .joinBlocking()

        testDispatcher.advanceUntilIdle()

        verify(toolbar).siteTrackingProtection = Toolbar.SiteTrackingProtection.OFF_FOR_A_SITE
    }

    @Test
    fun `Stopping presenter stops renderer`() {
        val store = BrowserStore()
        val presenter = ToolbarPresenter(mock(), store)

        val renderer: URLRenderer = mock()
        presenter.renderer = renderer

        presenter.start()

        verify(renderer, never()).stop()

        presenter.stop()

        verify(renderer).stop()
    }

    @Test
    fun `Toolbar displays empty state without tabs`() {
        val store = BrowserStore()
        val toolbar: Toolbar = mock()
        val presenter = ToolbarPresenter(toolbar, store)
        presenter.renderer = mock()

        presenter.start()

        testDispatcher.advanceUntilIdle()

        verify(presenter.renderer).post("")
        verify(toolbar).setSearchTerms("")
        verify(toolbar).displayProgress(0)
        verify(toolbar).siteSecure = Toolbar.SiteSecurity.INSECURE
    }
}
