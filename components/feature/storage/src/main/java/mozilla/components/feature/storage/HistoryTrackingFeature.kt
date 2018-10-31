/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.storage

import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.history.HistoryTrackingDelegate
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.VisitType

/**
 * Feature implementation for connecting an engine implementation with a history storage implementation
 * in order to enable engine to track history.
 */
class HistoryTrackingFeature(engine: Engine, historyStorage: HistoryStorage) {
    init {
        engine.settings.historyTrackingDelegate = HistoryDelegate(historyStorage)
    }
}

@VisibleForTesting
internal class HistoryDelegate(private val historyStorage: HistoryStorage) : HistoryTrackingDelegate {
    override suspend fun onVisited(uri: String, isReload: Boolean, privateMode: Boolean) {
        if (privateMode) {
            return
        }
        val visitType = when (isReload) {
            true -> VisitType.RELOAD
            false -> VisitType.LINK
        }
        historyStorage.recordVisit(uri, visitType)
    }

    override suspend fun onTitleChanged(uri: String, title: String, privateMode: Boolean) {
        if (privateMode) {
            return
        }
        historyStorage.recordObservation(uri, PageObservation(title = title))
    }

    override fun getVisited(uris: List<String>, privateMode: Boolean): Deferred<List<Boolean>> {
        if (privateMode) {
            return CompletableDeferred(List(uris.size) { false })
        }

        return historyStorage.getVisited(uris)
    }

    override fun getVisited(privateMode: Boolean): Deferred<List<String>> {
        if (privateMode) {
            return CompletableDeferred(listOf())
        }
        return historyStorage.getVisited()
    }
}
