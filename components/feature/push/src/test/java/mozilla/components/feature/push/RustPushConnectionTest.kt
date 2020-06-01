/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.push

import kotlinx.coroutines.runBlocking
import mozilla.appservices.push.DispatchInfo
import mozilla.appservices.push.KeyInfo
import mozilla.appservices.push.PushAPI
import mozilla.components.support.test.any
import mozilla.appservices.push.SubscriptionInfo
import mozilla.appservices.push.SubscriptionResponse
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.nullable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class RustPushConnectionTest {

    @Ignore("Requires push-forUnitTests; seems unnecessary to introduce it for this one test.")
    @Test
    fun `new token initializes API`() {
        val connection = createConnection()

        assertNull(connection.api)

        runBlocking {
            connection.updateToken("token")
        }

        assertNotNull(connection.api)
    }

    @Test
    fun `new token calls update if API is already initialized`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        connection.api = api

        runBlocking {
            connection.updateToken("123")
        }

        verify(api, never()).subscribe(any(), any(), any())
        verify(api).update(anyString())
    }

    @Test(expected = IllegalStateException::class)
    fun `subscribe throws if API is not initialized first`() {
        val connection = createConnection()

        runBlocking {
            connection.subscribe("123")
        }
    }

    @Test
    fun `subscribe calls Rust API`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        val response = SubscriptionResponse(
            channelID = "1234",
            subscriptionInfo = SubscriptionInfo(
                endpoint = "https://foo",
                keys = KeyInfo(
                    auth = "auth",
                    p256dh = "p256dh"
                )
            )
        )

        connection.api = api

        `when`(api.subscribe(anyString(), anyString(), nullable())).thenReturn(response)

        runBlocking {
            val sub = connection.subscribe("123")

            assertEquals("123", sub.scope)
            assertEquals("auth", sub.authKey)
            assertEquals("p256dh", sub.publicKey)
            assertEquals("https://foo", sub.endpoint)
        }

        verify(api).subscribe(anyString(), anyString(), nullable())
    }

    @Test(expected = IllegalStateException::class)
    fun `unsubscribe throws if API is not initialized first`() {
        val connection = createConnection()

        runBlocking {
            connection.unsubscribe("123")
        }
    }

    @Test
    fun `unsubscribe calls Rust API`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        connection.api = api

        runBlocking {
            connection.unsubscribe("123")
        }

        verify(api).unsubscribe(anyString())
    }

    @Test(expected = IllegalStateException::class)
    fun `unsubscribeAll throws if API is not initialized first`() {
        val connection = createConnection()

        runBlocking {
            connection.unsubscribeAll()
        }
    }

    @Test
    fun `unsubscribeAll calls Rust API`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        connection.api = api

        runBlocking {
            connection.unsubscribeAll()
        }

        verify(api).unsubscribeAll()
    }

    @Test
    fun `containsSubscription returns true if a subscription exists`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        connection.api = api

        `when`(api.dispatchInfoForChid(ArgumentMatchers.anyString()))
            .thenReturn(mock())
            .thenReturn(null)

        runBlocking {
            assertTrue(connection.containsSubscription("validSubscription"))

            assertFalse(connection.containsSubscription("invalidSubscription"))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `verifyConnection throws if API is not initialized first`() {
        val connection = createConnection()

        runBlocking {
            connection.verifyConnection()
        }
    }

    @Test
    fun `verifyConnection calls Rust API`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        connection.api = api

        runBlocking {
            connection.verifyConnection()
        }

        verify(api).verifyConnection()
    }

    @Test(expected = IllegalStateException::class)
    fun `decrypt throws if API is not initialized first`() {
        val connection = createConnection()

        runBlocking {
            connection.decryptMessage("123", "plain text")
        }
    }

    @Test
    fun `decrypt calls Rust API`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        val dispatchInfo: DispatchInfo = mock()
        connection.api = api

        runBlocking {
            connection.decryptMessage("123", "body")
        }

        verify(api, never()).decrypt(anyString(), anyString(), eq(""), eq(""), eq(""))

        `when`(api.dispatchInfoForChid(anyString())).thenReturn(dispatchInfo)
        `when`(dispatchInfo.scope).thenReturn("test")

        runBlocking {
            connection.decryptMessage("123", "body")
        }

        verify(api).decrypt(anyString(), anyString(), eq(""), eq(""), eq(""))

        runBlocking {
            connection.decryptMessage("123", "body", "enc", "salt", "key")
        }

        verify(api).decrypt(anyString(), anyString(), eq("enc"), eq("salt"), eq("key"))
    }

    @Test
    fun `empty body decrypts nothing`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        val dispatchInfo: DispatchInfo = mock()
        connection.api = api

        runBlocking {
            connection.decryptMessage("123", null)
        }

        verify(api, never()).decrypt(anyString(), anyString(), eq(""), eq(""), eq(""))

        `when`(api.dispatchInfoForChid(anyString())).thenReturn(dispatchInfo)
        `when`(dispatchInfo.scope).thenReturn("test")

        runBlocking {
            val (scope, message) = connection.decryptMessage("123", null)!!
            assertEquals("test", scope)
            assertNull(message)
        }

        verify(api, never()).decrypt(anyString(), nullable(), eq(""), eq(""), eq(""))
    }

    @Test(expected = IllegalStateException::class)
    fun `close throws if API is not initialized first`() {
        val connection = createConnection()

        runBlocking {
            connection.close()
        }
    }

    @Test
    fun `close calls Rust API`() {
        val connection = createConnection()
        val api: PushAPI = mock()
        connection.api = api

        runBlocking {
            connection.close()
        }

        verify(api).close()
    }

    @Test
    fun `initialized is true when api is not null`() {
        val connection = createConnection()

        assertFalse(connection.isInitialized())

        connection.api = mock()

        assertTrue(connection.isInitialized())
    }

    private fun createConnection() = RustPushConnection(
        "/sdcard/",
        "push-test",
        "push.mozilla.com",
        Protocol.HTTPS,
        ServiceType.FCM
    )
}
