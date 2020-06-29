/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.thumbnails

import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareStore
import mozilla.components.support.images.ImageRequest

/**
 * [Middleware] implementation for handling [ContentAction.UpdateThumbnailAction] and storing
 * the thumbnail to the disk cache.
 */
class ThumbnailsMiddleware(
    private val thumbnailStorage: ThumbnailStorage
) : Middleware<BrowserState, BrowserAction> {
    override fun invoke(
        store: MiddlewareStore<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        when (action) {
            is ContentAction.UpdateThumbnailAction -> {
                // Store the captured tab screenshot from the EngineView when the session's
                // thumbnail is updated.
                thumbnailStorage.saveThumbnail(ImageRequest(action.sessionId), action.thumbnail)
            }
            is TabListAction.RemoveAllNormalTabsAction -> {
                store.state.tabs.filterNot { it.content.private }.forEach { tab ->
                    thumbnailStorage.deleteThumbnail(tab.id)
                }
            }
            is TabListAction.RemoveAllPrivateTabsAction -> {
                store.state.tabs.filter { it.content.private }.forEach { tab ->
                    thumbnailStorage.deleteThumbnail(tab.id)
                }
            }
            is TabListAction.RemoveAllTabsAction -> {
                thumbnailStorage.clearThumbnails()
            }
            is TabListAction.RemoveTabAction -> {
                // Delete the tab screenshot from the storage when the tab is removed.
                thumbnailStorage.deleteThumbnail(action.tabId)
            }
        }

        next(action)
    }
}
