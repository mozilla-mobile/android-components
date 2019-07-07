/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.history

/**
 * A representation of an entry in browser history.
 */
data class HistoryItem(
    val title: String,
    val uri: String,
    val selected: Boolean
)
