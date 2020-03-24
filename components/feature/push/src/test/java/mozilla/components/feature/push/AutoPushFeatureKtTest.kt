/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.appservices.push.AlreadyRegisteredError
import mozilla.appservices.push.CommunicationError
import mozilla.appservices.push.CommunicationServerError
import mozilla.appservices.push.CryptoError
import mozilla.appservices.push.GeneralError
import mozilla.appservices.push.InternalPanic
import mozilla.appservices.push.MissingRegistrationTokenError
import mozilla.appservices.push.RecordNotFoundError
import mozilla.appservices.push.StorageError
import mozilla.appservices.push.StorageSqlError
import mozilla.appservices.push.TranscodingError
import mozilla.appservices.push.UrlParseError
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
@Suppress("Deprecation")
class AutoPushFeatureKtTest {

    @Test
    fun `asserts PushConfig's default values`() {
        val config = PushConfig("sample-browser")
        assertEquals("sample-browser", config.senderId)
        assertEquals("updates.push.services.mozilla.com", config.serverHost)
        assertEquals(Protocol.HTTPS, config.protocol)
        assertEquals(ServiceType.FCM, config.serviceType)

        val config2 = PushConfig("sample-browser", "push.test.mozilla.com", Protocol.HTTP, ServiceType.ADM)
        assertEquals("sample-browser", config2.senderId)
        assertEquals("push.test.mozilla.com", config2.serverHost)
        assertEquals(Protocol.HTTP, config2.protocol)
        assertEquals(ServiceType.ADM, config2.serviceType)
    }

    @Test(expected = InternalPanic::class)
    fun `launchAndTry throws on unrecoverable Rust exceptions`() = runBlockingTest {
        CoroutineScope(coroutineContext).launchAndTry({ throw InternalPanic("unit test") }, { assert(false) })
    }

    @Test
    fun `launchAndTry should NOT throw on recoverable Rust exceptions`() = runBlockingTest {
        CoroutineScope(coroutineContext).launchAndTry(
            { throw CryptoError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw CommunicationServerError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw CommunicationError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw AlreadyRegisteredError() },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw StorageError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw MissingRegistrationTokenError() },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw StorageSqlError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw TranscodingError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw RecordNotFoundError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw UrlParseError("should not fail test") },
            { assert(true) }
        )

        CoroutineScope(coroutineContext).launchAndTry(
            { throw GeneralError("should not fail test") },
            { assert(true) }
        )
    }
}
