/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.sync

import kotlinx.coroutines.Deferred

/**
 * An auth-related exception type, for use with [AuthException].
 *
 * @property msg string value of the auth exception type
 */
enum class AuthExceptionType(val msg: String) {
    KEY_INFO("Missing key info"),
    NO_TOKEN("Missing access token"),
    UNAUTHORIZED("Unauthorized")
}

/**
 * The access-type determines whether the code can be exchanged for a refresh token for
 * offline use or not.
 *
 * @property msg string value of the access-type
 */
enum class AccessType(val msg: String) {
    ONLINE("online"),
    OFFLINE("offline")
}

/**
 * An exception which may happen while obtaining auth information using [OAuthAccount].
 */
class AuthException(type: AuthExceptionType, cause: Exception? = null) : Throwable(type.msg, cause)

/**
 * An object that represents a login flow initiated by [OAuthAccount].
 * @property state OAuth state parameter, identifying a specific authentication flow.
 * This string is randomly generated during [OAuthAccount.beginOAuthFlowAsync] and [OAuthAccount.beginPairingFlowAsync].
 * @property url Url which needs to be loaded to go through the authentication flow identified by [state].
 */
data class AuthFlowUrl(val state: String, val url: String)

/**
 * Facilitates testing consumers of FirefoxAccount.
 */
@SuppressWarnings("TooManyFunctions")
interface OAuthAccount : AutoCloseable {

    /**
     * Constructs a URL used to begin the OAuth flow for the requested scopes and keys.
     *
     * @param scopes List of OAuth scopes for which the client wants access
     * @return Deferred AuthFlowUrl that resolves to the flow URL when complete
     */
    fun beginOAuthFlowAsync(scopes: Set<String>): Deferred<AuthFlowUrl?>

    /**
     * Constructs a URL used to begin the pairing flow for the requested scopes and pairingUrl.
     *
     * @param pairingUrl URL string for pairing
     * @param scopes List of OAuth scopes for which the client wants access
     * @return Deferred AuthFlowUrl Optional that resolves to the flow URL when complete
     */
    fun beginPairingFlowAsync(pairingUrl: String, scopes: Set<String>): Deferred<AuthFlowUrl?>

    /**
     * Returns current FxA Device ID for an authenticated account.
     *
     * @return Current device's FxA ID, if available. `null` otherwise.
     */
    fun getCurrentDeviceId(): String?

    /**
     * Returns session token for an authenticated account.
     *
     * @return Current account's session token, if available. `null` otherwise.
     */
    fun getSessionToken(): String?

    /**
     * Provisions a scoped OAuth code for a given [clientId] and the passed [scopes].
     *
     * @param clientId the client id string
     * @param scopes the list of scopes to request access to
     * @param state the state token string
     * @param accessType the accessType method to be used by the returned code, determines whether
     * the code can be exchanged for a refresh token to be used offline or not
     * @return the authorized auth code string
     */
    fun authorizeOAuthCode(
        clientId: String,
        scopes: Array<String>,
        state: String,
        accessType: AccessType = AccessType.ONLINE
    ): String?

    /**
     * Fetches the profile object for the current client either from the existing cached state
     * or from the server (requires the client to have access to the profile scope).
     *
     * @param ignoreCache Fetch the profile information directly from the server
     * @return Profile (optional, if successfully retrieved) representing the user's basic profile info
     */
    fun getProfileAsync(ignoreCache: Boolean = false): Deferred<Profile?>

    /**
     * Authenticates the current account using the [code] and [state] parameters obtained via the
     * OAuth flow initiated by [beginOAuthFlowAsync].
     *
     * Modifies the FirefoxAccount state.
     * @param code OAuth code string
     * @param state state token string
     * @return Deferred boolean representing success or failure
     */
    fun completeOAuthFlowAsync(code: String, state: String): Deferred<Boolean>

    /**
     * Tries to fetch an access token for the given scope.
     *
     * @param singleScope Single OAuth scope (no spaces) for which the client wants access
     * @return [AccessTokenInfo] that stores the token, along with its scope, key and
     *                           expiration timestamp (in seconds) since epoch when complete
     */
    fun getAccessTokenAsync(singleScope: String): Deferred<AccessTokenInfo?>

