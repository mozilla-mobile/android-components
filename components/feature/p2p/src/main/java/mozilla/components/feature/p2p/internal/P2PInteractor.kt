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
import mozilla.components.lib.nearby.NearbyConnectionListener
import mozilla.components.support.base.log.logger.Logger

/**
 * Interactor that implements [P2PView.Listener] and notifies the engine or feature about actions the user
 * performed (such as receiving a URL from another device).
 */
internal class P2PInteractor(
    private val feature: P2PFeature,
    private val view: P2PView
) : P2PView.Listener {
    private var engineSession: EngineSession? = null
    private lateinit var nearbyConnection: NearbyConnection

    fun start() {
        Logger.error("In P2PInteractor.start(), about to initialize listener")
        view.listener = this
        nearbyConnection = NearbyConnection(
            view.asView().context,
            Build.MODEL,
            true,
            object : NearbyConnectionListener {
                override fun updateState(connectionState: NearbyConnection.ConnectionState) {
                    Logger.error("In updateState()")
                    view.updateStatus(connectionState.name)
                }

                override fun messageDelivered(payloadId: Long) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun receiveMessage(endpointId: String, message: String) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun onAdvertise() {
        Logger.error("Calling startAdvertising()")
        nearbyConnection.startAdvertising()
    }

    override fun onDiscover() {
        nearbyConnection.startDiscovering()
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
