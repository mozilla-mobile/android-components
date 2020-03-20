/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons

/**
 * A contract that indicate how an add-on provider must behave.
 */
interface AddonsProvider {

    /**
     * Provides a list of all available add-ons.
     *
     * @param allowCache whether or not the result may be provided
     * from a previously cached response, defaults to true.
     */
    suspend fun getAvailableAddons(allowCache: Boolean = true): List<Addon>
}
