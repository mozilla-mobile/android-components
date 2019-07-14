/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.action

import android.graphics.Bitmap
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.SecurityInfoState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.lib.state.Action

/**
 * [Action] implementation related to [BrowserState].
 */
sealed class BrowserAction : Action

/**
 * [BrowserAction] implementations to react to system events.
 */
sealed class SystemAction : BrowserAction() {
    /**
     * Optimizes the [BrowserState] by removing unneeded and optional
     * resources if the system is in a low memory condition.
     */
    object LowMemoryAction : SystemAction()
}

/**
 * [BrowserAction] implementations related to updating the list of [TabSessionState] inside [BrowserState].
 */
sealed class TabListAction : BrowserAction() {
    /**
     * Adds a new [TabSessionState] to the list.
     *
     * @property tab the [TabSessionState] to add
     * @property select whether or not to the tab should be selected.
     */
    data class AddTabAction(val tab: TabSessionState, val select: Boolean = false) : TabListAction()

    /**
     * Marks the [TabSessionState] with the given [tabId] as selected tab.
     *
     * @property tabId the ID of the tab to select.
     */
    data class SelectTabAction(val tabId: String) : TabListAction()

    /**
     * Removes the [TabSessionState] with the given [tabId] from the list of sessions.
     *
     * @property tabId the ID of the tab to remove.
     * @property selectParentIfExists whether or not a parent tab should be
     * selected if one exists, defaults to true.
     */
    data class RemoveTabAction(val tabId: String, val selectParentIfExists: Boolean = true) : TabListAction()

    /**
     * Restores state from a (partial) previous state.
     *
     * @property tabs the [TabSessionState]s to restore.
     * @property selectedTabId the ID of the tab to select.
     */
    data class RestoreAction(val tabs: List<TabSessionState>, val selectedTabId: String? = null) : TabListAction()

    /**
     * Removes both private and normal [TabSessionState]s.
     */
    object RemoveAllTabsAction : TabListAction()

    /**
     * Removes all private [TabSessionState]s.
     */
    object RemoveAllPrivateTabsAction : TabListAction()

    /**
     * Removes all non-private [TabSessionState]s.
     */
    object RemoveAllNormalTabsAction : TabListAction()
}

/**
 * [BrowserAction] implementations related to updating [BrowserState.customTabs].
 */
sealed class CustomTabListAction : BrowserAction() {
    /**
     * Adds a new [CustomTabSessionState] to [BrowserState.customTabs].
     *
     * @property tab the [CustomTabSessionState] to add.
     */
    data class AddCustomTabAction(val tab: CustomTabSessionState) : CustomTabListAction()

    /**
     * Removes an existing [CustomTabSessionState] to [BrowserState.customTabs].
     *
     * @property tabId the ID of the custom tab to remove.
     */
    data class RemoveCustomTabAction(val tabId: String) : CustomTabListAction()

    /**
     * Removes all custom tabs [TabSessionState]s.
     */
    object RemoveAllCustomTabsAction : CustomTabListAction()
}

/**
 * [BrowserAction] implementations related to updating the [ContentState] of a single [SessionState] inside
 * [BrowserState].
 */
sealed class ContentAction : BrowserAction() {
    /**
     * Removes the thumbnail of the [ContentState] with the given [sessionId].
     */
    data class RemoveThumbnailAction(val sessionId: String) : ContentAction()

    /**
     * Updates the URL of the [ContentState] with the given [sessionId].
     */
    data class UpdateUrlAction(val sessionId: String, val url: String) : ContentAction()

    /**
     * Updates the progress of the [ContentState] with the given [sessionId].
     */
    data class UpdateProgressAction(val sessionId: String, val progress: Int) : ContentAction()

    /**
     * Updates the title of the [ContentState] with the given [sessionId].
     */
    data class UpdateTitleAction(val sessionId: String, val title: String) : ContentAction()

    /**
     * Updates the loading state of the [ContentState] with the given [sessionId].
     */
    data class UpdateLoadingStateAction(val sessionId: String, val loading: Boolean) : ContentAction()

    /**
     * Updates the search terms of the [ContentState] with the given [sessionId].
     */
    data class UpdateSearchTermsAction(val sessionId: String, val searchTerms: String) : ContentAction()

    /**
     * Updates the [SecurityInfoState] of the [ContentState] with the given [sessionId].
     */
    data class UpdateSecurityInfo(val sessionId: String, val securityInfo: SecurityInfoState) : ContentAction()

    /**
     * Updates the thumbnail of the [ContentState] with the given [sessionId].
     */
    data class UpdateThumbnailAction(val sessionId: String, val thumbnail: Bitmap) : ContentAction()
}