    /**
     * This method should be called when a request made with an OAuth token failed with an
     * authentication error. It will re-build cached state and perform a connectivity check.
     *
     * In time, fxalib will grow a similar method, at which point we'll just relay to it.
     * See https://github.com/mozilla/application-services/issues/1263
     *
     * @param singleScope An oauth scope for which to check authorization state.
     * @return An optional [Boolean] flag indicating if we're connected, or need to go through
     * re-authentication. A null result means we were not able to determine state at this time.
     */
    fun checkAuthorizationStatusAsync(singleScope: String): Deferred<Boolean?>

    /**
     * Fetches the token server endpoint, for authentication using the SAML bearer flow.
     *
     * @return Token server endpoint URL string
     */
    fun getTokenServerEndpointURL(): String

    /**
     * Registers a callback for when the account state gets persisted
     *
     * @param callback the account state persistence callback
     */
    fun registerPersistenceCallback(callback: StatePersistenceCallback)

    /**
     * Attempts to migrate from an existing session token without user input
     *
     * @param sessionToken token string to use for login
     * @param kSync sync string for login
     * @param kXCS XCS string for login
     * @return Deferred boolean success or failure for the migration event
     */
    fun migrateFromSessionTokenAsync(sessionToken: String, kSync: String, kXCS: String): Deferred<Boolean>

    /**
     * Returns the device constellation for the current account
     *
     * @return Device constellation for the current account
     */
    fun deviceConstellation(): DeviceConstellation

    /**
     * Reset internal account state and destroy current device record.
     * Use this when device record is no longer relevant, e.g. while logging out. On success, other
     * devices will no longer see the current device in their device lists.
     *
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     * Failure indicates that we may have failed to destroy current device record. Nothing to do for
     * the consumer; device record will be cleaned up eventually via TTL.
     */
    fun disconnectAsync(): Deferred<Boolean>

    /**
     * Serializes the current account's authentication state as a JSON string, for persistence in
     * the Android KeyStore/shared preferences. The authentication state can be restored using
     * [FirefoxAccount.fromJSONString].
     *
     * @return String containing the authentication details in JSON format
     */
    fun toJSONString(): String
}

/**
 * Describes a delegate object that is used by [OAuthAccount] to persist its internal state as it changes.
 */
interface StatePersistenceCallback {
    /**
     * @param data Account state representation as a string (e.g. as json).
     */
    fun persist(data: String)
}

sealed class AuthType {
    /**
     * Account restored from hydrated state on disk.
     */
    object Existing : AuthType()

    /**
     * Account created in response to a sign-in.
     */
    object Signin : AuthType()

    /**
     * Account created in response to a sign-up.
     */
    object Signup : AuthType()

    /**
     * Account created via pairing (similar to sign-in, but without requiring credentials).
     */
    object Pairing : AuthType()

    /**
     * Account was created for an unknown external reason, hopefully identified by [action].
     */
    data class OtherExternal(val action: String?) : AuthType()

    /**
     * Account created via a shared account state from another app.
     */
    object Shared : AuthType()

    /**
     * Existing account was recovered from an authentication problem.
     */
    object Recovered : AuthType()
}

/**
 * Observer interface which lets its users monitor account state changes and major events.
 */
interface AccountObserver {
    /**
     * Account just got logged out.
     */
    fun onLoggedOut() = Unit

    /**
     * Account was successfully authenticated.
     *
     * @param account An authenticated instance of a [OAuthAccount].
     * @param authType Describes what kind of authentication event caused this invocation.
     */
    fun onAuthenticated(account: OAuthAccount, authType: AuthType) = Unit

    /**
     * Account's profile is now available.
     * @param profile A fresh version of account's [Profile].
     */
    fun onProfileUpdated(profile: Profile) = Unit

    /**
     * Account needs to be re-authenticated (e.g. due to a password change).
     */
    fun onAuthenticationProblems() = Unit
}

data class Avatar(
    val url: String,
    val isDefault: Boolean
)

data class Profile(
    val uid: String?,
    val email: String?,
    val avatar: Avatar?,
    val displayName: String?
)

/**
 * Scoped key data.
 *
 * @property kid The JWK key identifier.
 * @property k The JWK key data.
 */
data class OAuthScopedKey(
    val kty: String,
    val scope: String,
    val kid: String,
    val k: String
)

/**
 * The result of authentication with FxA via an OAuth flow.
 *
 * @property token The access token produced by the flow.
 * @property key An OAuthScopedKey if present.
 * @property expiresAt The expiry date timestamp of this token since unix epoch (in seconds).
 */
data class AccessTokenInfo(
    val scope: String,
    val token: String,
    val key: OAuthScopedKey?,
    val expiresAt: Long
)
