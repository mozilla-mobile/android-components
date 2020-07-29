/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.CrashAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore

/**
 * [Middleware] responsible for recovering crashed [EngineSession] instances.
 */
class CrashMiddleware : Middleware<BrowserState, BrowserAction> {
    override fun invoke(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        if (action is CrashAction.RestoreCrashedSessionAction) {
            restore(store, action)
        }

        next(action)
    }

    private fun restore(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        action: CrashAction.RestoreCrashedSessionAction
    ) {
        if (lookupEngineSession(store, action.tabId) == null) {
            // Currently we are forcing the creation of an engine session here. This is mimicing
            // the previous behavior. But it is questionable if this is the right approach:
            // - How did this tab crash if it does not have an engine session?
            // - Instead of creating the engine session, could we turn it into a suspended
            //   session with the "crash state" as the last state?
            store.dispatch(EngineAction.CreateEngineSessionAction(action.tabId))
        }

        lookupEngineSession(store, action.tabId)
            ?.recoverFromCrash()
    }

    private fun lookupEngineSession(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        tabId: String
    ): EngineSession? {
        return store.state.findTabOrCustomTab(tabId)?.engineState?.engineSession
    }
}
