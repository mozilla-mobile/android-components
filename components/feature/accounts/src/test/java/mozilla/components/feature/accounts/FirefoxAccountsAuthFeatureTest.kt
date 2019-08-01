/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.accounts

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.ServerConfig
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.service.fxa.DeviceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

// Same as the actual account manager, except we get to control how FirefoxAccountShaped instances
// are created. This is necessary because due to some build issues (native dependencies not available
// within the test environment) we can't use fxaclient supplied implementation of FirefoxAccountShaped.
// Instead, we express all of our account-related operations over an interface.
class TestableFxaAccountManager(
    context: Context,
    config: ServerConfig,
    scopes: Set<String>,
    val block: () -> OAuthAccount = { mock() }
) : FxaAccountManager(context, config, DeviceConfig("test", DeviceType.MOBILE, setOf()), null, scopes) {

    override fun createAccount(config: ServerConfig): OAuthAccount {
        return block()
    }
}

@RunWith(AndroidJUnit4::class)
class FirefoxAccountsAuthFeatureTest {

    @Test
    fun `begin authentication`() {
        val manager = prepareAccountManagerForSuccessfulAuthentication()
        var path = ""
        var wasCalled = false
        val onBeginAuthentication: (String) -> Unit = {
            path = it
            wasCalled = true
        }

        runBlocking {
            val feature = FirefoxAccountsAuthFeature(
                manager,
                "somePath",
                this.coroutineContext,
                onBeginAuthentication
            )
            feature.beginAuthentication()
        }

        assertTrue(wasCalled)
        assertEquals("auth://url", path)
    }

    @Test
    fun `begin pairing authentication`() {
        val manager = prepareAccountManagerForSuccessfulAuthentication()
        var path = ""
        var wasCalled = false
        val onBeginAuthentication: (String) -> Unit = {
            path = it
            wasCalled = true
        }

        runBlocking {
            val feature = FirefoxAccountsAuthFeature(
                manager,
                "somePath",
                this.coroutineContext,
                onBeginAuthentication
            )
            feature.beginPairingAuthentication("auth://pair")
        }

        assertTrue(wasCalled)
        assertEquals("auth://url", path)
    }

    @Test
    fun `begin authentication with errors`() {
        val manager = prepareAccountManagerForFailedAuthentication()
        var path = ""
        var wasCalled = false
        val onBeginAuthentication: (String) -> Unit = {
            path = it
            wasCalled = true
        }

        runBlocking {
            val feature = FirefoxAccountsAuthFeature(
                manager,
                "somePath",
                this.coroutineContext,
                onBeginAuthentication
            )
            feature.beginAuthentication()
        }

        // Fallback url is invoked.
        assertTrue(wasCalled)
        assertEquals("https://accounts.firefox.com/signin", path)
    }

    @Test
    fun `begin pairing authentication with errors`() {
        val manager = prepareAccountManagerForFailedAuthentication()
        var path = ""
        var wasCalled = false
        val onBeginAuthentication: (String) -> Unit = {
            path = it
            wasCalled = true
        }

        runBlocking {
            val feature = FirefoxAccountsAuthFeature(
                manager,
                "somePath",
                this.coroutineContext,
                onBeginAuthentication
            )
            feature.beginPairingAuthentication("auth://pair")
        }

        // Fallback url is invoked.
        assertTrue(wasCalled)
        assertEquals("https://accounts.firefox.com/signin", path)
    }

    private fun prepareAccountManagerForSuccessfulAuthentication(): TestableFxaAccountManager {
        val mockAccount: OAuthAccount = mock()
        val profile = Profile(uid = "testUID", avatar = null, email = "test@example.com", displayName = "test profile")

        `when`(mockAccount.getProfileAsync(anyBoolean())).thenReturn(CompletableDeferred(profile))
        `when`(mockAccount.beginOAuthFlowAsync(any(), anyBoolean())).thenReturn(CompletableDeferred("auth://url"))
        `when`(mockAccount.beginPairingFlowAsync(anyString(), any())).thenReturn(CompletableDeferred("auth://url"))
        `when`(mockAccount.completeOAuthFlowAsync(anyString(), anyString())).thenReturn(CompletableDeferred(true))

        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig.release("dummyId", "bad://url"),
            setOf("test-scope")
        ) {
            mockAccount
        }

        runBlocking {
            manager.initAsync().await()
        }

        return manager
    }

    private fun prepareAccountManagerForFailedAuthentication(): TestableFxaAccountManager {
        val mockAccount: OAuthAccount = mock()
        val profile = Profile(uid = "testUID", avatar = null, email = "test@example.com", displayName = "test profile")

        `when`(mockAccount.getProfileAsync(anyBoolean())).thenReturn(CompletableDeferred(profile))

        `when`(mockAccount.beginOAuthFlowAsync(any(), anyBoolean())).thenReturn(CompletableDeferred(value = null))
        `when`(mockAccount.beginPairingFlowAsync(anyString(), any())).thenReturn(CompletableDeferred(value = null))
        `when`(mockAccount.completeOAuthFlowAsync(anyString(), anyString())).thenReturn(CompletableDeferred(true))

        val manager = TestableFxaAccountManager(
            testContext,
            ServerConfig.release("dummyId", "bad://url"),
            setOf("test-scope")
        ) {
            mockAccount
        }

        runBlocking {
            manager.initAsync().await()
        }

        return manager
    }
}