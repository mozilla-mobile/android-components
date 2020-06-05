/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.webextensions

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.Action
import mozilla.components.concept.engine.webextension.ActionHandler
import mozilla.components.concept.engine.webextension.TabHandler
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.webextension.WebExtensionDelegate
import mozilla.components.concept.engine.webextension.WebExtensionRuntime
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlinx.coroutines.flow.filterChanged
import mozilla.components.support.webextensions.facts.emitWebExtensionsInitializedFact
import java.util.concurrent.ConcurrentHashMap

/**
 * Function to relay the permission request to the app / user.
 */
typealias onUpdatePermissionRequest = (
    current: WebExtension,
    updated: WebExtension,
    newPermissions: List<String>,
    onPermissionsGranted: ((Boolean) -> Unit)
) -> Unit

/**
 * Provides functionality to make sure web extension related events in the
 * [WebExtensionRuntime] are reflected in the browser state by dispatching the
 * corresponding actions to the [BrowserStore].
 *
 * Note that this class can be removed once the browser-state migration
 * is completed and the [WebExtensionRuntime] (engine) has direct access to the
 * [BrowserStore]: https://github.com/orgs/mozilla-mobile/projects/31
 */
@Suppress("TooManyFunctions")
object WebExtensionSupport {
    private val logger = Logger("mozac-webextensions")
    private var onUpdatePermissionRequest: ((
        current: WebExtension,
        updated: WebExtension,
        newPermissions: List<String>,
        onPermissionsGranted: (Boolean) -> Unit
    ) -> Unit)? = null
    private var onExtensionsLoaded: ((List<WebExtension>) -> Unit)? = null
    private var onCloseTabOverride: ((WebExtension?, String) -> Unit)? = null
    private var onSelectTabOverride: ((WebExtension?, String) -> Unit)? = null

    val installedExtensions = ConcurrentHashMap<String, WebExtension>()

    /**
     * A [Deferred] completed during [initialize] once the state of all
     * installed extensions is known.
     */
    private val initializationResult = CompletableDeferred<Unit>()

    /**
     * [ActionHandler] for session-specific overrides. Forwards actions to the
     * the provided [store].
     */
    private class SessionActionHandler(
        private val store: BrowserStore,
        private val sessionId: String
    ) : ActionHandler {

        override fun onBrowserAction(extension: WebExtension, session: EngineSession?, action: Action) {
            store.dispatch(WebExtensionAction.UpdateTabBrowserAction(sessionId, extension.id, action))
        }

        override fun onPageAction(extension: WebExtension, session: EngineSession?, action: Action) {
            store.dispatch(WebExtensionAction.UpdateTabPageAction(sessionId, extension.id, action))
        }
    }

