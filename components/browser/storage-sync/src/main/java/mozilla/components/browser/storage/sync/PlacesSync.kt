/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import android.content.Context
import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import mozilla.components.support.base.log.logger.Logger

import org.mozilla.places.SyncAuthInfo

/**
 * Implementation of the [HistoryStorage] which is backed by a Rust Places lib via [PlacesConnection].
 */
open class PlacesSync(context: Context) {
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }
    private val logger = Logger("storage-sync/sync")

    // Ultimately we might want finer-grained errors here, but for now...
    // (ideally this would be an enum, but I can't work out how to sanely
    // to that, as only the instance to this is available)
    val COMPLETED_OK = 0
    val FAILED_ACCOUNT_PROBLEM = 1
    val FAILED_OTHER_PROBLEM = 2

    // Note that we use our own connection here and don't share the one used by
    // PlacesHistoryStorage, as otherwise a sync would block work done by that
    // other connection - but it's important they are both opening the same file!
    @VisibleForTesting
    internal open val places: Connection by lazy {
        RustPlacesConnection.init(context.getDatabasePath(DB_NAME).canonicalPath, null)
        RustPlacesConnection
    }

    // Performs a sync.
    suspend fun sync(sui: SyncAuthInfo): Deferred<Int> {
        return scope.async {
            try {
                places.api().sync(sui)
                COMPLETED_OK
            } catch (t: Throwable) {
                // XXX - need to extract whether this was an account problem
                // from the exception.
                logger.warn("sync failed: $t")
                FAILED_OTHER_PROBLEM
            }
        }
    }
}
