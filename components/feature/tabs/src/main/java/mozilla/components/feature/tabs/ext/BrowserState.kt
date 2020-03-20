/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.tabs.ext

import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.tabstray.Tabs

internal fun BrowserState.toTabs(
    tabsFilter: (TabSessionState) -> Boolean = { true }
) = Tabs(
    list = tabs
        .filter(tabsFilter)
        .map { it.toTab() },
    selectedIndex = tabs.indexOfFirst { it.id == selectedTabId }
)
