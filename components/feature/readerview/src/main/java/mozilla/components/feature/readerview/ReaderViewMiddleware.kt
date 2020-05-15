/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.readerview

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.ReaderAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.feature.readerview.ReaderViewFeature.Companion.READER_VIEW_EXTENSION_ID
import mozilla.components.feature.readerview.ReaderViewFeature.Companion.READER_VIEW_EXTENSION_URL
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore
import mozilla.components.support.webextensions.WebExtensionController

/**
 * [Middleware] implementation for translating [BrowserAction]s to
 * [ReaderAction]s (e.g. if the URL is updated a new "readerable"
 * check should be executed.)
 */
class ReaderViewMiddleware : Middleware<BrowserState, BrowserAction> {

    @VisibleForTesting
    internal var extensionController = WebExtensionController(READER_VIEW_EXTENSION_ID, READER_VIEW_EXTENSION_URL)

    override fun invoke(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        if (preProcess(store, action)) {
            next(action)
            postProcess(store, action)
        }
    }

    /**
     * Processes the action before it is dispatched to the store.
     *
     * @param store a reference to the [MiddlewareStore].
     * @param action the action to process.
     * @return true if the original action should be processed, otherwise false.
     */
    private fun preProcess(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        action: BrowserAction
    ): Boolean {
        return when (action) {
            // We want to bind the feature instance to the lifecycle of the browser
            // fragment. So it won't necessarily be active when a tab is removed
            // (e.g. via a tabs tray fragment). In order to disconnect the port as
            // early as possible it's best to do it here directly.
            is EngineAction.UnlinkEngineSessionAction -> {
                store.state.findTab(action.sessionId)?.engineState?.engineSession?.let {
                    extensionController.disconnectPort(it, READER_VIEW_EXTENSION_ID)
                }
                true
            }
            is ContentAction.UpdateUrlAction -> {
                // Activate reader view when navigating to a reader page and deactivate it
                // when navigating away. In addition, we want to mask moz-extension://
                // URLs in the toolbar. So, if we detect the URL is coming from our
                // extension we show the original URL instead. This is needed until
                // we have a solution for:
                // https://bugzilla.mozilla.org/show_bug.cgi?id=1550144
                // https://bugzilla.mozilla.org/show_bug.cgi?id=1322304
                // https://github.com/mozilla-mobile/android-components/issues/2879
                val tab = store.state.findTab(action.sessionId)
                if (isReaderUrl(tab, action.url)) {
                    val urlReplaced = tab?.readerState?.activeUrl?.let { activeUrl ->
                        store.dispatch(ContentAction.UpdateUrlAction(action.sessionId, activeUrl))
                        true
                    } ?: false
                    store.dispatch(ReaderAction.UpdateReaderActiveAction(action.sessionId, true))
                    !urlReplaced
                } else {
                    if (action.url != tab?.readerState?.activeUrl) {
                        store.dispatch(ReaderAction.UpdateReaderActiveAction(action.sessionId, false))
                        store.dispatch(ReaderAction.UpdateReaderableAction(action.sessionId, false))
                        store.dispatch(ReaderAction.UpdateReaderableCheckRequiredAction(action.sessionId, true))
                        store.dispatch(ReaderAction.ClearReaderActiveUrlAction(action.sessionId))
                    }
                    true
                }
            }
            else -> true
        }
    }

    private fun postProcess(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        action: BrowserAction
    ) {
        when (action) {
            is TabListAction.SelectTabAction -> {
                store.dispatch(ReaderAction.UpdateReaderableAction(action.tabId, false))
                store.dispatch(ReaderAction.UpdateReaderableCheckRequiredAction(action.tabId, true))
            }
            is EngineAction.LinkEngineSessionAction -> {
                store.dispatch(ReaderAction.UpdateReaderConnectRequiredAction(action.sessionId, true))
            }
            is ReaderAction.UpdateReaderActiveUrlAction -> {
                // When a tab is restored, the reader page will connect, but we won't get a
                // UpdateUrlAction. We still want to mask the moz-extension:// URL though
                // so we update the URL here. See comment on handling UpdateUrlAction.
                val tab = store.state.findTab(action.tabId)
                val url = tab?.content?.url
                if (url != null && isReaderUrl(tab, url)) {
                    store.dispatch(ContentAction.UpdateUrlAction(tab.id, url))
                }
            }
        }
    }

    private fun isReaderUrl(tab: TabSessionState?, url: String): Boolean {
        val readerViewBaseUrl = tab?.readerState?.baseUrl
        return readerViewBaseUrl != null && url.startsWith(readerViewBaseUrl)
    }
}
