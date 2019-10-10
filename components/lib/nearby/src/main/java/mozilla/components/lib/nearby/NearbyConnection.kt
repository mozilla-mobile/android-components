/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.nearby

import android.Manifest
import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status
import com.google.android.gms.nearby.connection.Strategy
import mozilla.components.support.base.log.logger.Logger
import java.nio.charset.StandardCharsets.UTF_8

/**
 * A class that can be run on two devices to allow them to connect. This supports sending a single
 * message at a time in each direction. It contains internal synchronization and may be accessed
 * from any thread
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
class NearbyConnection(
    private val context: Context,
    private val name: String,
    private val authenticate: Boolean,
    private val listener: NearbyConnectionListener
) {
    // Compile-time constants
    private val TAG = "NearbyConnection"
    private val PACKAGE_NAME = "mozilla.components.lib.nearby"
    private val STRATEGY = Strategy.P2P_STAR

    /**
     * The state of the connection. Changes in state are communicated through
     * [NearbyConnectionListener.updateState].
     */
    public sealed class ConnectionState() {
        val name = javaClass.simpleName
        object Isolated : ConnectionState()
        object Advertising : ConnectionState()
        object Discovering : ConnectionState()
        class Authenticating(
            // sealed classes can't be inner, so we need to pass in the connection
            private val nearbyConnection: NearbyConnection,
            val neighborId: String,
            val neighborName: String,
            val token: String
        ) : ConnectionState() {
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
        class Connecting(val neighborId: String, val neighborName: String) : ConnectionState()
        class ReadyToSend(val neighborEndpointId: String) : ConnectionState()
        class Sending(val neighborEndpointId: String, val payloadId: Long) : ConnectionState()
        class Failure(val message: String) : ConnectionState()
    }

    private var connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    // The can be modified in both the main thread and in callbacks. Modification occurs
    // only in updateState(), which is synchronized.
    private lateinit var connectionState: ConnectionState

    init {
        listener.updateState(ConnectionState.Isolated)
    }

    // This method is called from both the main thread and callbacks.
    @Synchronized private fun updateState(cs: ConnectionState) {
        connectionState = cs
        listener.updateState(cs)
    }

    /**
     * Starts advertising this device. After calling this, the state will be updated to
     * [ConnectionState.Advertising] or [ConnectionState.Failure]. If all goes well, eventually
     * the state will be updated to [ConnectionState.Authenticating] (if [authenticate] is true)
     * or [ConnectionState.Connecting]. A client should call either [startAdvertising] or
     * [startDiscovering] to make a connection, not both.
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
            Logger.error("failed to start advertising: $it")
            updateState(ConnectionState.Failure(it.toString()))
        }
    }

    /**
     * Starts trying to discover nearby advertising devices. After calling this, the state will
     * be updated to [ConnectionState.Discovering] or [ConnectionState.Failure]. If all goes well,
     * eventually the state will be updated to [ConnectionState.Authenticating] (if [authenticate]
     * is true) or [ConnectionState.Connecting]. A client should call either [startAdvertising] or
     * [startDiscovering] to make a connection, not both.
     */
    fun startDiscovering() {
        connectionsClient.startDiscovery(
            PACKAGE_NAME, endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build())
            .addOnSuccessListener {
                updateState(ConnectionState.Discovering)
            }.addOnFailureListener {
                Logger.error("failed to start discovering: $it")
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
            Logger.error("Lost endpoint during discovery")
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
                updateState(ConnectionState.ReadyToSend(endpointId))
            } else {
                Logger.error("onConnectionResult: connection failed with status ${result.status}")
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
