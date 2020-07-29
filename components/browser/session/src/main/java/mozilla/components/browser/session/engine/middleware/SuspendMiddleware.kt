/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.EngineState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore

/**
 * [Middleware] implementation responsible for suspending an [EngineSession].
 *
 * Suspending an [EngineSession] means that we will take the last [EngineSessionState], attach that
 * to [EngineState] and then clear the [EngineSession] reference and close it. The next time we
 * need an [EngineSession] for this tab we will create a new instance and restore the attached
 * [EngineSessionState].
 */
internal class SuspendMiddleware(
    private val scope: CoroutineScope
) : Middleware<BrowserState, BrowserAction> {
    override fun invoke(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        if (action is EngineAction.SuspendEngineSessionAction) {
            suspend(store, next, action)
        } else {
            next(action)
        }
    }

    private fun suspend(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: EngineAction.SuspendEngineSessionAction
    ) {
        val tab = store.state.findTab(action.sessionId)
        val state = tab?.engineState?.engineSession?.saveState()

        if (tab == null || state == null) {
            // If we can't find this tab or if there's no state for this tab then there's nothing
            // to do here.
            return
        }

        // First we unlink (which clearsEngineSession and state)
        store.dispatch(EngineAction.UnlinkEngineSessionAction(
            tab.id
        ))

        // Then we attach the saved state to it.
        store.dispatch(EngineAction.UpdateEngineSessionStateAction(
            tab.id,
            state
        ))

        // Now we can close the unlinked EngineSession (on the main thread).
        scope.launch {
            tab.engineState.engineSession?.close()
        }
    }
}
