package mozilla.components.feature.session

import android.net.Uri
import mozilla.components.concept.engine.history.DownloadsTrackingDelegate
import mozilla.components.concept.storage.DownloadsStorage
import mozilla.components.concept.storage.DownloadsStorage.DownloadInfo

/**
 * Implementation of the [DownloadsTrackingDelegate] which delegates work to an instance of [DownloadsStorage].
 */
class DownloadsDelegate(private val downloadsStorage: Lazy<DownloadsStorage>) : DownloadsTrackingDelegate {
    override suspend fun onDownloaded(filepath: String, downloadInfo: DownloadInfo) {
            downloadsStorage.value.recordDownload(filepath, downloadInfo)
    }
}