package mozilla.components.concept.engine.history

/**
 * An interface used for providing history information to an engine (e.g. for link highlighting),
 * and receiving history updates from the engine (visits to URLs, title changes).
 *
 * Even though this interface is defined at the "concept" layer, its get* methods are tailored to
 * two types of engines which we support (system's WebView and GeckoView).
 */
interface DownloadsTrackingDelegate {
    /**
     * A URI visit happened that an engine considers worthy of being recorded in browser's history.
     */
    suspend fun onDownloaded(filepath: String, contentType: String)

}