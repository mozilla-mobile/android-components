/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.storage

/**
 * An interface which defines read/write methods for history data.
 */
interface HistoryStorage {
    /**
     * Records a visit to a page.
     * @param uri of the page which was visited.
     * @param visitType type of the visit, see [VisitType].
     */
    suspend fun recordVisit(uri: String, visitType: VisitType)

    /**
     * Records an observation about a page.
     * @param uri of the page for which to record an observation.
     * @param observation a [PageObservation] which encapsulates meta data observed about the page.
     */
    suspend fun recordObservation(uri: String, observation: PageObservation)

    /**
     * Maps a list of page URIs to a list of booleans indicating if each URI was visited.
     * @param uris a list of page URIs about which "visited" information is being requested.
     * @return A list of booleans indicating visited status of each
     * corresponding page URI from [uris].
     */
    suspend fun getVisited(uris: List<String>): List<Boolean>

    /**
     * Retrieves a list of all visited pages.
     * @return A list of all visited page URIs.
     */
    suspend fun getVisited(): List<String>

    /**
     * Retrieves detailed information about all visits that occurred in the given time range.
     * @param start The (inclusive) start time to bound the query.
     * @param end The (inclusive) end time to bound the query.
     * @return A list of all visits within the specified range, described by [VisitInfo].
     */
    suspend fun getDetailedVisits(start: Long, end: Long = Long.MAX_VALUE): List<VisitInfo>

    /**
     * Retrieves suggestions matching the [query].
     * @param query A query by which to search the underlying store.
     * @return A List of [SearchResult] matching the query, in no particular order.
     */
    fun getSuggestions(query: String, limit: Int): List<SearchResult>

    /**
     * Retrieves domain suggestions which best match the [query].
     * @param query A query by which to search the underlying store.
     * @return An optional domain URL which best matches the query.
     */
    fun getAutocompleteSuggestion(query: String): HistoryAutocompleteResult?

    /**
     * Cleanup any allocated resources.
     */
    fun cleanup()
}

data class PageObservation(val title: String?)

/**
 * Information about a history visit.
 *
 * @property url The URL of the page that was visited.
 * @property title The title of the page that was visited, if known.
 * @property visitTime The time the page was visited in integer milliseconds since the unix epoch.
 * @property visitType What the transition type of the visit is, expressed as [VisitType].
 */
data class VisitInfo(
    val url: String,
    val title: String?,
    val visitTime: Long,
    val visitType: VisitType
)

/**
 * Visit type constants as defined by Desktop Firefox.
 */
@SuppressWarnings("MagicNumber")
enum class VisitType(val type: Int) {
    // Internal visit type used for meta data updates. Doesn't represent an actual page visit.
    NOT_A_VISIT(-1),
    // User followed a link.
    LINK(1),
    // User typed a URL or selected it from the UI (autocomplete results, etc).
    TYPED(2),
    BOOKMARK(3),
    EMBED(4),
    REDIRECT_PERMANENT(5),
    REDIRECT_TEMPORARY(6),
    DOWNLOAD(7),
    FRAMED_LINK(8),
    RELOAD(9)
}

/**
 * Encapsulates a set of properties which define a result of querying history storage.
 *
 * @property id A permanent identifier which might be used for caching or at the UI layer.
 * @property url A URL of the page.
 * @property score An unbounded, nonlinear score (larger is more relevant) which is used to rank
 *  this [SearchResult] against others.
 * @property title An optional title of the page.
 */
data class SearchResult(
    val id: String,
    val url: String,
    val score: Int,
    val title: String? = null
)

/**
 * Describes an autocompletion result against history storage.
 * @property input Input for which this result is being provided.
 * @property text Result of autocompletion, text to be displayed.
 * @property url Result of autocompletion, full matching url.
 * @property source Name of the autocompletion source.
 * @property totalItems A total number of results also available.
 */
data class HistoryAutocompleteResult(
    val input: String,
    val text: String,
    val url: String,
    val source: String,
    val totalItems: Int
)
