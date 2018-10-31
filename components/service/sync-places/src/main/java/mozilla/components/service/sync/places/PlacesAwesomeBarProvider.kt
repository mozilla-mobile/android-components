/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.places

import android.content.Context
import org.mozilla.places.PlacesConnection
import java.net.URL
import mozilla.components.support.base.log.logger.Logger

const val DB_NAME = "places.sqlite"

// XXX - although mozilla.components.concept.awesomebar exists, it's premature for
// this component to try and use it before there's a little more support.
// So we'll move this to that interface eventually, but for now, let's do the
// smallest possible thing.
class PlacesAwesomeBarProvider(context: Context) {

    // XXX - we probably want a way to share a single places connection between
    // autocomplete and history, but for now we have our own.
    private val placesConnection = PlacesConnection(context.getDatabasePath(DB_NAME).canonicalPath)
    private val logger = Logger("PlacesAwesomeBarProvider")

    // Gets a single suggestion.
    fun getSuggestion(value: String): String? {
        this.logger.debug("starting places search for $value")
        val placesResults = placesConnection.queryAutocomplete(value)
        this.logger.debug("places search for $value, gave ${placesResults.size} matches")

        // Iterate all results filtering on ones that match at the start.
        for (r in placesResults) {
            var h = URL(r.url).host;
            if (h.startsWith("www.")) {
                h = h.substring(4)
            }
            if (h.startsWith(value)) {
                this.logger.debug("places found matching result $h")
                return h
            }
        }
        this.logger.debug("places found no matching results")
        return null
    }
}
