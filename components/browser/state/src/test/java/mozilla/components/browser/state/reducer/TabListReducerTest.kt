/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

private val tab1 = TabSessionState(id = "tab1", content = mock())
private val tab2 = TabSessionState(id = "tab2", content = mock(), parentId = tab1.id)
private val tab3 = TabSessionState(id = "tab3", content = mock(), parentId = tab2.id)
private val tab4 = TabSessionState(id = "tab4", content = mock(), parentId = tab3.id)

class TabListReducerTest {
    @Test
    fun `GIVEN a tab which doesn't exist in state WHEN RemoveTabAction is called for it THEN the state is not updated`() {
        val state = BrowserState(tabs = listOf(tab3))

        var result = TabListReducer.reduce(state, TabListAction.RemoveTabAction("random1", true))
        assertSame(state, result)

        result = TabListReducer.reduce(state, TabListAction.RemoveTabAction("random2", false))
        assertSame(state, result)
    }

    @Test
    fun `GIVEN a list of tabs WHEN RemoveTabAction is called for one THEN return all but the one removed`() {
        val initialTabs = listOf(tab1, tab2, tab3, tab4)
        val state = BrowserState(initialTabs)

        var result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab3.id, true))
        // Verifying the ids since the tabs are reparented so object equality fails.
        assertEquals((initialTabs - tab3).map { it.id }, result.tabs.map { it.id })

        result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab2.id, false))
        assertEquals((initialTabs - tab2).map { it.id }, result.tabs.map { it.id })
    }

    @Test
    fun `GIVEN a tab is the parent of other WHEN RemoveTabAction is called for the parent THEN reparent other to it's grandparent`() {
        val initialTabs = listOf(tab1, tab2, tab3, tab4)
        val state = BrowserState(initialTabs)

        var result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab3.id, true))
        assertEquals(tab2.id, result.findTab(tab4.id)!!.parentId)

        result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab2.id, false))
        assertEquals(tab1.id, result.findTab(tab3.id)!!.parentId)
    }

    @Test
    fun `GIVEN a tab with no parent WHEN RemoveTabAction is called for it THEN don't reparent any`() {
        val noParentTab = TabSessionState(content = mock())
        val initialTabs = listOf(tab1, tab2, noParentTab, tab3, tab4)
        val state = BrowserState(initialTabs)

        var result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(noParentTab.id, true))
        assertEquals(tab1.id, result.findTab(tab2.id)!!.parentId)
        assertEquals(tab2.id, result.findTab(tab3.id)!!.parentId)
        assertEquals(tab3.id, result.findTab(tab4.id)!!.parentId)

        result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(noParentTab.id, false))
        assertEquals(tab1.id, result.findTab(tab2.id)!!.parentId)
        assertEquals(tab2.id, result.findTab(tab3.id)!!.parentId)
        assertEquals(tab3.id, result.findTab(tab4.id)!!.parentId)
    }

    @Test
    fun `GIVEN a list of tabs WHEN RemoveTabAction is called for other than the selected one THEN keep the selection`() {
        val initialTabs = listOf(tab1, tab2, tab3, tab4)
        val state = BrowserState(initialTabs, selectedTabId = tab2.id)

        var result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab1.id, true))
        assertEquals(tab2.id, result.selectedTabId)

        result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab1.id, false))
        assertEquals(tab2.id, result.selectedTabId)
    }

    @Test
    fun `GIVEN a list of tabs WHEN RemoveTabAction is called for the currently selected one with selectParentIfExists THEN select the parent`() {
        val initialTabs = listOf(tab1, tab2, tab3, tab4)
        val state = BrowserState(initialTabs, selectedTabId = tab3.id)

        val result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab3.id, true))

        assertEquals(tab2.id, result.selectedTabId)
    }

    @Test
    fun `GIVEN a list of tabs WHEN RemoveTabAction is called for the currently selected one with selectParentIfExists false THEN select another`() {
        val initialTabs = listOf(tab1, tab2, tab3, tab4)
        val state = BrowserState(initialTabs, selectedTabId = tab3.id)

        val result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(tab3.id, false))

        assertNotEquals(tab2.id, result.selectedTabId)
    }

    @Test
    fun `GIVEN a list of tabs WHEN RemoveTabAction is called for the currently selected one no parent and selectParentIfExists THEN select another`() {
        val noParentTab = TabSessionState(content = mock())
        val initialTabs = listOf(tab1, tab2, noParentTab, tab3, tab4)
        val state = BrowserState(initialTabs, selectedTabId = noParentTab.id)

        val result = TabListReducer.reduce(state, TabListAction.RemoveTabAction(noParentTab.id, false))

        assertNotNull(result.selectedTabId)
    }
}
