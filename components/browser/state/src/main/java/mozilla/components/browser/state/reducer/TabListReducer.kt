/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import kotlin.math.max

internal object TabListReducer {
    /**
     * [TabListAction] Reducer function for modifying the list of [TabSessionState] objects in [BrowserState.tabs].
     */
    fun reduce(state: BrowserState, action: TabListAction): BrowserState {
        return when (action) {
            is TabListAction.AddTabAction -> {
                // Verify that tab doesn't already exist
                requireUniqueTab(state, action.tab)

                val isPrivate = action.tab.content.private
                val tabList = if (isPrivate) state.privateTabs else state.normalTabs

                val updatedTabList = if (action.tab.parentId != null) {
                    val parentIndex = tabList.indexOfFirst { it.id == action.tab.parentId }
                    if (parentIndex == -1) {
                        throw IllegalArgumentException("The parent does not exist")
                    }

                    // Add the child tab next to its parent
                    val childIndex = parentIndex + 1
                    tabList.subList(0, childIndex) + action.tab + tabList.subList(childIndex, tabList.size)
                } else {
                    tabList + action.tab
                }

                state.copy(
                    normalTabs = if (isPrivate) state.normalTabs else updatedTabList,
                    privateTabs = if (isPrivate) updatedTabList else state.privateTabs,
                    selectedTabId = if (action.select || state.selectedTabId == null) {
                        action.tab.id
                    } else {
                        state.selectedTabId
                    }
                )
            }

            is TabListAction.AddMultipleTabsAction -> {
                action.tabs.forEach { requireUniqueTab(state, it) }

                action.tabs.find { tab -> tab.parentId != null }?.let {
                    throw IllegalArgumentException("Adding multiple tabs with a parent id is not supported")
                }

                state.copy(
                    normalTabs = state.normalTabs + action.tabs.filterNot { it.content.private },
                    privateTabs = state.privateTabs + action.tabs.filter { it.content.private },
                    selectedTabId = state.selectedTabId
                        ?: action.tabs.find { tab -> !tab.content.private }?.id
                )
            }

            is TabListAction.SelectTabAction -> {
                state.copy(selectedTabId = action.tabId)
            }

            is TabListAction.RemoveTabAction -> {
                val tabToRemove = state.findTab(action.tabId)

                if (tabToRemove == null) {
                    state
                } else {
                    val isPrivate = tabToRemove.content.private
                    val tabList = if (isPrivate) state.privateTabs else state.normalTabs

                    // Remove tab and update child tabs in case their parent was removed
                    val updatedTabList = (tabList - tabToRemove).map {
                        if (it.parentId == tabToRemove.id) it.copy(parentId = tabToRemove.parentId) else it
                    }

                    val updatedSelection = if (action.selectParentIfExists && tabToRemove.parentId != null) {
                        // The parent tab should be selected if one exists
                        tabToRemove.parentId
                    } else if (state.selectedTabId == tabToRemove.id) {
                        // The selected tab was removed and we need to find a new one
                        findNewSelectedTabId(
                            state,
                            updatedTabList,
                            isPrivate = tabToRemove.content.private,
                            previousIndex = tabList.indexOf(tabToRemove)
                        )
                    } else {
                        // The selected tab is not affected and can stay the same
                        state.selectedTabId
                    }

                    state.copy(
                        normalTabs = if (isPrivate) state.normalTabs else updatedTabList,
                        privateTabs = if (isPrivate) updatedTabList else state.privateTabs,
                        selectedTabId = updatedSelection
                    )
                }
            }

            is TabListAction.RestoreAction -> {
                // Verify that none of the tabs to restore already exist
                action.tabs.forEach { requireUniqueTab(state, it) }

                // We are adding the restored tabs first and then the already existing tabs. Since restore can or should
                // happen asynchronously we may already have a tab at this point (e.g. from an `Intent`) and so we
                // pretend we restored the list of tabs before any tab was added.
                state.copy(
                    normalTabs = action.tabs.filterNot { it.content.private } + state.normalTabs,
                    privateTabs = action.tabs.filter { it.content.private } + state.privateTabs,
                    selectedTabId = if (action.selectedTabId != null && state.selectedTabId == null) {
                        // We only want to update the selected tab if none has been already selected. Otherwise we may
                        // switch to a restored tab even though the user is already looking at an existing tab (e.g.
                        // a tab that came from an intent).
                        action.selectedTabId
                    } else {
                        state.selectedTabId
                    }
                )
            }

            is TabListAction.RemoveAllTabsAction -> {
                state.copy(
                    normalTabs = emptyList(),
                    privateTabs = emptyList(),
                    selectedTabId = null
                )
            }

            is TabListAction.RemoveAllPrivateTabsAction -> {
                val selectionAffected = state.selectedTab?.content?.private == true
                state.copy(
                    privateTabs = emptyList(),
                    selectedTabId = if (selectionAffected && state.normalTabs.isNotEmpty()) {
                        // If the selection is affected, select the last normal tab, if available.
                        state.normalTabs.last().id
                    } else {
                        state.selectedTabId
                    }
                )
            }

            is TabListAction.RemoveAllNormalTabsAction -> {
                val selectionAffected = state.selectedTab?.content?.private == false
                state.copy(
                    normalTabs = emptyList(),
                    selectedTabId = if (selectionAffected) {
                        // If the selection is affected, we'll set it to null as there's no
                        // normal tab left and NO private tab should get selected instead.
                        null
                    } else {
                        state.selectedTabId
                    }
                )
            }
        }
    }
}

