/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.nearby

import com.google.android.gms.nearby.connection.ConnectionsClient
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NearbyConnectionTest {
    private val DEVICE_NAME = "NAME"
    @Mock
    private lateinit var mockConnectionsClient: ConnectionsClient
    @Mock
    private lateinit var mockNearbyConnectionObserver: NearbyConnectionObserver

    @Test
    fun `Should make initial onStatusUpdated() call`() {
        val nearbyConnection = NearbyConnection(mockConnectionsClient, DEVICE_NAME)
        var state: NearbyConnection.ConnectionState? = null
        nearbyConnection.register(
                object : NearbyConnectionObserver by mockNearbyConnectionObserver {
                    override fun onStateUpdated(connectionState: NearbyConnection.ConnectionState) {
                        state = connectionState
                    }
                })
        assertEquals(NearbyConnection.ConnectionState.Isolated, state)
    }
}
