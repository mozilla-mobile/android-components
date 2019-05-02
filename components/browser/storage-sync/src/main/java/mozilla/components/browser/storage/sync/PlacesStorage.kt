/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withContext
import mozilla.appservices.places.PlacesReaderConnection
import mozilla.appservices.places.PlacesWriterConnection
import mozilla.components.concept.storage.Storage
import mozilla.components.concept.sync.SyncableStore

/**
 * A base class for concrete implementations of PlacesStorages
 */
abstract class PlacesStorage(context: Context) : Storage, SyncableStore {

    internal val scope by lazy { CoroutineScope(Dispatchers.IO) }
    private val storageDir by lazy { context.filesDir }

    @VisibleForTesting
    internal open val places: Connection by lazy {
        RustPlacesConnection.init(storageDir)
        RustPlacesConnection
    }

    internal val writer: PlacesWriterConnection by lazy { places.writer() }
    internal val reader: PlacesReaderConnection by lazy { places.reader() }

    /**
     * Internal database maintenance tasks. Ideally this should be called once a day.
     */
    override suspend fun runMaintenance() {
        withContext(scope.coroutineContext) {
            places.writer().runMaintenance()
        }
    }

    /**
     * Cleans up background work and database connections
     */
    override fun cleanup() {
        scope.coroutineContext.cancelChildren()
        places.close()
    }
}
