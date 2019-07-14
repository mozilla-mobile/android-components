/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.action

import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TabListActionTest {

    @Test
    fun `AddTabAction - Adds provided SessionState`() {
        val store = BrowserStore()

        assertEquals(0, store.state.tabs.size)
        assertNull(store.state.selectedTabId)

        val tab = createTab(url = "https://www.mozilla.org")

        store.dispatch(TabListAction.AddTabAction(tab)).joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals(tab.id, store.state.selectedTabId)
    }

    @Test
    fun `AddTabAction - Add tab and update selection`() {
        val existingTab = createTab("https://www.mozilla.org")

        val state = BrowserState(
            tabs = listOf(existingTab),
            selectedTabId = existingTab.id
        )

        val store = BrowserStore(state)

        assertEquals(1, store.state.tabs.size)
        assertEquals(existingTab.id, store.state.selectedTabId)

        val newTab = createTab("https://firefox.com")

        store.dispatch(TabListAction.AddTabAction(newTab, select = true)).joinBlocking()

        assertEquals(2, store.state.tabs.size)
        assertEquals(newTab.id, store.state.selectedTabId)
    }

    @Test
    fun `AddTabAction - Select first tab automatically`() {
        val existingTab = createTab("https://www.mozilla.org")

        val store = BrowserStore()

        assertEquals(0, store.state.tabs.size)
        assertNull(existingTab.id, store.state.selectedTabId)

        val newTab = createTab("https://firefox.com")
        store.dispatch(TabListAction.AddTabAction(newTab, select = false)).joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals(newTab.id, store.state.selectedTabId)
    }

    @Test
    fun `AddTabAction - Specify parent tab`() {
        val store = BrowserStore()

        val tab1 = createTab("https://www.mozilla.org")
        val tab2 = createTab("https://www.firefox.com")
        val tab3 = createTab("https://wiki.mozilla.org", parent = tab1)
        val tab4 = createTab("https://github.com/mozilla-mobile/android-components", parent = tab2)

        store.dispatch(TabListAction.AddTabAction(tab1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab2)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab3)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab4)).joinBlocking()

        assertEquals(4, store.state.tabs.size)
        assertNull(store.state.tabs[0].parentId)
        assertNull(store.state.tabs[2].parentId)
        assertEquals(tab1.id, store.state.tabs[1].parentId)
        assertEquals(tab2.id, store.state.tabs[3].parentId)
    }

    @Test
    fun `AddTabAction - Tabs with parent are added after (next to) parent`() {
        val store = BrowserStore()

        val parent01 = createTab("https://www.mozilla.org")
        val parent02 = createTab("https://getpocket.com")
        val tab1 = createTab("https://www.firefox.com")
        val tab2 = createTab("https://developer.mozilla.org/en-US/")
        val child001 = createTab("https://www.mozilla.org/en-US/internet-health/", parent = parent01)
        val child002 = createTab("https://www.mozilla.org/en-US/technology/", parent = parent01)
        val child003 = createTab("https://getpocket.com/add/", parent = parent02)

        store.dispatch(TabListAction.AddTabAction(parent01)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(child001)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab2)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(parent02)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(child002)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(child003)).joinBlocking()

        assertEquals(parent01.id, store.state.tabs[0].id) // ├── parent 1
        assertEquals(child002.id, store.state.tabs[1].id) // │   ├── child 2
        assertEquals(child001.id, store.state.tabs[2].id) // │   └── child 1
        assertEquals(tab1.id, store.state.tabs[3].id) //     ├──tab 1
        assertEquals(tab2.id, store.state.tabs[4].id) //     ├──tab 2
        assertEquals(parent02.id, store.state.tabs[5].id) // └── parent 2
        assertEquals(child003.id, store.state.tabs[6].id) //     └── child 3
    }

    @Test
    fun `SelectTabAction - Selects SessionState by id`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org"),
                createTab(id = "b", url = "https://www.firefox.com")
            )
        )
        val store = BrowserStore(state)

        assertNull(store.state.selectedTabId)

        store.dispatch(TabListAction.SelectTabAction("a"))
            .joinBlocking()

        assertEquals("a", store.state.selectedTabId)
    }

    @Test
    fun `RemoveTabAction - Removes SessionState`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org"),
                createTab(id = "b", url = "https://www.firefox.com")
            )
        )
        val store = BrowserStore(state)

        store.dispatch(TabListAction.RemoveTabAction("a"))
            .joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals("https://www.firefox.com", store.state.tabs[0].content.url)
    }

    @Test
    fun `RemoveTabAction - Noop for unknown id`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org"),
                createTab(id = "b", url = "https://www.firefox.com")
            )
        )
        val store = BrowserStore(state)

        store.dispatch(TabListAction.RemoveTabAction("c"))
            .joinBlocking()

        assertEquals(2, store.state.tabs.size)
        assertEquals("https://www.mozilla.org", store.state.tabs[0].content.url)
        assertEquals("https://www.firefox.com", store.state.tabs[1].content.url)
    }

    @Test
    fun `RemoveTabAction - Selected tab id is set to null if selected and last tab is removed`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org")
            ),
            selectedTabId = "a"
        )

        val store = BrowserStore(state)

        assertEquals("a", store.state.selectedTabId)

        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()

        assertNull(store.state.selectedTabId)
    }

    @Test
    fun `RemoveTabAction - Does not select custom tab`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org")
            ),
            customTabs = listOf(
                createCustomTab(id = "b", url = "https://www.firefox.com")
            ),
            selectedTabId = "a"
        )

        val store = BrowserStore(state)

        assertEquals("a", store.state.selectedTabId)

        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()

        assertNull(store.state.selectedTabId)
    }

    @Test
    fun `RemoveTabAction - Will select next nearby tab after removing selected tab`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org"),
                createTab(id = "b", url = "https://www.firefox.com"),
                createTab(id = "c", url = "https://www.example.org"),
                createTab(id = "d", url = "https://getpocket.com")
            ),
            customTabs = listOf(
                createCustomTab(id = "a1", url = "https://www.firefox.com")
            ),
            selectedTabId = "c"
        )

        val store = BrowserStore(state)

        assertEquals("c", store.state.selectedTabId)

        store.dispatch(TabListAction.RemoveTabAction("c")).joinBlocking()
        assertEquals("d", store.state.selectedTabId)

        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()
        assertEquals("d", store.state.selectedTabId)

        store.dispatch(TabListAction.RemoveTabAction("d")).joinBlocking()
        assertEquals("b", store.state.selectedTabId)

        store.dispatch(TabListAction.RemoveTabAction("b")).joinBlocking()
        assertNull(store.state.selectedTabId)
    }

    @Test
    fun `RemoveTabAction - Selects private tab after private tab was removed`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org", private = true),
                createTab(id = "b", url = "https://www.firefox.com", private = false),
                createTab(id = "c", url = "https://www.example.org", private = false),
                createTab(id = "d", url = "https://getpocket.com", private = true),
                createTab(id = "e", url = "https://developer.mozilla.org/", private = true)
            ),
            customTabs = listOf(
                createCustomTab(id = "a1", url = "https://www.firefox.com"),
                createCustomTab(id = "b1", url = "https://hubs.mozilla.com")
            ),
            selectedTabId = "d"
        )

        val store = BrowserStore(state)

        // [a*, b, c, (d*), e*] -> [a*, b, c, (e*)]
        store.dispatch(TabListAction.RemoveTabAction("d")).joinBlocking()
        assertEquals("e", store.state.selectedTabId)

        // [a*, b, c, (e*)] -> [(a*), b, c]
        store.dispatch(TabListAction.RemoveTabAction("e")).joinBlocking()
        assertEquals("a", store.state.selectedTabId)

        // After removing the last private tab a normal tab will be selected
        // [(a*), b, c] -> [b, (c)]
        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()
        assertEquals("c", store.state.selectedTabId)
    }

    @Test
    fun `RemoveTabAction - Selects normal tab after normal tab was removed`() {
        val state = BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org", private = false),
                createTab(id = "b", url = "https://www.firefox.com", private = true),
                createTab(id = "c", url = "https://www.example.org", private = true),
                createTab(id = "d", url = "https://getpocket.com", private = false),
                createTab(id = "e", url = "https://developer.mozilla.org/", private = false)
            ),
            customTabs = listOf(
                createCustomTab(id = "a1", url = "https://www.firefox.com"),
                createCustomTab(id = "b1", url = "https://hubs.mozilla.com")
            ),
            selectedTabId = "d"
        )

        val store = BrowserStore(state)

        // [a, b*, c*, (d), e] -> [a, b*, c* (e)]
        store.dispatch(TabListAction.RemoveTabAction("d")).joinBlocking()
        assertEquals("e", store.state.selectedTabId)

        // [a, b*, c*, (e)] -> [(a), b*, c*]
        store.dispatch(TabListAction.RemoveTabAction("e")).joinBlocking()
        assertEquals("a", store.state.selectedTabId)

        // After removing the last normal tab NO private tab should get selected
        // [(a), b*, c*] -> [b*, c*]
        store.dispatch(TabListAction.RemoveTabAction("a")).joinBlocking()
        assertNull(store.state.selectedTabId)
    }

    @Test
    fun `RemoveTabAction - Parent will be selected if child is removed and flag is set to true (default)`() {
        val store = BrowserStore()

        val parent = createTab("https://www.mozilla.org")
        val tab1 = createTab("https://www.firefox.com")
        val tab2 = createTab("https://getpocket.com")
        val child = createTab("https://www.mozilla.org/en-US/internet-health/", parent = parent)

        store.dispatch(TabListAction.AddTabAction(parent)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab2)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(child)).joinBlocking()

        store.dispatch(TabListAction.SelectTabAction(child.id)).joinBlocking()
        store.dispatch(TabListAction.RemoveTabAction(child.id, selectParentIfExists = true)).joinBlocking()

        assertEquals(parent.id, store.state.selectedTabId)
        assertEquals("https://www.mozilla.org", store.state.selectedTab?.content?.url)
    }

    @Test
    fun `RemoveTabAction - Parent will not be selected if child is removed and flag is set to false`() {
        val store = BrowserStore()

        val parent = createTab("https://www.mozilla.org")

        val tab1 = createTab("https://www.firefox.com")
        val tab2 = createTab("https://getpocket.com")
        val child1 = createTab("https://www.mozilla.org/en-US/internet-health/", parent = parent)
        val child2 = createTab("https://www.mozilla.org/en-US/technology/", parent = parent)

        store.dispatch(TabListAction.AddTabAction(parent)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab2)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(child1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(child2)).joinBlocking()

        store.dispatch(TabListAction.SelectTabAction(child1.id)).joinBlocking()
        store.dispatch(TabListAction.RemoveTabAction(child1.id, selectParentIfExists = false)).joinBlocking()

        assertEquals(tab1.id, store.state.selectedTabId)
        assertEquals("https://www.firefox.com", store.state.selectedTab?.content?.url)
    }

    @Test
    fun `RemoveTabAction - Providing selectParentIfExists when removing tab without parent has no effect`() {
        val store = BrowserStore()

        val tab1 = createTab("https://www.firefox.com")
        val tab2 = createTab("https://getpocket.com")
        val tab3 = createTab("https://www.mozilla.org/en-US/internet-health/")

        store.dispatch(TabListAction.AddTabAction(tab1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab2)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab3)).joinBlocking()

        store.dispatch(TabListAction.SelectTabAction(tab3.id)).joinBlocking()
        store.dispatch(TabListAction.RemoveTabAction(tab3.id, selectParentIfExists = true)).joinBlocking()

        assertEquals(tab2.id, store.state.selectedTabId)
        assertEquals("https://getpocket.com", store.state.selectedTab?.content?.url)
    }

    @Test
    fun `RemoveTabAction - Children are updated when parent is removed`() {
        val store = BrowserStore()

        val tab0 = createTab("https://www.firefox.com")
        val tab1 = createTab("https://developer.mozilla.org/en-US/", parent = tab0)
        val tab2 = createTab("https://www.mozilla.org/en-US/internet-health/", parent = tab1)
        val tab3 = createTab("https://www.mozilla.org/en-US/technology/", parent = tab2)

        store.dispatch(TabListAction.AddTabAction(tab0)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab1)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab2)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(tab3)).joinBlocking()

        // tab0 <- tab1 <- tab2 <- tab3
        assertEquals(tab0.id, store.state.tabs[0].id)
        assertEquals(tab1.id, store.state.tabs[1].id)
        assertEquals(tab2.id, store.state.tabs[2].id)
        assertEquals(tab3.id, store.state.tabs[3].id)

        assertNull(store.state.tabs[0].parentId)
        assertEquals(tab0.id, store.state.tabs[1].parentId)
        assertEquals(tab1.id, store.state.tabs[2].parentId)
        assertEquals(tab2.id, store.state.tabs[3].parentId)

        store.dispatch(TabListAction.RemoveTabAction(tab2.id)).joinBlocking()

        // tab0 <- tab1 <- tab3
        assertEquals(tab0.id, store.state.tabs[0].id)
        assertEquals(tab1.id, store.state.tabs[1].id)
        assertEquals(tab3.id, store.state.tabs[2].id)

        assertNull(store.state.tabs[0].parentId)
        assertEquals(tab0.id, store.state.tabs[1].parentId)
        assertEquals(tab1.id, store.state.tabs[2].parentId)

        store.dispatch(TabListAction.RemoveTabAction(tab0.id)).joinBlocking()

        // tab1 <- tab3
        assertEquals(tab1.id, store.state.tabs[0].id)
        assertEquals(tab3.id, store.state.tabs[1].id)

        assertNull(store.state.tabs[0].parentId)
        assertEquals(tab1.id, store.state.tabs[1].parentId)
    }

    @Test
    fun `RestoreAction - Adds restored tabs and updates selected tab`() {
        val store = BrowserStore()

        assertEquals(0, store.state.tabs.size)

        store.dispatch(TabListAction.RestoreAction(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org", private = false),
                createTab(id = "b", url = "https://www.firefox.com", private = true),
                createTab(id = "c", url = "https://www.example.org", private = true),
                createTab(id = "d", url = "https://getpocket.com", private = false)
            ),
            selectedTabId = "d"
        )).joinBlocking()

        assertEquals(4, store.state.tabs.size)
        assertEquals("a", store.state.tabs[0].id)
        assertEquals("b", store.state.tabs[1].id)
        assertEquals("c", store.state.tabs[2].id)
        assertEquals("d", store.state.tabs[3].id)
        assertEquals("d", store.state.selectedTabId)
    }

    @Test
    fun `RestoreAction - Adds restored tabs to existing tabs without updating selection`() {
        val store = BrowserStore(BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org", private = false),
                createTab(id = "b", url = "https://www.firefox.com", private = true)
            ),
            selectedTabId = "a"
        ))

        assertEquals(2, store.state.tabs.size)

        store.dispatch(TabListAction.RestoreAction(
            tabs = listOf(
                createTab(id = "c", url = "https://www.example.org", private = true),
                createTab(id = "d", url = "https://getpocket.com", private = false)
            ),
            selectedTabId = "d"
        )).joinBlocking()

        assertEquals(4, store.state.tabs.size)
        assertEquals("c", store.state.tabs[0].id)
        assertEquals("d", store.state.tabs[1].id)
        assertEquals("a", store.state.tabs[2].id)
        assertEquals("b", store.state.tabs[3].id)
        assertEquals("a", store.state.selectedTabId)
    }

    @Test
    fun `RestoreAction - Adds restored tabs to existing tabs with updating selection`() {
        val store = BrowserStore(BrowserState(
            tabs = listOf(
                createTab(id = "a", url = "https://www.mozilla.org", private = false),
                createTab(id = "b", url = "https://www.firefox.com", private = true)
            )
        ))

        assertEquals(2, store.state.tabs.size)

        store.dispatch(TabListAction.RestoreAction(
            tabs = listOf(
                createTab(id = "c", url = "https://www.example.org", private = true),
                createTab(id = "d", url = "https://getpocket.com", private = false)
            ),
            selectedTabId = "d"
        )).joinBlocking()

        assertEquals(4, store.state.tabs.size)
        assertEquals("c", store.state.tabs[0].id)
        assertEquals("d", store.state.tabs[1].id)
        assertEquals("a", store.state.tabs[2].id)
        assertEquals("b", store.state.tabs[3].id)
        assertEquals("d", store.state.selectedTabId)
    }

    @Test
    fun `RestoreAction - Does not update selection if none was provided`() {
        val store = BrowserStore(
            BrowserState(
                tabs = listOf(
                    createTab(id = "a", url = "https://www.mozilla.org", private = false),
                    createTab(id = "b", url = "https://www.firefox.com", private = true)
                )
            )
        )

        assertEquals(2, store.state.tabs.size)

        store.dispatch(
            TabListAction.RestoreAction(
                tabs = listOf(
                    createTab(id = "c", url = "https://www.example.org", private = true),
                    createTab(id = "d", url = "https://getpocket.com", private = false)
                ),
                selectedTabId = null
            )
        ).joinBlocking()

        assertEquals(4, store.state.tabs.size)
        assertEquals("c", store.state.tabs[0].id)
        assertEquals("d", store.state.tabs[1].id)
        assertEquals("a", store.state.tabs[2].id)
        assertEquals("b", store.state.tabs[3].id)
        assertNull(store.state.selectedTabId)
    }

    @Test
    fun `RemoveAllTabsAction - Removes both private and non-private tabs (but not custom tabs)`() {
        val state = BrowserState(
                tabs = listOf(
                        createTab(id = "a", url = "https://www.mozilla.org", private = false),
                        createTab(id = "b", url = "https://www.firefox.com", private = true)
                ),
                customTabs = listOf(
                        createCustomTab(id = "a1", url = "https://www.firefox.com")
                ),
                selectedTabId = "a"
        )

        val store = BrowserStore(state)
        store.dispatch(TabListAction.RemoveAllTabsAction).joinBlocking()

        assertTrue(store.state.tabs.isEmpty())
        assertNull(store.state.selectedTabId)
        assertEquals(1, store.state.customTabs.size)
        assertEquals("a1", store.state.customTabs.last().id)
    }

    @Test
    fun `RemoveAllPrivateTabsAction - Removes only private tabs`() {
        val state = BrowserState(
                tabs = listOf(
                        createTab(id = "a", url = "https://www.mozilla.org", private = false),
                        createTab(id = "b", url = "https://www.firefox.com", private = true)
                ),
                customTabs = listOf(
                        createCustomTab(id = "a1", url = "https://www.firefox.com")
                ),
                selectedTabId = "a"
        )

        val store = BrowserStore(state)
        store.dispatch(TabListAction.RemoveAllPrivateTabsAction).joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals("a", store.state.tabs[0].id)
        assertEquals("a", store.state.selectedTabId)

        assertEquals(1, store.state.customTabs.size)
        assertEquals("a1", store.state.customTabs.last().id)
    }

    @Test
    fun `RemoveAllPrivateTabsAction - Updates selection if affected`() {
        val state = BrowserState(
                tabs = listOf(
                        createTab(id = "a", url = "https://www.mozilla.org", private = false),
                        createTab(id = "b", url = "https://www.firefox.com", private = true)
                ),
                customTabs = listOf(
                        createCustomTab(id = "a1", url = "https://www.firefox.com")
                ),
                selectedTabId = "b"
        )

        val store = BrowserStore(state)
        store.dispatch(TabListAction.RemoveAllPrivateTabsAction).joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals("a", store.state.tabs[0].id)
        assertEquals("a", store.state.selectedTabId)

        assertEquals(1, store.state.customTabs.size)
        assertEquals("a1", store.state.customTabs.last().id)
    }

    @Test
    fun `RemoveAllNormalTabsAction - Removes only normal (non-private) tabs`() {
        val state = BrowserState(
                tabs = listOf(
                        createTab(id = "a", url = "https://www.mozilla.org", private = false),
                        createTab(id = "b", url = "https://www.firefox.com", private = true)
                ),
                customTabs = listOf(
                        createCustomTab(id = "a1", url = "https://www.firefox.com")
                ),
                selectedTabId = "b"
        )

        val store = BrowserStore(state)
        store.dispatch(TabListAction.RemoveAllNormalTabsAction).joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals("b", store.state.tabs[0].id)
        assertEquals("b", store.state.selectedTabId)

        assertEquals(1, store.state.customTabs.size)
        assertEquals("a1", store.state.customTabs.last().id)
    }

    @Test
    fun `RemoveAllNormalTabsAction - Updates selection if affected`() {
        val state = BrowserState(
                tabs = listOf(
                        createTab(id = "a", url = "https://www.mozilla.org", private = false),
                        createTab(id = "b", url = "https://www.firefox.com", private = true)
                ),
                customTabs = listOf(
                        createCustomTab(id = "a1", url = "https://www.firefox.com")
                ),
                selectedTabId = "a"
        )

        val store = BrowserStore(state)
        store.dispatch(TabListAction.RemoveAllNormalTabsAction).joinBlocking()

        assertEquals(1, store.state.tabs.size)
        assertEquals("b", store.state.tabs[0].id)
        // After removing the last normal tab NO private tab should get selected
        assertNull(store.state.selectedTabId)

        assertEquals(1, store.state.customTabs.size)
        assertEquals("a1", store.state.customTabs.last().id)
    }
}
