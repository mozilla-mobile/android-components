/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.memory

import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.storage.PageObservation
import mozilla.components.concept.storage.SearchResult
import mozilla.components.concept.storage.VisitType

data class Visit(val timestamp: Long, val type: VisitType)

private const val URL_MATCH_WEIGHT = 10
private const val TITLE_MATCH_WEIGHT = 5

/**
 * An in-memory implementation of [mozilla.components.concept.storage.HistoryStorage].
 */
class InMemoryHistoryStorage : HistoryStorage {
    @VisibleForTesting
    internal val pages: LinkedHashMap<String, MutableList<Visit>> = linkedMapOf()
    @VisibleForTesting
    internal val pageMeta: HashMap<String, PageObservation> = hashMapOf()

    override suspend fun recordVisit(uri: String, visitType: VisitType) {
        val now = System.currentTimeMillis()

        synchronized(pages) {
            if (!pages.containsKey(uri)) {
                pages[uri] = mutableListOf(Visit(now, visitType))
            } else {
                pages[uri]!!.add(Visit(now, visitType))
            }
        }
    }

    override suspend fun recordObservation(uri: String, observation: PageObservation) {
        synchronized(pageMeta) {
            pageMeta[uri] = observation
        }
    }

    override fun getVisited(uris: List<String>): Deferred<List<Boolean>> {
        return CompletableDeferred(synchronized(pages) {
            uris.map {
                if (pages[it] != null && pages[it]!!.size > 0) {
                    return@map true
                }
                return@map false
            }
        })
    }

    override fun getVisited(): Deferred<List<String>> {
        return CompletableDeferred(synchronized(pages) {
            pages.keys.toList()
        })
    }

    override fun getSuggestions(query: String): List<SearchResult> = synchronized(pages + pageMeta) {
        data class Hit(val url: String, val score: Int)

        val urlMatches = pages.asSequence().map {
            Hit(it.key, levenshteinDistance(it.key, query))
        }
        val titleMatches = pageMeta.asSequence().map {
            Hit(it.key, levenshteinDistance(it.value.title ?: "", query))
        }
        val matchedUrls = mutableMapOf<String, Int>()
        urlMatches.plus(titleMatches).forEach {
            if (matchedUrls.containsKey(it.url) && matchedUrls[it.url]!! < it.score) {
                matchedUrls[it.url] = it.score
            } else {
                matchedUrls[it.url] = it.score
            }
        }
        // Calculate maxScore so that we can invert our scoring.
        // Lower Levenshtein distance should produce a higher score.
        val maxScore = urlMatches.maxBy { it.score }?.score
        if (maxScore == null) return@synchronized listOf()

        // TODO exclude non-matching results entirely? Score that implies complete mismatch.
        matchedUrls.asSequence().sortedBy { it.value }.map {
            SearchResult(id = it.key, score = maxScore - it.value, url = it.key, title = pageMeta[it.key]?.title)
        }.toList()
    }

    // Borrowed from https://gist.github.com/ademar111190/34d3de41308389a0d0d8
    private fun levenshteinDistance(a: String, b: String): Int {
        val lhsLength = a.length
        val rhsLength = b.length

        var cost = Array(lhsLength) { it }
        var newCost = Array(lhsLength) { 0 }

        for (i in 1..rhsLength - 1) {
            newCost[0] = i

            for (j in 1..lhsLength - 1) {
                val match = if (a[j - 1] == b[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = Math.min(Math.min(costInsert, costDelete), costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength - 1]
    }
}
