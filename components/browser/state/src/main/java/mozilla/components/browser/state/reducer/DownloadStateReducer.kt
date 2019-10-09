/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.BrowserState

internal object DownloadStateReducer {

    /**
     * [DownloadAction] Reducer function for modifying [BrowserState.completedDownloads].
     */
    fun reduce(state: BrowserState, action: DownloadAction): BrowserState = when (action) {
        is DownloadAction.DownloadCompletedAction ->
            state.copy(completedDownloads = state.completedDownloads + action.downloadId)
    }
}
