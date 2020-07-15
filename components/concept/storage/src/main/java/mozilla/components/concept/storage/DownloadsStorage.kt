package mozilla.components.concept.storage

interface DownloadsStorage :Storage {
    /**
     * Records a visit to a page.
     * @param uri of the page which was visited.
     * @param visit Information about the visit; see [PageVisit].
     */
    suspend fun recordDownload(filepath: String, contentType: String)

    suspend fun getDownloads(): List<String>


    /**
     * Class for holding metadata about any Download
     */
    data class Download(
            val type: BookmarkNodeType,
            val guid: String,
            val parentGuid: String?,
            val position: Int?,
            val title: String?,
            val url: String?,
            val children: List<BookmarkNode>?
    )
}