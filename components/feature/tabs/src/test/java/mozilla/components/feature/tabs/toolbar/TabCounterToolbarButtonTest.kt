/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.toolbar

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.menu.MenuController
import mozilla.components.feature.tabs.R
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.whenever
import mozilla.components.ui.tabcounter.TabCounter
import mozilla.components.ui.tabcounter.TabCounterMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.eq

@RunWith(AndroidJUnit4::class)
class TabCounterToolbarButtonTest {
    private val showTabs: () -> Unit = mock()
    private val tabCounterMenu: TabCounterMenu = mock()
    private val menuController: MenuController = mock()

    private lateinit var lifecycleOwner: MockedLifecycleOwner

    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    internal class MockedLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this).apply {
            currentState = initialState
        }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }

    @Before
    fun setUp() {
        whenever(tabCounterMenu.menuController).thenReturn(menuController)
        lifecycleOwner = MockedLifecycleOwner(Lifecycle.State.STARTED)
    }

    @Test
    fun `WHEN tab counter is created THEN count is 0`() {
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = showTabs,
                store = BrowserStore(),
                menu = tabCounterMenu
            )
        )

        val view = button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter
        val counterText: TextView = view.findViewById(R.id.counter_text)
        assertEquals("0", counterText.text)
    }

    @Test
    fun `WHEN tab is added THEN tab count is updated`() {
        val store = BrowserStore()
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = showTabs,
                store = store,
                menu = tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(
            TabListAction.AddTabAction(createTab("https://www.mozilla.org"))
        ).joinBlocking()

        verify(button).updateCount(eq(1))
    }

    @Test
    fun `WHEN tab is restored THEN tab count is updated`() {
        val store = BrowserStore()
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = showTabs,
                store = store,
                menu = tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(
            TabListAction.RestoreAction(listOf(createTab("https://www.mozilla.org")))
        ).joinBlocking()

        verify(button).updateCount(eq(1))
    }

    @Test
    fun `WHEN tab is removed THEN tab count is updated`() {
        val tab = createTab("https://www.mozilla.org")
        val store = BrowserStore(BrowserState(tabs = listOf(tab)))
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = showTabs,
                store = store,
                menu = tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(TabListAction.RemoveTabAction(tab.id)).joinBlocking()
        verify(button).updateCount(eq(0))
    }

    @Test
    fun `WHEN private tab is added THEN tab count is updated`() {
        val store = BrowserStore()
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = showTabs,
                store = store,
                menu = tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        whenever(button.isPrivate(store)).then { true }

        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(
            TabListAction.AddTabAction(createTab("https://www.mozilla.org", private = true))
        ).joinBlocking()

        verify(button).updateCount(eq(1))
    }

    @Test
    fun `WHEN private tab is removed THEN tab count is updated`() {
        val tab = createTab("https://www.mozilla.org", private = true)
        val store = BrowserStore(BrowserState(tabs = listOf(tab)))
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = showTabs,
                store = store,
                menu = tabCounterMenu
            )
        )

        whenever(button.updateCount(anyInt())).then { }
        whenever(button.isPrivate(store)).then { true }

        button.createView(LinearLayout(testContext) as ViewGroup) as TabCounter

        store.dispatch(TabListAction.RemoveTabAction(tab.id)).joinBlocking()
        verify(button).updateCount(eq(0))
    }

    @Test
    fun `WHEN tab counter is clicked THEN showTabs function is invoked`() {
        var callbackInvoked = false
        val store = BrowserStore(BrowserState(tabs = listOf()))
        val button = spy(
            TabCounterToolbarButton(
                lifecycleOwner,
                false,
                showTabs = {
                    callbackInvoked = true
                },
                store = store,
                menu = tabCounterMenu
            )
        )

        val parent = spy(LinearLayout(testContext))
        doReturn(true).`when`(parent).isAttachedToWindow

        val view = button.createView(parent) as TabCounter
        view.performClick()
        assertTrue(callbackInvoked)
    }
}
