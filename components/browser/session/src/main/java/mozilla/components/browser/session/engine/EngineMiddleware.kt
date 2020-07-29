/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.engine.middleware.CrashMiddleware
import mozilla.components.browser.session.engine.middleware.CreateEngineSessionMiddleware
import mozilla.components.browser.session.engine.middleware.EngineDelegateMiddleware
import mozilla.components.browser.session.engine.middleware.LinkingMiddleware
import mozilla.components.browser.session.engine.middleware.SuspendMiddleware
import mozilla.components.browser.session.engine.middleware.TabsRemovedMiddleware
import mozilla.components.browser.session.engine.middleware.TrimMemoryMiddleware
import mozilla.components.browser.session.engine.middleware.WebExtensionsMiddleware
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore
import mozilla.components.support.base.log.logger.Logger

/**
 * Helper for creating a list of [Middleware] instances for supporting all [EngineAction]s.
 */
object EngineMiddleware {
    /**
     * Creates a list of [Middleware] to be installed on a [BrowserStore] in order to support all
     * [EngineAction]s.
     */
    fun create(
        engine: Engine,
        sessionLookup: (String) -> Session?,
        scope: CoroutineScope = MainScope()
    ): List<Middleware<BrowserState, BrowserAction>> {
        return listOf(
            EngineDelegateMiddleware(
                engine,
                sessionLookup,
                scope
            ),
            CreateEngineSessionMiddleware(
                engine,
                sessionLookup,
                scope
            ),
            LinkingMiddleware(),
            TabsRemovedMiddleware(scope),
            SuspendMiddleware(scope),
            WebExtensionsMiddleware(),
            TrimMemoryMiddleware(),
            CrashMiddleware()
        )
    }
}

internal fun getOrCreateEngineSession(
    engine: Engine,
    tabId: String,
    logger: Logger,
    sessionLookup: (String) -> Session?,
    store: MiddlewareStore<BrowserState, BrowserAction>
): EngineSession? {
    val tab = store.state.findTabOrCustomTab(tabId)
    if (tab == null) {
        logger.warn("Requested engine session for tab. But tab does not exist. ($tabId)")
        return null
    }

    return if (tab.engineState.engineSession != null) {
        tab.engineState.engineSession
    } else {
        createEngineSession(
            engine,
            logger,
            sessionLookup,
            store,
            tab
        )
    }
}

private fun createEngineSession(
    engine: Engine,
    logger: Logger,
    sessionLookup: (String) -> Session?,
    store: MiddlewareStore<BrowserState, BrowserAction>,
    tab: SessionState
): EngineSession? {
    val session = sessionLookup(tab.id)
    if (session == null) {
        logger.error("Requested creation of EngineSession without matching Session (${tab.id})")
        return null
    }

    val engineSession = engine.createSession(tab.content.private, tab.contextId)

    val observer = EngineObserver(session, store)
    engineSession.register(observer)

    logger.debug("Created engine session for tab ${tab.id}")

    val engineSessionState = tab.engineState.engineSessionState
    val skipLoading = if (engineSessionState != null) {
        engineSession.restoreState(engineSessionState)
        true
    } else {
        false
    }

    store.dispatch(
        EngineAction.LinkEngineSessionAction(tab.id, engineSession, observer, skipLoading)
    )

    return engineSession
}