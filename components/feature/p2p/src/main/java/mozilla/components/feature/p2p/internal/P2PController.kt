/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.internal

import android.os.Build
import mozilla.components.browser.state.state.SessionState
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
    private var session: SessionState? = null
    private lateinit var nearbyConnection: NearbyConnection
    private var savedConnectionState: ConnectionState? = null

    fun start() {
        view.listener = this
        nearbyConnection = NearbyConnection(
            view.asView().context,
            Build.MODEL,
            true,
            object : NearbyConnectionListener {
                override fun updateState(connectionState: ConnectionState) {
                    savedConnectionState = connectionState
                    view.updateStatus(connectionState.name)
                    when (connectionState) {
                        is ConnectionState.Authenticating -> view.authenticate(
                            connectionState.neighborId,
                            connectionState.neighborName,
                            connectionState.token)
                        is ConnectionState.ReadyToSend -> view.readyToSend()
                        is ConnectionState.Failure -> view.failure(connectionState.message)
                        is ConnectionState.Isolated -> view.clear()
                    }
                }

                override fun messageDelivered(payloadId: Long) {
                    // For now, do nothing.
                }

                override fun receiveMessage(endpointId: String, message: String) {
                    view.receiveURL(endpointId, message)
                }
            }
        )
    }

    fun stop() {
        if (::nearbyConnection.isInitialized) {
            nearbyConnection.disconnect()
        }
        view.listener = null
    }

    fun bind(session: SessionState) {
        this.session = session
    }

    // P2PView.Listener implementation

    override fun onAdvertise() {
        nearbyConnection.startAdvertising()
    }

    override fun onDiscover() {
        nearbyConnection.startDiscovering()
    }

    private fun reportError(msg: String) {
        Logger.error(msg)
        view.failure(msg)
    }

    inline fun <reified T : ConnectionState> cast() =
        (savedConnectionState as? T).also {
            if (it == null) {
                reportError("savedConnection was expected to be type ${T::class} but is $savedConnectionState")
            }
        }

    override fun onAccept(token: String) {
        cast<ConnectionState.Authenticating>()?.accept()
    }

    override fun onReject(token: String) {
        cast<ConnectionState.Authenticating>()?.reject()
    }

    override fun onSendUrl() {
        if (cast<ConnectionState.ReadyToSend>() != null) {
            val payloadID = nearbyConnection.sendMessage(session?.content?.url ?: "no URL")
            if (payloadID == null) {
                reportError("Unable to send message: sendMessage() returns null")
            }
        }
    }

    override fun onSetUrl(url: String) {
        session?.engineState?.engineSession?.loadUrl(url)
    }

    override fun onReset() {
        nearbyConnection.disconnect()
    }

    fun unbind() {
        session = null
    }
}
