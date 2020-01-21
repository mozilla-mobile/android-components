/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.ext

import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.browser.menu.HighlightableMenuItem

/**
 * Get the highlight effect present in the list of menu items, if any.
 */
@Suppress("Deprecation")
fun List<BrowserMenuItem>.getHighlight() = asSequence()
    .mapNotNull { it as? HighlightableMenuItem }
    .filter { it.isHighlighted() }
    .map { it.highlight }
    .maxBy {
        // Select the highlight with the highest priority
        when (it) {
            is BrowserMenuHighlight.HighPriority -> 2
            is BrowserMenuHighlight.LowPriority -> 1
            is BrowserMenuHighlight.ClassicHighlight -> 0
        }
    }
