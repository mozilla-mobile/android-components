package mozilla.components.browser.storage.memory

import androidx.annotation.VisibleForTesting
import mozilla.components.concept.storage.HistoryAutocompleteResult
import mozilla.components.concept.storage.DownloadsStorage
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.SearchResult
import mozilla.components.concept.storage.PageVisit
import mozilla.components.concept.storage.RedirectSource
import mozilla.components.concept.storage.TopFrecentSiteInfo
import mozilla.components.concept.storage.VisitInfo
import mozilla.components.concept.storage.VisitType
import mozilla.components.support.utils.StorageUtils.levenshteinDistance
import mozilla.components.support.utils.segmentAwareDomainMatch

data class Download(val timestamp: Long, val type: VisitType)

/**
 * An in-memory implementation of [mozilla.components.concept.storage.HistoryStorage].
 */
@SuppressWarnings("TooManyFunctions")
class InMemoryDownloadsStorage : DownloadsStorage {
    @VisibleForTesting
    internal var downloads: HashMap<String, String> = linkedMapOf()
    @VisibleForTesting
    internal val pageMeta: HashMap<String, PageObservation> = hashMapOf()

    override suspend fun warmUp() {
        // No-op for an in-memory store
    }

    override suspend fun recordDownload(filepath:String, contentType: String) {
        val now = System.currentTimeMillis()

        synchronized(downloads) {
                downloads[filepath] = contentType
        }
    }

    override suspend fun getDownloads(): List<String> = synchronized(downloads) {
        return downloads.keys.toList()
    }

    override suspend fun runMaintenance() {
        // Not applicable.
    }

    override fun cleanup() {
        // GC will take care of our internal data structures, so there's nothing to do here.
    }
}