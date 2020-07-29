/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore
import mozilla.components.support.ktx.kotlin.isExtensionUrl

class LinkingMiddleware : Middleware<BrowserState, BrowserAction> {
    override fun invoke(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        next(action)

        if (action is EngineAction.LinkEngineSessionAction) {
            link(store, action)
        }
    }

    private fun link(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        action: EngineAction.LinkEngineSessionAction
    ) {
        val tab = store.state.findTabOrCustomTab(action.sessionId) ?: return

        if (action.skipLoading) {
            return
        }

        if (tab.content.url.isExtensionUrl()) {
            // The parent tab/session is used as a referrer which is not accurate
            // for extension pages. The extension page is not loaded by the parent
            // tab, but opened by an extension e.g. via browser.tabs.update.
            action.engineSession.loadUrl(tab.content.url)
        } else {
            val parentEngineSession = if (tab is TabSessionState) {
                tab.parentId?.let { store.state.findTabOrCustomTab(it)?.engineState?.engineSession }
            } else {
                null
            }

            action.engineSession.loadUrl(tab.content.url, parent = parentEngineSession)
        }
    }
}