    /**
     * [TabHandler] for session-specific tab events. Forwards actions to the
     * the provided [store].
     */
    private class SessionTabHandler(
        private val store: BrowserStore,
        private val sessionId: String,
        private val onCloseTabOverride: ((WebExtension?, String) -> Unit)? = null,
        private val onSelectTabOverride: ((WebExtension?, String) -> Unit)? = null
    ) : TabHandler {

        override fun onCloseTab(webExtension: WebExtension, engineSession: EngineSession): Boolean {
            val tab = store.state.findTab(sessionId)
            return if (tab != null) {
                closeTab(tab.id, store, onCloseTabOverride, webExtension)
                true
            } else {
                false
            }
        }

        override fun onUpdateTab(
            webExtension: WebExtension,
            engineSession: EngineSession,
            active: Boolean,
            url: String?
        ): Boolean {
            val tab = store.state.findTab(sessionId)
            return if (tab != null) {
                if (active) {
                    onSelectTabOverride?.invoke(webExtension, tab.id)
                        ?: store.dispatch(TabListAction.SelectTabAction(tab.id))
                }
                url?.let {
                    engineSession.loadUrl(it)
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * Registers a listener for web extension related events on the provided
     * [WebExtensionRuntime] and reacts by dispatching the corresponding actions to the
     * provided [BrowserStore].
     *
     * @param runtime the browser [WebExtensionRuntime] to use.
     * @param store the application's [BrowserStore].
     * @param openPopupInTab (optional) flag to determine whether a browser or page action would
     * display a web extension popup in a tab or not. Defaults to false.
     * @param onNewTabOverride (optional) override of behaviour that should
     * be triggered when web extensions open a new tab e.g. when dispatching
     * to the store isn't sufficient while migrating from browser-session
     * to browser-state. This is a lambda accepting the [WebExtension], the
     * [EngineSession] to use, as well as the URL to load, return the ID of
     * the created session.
     * @param onCloseTabOverride (optional) override of behaviour that should
     * be triggered when web extensions close tabs e.g. when dispatching
     * to the store isn't sufficient while migrating from browser-session
     * to browser-state. This is a lambda accepting the [WebExtension] and
     * the session/tab ID to close.
     * @param onSelectTabOverride (optional) override of behaviour that should
     * be triggered when a tab is selected to display a web extension popup.
     * This is a lambda accepting the [WebExtension] and the session/tab ID to
     * select.
     * @param onUpdatePermissionRequest (optional) Invoked when a web extension has changed its
     * permissions while trying to update to a new version. This requires user interaction as
     * the updated extension will not be installed, until the user grants the new permissions.
     * @param onExtensionsLoaded (optional) callback invoked when the extensions are loaded by the
     * engine. Note that the UI (browser/page actions etc.) may not be initialized at this point.
     */
    @Suppress("LongParameterList")
    fun initialize(
        runtime: WebExtensionRuntime,
        store: BrowserStore,
        openPopupInTab: Boolean = false,
        onNewTabOverride: ((WebExtension?, EngineSession, String) -> String)? = null,
        onCloseTabOverride: ((WebExtension?, String) -> Unit)? = null,
        onSelectTabOverride: ((WebExtension?, String) -> Unit)? = null,
        onUpdatePermissionRequest: onUpdatePermissionRequest? = { _, _, _, _ -> },
        onExtensionsLoaded: ((List<WebExtension>) -> Unit)? = null
    ) {
        this.onUpdatePermissionRequest = onUpdatePermissionRequest
        this.onExtensionsLoaded = onExtensionsLoaded
        this.onCloseTabOverride = onCloseTabOverride
        this.onSelectTabOverride = onSelectTabOverride

        // Queries the runtime for installed extensions and adds them to the store
        registerInstalledExtensions(store, runtime)

        // Observes the store and registers action and tab handlers for newly added engine sessions
        registerHandlersForNewSessions(store)

        runtime.registerWebExtensionDelegate(object : WebExtensionDelegate {
            override fun onNewTab(extension: WebExtension, engineSession: EngineSession, active: Boolean, url: String) {
                openTab(store, onNewTabOverride, onSelectTabOverride, extension, engineSession, url, active)
            }

            override fun onBrowserActionDefined(extension: WebExtension, action: Action) {
                store.dispatch(WebExtensionAction.UpdateBrowserAction(extension.id, action))
            }

            override fun onPageActionDefined(extension: WebExtension, action: Action) {
                store.dispatch(WebExtensionAction.UpdatePageAction(extension.id, action))
            }

            override fun onToggleActionPopup(
                extension: WebExtension,
                engineSession: EngineSession,
                action: Action
            ): EngineSession? {
                return if (!openPopupInTab) {
                    store.dispatch(WebExtensionAction.UpdatePopupSessionAction(extension.id, null, engineSession))
                    engineSession
                } else {
                    val popupSessionId = store.state.extensions[extension.id]?.popupSessionId
                    if (popupSessionId != null && store.state.tabs.find { it.id == popupSessionId } != null) {
                        if (popupSessionId == store.state.selectedTabId) {
                            closeTab(popupSessionId, store, onCloseTabOverride, extension)
                        } else {
                            onSelectTabOverride?.invoke(extension, popupSessionId)
                                    ?: store.dispatch(TabListAction.SelectTabAction(popupSessionId))
                        }
                        null
                    } else {
                        val sessionId = openTab(store, onNewTabOverride, onSelectTabOverride, extension, engineSession)
                        store.dispatch(WebExtensionAction.UpdatePopupSessionAction(extension.id, sessionId))
                        engineSession
                    }
                }
            }

            override fun onInstalled(extension: WebExtension) {
                registerInstalledExtension(store, extension)
            }

            override fun onUninstalled(extension: WebExtension) {
                installedExtensions.remove(extension.id)
                store.dispatch(WebExtensionAction.UninstallWebExtensionAction(extension.id))
            }

            override fun onEnabled(extension: WebExtension) {
                installedExtensions[extension.id] = extension
                store.dispatch(WebExtensionAction.UpdateWebExtensionEnabledAction(extension.id, true))
            }

            override fun onDisabled(extension: WebExtension) {
                installedExtensions[extension.id] = extension
                store.dispatch(WebExtensionAction.UpdateWebExtensionEnabledAction(extension.id, false))
            }

            override fun onAllowedInPrivateBrowsingChanged(extension: WebExtension) {
                installedExtensions[extension.id] = extension
                store.dispatch(WebExtensionAction.UpdateWebExtensionAllowedInPrivateBrowsingAction(
                    extension.id, extension.isAllowedInPrivateBrowsing()
                ))
            }

            override fun onInstallPermissionRequest(extension: WebExtension): Boolean {
                // Our current installation flow has us approve permissions before we call
                // install on the engine. Therefore we can just approve the permission request
                // here during installation.
                return true
            }

            override fun onUpdatePermissionRequest(
                current: WebExtension,
                updated: WebExtension,
                newPermissions: List<String>,
                onPermissionsGranted: ((Boolean) -> Unit)
            ) {
                this@WebExtensionSupport.onUpdatePermissionRequest?.invoke(
                    current,
                    updated,
                    newPermissions,
                    onPermissionsGranted
                )
            }

            override fun onExtensionListUpdated() {
                installedExtensions.clear()
                store.dispatch(WebExtensionAction.UninstallAllWebExtensionsAction)
                registerInstalledExtensions(store, runtime)
            }
        })
    }

    /**
     * Awaits for completion of the initialization process (completes when the
     * state of all installed extensions is known).
     */
    suspend fun awaitInitialization() = initializationResult.await()

    /**
     * Queries the [WebExtensionRuntime] for installed web extensions and adds them to the [store].
     */
    private fun registerInstalledExtensions(store: BrowserStore, runtime: WebExtensionRuntime) {
        runtime.listInstalledWebExtensions(
            onSuccess = {
                extensions -> extensions.forEach { registerInstalledExtension(store, it) }
                emitWebExtensionsInitializedFact(extensions)
                initializationResult.complete(Unit)
                onExtensionsLoaded?.invoke(extensions)
            },
            onError = {
                throwable -> logger.error("Failed to query installed extension", throwable)
                initializationResult.completeExceptionally(throwable)
            }
        )
    }

    /**
     * Marks the provided [webExtension] as installed by adding it to the [store].
     */
    private fun registerInstalledExtension(store: BrowserStore, webExtension: WebExtension) {
        installedExtensions[webExtension.id] = webExtension
        store.dispatch(WebExtensionAction.InstallWebExtensionAction(webExtension.toState()))

        // Register action handler for all existing engine sessions on the new extension,
        // an issue was filed to get us an API, so we don't have to do this per extension:
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1603559
        store.state.tabs
            .forEach { tab ->
                tab.engineState.engineSession?.let { session ->
                    registerSessionHandlers(webExtension, store, session, tab.id)
                }
            }
    }

    /**
     * Marks the provided [updatedExtension] as updated in the [store].
     */
    fun markExtensionAsUpdated(store: BrowserStore, updatedExtension: WebExtension) {
        installedExtensions[updatedExtension.id] = updatedExtension
        store.dispatch(WebExtensionAction.UpdateWebExtensionAction(updatedExtension.toState()))

        // Register action handler for all existing engine sessions on the new extension
        store.state.tabs.forEach { tab ->
            tab.engineState.engineSession?.let { session ->
                registerSessionHandlers(updatedExtension, store, session, tab.id)
            }
        }
    }

    /**
     * Observes the provided store to register session-specific [ActionHandler]s
     * for all installed extensions on newly added sessions.
     */
    private fun registerHandlersForNewSessions(store: BrowserStore) {
        // We need to observe for the entire lifetime of the application,
        // as web extension support is not tied to any particular view.
        store.flowScoped { flow ->
            flow.mapNotNull { state -> state.tabs }
                .filterChanged {
                    it.engineState.engineSession
                }
                .collect { state ->
                    state.engineState.engineSession?.let { session ->
                        installedExtensions.values.forEach { extension ->
                            registerSessionHandlers(extension, store, session, state.id)
                        }
                    }
                }
        }
    }

    private fun registerSessionHandlers(
        extension: WebExtension,
        store: BrowserStore,
        session: EngineSession,
        sessionId: String
    ) {
        if (extension.supportActions && !extension.hasActionHandler(session)) {
            val actionHandler = SessionActionHandler(store, sessionId)
            extension.registerActionHandler(session, actionHandler)
        }

        if (!extension.hasTabHandler(session)) {
            val tabHandler = SessionTabHandler(store, sessionId, onCloseTabOverride, onSelectTabOverride)
            extension.registerTabHandler(session, tabHandler)
        }
    }

    @Suppress("LongParameterList")
    private fun openTab(
        store: BrowserStore,
        onNewTabOverride: ((WebExtension?, EngineSession, String) -> String)? = null,
        onSelectTabOverride: ((WebExtension?, String) -> Unit)? = null,
        webExtension: WebExtension?,
        engineSession: EngineSession,
        url: String = "",
        selected: Boolean = true
    ): String {
        return if (onNewTabOverride != null) {
            val sessionId = onNewTabOverride.invoke(webExtension, engineSession, url)
            if (selected) {
                onSelectTabOverride?.invoke(webExtension, sessionId)
            }
            sessionId
        } else {
            val tab = createTab(url)
            store.dispatch(TabListAction.AddTabAction(tab, selected))
            store.dispatch(EngineAction.LinkEngineSessionAction(tab.id, engineSession))
            tab.id
        }
    }

    private fun closeTab(
        id: String,
        store: BrowserStore,
        onCloseTabOverride: ((WebExtension?, String) -> Unit)? = null,
        webExtension: WebExtension?
    ) {
        onCloseTabOverride?.invoke(webExtension, id) ?: store.dispatch(TabListAction.RemoveTabAction(id))
    }

    private fun WebExtension.toState() =
        WebExtensionState(
            id,
            url,
            getMetadata()?.name,
            isEnabled(),
            isAllowedInPrivateBrowsing()
        )
}
