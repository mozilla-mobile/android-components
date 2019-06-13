/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.ext

/**
 * Perforce cartesian product on two collections.
 *
 * For example result of
 * ```
 * listOf("a", "b").combineWith(listOf(1, 2))
 * ```
 *
 * will be
 * `[("a", 1), ("a", 2), ("b", 1), ("b", 2)]`
 */
fun <T, R> Iterable<T>.combineWith(other: Iterable<R>): Iterable<Pair<T, R>> =
    flatMap { first ->
        other.map { second ->
            first to second
        }
    }