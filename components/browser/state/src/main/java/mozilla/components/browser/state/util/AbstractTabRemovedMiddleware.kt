/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.util

import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.CustomTabListAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext

/**
 * Base class for creating a [Middleware] that reacts to tabs getting removed. Before tabs get removed
 * from the [BrowserState], [onTabsRemoved] will get invoked with the list of tabs about to be
 * removed.
 */
abstract class AbstractTabRemovedMiddleware : Middleware<BrowserState, BrowserAction> {
    override fun invoke(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        when (action) {
            is TabListAction.RemoveAllNormalTabsAction -> onTabsRemoved(context, context.state.normalTabs)
            is TabListAction.RemoveAllPrivateTabsAction -> onTabsRemoved(context, context.state.privateTabs)
            is TabListAction.RemoveAllTabsAction -> onTabsRemoved(context, context.state.tabs)
            is TabListAction.RemoveTabAction -> context.state.findTab(action.tabId)?.let {
                onTabsRemoved(context, listOf(it))
            }
            is CustomTabListAction.RemoveAllCustomTabsAction -> onTabsRemoved(context, context.state.customTabs)
            is CustomTabListAction.RemoveCustomTabAction -> context.state.findCustomTab(action.tabId)?.let {
                onTabsRemoved(context, listOf(it))
            }
        }

        next(action)
    }

    protected abstract fun onTabsRemoved(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        tabs: List<SessionState>
    )
}