/**
 * Find a new selected tab and return its id after the tab at [previousIndex] was removed.
 */
private fun findNewSelectedTabId(
    state: BrowserState,
    newTabs: List<TabSessionState>,
    isPrivate: Boolean,
    previousIndex: Int
): String? {
    val allTabs: List<TabSessionState>
    val previousIndexAll: Int
    if (isPrivate) {
        // Can switch from private tabs to normal tabs
        allTabs = state.normalTabs + newTabs
        previousIndexAll = state.normalTabs.size + previousIndex
    } else {
        // Cannot switch from normal tabs to private tabs
        allTabs = newTabs
        previousIndexAll = previousIndex
    }

    if (allTabs.isEmpty()) {
        // There's no tab left to select.
        return null
    }

    // If the previous index is still a valid index and if this is a private/normal tab we are looking for then
    // let's use the tab at the same index.
    if (previousIndex in newTabs.indices) {
        return newTabs[previousIndex].id
    }

    // Find a tab that matches the predicate and is nearby the tab that was selected previously
    val nearbyTab = findNearbyTab(allTabs, previousIndexAll) { tab ->
        tab.content.private == isPrivate
    }

    return when {
        // We found a nearby tab, let's select it.
        nearbyTab != null -> nearbyTab.id

        // If there's no private tab to select anymore then just select the last regular tab
        isPrivate -> state.normalTabs.lastOrNull()?.id

        // Removing the last normal tab should NOT cause a private tab to be selected
        else -> null
    }
}

/**
 * Find a tab that is near the provided [index] and matches the [predicate].
 */
private fun findNearbyTab(
    tabs: List<TabSessionState>,
    index: Int,
    predicate: (TabSessionState) -> Boolean
): TabSessionState? {
    val maxSteps = max(tabs.lastIndex - index, index)
    if (maxSteps < 0) {
        return null
    }

    // Try tabs oscillating near the index.
    for (steps in 1..maxSteps) {
        listOf(index - steps, index + steps).forEach { current ->
            if (current in 0..tabs.lastIndex &&
                predicate(tabs[current])) {
                return tabs[current]
            }
        }
    }

    return null
}

/**
 * Checks that the provided tab doesn't already exist and throws an
 * [IllegalArgumentException] otherwise.
 *
 * @param state the current [BrowserState] (including all existing tabs).
 * @param tab the [TabSessionState] to check.
 */
private fun requireUniqueTab(state: BrowserState, tab: TabSessionState) {
    require(state.tabs.none { it.id == tab.id }) {
        "Tab with same ID already exists"
    }
}
