/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.state.action.CrashAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags

/**
 * Contains use cases related to the session feature.
 */
class SessionUseCases(
    store: BrowserStore,
    onNoTab: (String) -> TabSessionState = { url ->
        createTab(url).apply { store.dispatch(TabListAction.AddTabAction(this)) }
    }
) {

    /**
     * Contract for use cases that load a provided URL.
     */
    interface LoadUrlUseCase {
        /**
         * Loads the provided URL using the currently selected session.
         */
        fun invoke(
            url: String,
            flags: LoadUrlFlags = LoadUrlFlags.none(),
            additionalHeaders: Map<String, String>? = null
        )
    }

    class DefaultLoadUrlUseCase internal constructor(
        private val store: BrowserStore,
        private val onNoTab: (String) -> TabSessionState
    ) : LoadUrlUseCase {

        /**
         * Loads the provided URL using the currently selected session. If
         * there's no selected session a new session will be created using
         * [onNoTab].
         *
         * @param url The URL to be loaded using the selected session.
         * @param flags The [LoadUrlFlags] to use when loading the provided url.
         * @param additionalHeaders the extra headers to use when loading the provided url.
         */
        override operator fun invoke(
            url: String,
            flags: LoadUrlFlags,
            additionalHeaders: Map<String, String>?
        ) {
            this.invoke(url, store.state.selectedTabId, flags, additionalHeaders)
        }

        /**
         * Loads the provided URL using the specified session. If no session
         * is provided the currently selected session will be used. If there's
         * no selected session a new session will be created using [onNoTab].
         *
         * @param url The URL to be loaded using the provided session.
         * @param sessionId the ID of the session for which the URL should be loaded.
         * @param flags The [LoadUrlFlags] to use when loading the provided url.
         * @param additionalHeaders the extra headers to use when loading the provided url.
         */
        operator fun invoke(
            url: String,
            sessionId: String? = null,
            flags: LoadUrlFlags = LoadUrlFlags.none(),
            additionalHeaders: Map<String, String>? = null
        ) {
            val loadSessionId = sessionId
                ?: store.state.selectedTabId
                ?: onNoTab.invoke(url).id

            store.dispatch(EngineAction.LoadUrlAction(
                loadSessionId,
                url,
                flags,
                additionalHeaders
            ))
        }
    }

    class LoadDataUseCase internal constructor(
        private val store: BrowserStore,
        private val onNoTab: (String) -> TabSessionState
    ) {
        /**
         * Loads the provided data based on the mime type using the provided session (or the
         * currently selected session if none is provided).
         */
        operator fun invoke(
            data: String,
            mimeType: String,
            encoding: String = "UTF-8",
            tabId: String? = store.state.selectedTabId
        ) {
            val loadTabId = tabId ?: onNoTab.invoke("about:blank").id

            store.dispatch(EngineAction.LoadDataAction(
                loadTabId,
                data,
                mimeType,
                encoding
            ))
        }
    }

    class ReloadUrlUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Reloads the current URL of the provided session (or the currently
         * selected session if none is provided).
         *
         * @param tabId the ID of the tab for which the reload should be triggered.
         * @param flags the [LoadUrlFlags] to use when reloading the given session.
         */
        operator fun invoke(
            tabId: String? = store.state.selectedTabId,
            flags: LoadUrlFlags = LoadUrlFlags.none()
        ) {
            if (tabId == null) {
                return
            }

            store.dispatch(EngineAction.ReloadAction(
                tabId,
                flags
            ))
        }
    }

    class StopLoadingUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Stops the current URL of the provided session from loading.
         *
         * @param tabId the ID of the tab for which loading should be stopped.
         */
        operator fun invoke(
            tabId: String? = store.state.selectedTabId
        ) {
            if (tabId == null) {
                return
            }

            store.state.findTabOrCustomTab(tabId)
                ?.engineState
                ?.engineSession
                ?.stopLoading()
        }
    }

    class GoBackUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Navigates back in the history of the currently selected tab
         */
        operator fun invoke(
            tabId: String? = store.state.selectedTabId
        ) {
            if (tabId == null) {
                return
            }

            store.dispatch(EngineAction.GoBackAction(
                tabId
            ))
        }
    }

    class GoForwardUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Navigates forward in the history of the currently selected session
         */
        operator fun invoke(
            tabId: String? = store.state.selectedTabId
        ) {
            if (tabId == null) {
                return
            }

            store.dispatch(EngineAction.GoForwardAction(
                tabId
            ))
        }
    }

    /**
     * Use case to jump to an arbitrary history index in a session's backstack.
     */
    class GoToHistoryIndexUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Navigates to a specific index in the [HistoryState] of the given session.
         * Invalid index values will be ignored.
         *
         * @param index the index in the session's [HistoryState] to navigate to.
         * @param session the session whose [HistoryState] is being accessed, defaulting
         * to the selected session.
         */
        operator fun invoke(
            index: Int,
            tabId: String? = store.state.selectedTabId
        ) {
            if (tabId == null) {
                return
            }

            store.dispatch(EngineAction.GoToHistoryIndexAction(
                tabId,
                index
            ))
        }
    }

    class RequestDesktopSiteUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Requests the desktop version of the current session and reloads the page.
         */
        operator fun invoke(
            enable: Boolean,
            tabId: String? = store.state.selectedTabId
        ) {
            if (tabId == null) {
                return
            }

            store.dispatch(EngineAction.ToggleDesktopModeAction(
                tabId,
                enable
            ))
        }
    }

    class ExitFullScreenUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Exits fullscreen mode of the current session.
         */
        operator fun invoke(
            tabId: String? = store.state.selectedTabId
        ) {
            if (tabId == null) {
                return
            }

            store.dispatch(EngineAction.ExitFullScreenModeAction(
                tabId
            ))
        }
    }

    /**
     * Tries to recover from a crash by restoring the last know state.
     */
    class CrashRecoveryUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Tries to recover the state of all crashed sessions.
         */
        fun invoke() {
            val tabIds = store.state.let {
                it.tabs + it.customTabs
            }.filter {
                it.engineState.crashed
            }.map {
                it.id
            }

            return invoke(tabIds)
        }

        /**
         * Tries to recover the state of all sessions.
         */
        fun invoke(tabIds: List<String>) {
            tabIds.forEach { tabId ->
                store.dispatch(
                    CrashAction.RestoreCrashedSessionAction(tabId)
                )
            }
        }
    }

    /**
     * UseCase for purging the (back and forward) history of all tabs and custom tabs.
     */
    class PurgeHistoryUseCase internal constructor(
        private val store: BrowserStore
    ) {
        /**
         * Purges the (back and forward) history of all tabs and custom tabs.
         */
        operator fun invoke() {
            store.dispatch(EngineAction.PurgeHistoryAction)
        }
    }

    val loadUrl: DefaultLoadUrlUseCase by lazy { DefaultLoadUrlUseCase(store, onNoTab) }
    val loadData: LoadDataUseCase by lazy { LoadDataUseCase(store, onNoTab) }
    val reload: ReloadUrlUseCase by lazy { ReloadUrlUseCase(store) }
    val stopLoading: StopLoadingUseCase by lazy { StopLoadingUseCase(store) }
    val goBack: GoBackUseCase by lazy { GoBackUseCase(store) }
    val goForward: GoForwardUseCase by lazy { GoForwardUseCase(store) }
    val goToHistoryIndex: GoToHistoryIndexUseCase by lazy { GoToHistoryIndexUseCase(store) }
    val requestDesktopSite: RequestDesktopSiteUseCase by lazy { RequestDesktopSiteUseCase(store) }
    val exitFullscreen: ExitFullScreenUseCase by lazy { ExitFullScreenUseCase(store) }
    val crashRecovery: CrashRecoveryUseCase by lazy { CrashRecoveryUseCase(store) }
    val purgeHistory: PurgeHistoryUseCase by lazy { PurgeHistoryUseCase(store) }
}
