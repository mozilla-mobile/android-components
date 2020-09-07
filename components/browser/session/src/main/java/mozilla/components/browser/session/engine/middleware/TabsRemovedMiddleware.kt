/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.util.AbstractTabRemovedMiddleware
import mozilla.components.concept.engine.EngineSession
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext

/**
 * [Middleware] responsible for closing and unlinking [EngineSession] instances whenever tabs get
 * removed.
 */
internal class TabsRemovedMiddleware(
    private val scope: CoroutineScope
) : AbstractTabRemovedMiddleware() {
    override fun onTabsRemoved(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        tabs: List<SessionState>
    ) {
        tabs.forEach { tab ->
            if (tab.engineState.engineSession != null) {
                context.dispatch(
                    EngineAction.UnlinkEngineSessionAction(tab.id)
                )

                scope.launch {
                    tab.engineState.engineSession?.close()
                }
            }
        }
    }
}
