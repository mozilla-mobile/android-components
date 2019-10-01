package mozilla.components.feature.nearby;

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status
import java.nio.charset.StandardCharsets.UTF_8


/**
 * A connection to another device using the same class. This supports sending a single message at a
 * time in each direction. This does not have internal synchronization. It should be called from the
 * UI thread or externally synchronized.
 */
@UiThread
class NearbyConnection(
        private val context: Context,
        private val name: String,
        private val listener: NearbyConnectionListener
) {
    // Compile-time constants
    private val TAG = "NearbyConnection"
    private val PACKAGE_NAME = "mozilla.components.feature.nearby.NearbyConnection"
    private val STRATEGY = Strategy.P2P_STAR

    sealed class ConnectionState(val name: String) {
        object Isolated : ConnectionState("isolated")
        object Advertising : ConnectionState("advertising")
        object Discovering : ConnectionState("discovering")
        class Connecting(val neighborId: String, val neighborName: String) : ConnectionState("connecting")
        class ReadyToSend(val neighborEndpointId: String) : ConnectionState("ready-to-send")
        class Sending(val neighborEndpointId: String, val payloadId: Long) : ConnectionState("sending")
        class Failure(val message: String) : ConnectionState("failure")
    }

    private var connectionState: ConnectionState = ConnectionState.Isolated
    private var connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    init {
        listener.updateState(connectionState)
    }

    private fun updateState(cs: ConnectionState) {
        connectionState = cs
        listener.updateState(cs)
    }

    /**
     * Starts advertising this device. After calling this, the state will be updated to
     * [ConnectionState.Advertising] or [ConnectionState.Failure]. If all goes well, eventually
     * the state will be updated to [ConnectionState.Connecting].
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
     * eventually the state will be updated to [ConnectionState.Connecting].
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
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            updateState(ConnectionState.Connecting(endpointId, connectionInfo.endpointName))
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
        val PERMISSIONS = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        )
    }
}

class NearbyConnectionException(message: String) : Exception(message)

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
