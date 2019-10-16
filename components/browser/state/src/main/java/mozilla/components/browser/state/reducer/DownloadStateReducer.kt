/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadStatus

internal object DownloadStateReducer {

    /**
     * [DownloadAction] Reducer function for modifying [BrowserState.queuedDownloads].
     */
    fun reduce(state: BrowserState, action: DownloadAction): BrowserState = when (action) {
        is DownloadAction.DownloadStartedAction -> {
            state.changeDownloadStatusOf(downloadState = action.download, newState = DownloadStatus.RUNNING)
        }
        is DownloadAction.AddDownloadAction -> {
            state.changeDownloadStatusOf(downloadState = action.download, newState = DownloadStatus.QUEUED)
        }
        is DownloadAction.DownloadCompletedAction -> {
            state.changeDownloadStatusOf(downloadState = action.download, newState = DownloadStatus.COMPLETED)
        }
        is DownloadAction.DownloadFailedAction -> {
            state.changeDownloadStatusOf(downloadState = action.download, newState = DownloadStatus.FAILED)
        }
    }
}

/**
 * Retrieves the [downloadState] from [BrowserState.queuedDownloads] and change its status to the
 * [newState].
 *
 * @return a new copy of [BrowserState] with newly updated [downloadState] status.
 */
private fun BrowserState.changeDownloadStatusOf(downloadState: DownloadState,
                                                newState: DownloadStatus): BrowserState {
    return copy(queuedDownloads = queuedDownloads.map { queuedDownloadState ->
        if (downloadState.id == queuedDownloadState.id) {
            queuedDownloadState.copy(status = newState)
        } else {
            queuedDownloadState
        }
    })
}
