/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.concept.push

import android.content.Context

/**
 * Implemented by push services like Firebase Cloud Messaging and Amazon Device Messaging SDKs to allow
 * the [PushProcessor] to manage their lifecycle.
 */
interface PushService {

    /**
     * Starts the push service.
     */
    fun start()

    /**
     * Stops the push service.
     */
    fun stop()

    /**
     * Forces the push service to renew it's registration. This may lead to a new registration token being received.
     */
    fun forceRegistrationRenewal()

    /**
     * Checks if the messaging service is available to use.
     */
    fun isAvailable(context: Context): Boolean
}
