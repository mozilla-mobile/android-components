/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import mozilla.appservices.fxaclient.FirefoxAccount as InternalFxAcct
import mozilla.appservices.fxaclient.FxaException.Unauthorized as Unauthorized

import mozilla.components.concept.sync.AccessTokenInfo
import mozilla.components.concept.sync.AuthException
import mozilla.components.concept.sync.AuthExceptionType
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
        config: Config,
        persistCallback: PersistCallback? = null
    ) : this(InternalFxAcct(config, persistCallback))

    override fun close() {
        deviceConstellation.stopPeriodicRefresh()
        job.cancel()
        inner.close()
    }

    override fun registerPersistenceCallback(callback: mozilla.components.concept.sync.StatePersistenceCallback) {
        persistCallback.setCallback(callback)
    }

    /**
     * Constructs a URL used to begin the OAuth flow for the requested scopes and keys.
     *
     * @param scopes List of OAuth scopes for which the client wants access
     * @param wantsKeys Fetch keys for end-to-end encryption of data from Mozilla-hosted services
     * @return Deferred<String> that resolves to the flow URL when complete
     */
    override fun beginOAuthFlow(scopes: Array<String>, wantsKeys: Boolean): Deferred<String> {
        return scope.async { inner.beginOAuthFlow(scopes, wantsKeys) }
    }

    override fun beginPairingFlow(pairingUrl: String, scopes: Array<String>): Deferred<String> {
        return scope.async { inner.beginPairingFlow(pairingUrl, scopes) }
    }

    /**
     * Fetches the profile object for the current client either from the existing cached account,
     * or from the server (requires the client to have access to the profile scope).
     *
     * @param ignoreCache Fetch the profile information directly from the server
     * @return Deferred<[Profile]> representing the user's basic profile info
     * @throws Unauthorized We couldn't find any suitable access token to make that call.
     * The caller should then start the OAuth Flow again with the "profile" scope.
     */
    override fun getProfile(ignoreCache: Boolean): Deferred<Profile> {
        return scope.async {
            inner.getProfile(ignoreCache).into()
        }
    }

    /**
     * Convenience method to fetch the profile from a cached account by default, but fall back
     * to retrieval from the server.
     *
     * @return Deferred<[Profile]> representing the user's basic profile info
     * @throws Unauthorized We couldn't find any suitable access token to make that call.
     * The caller should then start the OAuth Flow again with the "profile" scope.
     */
    override fun getProfile(): Deferred<Profile> = getProfile(false)

    /**
     * Fetches the token server endpoint, for authentication using the SAML bearer flow.
     */
    override fun getTokenServerEndpointURL(): String {
        return inner.getTokenServerEndpointURL()
    }

    /**
     * Fetches the connection success url.
     */
    fun getConnectionSuccessURL(): String {
        return inner.getConnectionSuccessURL()
    }

    /**
     * Authenticates the current account using the code and state parameters fetched from the
     * redirect URL reached after completing the sign in flow triggered by [beginOAuthFlow].
     *
     * Modifies the FirefoxAccount state.
     */
    override fun completeOAuthFlow(code: String, state: String): Deferred<Unit> {
        return scope.async { inner.completeOAuthFlow(code, state) }
    }

    /**
     * Tries to fetch an access token for the given scope.
     *
     * @param singleScope Single OAuth scope (no spaces) for which the client wants access
     * @return [AccessTokenInfo] that stores the token, along with its scope, key and
     *                           expiration timestamp (in seconds) since epoch when complete
     * @throws AuthException We couldn't provide an access token for this scope.
     * The caller should then start the OAuth Flow again with the desired scope.
     */
    override fun getAccessToken(singleScope: String): Deferred<AccessTokenInfo> {
        return scope.async {
            try {
                inner.getAccessToken(singleScope).into()
            } catch (e: FxaUnauthorizedException) {
                // Re-wrap an internal auth error to a concept-level auth error.
                throw AuthException(AuthExceptionType.UNAUTHORIZED, cause = e)
            }
        }
    }

    override fun deviceConstellation(): DeviceConstellation {
        return deviceConstellation
    }

    /**
     * Saves the current account's authentication state as a JSON string, for persistence in
     * the Android KeyStore/shared preferences. The authentication state can be restored using
     * [FirefoxAccount.fromJSONString].
     *
     * @return String containing the authentication details in JSON format
     */
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
