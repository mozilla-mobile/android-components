package mozilla.components.feature.session.engine

import android.net.Uri
import mozilla.components.concept.engine.history.DownloadsTrackingDelegate
import mozilla.components.concept.storage.DownloadsStorage

/**
 * Implementation of the [DownloadsTrackingDelegate] which delegates work to an instance of [DownloadsStorage].
 */
class DownloadsDelegate(private val downloadsStorage: Lazy<DownloadsStorage>) : DownloadsTrackingDelegate {
    override suspend fun onDownloaded(filepath: String, contentType: String) {
            downloadsStorage.value.recordDownload(filepath, contentType)
    }
}