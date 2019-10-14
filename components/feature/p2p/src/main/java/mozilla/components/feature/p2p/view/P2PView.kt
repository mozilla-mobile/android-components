/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.view

import android.view.View

/**
 * An interface for views that can display the peer-to-peer communication toolbar.
 */
interface P2PView {
    /**
     * Listener to be invoked after the user performs certain actions, such as initiating or
     * accepting a connection.
     */
    var listener: Listener?

    /**
     * Optionally displays the status of the connection. The implementation should not take other
     * actions based on the status, since the values of the strings could change.
     *
     * @param status the current status
     */
    fun updateStatus(status: String)

    /**
     * Asks user whether they wish to connection to another device having the specified connection
     * information. It is highly recommended that the [token] is displayed, since it uniquely
     * identifies the connection.
     *
     * @param neighborId a machine-generated ID uniquely identifying the other device
     * @param neighborName a human-readable name of the other device
     * @param token a short string of characters shared between the two devices
     */
    fun authenticate(neighborId: String, neighborName: String, token: String)

    /**
     * Clears the UI state.
     */
    fun clear()

    /**
     * Enables the buttons. Make sure [listener] is initialized before calling this.
     */
    fun reset()

    /**
     * Casts this [P2PView] interface to an actual Android [View] object.
     */
    fun asView(): View = (this as View)

    fun readyToSend()

    fun receiveURL(neighborId: String, message: String)

    /**
     * An interface enabling the [P2PView] to make requests of a controller.
     */
    interface Listener {
        /**
         * Handles a request to advertise the device's presence and desire to connect.
         */
        fun onAdvertise()

        /**
         * Handles a request to discovery other devices wishing to connect.
         */
        fun onDiscover()

        /**
         * Handles a decision to accept the connection specified by the given token. The value
         * of the token is what was passed to [authenticate].
         *
         * @param token a short string uniquely identifying a connection between two devices
         */
        fun onAccept(token: String)

        /**
         * Handles a decision to reject the connection specified by the given token. The value
         * of the token is what was passed to [authenticate].
         *
         * @param token a short string uniquely identifying a connection between two devices
         */
        fun onReject(token: String)

        /**
         * Handles a request to send the current page's URL to the neighbor.
         */
        fun onSendUrl()

        /**
         * Handles a request to set the current page's URL to the given value.
         * This will typically be one sent from a neighbor.
         *
         * @param url the URL
         */
        fun onSetUrl(url: String)

        fun onClose()
    }
}
