/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.content.DownloadState.Status.CANCELLED
import mozilla.components.browser.state.state.content.DownloadState.Status.COMPLETED
import mozilla.components.browser.state.state.content.DownloadState.Status.FAILED
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.Store
import mozilla.components.support.base.log.logger.Logger
import kotlin.coroutines.CoroutineContext

/**
 * [Middleware] implementation for managing downloads via the provided download service. Its
 * purpose is to react to global download state changes (e.g. of [BrowserState.downloads])
 * and notify the download service, as needed.
 */
@Suppress("ComplexMethod")
class DownloadMiddleware(
    private val applicationContext: Context,
    private val downloadServiceClass: Class<*>,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    @VisibleForTesting
    internal val downloadStorage: DownloadStorage = DownloadStorage(applicationContext)
) : Middleware<BrowserState, BrowserAction> {
    private val logger = Logger("DownloadMiddleware")

    private var scope = CoroutineScope(coroutineContext)

    @InternalCoroutinesApi
    override fun invoke(
        context: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction
    ) {
        when (action) {
            is DownloadAction.RemoveDownloadAction -> removeDownload(action.downloadId, context.store)
            is DownloadAction.RemoveAllDownloadsAction -> removeDownloads()
            is DownloadAction.UpdateDownloadAction -> updateDownload(action.download, context)
            is DownloadAction.RestoreDownloadsStateAction -> restoreDownloads(context.store)
            is DownloadAction.AddDownloadAction -> {
                if (!saveDownload(context.store, action.download)) {
                    // The download was already added before, so we are ignoring this request.
                    logger.debug("Ignored add action for ${action.download.id} " +
                            "download already in store.downloads")
                    return
                }
            }
        }

        next(action)

        when (action) {
            is DownloadAction.AddDownloadAction -> sendDownloadIntent(action.download)
            is DownloadAction.RestoreDownloadStateAction -> sendDownloadIntent(action.download)
        }
    }

    private fun removeDownload(
        downloadId: String,
        store: Store<BrowserState, BrowserAction>
    ) = scope.launch {
        store.state.downloads[downloadId]?.let {
            downloadStorage.remove(it)
            logger.debug("Removed download ${it.fileName} from the storage")
        }
    }

    private fun removeDownloads() = scope.launch {
        downloadStorage.removeAllDownloads()
    }

    private fun updateDownload(updated: DownloadState, context: MiddlewareContext<BrowserState, BrowserAction>) {
        context.state.downloads[updated.id]?.let { old ->
            // To not overwhelm the storage, we only send updates that are relevant,
            // we only care about properties, that we are stored on the storage.
            if (!DownloadStorage.isSameDownload(old, updated)) {
                scope.launch {
                    downloadStorage.update(updated)
                }
                logger.debug("Updated download ${updated.fileName} on the storage")
            }
        }
    }

    private fun restoreDownloads(store: Store<BrowserState, BrowserAction>) = scope.launch {
        downloadStorage.getDownloads().collect { downloads ->
            downloads.forEach { download ->
                if (!store.state.downloads.containsKey(download.id)) {
                    store.dispatch(DownloadAction.RestoreDownloadStateAction(download))
                    logger.debug("Download restored from the storage ${download.fileName}")
                }
            }
        }
    }

    private fun saveDownload(store: Store<BrowserState, BrowserAction>, download: DownloadState): Boolean {
        return if (!store.state.downloads.containsKey(download.id)) {
            scope.launch {
                downloadStorage.add(download)
                logger.debug("Added download ${download.fileName} to the storage")
            }
            true
        } else {
            false
        }
    }

    @VisibleForTesting
    internal fun sendDownloadIntent(download: DownloadState) {
        if (download.status !in arrayOf(COMPLETED, CANCELLED, FAILED)) {
            val intent = Intent(applicationContext, downloadServiceClass)
            intent.putExtra(DownloadManager.EXTRA_DOWNLOAD_ID, download.id)
            startForegroundService(intent)
            logger.debug("Sending download intent ${download.fileName}")
        }
    }

    @VisibleForTesting
    internal fun startForegroundService(intent: Intent) {
        ContextCompat.startForegroundService(applicationContext, intent)
    }
}
