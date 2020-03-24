/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.location

/**
 * Interface describing a [LocationService] that returns a [Region].
 */
interface LocationService {
    /**
     * Determines the current [Region] of the user.
     */
    suspend fun fetchRegion(readFromCache: Boolean = true): Region?

    /**
     * A [Region] returned by the location service.
     *
     * The [Region] use region codes and names from the GENC dataset, which is for the most part
     * compatible with the ISO 3166 standard. While the API endpoint and [Region] class refers to
     * country, no claim about the political status of any region is made by this service.
     *
     * @param countryCode Country code; ISO 3166.
     * @param countryName Name of the country (English); ISO 3166.
     */
    data class Region(
        val countryCode: String,
        val countryName: String
    )

    companion object {
        /**
         * Creates a dummy [LocationService] implementation that always returns `null`.
         */
        fun dummy() = object : LocationService {
            override suspend fun fetchRegion(readFromCache: Boolean): Region? = null
        }
    }
}
