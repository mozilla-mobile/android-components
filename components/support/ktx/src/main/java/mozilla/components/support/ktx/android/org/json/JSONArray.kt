/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.org.json

import org.json.JSONArray
import org.json.JSONException

/*
 * Convenience method to convert a JSONArray into a sequence.
 */
fun JSONArray.asSequence(): Sequence<Any> {
    return object : Sequence<Any> {

        override fun iterator() = object : Iterator<Any> {
            val it = (0 until this@asSequence.length()).iterator()

            override fun next(): Any {
                val i = it.next()
                return this@asSequence.get(i)
            }

            override fun hasNext() = it.hasNext()
        }
    }
}

/**
 * Convenience method to convert a JSONArray into a List
 *
 * @return list with the JSONArray values, or an empty list if the JSONArray was null
 */
@Suppress("UNCHECKED_CAST")
fun <T> JSONArray?.toList(): List<T> {
    if (this != null) {
        return asSequence().map { it as T }.toList()
    }
    return listOf()
}

// #2305: inline this function when Android gradle plugin v3.4.0 is released
/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original collection as returned by [getFromArray]. If [getFromArray]
 * or [transform] throws a [JSONException], these elements will also be omitted.
 *
 * Here's an example call:
 * ```kotlin
 * jsonArray.mapNotNull(JSONArray::getJSONObject) { jsonObj -> jsonObj.getString("author") }
 * ```
 */
fun <T, R : Any> JSONArray.mapNotNull(getFromArray: JSONArray.(index: Int) -> T, transform: (T) -> R?): List<R> {
    val transformedResults = mutableListOf<R>()
    for (i in 0 until this.length()) {
        try {
            val transformed = transform(getFromArray(i))
            if (transformed != null) { transformedResults.add(transformed) }
        } catch (e: JSONException) { /* Do nothing: we skip bad data. */ }
    }

    return transformedResults
}
