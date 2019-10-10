/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.internal

import android.os.Build
import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.p2p.P2PFeature
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.lib.nearby.NearbyConnection.ConnectionState
import mozilla.components.lib.nearby.NearbyConnectionListener
import mozilla.components.support.base.log.logger.Logger

/**
 * Controller that mediates between [P2PView] and [NearbyConnection].
 */
internal class P2PController(
    private val feature: P2PFeature,
    private val view: P2PView
) : P2PView.Listener {
    private var engineSession: EngineSession? = null
    private lateinit var nearbyConnection: NearbyConnection
    private var savedConnectionState: ConnectionState.Authenticating? = null

    fun start() {
        Logger.error("In P2PController.start(), about to initialize listener")
        view.listener = this
        view.enable() // the listener must be assigned before calling enable()
        nearbyConnection = NearbyConnection(
            view.asView().context,
            Build.MODEL,
            true,
            object : NearbyConnectionListener {
                override fun updateState(connectionState: ConnectionState) {
                    view.updateStatus(connectionState.name)
                    if (connectionState is ConnectionState.Authenticating) {
                        // I need to store this state for use in accept() and reject() below.
                        // Should I make it a map from String (the token) to ConnectionState.Authenticating?
                        require(savedConnectionState == null)
                        savedConnectionState = connectionState
                        view.authenticate(connectionState.neighborId, connectionState.neighborName, connectionState.token)
                    }
                }

                override fun messageDelivered(payloadId: Long) {
                    TODO("not implemented")
                }

                override fun receiveMessage(endpointId: String, message: String) {
                    TODO("not implemented")
                }
            }
        )
    }

    fun stop() {
        view.listener = null
    }

    fun bind(session: SessionState) {
        engineSession = session.engineState.engineSession
    }

    // P2PView.Listener implementation

    override fun onAdvertise() {
        nearbyConnection.startAdvertising()
    }

    override fun onDiscover() {
        nearbyConnection.startDiscovering()
    }

    override fun onAccept(token: String) {
        savedConnectionState!!.accept()
        savedConnectionState = null
    }

    override fun onReject(token: String) {
        savedConnectionState!!.reject()
        savedConnectionState = null
    }

    override fun onClose() {
        // We pass this event up to the feature. The feature is responsible for unbinding its sub components and
        // potentially notifying other dependencies.
        feature.unbind()
    }

    fun unbind() {
        engineSession?.clearFindMatches()
        engineSession = null
    }
}
