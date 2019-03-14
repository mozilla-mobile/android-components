/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket.net

/**
 * A response from the Pocket API: the subclasses determine the type of the result and contain usable data.
 */
sealed class PocketResponse<T> {

    /**
     * A successful response from the Pocket API.
     *
     * @property data The data returned from the Pocket API.
     */
    data class Success<T> internal constructor(val data: T) : PocketResponse<T>()

    /**
     * A failure response from the Pocket API.
     */
    class Failure<T> internal constructor() : PocketResponse<T>()
}
