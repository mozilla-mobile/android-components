package mozilla.components.browser.storage.sync

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.DownloadsStorage
import mozilla.components.concept.storage.DownloadsStorage.DownloadInfo
import mozilla.components.support.base.log.logger.Logger

data class Download(val timestamp: Long, val downloadInfo: DownloadInfo)

/**
 * An in-memory implementation of [mozilla.components.concept.storage.HistoryStorage].
 */
@SuppressWarnings("TooManyFunctions")
open class PlacesDownloadsStorage(context: Context) :  PlacesStorage(context), DownloadsStorage {
    override val logger = Logger("PlacesDownloadsStorage")
    @VisibleForTesting
    internal var downloads: HashMap<String, MutableList<Download>> = linkedMapOf()

    override suspend fun warmUp() {
        // No-op for an in-memory store
    }

    override suspend fun recordDownload(filepath: String, downloadInfo: DownloadInfo) {
        withContext(scope.coroutineContext) {
            val now = System.currentTimeMillis()

            synchronized(downloads) {
                if (!downloads.containsKey(filepath)) {
                    downloads[filepath] = mutableListOf(Download(now, downloadInfo))
                } else {
                    downloads[filepath]!!.add(Download(now, downloadInfo))
                }
            }
        }
    }

    override suspend fun getDownloadsPaginated(offset: Long, count: Long): List<DownloadInfo> {
            throw UnsupportedOperationException("Pagination is not yet supported by the in-memory history storage")
    }

    override suspend fun getDetailedDownloads(
            start: Long,
            end: Long
    ): List<DownloadInfo> = synchronized(downloads) {
            val downloadInfos = mutableListOf<DownloadInfo>()

            downloads.forEach {
                it.value.forEach { download ->
                    if (download.timestamp >= start && download.timestamp <= end) {
                        downloadInfos.add(DownloadInfo(
                                filepath = it.key,
                                contentType = download.downloadInfo.contentType,
                                downloadTime = download.downloadInfo.downloadTime
                        ))
                    }
                }
            }

            return downloadInfos
    }

    override fun getHandle(): Long {
        return places.getHandle()
    }

    override suspend fun runMaintenance() {
        // Not applicable.
    }

    override fun cleanup() {
        // GC will take care of our internal data structures, so there's nothing to do here.
    }
}