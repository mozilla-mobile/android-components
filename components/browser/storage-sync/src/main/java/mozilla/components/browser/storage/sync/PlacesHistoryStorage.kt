/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import android.content.Context
import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.HistoryAutocompleteResult
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.SearchResult
import mozilla.components.concept.storage.SyncError
import mozilla.components.concept.storage.SyncOk
import mozilla.components.concept.storage.SyncStatus
import mozilla.components.concept.storage.SyncableStore
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.utils.segmentAwareDomainMatch
import org.mozilla.places.PlacesConnection
import org.mozilla.places.PlacesException
import org.mozilla.places.SyncAuthInfo
import org.mozilla.places.VisitObservation

const val AUTOCOMPLETE_SOURCE_NAME = "placesHistory"

typealias SyncAuthInfo = org.mozilla.places.SyncAuthInfo

/**
 * Implementation of the [HistoryStorage] which is backed by a Rust Places lib via [PlacesConnection].
 */
open class PlacesHistoryStorage(context: Context) : HistoryStorage, SyncableStore<SyncAuthInfo> {
    private val scope by lazy { CoroutineScope(Dispatchers.IO) }
    private val storageDir by lazy { context.filesDir }

    @VisibleForTesting
    internal open val places: Connection by lazy {
        RustPlacesConnection.createLongLivedConnection(storageDir)
        RustPlacesConnection
    }

    override suspend fun recordVisit(uri: String, visitType: VisitType) {
        scope.launch {
            places.api().noteObservation(VisitObservation(uri, visitType = visitType.toPlacesType()))
        }.join()
    }

    override suspend fun recordObservation(uri: String, observation: PageObservation) {
        // NB: visitType of null means "record meta information about this URL".
        scope.launch {
            places.api().noteObservation(
                VisitObservation(
                    url = uri,
                    visitType = org.mozilla.places.VisitType.UPDATE_PLACE,
                    title = observation.title
                )
            )
        }.join()
    }

    override fun getVisited(uris: List<String>): Deferred<List<Boolean>> {
        return scope.async { places.api().getVisited(uris) }
    }

    override fun getVisited(): Deferred<List<String>> {
        return scope.async {
            places.api().getVisitedUrlsInRange(
                start = 0,
                end = System.currentTimeMillis(),
                includeRemote = true
            )
        }
    }

    override fun cleanup() {
        scope.coroutineContext.cancelChildren()
        places.close()
    }

    override fun getSuggestions(query: String, limit: Int): List<SearchResult> {
        require(limit >= 0) { "Limit must be a positive integer" }
        return places.api().queryAutocomplete(query, limit = limit).map {
            SearchResult(it.url, it.url, it.frecency.toInt(), it.title)
        }
    }

    override fun getAutocompleteSuggestion(query: String): HistoryAutocompleteResult? {
        val urls = places.api().queryAutocomplete(query, limit = 100)
        val resultText = segmentAwareDomainMatch(query, urls.map { it.url })
        resultText?.let {
            return HistoryAutocompleteResult(it.matchedSegment, it.url, AUTOCOMPLETE_SOURCE_NAME, urls.size)
        }
    }

    override suspend fun sync(authInfo: SyncAuthInfo): SyncStatus {
        return RustPlacesConnection.newConnection(storageDir).use {
            try {
                it.sync(authInfo)
                SyncOk
            } catch (e: PlacesException) {
                SyncError(e)
            }
        }
    }

    /**
     * We have an internal visit type definition defined at the concept level, and an external type
     * defined within Places. In practice these two types are the same, with the Places one being a
     * little richer.
     */
    private fun VisitType.toPlacesType(): org.mozilla.places.VisitType {
        return when (this) {
            VisitType.LINK -> org.mozilla.places.VisitType.LINK
            VisitType.RELOAD -> org.mozilla.places.VisitType.RELOAD
            VisitType.TYPED -> org.mozilla.places.VisitType.TYPED
        }
    }
}
