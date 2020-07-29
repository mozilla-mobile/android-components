/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine.middleware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.engine.getOrCreateEngineSession
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore
import mozilla.components.support.base.log.logger.Logger

/**
 * [Middleware] responsible for delegating calls to the appropriate [EngineSession] instance for
 * actions like [EngineAction.LoadUrlAction].
 */
class EngineDelegateMiddleware(
    private val engine: Engine,
    private val sessionLookup: (String) -> Session?,
    private val scope: CoroutineScope
) : Middleware<BrowserState, BrowserAction> {
    private val logger = Logger("EngineSessionMiddleware")

    override fun invoke(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        when (action) {
            is EngineAction.LoadUrlAction -> scope.launch {
                val engineSession = getOrCreateEngineSession(
                    engine,
                    action.sessionId,
                    logger,
                    sessionLookup,
                    store
                )

                val parentEngineSession = store.state.findTabOrCustomTab(action.sessionId)?.let {
                    store.state.findTabOrCustomTab(it.id)?.engineState?.engineSession
                }

                // TODO: If we created a new session then we already loaded the URL in the linking step.
                // TODO: So we do not need to load it here again.

                engineSession?.loadUrl(
                    url = action.url,
                    parent = parentEngineSession,
                    flags = action.flags,
                    additionalHeaders = action.additionalHeaders
                )
            }

            is EngineAction.LoadDataAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.loadData(action.data, action.mimeType, action.encoding)
            }

            is EngineAction.ReloadAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.reload(action.flags)
            }

            is EngineAction.StopLoadingAction -> scope.launch {
                // TODO: Do we need to create an engine session for this? Could we stop in the use case if there is
                // an engine session. If there's none then do nothing?
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.stopLoading()
            }

            is EngineAction.GoBackAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.goBack()
            }

            is EngineAction.GoForwardAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.goForward()
            }

            is EngineAction.GoToHistoryIndexAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.goToHistoryIndex(action.index)
            }

            is EngineAction.ToggleDesktopModeAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.toggleDesktopMode(action.enable, reload = true)
            }

            is EngineAction.ExitFullscreenModeAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.exitFullScreenMode()
            }

            is EngineAction.ClearDataAction -> scope.launch {
                val engineSession =
                    getOrCreateEngineSession(
                        engine,
                        action.sessionId,
                        logger,
                        sessionLookup,
                        store
                    )
                engineSession?.clearData(action.data)
            }

            else -> next(action)
        }
    }
}