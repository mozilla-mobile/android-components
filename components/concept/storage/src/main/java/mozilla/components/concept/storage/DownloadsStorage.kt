package mozilla.components.concept.storage

interface DownloadsStorage :Storage {
    /**
     * Records a visit to a page.
     * @param uri of the page which was visited.
     * @param visit Information about the visit; see [PageVisit].
     */
    suspend fun recordDownload(filepath: String, downloadInfo: DownloadInfo)

    /**
     * Return a "page" of history results. Each page will have visits in descending order
     * with respect to their visit timestamps. In the case of ties, their row id will
     * be used.
     *
     * Note that you may get surprising results if the items in the database change
     * while you are paging through records.
     *
     * @param offset The offset where the page begins.
     * @param count The number of items to return in the page.
     * @param excludeTypes List of visit types to exclude.
     */
    suspend fun getDownloadsPaginated(
            offset: Long,
            count: Long
    ): List<DownloadInfo>

    /**
     * Retrieves detailed information about all visits that occurred in the given time range.
     * @param start The (inclusive) start time to bound the query.
     * @param end The (inclusive) end time to bound the query.
     * @param excludeTypes List of visit types to exclude.
     * @return A list of all visits within the specified range, described by [VisitInfo].
     */
    suspend fun getDetailedDownloads(
            start: Long,
            end: Long = Long.MAX_VALUE
    ): List<DownloadInfo>


    /**
     * Class for holding metadata about any Download
     */
    data class DownloadInfo(
            val filepath: String,
            val contentType: String,
            val downloadTime: Long
    )
}