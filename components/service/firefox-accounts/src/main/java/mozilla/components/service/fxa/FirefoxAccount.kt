/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import android.net.Uri
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import mozilla.appservices.fxaclient.FirefoxAccount as InternalFxAcct
import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.components.concept.sync.AccessType
import mozilla.components.concept.sync.AuthFlowUrl
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.StatePersistenceCallback
import mozilla.components.support.base.log.logger.Logger

typealias PersistCallback = mozilla.appservices.fxaclient.FirefoxAccount.PersistCallback

/**
 * FirefoxAccount represents the authentication state of a client.
 */
@Suppress("TooManyFunctions")
class FirefoxAccount internal constructor(
    private val inner: InternalFxAcct
) : OAuthAccount {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO) + job

    private val logger = Logger("FirefoxAccount")

    /**
     * Why this exists: in the `init` block below you'll notice that we register a persistence callback
     * as soon as we initialize this object. Essentially, we _always_ have a persistence callback
     * registered with [InternalFxAcct]. However, our own lifecycle is such that we will not know
     * how to actually persist account state until sometime after this object has been created.
     * Currently, we're expecting [FxaAccountManager] to configure a real callback.
     * This wrapper exists to facilitate that flow of events.
     */
    private class WrappingPersistenceCallback : PersistCallback {
        private val logger = Logger("WrappingPersistenceCallback")
        @Volatile
        private var persistenceCallback: StatePersistenceCallback? = null

        fun setCallback(callback: StatePersistenceCallback) {
            logger.debug("Setting persistence callback")
            persistenceCallback = callback
        }

        override fun persist(data: String) {
            val callback = persistenceCallback

            if (callback == null) {
                logger.warn("InternalFxAcct tried persist state, but persistence callback is not set")
            } else {
                logger.debug("Logging state to $callback")
                callback.persist(data)
            }
        }
    }

    private var persistCallback = WrappingPersistenceCallback()
    private val deviceConstellation = FxaDeviceConstellation(inner, scope)

    init {
        inner.registerPersistCallback(persistCallback)
    }

    /**
     * Construct a FirefoxAccount from a [Config], a clientId, and a redirectUri.
     *
     * @param persistCallback This callback will be called every time the [FirefoxAccount]
     * internal state has mutated.
     * The FirefoxAccount instance can be later restored using the
     * [FirefoxAccount.fromJSONString]` class method.
     * It is the responsibility of the consumer to ensure the persisted data
     * is saved in a secure location, as it can contain Sync Keys and
     * OAuth tokens.
     *
     * Note that it is not necessary to `close` the Config if this constructor is used (however
     * doing so will not cause an error).
     */
    constructor(
        config: ServerConfig,
        persistCallback: PersistCallback? = null
    ) : this(InternalFxAcct(config, persistCallback))

    override fun close() {
        job.cancel()
        inner.close()
    }

    override fun registerPersistenceCallback(callback: StatePersistenceCallback) {
        persistCallback.setCallback(callback)
    }

    override fun beginOAuthFlowAsync(scopes: Set<String>): Deferred<AuthFlowUrl?> {
        return scope.async {
            handleFxaExceptions(logger, "begin oauth flow", { null }) {
                val url = inner.beginOAuthFlow(scopes.toTypedArray())
                val state = Uri.parse(url).getQueryParameter("state")!!
                AuthFlowUrl(state, url)
            }
        }
    }

    override fun beginPairingFlowAsync(pairingUrl: String, scopes: Set<String>): Deferred<AuthFlowUrl?> {
        return scope.async {
            handleFxaExceptions(logger, "begin oauth pairing flow", { null }) {
                val url = inner.beginPairingFlow(pairingUrl, scopes.toTypedArray())
                val state = Uri.parse(url).getQueryParameter("state")!!
                AuthFlowUrl(state, url)
            }
        }
    }

    override fun getProfileAsync(ignoreCache: Boolean): Deferred<Profile?> {
        return scope.async {
            handleFxaExceptions(logger, "getProfile", { null }) {
                inner.getProfile(ignoreCache).into()
            }
        }
    }

    override fun getCurrentDeviceId(): String? {
        return handleFxaExceptions(logger, "getCurrentDeviceId", { null }) {
            inner.getCurrentDeviceId()
        }
    }

    override fun authorizeOAuthCode(
        clientId: String,
        scopes: Array<String>,
        state: String,
        accessType: AccessType
    ): String? {
        return handleFxaExceptions(logger, "authorizeOAuthCode", { null }) {
            inner.authorizeOAuthCode(clientId, scopes, state, accessType.msg)
        }
    }

    override fun getSessionToken(): String? {
        return handleFxaExceptions(logger, "getSessionToken", { null }) {
            inner.getSessionToken()
        }
    }

    override fun migrateFromSessionTokenAsync(sessionToken: String, kSync: String, kXCS: String): Deferred<Boolean> {
        return scope.async {
            handleFxaExceptions(logger, "migrateFromSessionToken") {
                inner.migrateFromSessionToken(sessionToken, kSync, kXCS)
            }
        }
    }

    override fun getTokenServerEndpointURL(): String {
        return inner.getTokenServerEndpointURL()
    }

    /**
     * Fetches the connection success url.
     */
    fun getConnectionSuccessURL(): String {
        return inner.getConnectionSuccessURL()
    }

    override fun completeOAuthFlowAsync(code: String, state: String): Deferred<Boolean> {
        return scope.async {
            handleFxaExceptions(logger, "complete oauth flow") {
                inner.completeOAuthFlow(code, state)
            }
        }
    }

    override fun getAccessTokenAsync(singleScope: String): Deferred<AccessTokenInfo?> {
        return scope.async {
            handleFxaExceptions(logger, "get access token", { null }) {
                inner.getAccessToken(singleScope).into()
            }
        }
    }

    override fun checkAuthorizationStatusAsync(singleScope: String): Deferred<Boolean?> {
        // fxalib maintains some internal token caches that need to be cleared whenever we
        // hit an auth problem. Call below makes that clean-up happen.
        inner.clearAccessTokenCache()

        // Now that internal token caches are cleared, we can perform a connectivity check.
        // Do so by requesting a new access token using an internally-stored "refresh token".
        // Success here means that we're still able to connect - our cached access token simply expired.
        // Failure indicates that we need to re-authenticate.
        return scope.async {
            try {
                inner.getAccessToken(singleScope)
                // We were able to obtain a token, so we're in a good authorization state.
                true
            } catch (e: FxaUnauthorizedException) {
                // We got back a 401 while trying to obtain a new access token, which means our refresh
                // token is also in a bad state. We need re-authentication for the tested scope.
                false
            } catch (e: FxaPanicException) {
                // Re-throw any panics we may encounter.
                throw e
            } catch (e: FxaException) {
                // On any other FxaExceptions (networking, etc) we have to return an indeterminate result.
                null
            }
            // Re-throw all other exceptions.
        }
    }

    override fun disconnectAsync(): Deferred<Boolean> {
        return scope.async {
            // TODO can this ever throw FxaUnauthorizedException? would that even make sense? or is that a bug?
            handleFxaExceptions(logger, "disconnect", { false }) {
                inner.disconnect()
                true
            }
        }
    }

    override fun deviceConstellation(): DeviceConstellation {
        return deviceConstellation
    }

    override fun toJSONString(): String = inner.toJSONString()

    companion object {
        /**
         * Restores the account's authentication state from a JSON string produced by
         * [FirefoxAccount.toJSONString].
         *
         * @param persistCallback This callback will be called every time the [FirefoxAccount]
         * internal state has mutated.
         * The FirefoxAccount instance can be later restored using the
         * [FirefoxAccount.fromJSONString]` class method.
         * It is the responsibility of the consumer to ensure the persisted data
         * is saved in a secure location, as it can contain Sync Keys and
         * OAuth tokens.
         *
         * @return [FirefoxAccount] representing the authentication state
         */
        fun fromJSONString(json: String, persistCallback: PersistCallback? = null): FirefoxAccount {
            return FirefoxAccount(InternalFxAcct.fromJSONString(json, persistCallback))
        }
    }
}
