/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
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
                state.copy(
                    tabs = state.tabs + action.tab,
                    selectedTabId = if (action.select || state.selectedTabId == null) {
                        action.tab.id
                    } else {
                        state.selectedTabId
                    }
                )
            }

            is TabListAction.SelectTabAction -> {
                state.copy(selectedTabId = action.tabId)
            }

            is TabListAction.RemoveTabAction -> {
                val tab = state.findTab(action.tabId)

                if (tab == null) {
                    state
                } else {
                    val updatedTabList = state.tabs - tab
                    val updatedSelection = if (state.selectedTabId == tab.id) {
                        val previousIndex = state.tabs.indexOf(tab)
                        findNewSelectedTabId(updatedTabList, tab.content.private, previousIndex)
                    } else {
                        state.selectedTabId
                    }

                    state.copy(
                        tabs = updatedTabList,
                        selectedTabId = updatedSelection
                    )
                }
            }
        }
    }
}

/**
 * Find a new selected tab and return its id after the tab at [previousIndex] was removed.
 */
private fun findNewSelectedTabId(
    tabs: List<TabSessionState>,
    isPrivate: Boolean,
    previousIndex: Int
): String? {
    if (tabs.isEmpty()) {
        // There's no tab left to select.
        return null
    }

    val predicate: (TabSessionState) -> Boolean = { tab -> tab.content.private == isPrivate }

    // If the previous index is still a valid index and if this is a private/normal tab we are looking for then
    // let's use the tab at the same index.
    if (previousIndex <= tabs.lastIndex && predicate(tabs[previousIndex])) {
        return tabs[previousIndex].id
    }

    // Find a tab that matches the predicate and is nearby the tab that was selected previously
    val nearbyTab = findNearbyTab(tabs, previousIndex, predicate)

    return when {
        // We found a nearby tab, let's select it.
        nearbyTab != null -> nearbyTab.id

        // If there's no private tab to select anymore then just select the last regular tab
        isPrivate -> tabs.last().id

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
