/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.nearby

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status
import java.nio.charset.StandardCharsets.UTF_8
import androidx.appcompat.app.AlertDialog


/**
 * A class that can be run on two devices to allow them to connect. This supports sending a single
 * message at a time in each direction. This does not have internal synchronization. It should be
 * called from the UI thread or externally synchronized.
 *
 * @constructor Constructs a new connection, which will call [NearbyConnectionListener.updateState]
 *     with an argument of type [ConnectionState.Isolated]. No further action will be taken unless
 *     other methods are called by the client.
 * @param context context needed to initiate connection, used only at start
 * @param name name shown by this device to other devices
 * @param authenticate whether to authenticate the connection (true) or make it automatically (false)
 * @param listener listener to be notified of changes of state and message transmission
 *
 */
@UiThread
class NearbyConnection(
        private val context: Context,
        private val name: String,
        private val authenticate: Boolean,
        private val listener: NearbyConnectionListener
) {
    // Compile-time constants
    private val TAG = "NearbyConnection"
    private val PACKAGE_NAME = "mozilla.components.feature.nearby.NearbyConnection"
    private val STRATEGY = Strategy.P2P_STAR

    /**
     * The state of the connection. Changes in state are communicated through
     * [NearbyConnectionListener.updateState].
     *
     * @property name: an English-language name for the state
     */
    public sealed class ConnectionState(val name: String) {
        object Isolated : ConnectionState("isolated")
        object Advertising : ConnectionState("advertising")
        object Discovering : ConnectionState("discovering")
        class Authenticating(
                // sealed classes can't be inner, so we need to pass in the connection
                private val nearbyConnection: NearbyConnection,
                val neighborId: String,
                val neighborName: String,
                val token: String
        ) : ConnectionState("authenticating") {
            /**
             * Prompts the user to accept or reject the connection to [neighborName] with the
             * given [token]. Specifically, this shows an [androidx.appcompat.app.AlertDialog] with
             * the given [title] and [message] and calls [accept] or [reject] based on the user's
             * choice. This method is provided for convenience. Clients can implement their own
             * dialog or logic.
             *
             * @param context the context for the AlertDialog
             * @param title the title of the AlertDialog
             * @param message the message in the AlertDialog
             */
            fun showAuthenticationDialog(
                    context: Context,
                    title: String = "Accept connection to $neighborName",
                    message: String = "Confirm the code matches on both devices: $token")
            {
                AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(
                                "Accept") { _, _ ->
                            Nearby.getConnectionsClient(context)
                                    .acceptConnection(neighborId, nearbyConnection.payloadCallback)
                        }
                        .setNegativeButton(
                                android.R.string.cancel) { _, _ ->
                            Nearby.getConnectionsClient(context).rejectConnection(neighborId)
                        }
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
            }
            fun accept() {
                nearbyConnection.connectionsClient.acceptConnection(neighborId, nearbyConnection.payloadCallback)
                nearbyConnection.updateState(ConnectionState.Connecting(neighborId, neighborName))
            }
            fun reject() {
                nearbyConnection.connectionsClient.rejectConnection(neighborId)
                // This should put us back in advertising or discovering.
                nearbyConnection.updateState(nearbyConnection.connectionState)
            }
        }
        class Connecting(val neighborId: String, val neighborName: String) : ConnectionState("connecting")
        class ReadyToSend(val neighborEndpointId: String) : ConnectionState("ready-to-send")
        class Sending(val neighborEndpointId: String, val payloadId: Long) : ConnectionState("sending")
        class Failure(val message: String) : ConnectionState("failure")
    }

    private var connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private lateinit var connectionState: ConnectionState

    init {
        listener.updateState(ConnectionState.Isolated)
    }

    private fun updateState(cs: ConnectionState) {
        if (::connectionState.isInitialized) {
            Log.e(TAG, "Updating state from ${connectionState.name} to ${cs.name}")
        } else {
            Log.e(TAG, "Initializing state to ${cs.name}")
        }
        connectionState = cs
        listener.updateState(cs)
    }

    /**
     * Starts advertising this device. After calling this, the state will be updated to
     * [ConnectionState.Advertising] or [ConnectionState.Failure]. If all goes well, eventually
     * the state will be updated to [ConnectionState.Authenticating] (if [authenticate] is true)
     * or [ConnectionState.Connecting].
     */
    fun startAdvertising() {
        connectionsClient.startAdvertising(
                name,
                PACKAGE_NAME,
                connectionLifecycleCallback,
                AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        ).addOnSuccessListener {
            updateState(ConnectionState.Advertising)
        }.addOnFailureListener {
            Log.e(TAG, "failed to start advertising: ${it.toString()}")
            updateState(ConnectionState.Failure(it.toString()))
        }
    }

    /**
     * Starts trying to discover nearby advertising devices. After calling this, the state will
     * be updated to [ConnectionState.Discovering] or [ConnectionState.Failure]. If all goes well,
     * eventually the state will be updated to [ConnectionState.Authenticating] (if [authenticate]
     * is true) or [ConnectionState.Connecting].
     */
    fun startDiscovering() {
        connectionsClient.startDiscovery(
                PACKAGE_NAME, endpointDiscoveryCallback,
                DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
                .addOnSuccessListener {
                    updateState(ConnectionState.Discovering)
                }.addOnFailureListener {
                    Log.e(TAG, "failed to start discovering: ${it.toString()}")
                    updateState(ConnectionState.Failure(it.toString()))
                }
    }

    // Discovery
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            connectionsClient.requestConnection(name, endpointId, connectionLifecycleCallback)
            updateState(ConnectionState.Connecting(endpointId, info.endpointName))
        }

        override fun onEndpointLost(endpointId: String) {
            Log.e(TAG, "Lost endpoint during discovery.")
            updateState(ConnectionState.Discovering)
        }
    }

    // Used within startAdvertising() and startDiscovering() (via endpointDiscoveryCallback)
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            if (authenticate) {
                updateState(
                        ConnectionState.Authenticating(
                                this@NearbyConnection,
                                endpointId,
                                connectionInfo.endpointName,
                                connectionInfo.authenticationToken
                        )
                )
            } else {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                updateState(ConnectionState.Connecting(endpointId, connectionInfo.endpointName))
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectionsClient.stopDiscovery()
                connectionsClient.stopAdvertising()
                // Should we copy the endpoint name from the previous Connecting state?
                updateState(ConnectionState.ReadyToSend(endpointId))
            } else {
                Log.e(TAG, "onConnectionResult: connection failed with status ${result.status}")
                // Could keep trying.
                updateState(ConnectionState.Failure("onConnectionResult: connection failed with status ${result.status}"))
            }
        }

        override fun onDisconnected(endpointId: String) {
            updateState(ConnectionState.Isolated)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            listener.receiveMessage(endpointId, String(payload.asBytes()!!, UTF_8))
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == Status.SUCCESS) {
                // Make local variable so compiler knows it won't be changed by another thread.
                val state = connectionState
                if (state is ConnectionState.Sending) {
                    // Make sure it's reporting on our outgoing message, not an incoming one.
                    if (state.payloadId == update.payloadId) {
                        listener.messageDelivered(update.payloadId)
                        updateState(ConnectionState.ReadyToSend(endpointId))
                    }
                }
            }
        }
    }

    /**
     * Sends a message to a connected device. If the current state is not
     * [ConnectionState.ReadyToSend], the message will not be sent.
     *
     * @param message the message to send
     * @return an id that will be later passed back through
     *   [NearbyConnectionListener.messageDelivered], or null if the message could not be sent
     */
    fun sendMessage(message: String): Long? {
        val state = connectionState
        if (state is ConnectionState.ReadyToSend) {
            val payload: Payload = Payload.fromBytes(message.toByteArray(UTF_8))
            connectionsClient.sendPayload(state.neighborEndpointId, payload)
            updateState(ConnectionState.Sending(state.neighborEndpointId, payload.id))
            return payload.id
        }
        return null
    }

    /**
     * Breaks any connections to neighboring devices. This also stops advertising and
     * discovering. The state will be updated to [ConnectionState.Isolated].
     */
    fun disconnect() {
        connectionsClient.stopAllEndpoints() // also stops advertising and discovery
        updateState(ConnectionState.Isolated)
    }

    companion object {
        /**
         * The permissions needed by [NearbyConnection]. It is the client's responsibility
         * to ensure that all are granted before constructing an instance of this class.
         */
        val PERMISSIONS: Array<String> = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        )
    }
}

/**
 * Interface definition for listening to changes in a [NearbyConnection].
 */
interface NearbyConnectionListener {
    /**
     * Called whenever the connection's state is set. In the absence of failures, the
     * new state should differ from the prior state, but that is not guaranteed.
     *
     * @param connectionState the current state
     */
    fun updateState(connectionState: NearbyConnection.ConnectionState)

    /**
     * Called when a message is received from a neighboring device.
     *
     * @param endpointId the ID of the neighboring device
     * @param message the message
     */
    fun receiveMessage(endpointId: String, message: String)

    /**
     * Called when a message has been successfully delivered to a neighboring device.
     */
    fun messageDelivered(payloadId: Long)
}
