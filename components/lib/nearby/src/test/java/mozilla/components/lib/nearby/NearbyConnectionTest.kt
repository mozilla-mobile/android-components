/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.nearby

import com.google.android.gms.nearby.connection.ConnectionsClient
import mozilla.components.support.test.any
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NearbyConnectionTest {
    private val DEVICE_NAME = "NAME"
    @Mock
    private lateinit var mockConnectionsClient: ConnectionsClient
    @Mock
    private lateinit var mockNearbyConnectionObserver: NearbyConnectionObserver

    private var state: NearbyConnection.ConnectionState? = null // mutated by stateWatchingObserver
    private lateinit var stateWatchingObserver: NearbyConnectionObserver

    @Before
    fun setup() {
        // Must be done in method to ensure mockNearbyConnectionObserver has been initialized.
        stateWatchingObserver = object : NearbyConnectionObserver by mockNearbyConnectionObserver {
            override fun onStateUpdated(connectionState: NearbyConnection.ConnectionState) {
                state = connectionState
            }
        }
        state = null
    }

    @Test
    fun `Should make initial onStatusUpdated() call`() {
        val nearbyConnection = NearbyConnection(mockConnectionsClient, DEVICE_NAME)
        nearbyConnection.register(stateWatchingObserver)
        assertEquals(NearbyConnection.ConnectionState.Isolated, state)
    }

    @Test
    fun `Should enter advertising state if startAdvertising() succeeds`() {
        val client = Mockito.mock(ConnectionsClient::class.java)
        `when`(client.startAdvertising(anyString(), anyString(), any(), any()))
            .thenReturn(VoidTask.SUCCESS)
        val nearbyConnection = NearbyConnection(client, DEVICE_NAME)
        nearbyConnection.register(stateWatchingObserver)
        assertEquals(NearbyConnection.ConnectionState.Isolated.name, state?.name)
        nearbyConnection.startAdvertising()
        assertEquals(NearbyConnection.ConnectionState.Advertising.name, state?.name)
    }

    @Test
    fun `Should enter failure state if startAdvertising() fails`() {
        val client = Mockito.mock(ConnectionsClient::class.java)
        `when`(client.startAdvertising(anyString(), anyString(), any(), any()))
            .thenReturn(VoidTask.FAILURE)
        val nearbyConnection = NearbyConnection(client, DEVICE_NAME)
        nearbyConnection.register(stateWatchingObserver)
        assertEquals(NearbyConnection.ConnectionState.Isolated.name, state?.name)
        nearbyConnection.startAdvertising()
        assertEquals("Failure", state?.name)
    }

    @Test
    fun `Should enter discovering state if startDiscovery() succeeds`() {
        val client = Mockito.mock(ConnectionsClient::class.java)
        `when`(client.startDiscovery(anyString(), any(), any()))
            .thenReturn(VoidTask.SUCCESS)
        val nearbyConnection = NearbyConnection(client, DEVICE_NAME)
        nearbyConnection.register(stateWatchingObserver)
        assertEquals(NearbyConnection.ConnectionState.Isolated.name, state?.name)
        nearbyConnection.startDiscovering()
        assertEquals(NearbyConnection.ConnectionState.Discovering.name, state?.name)
    }

    @Test
    fun `Should enter discovering state if startDiscovery() fails`() {
        val client = Mockito.mock(ConnectionsClient::class.java)
        `when`(client.startDiscovery(anyString(), any(), any()))
            .thenReturn(VoidTask.FAILURE)
        val nearbyConnection = NearbyConnection(client, DEVICE_NAME)
        nearbyConnection.register(stateWatchingObserver)
        assertEquals(NearbyConnection.ConnectionState.Isolated.name, state?.name)
        nearbyConnection.startDiscovering()
        assertEquals("Failure", state?.name)
    }
}
