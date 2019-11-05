[android-components](../../index.md) / [mozilla.components.service.fxa](../index.md) / [FirefoxAccount](./index.md)

# FirefoxAccount

`class FirefoxAccount : `[`OAuthAccount`](../../mozilla.components.concept.sync/-o-auth-account/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/firefox-accounts/src/main/java/mozilla/components/service/fxa/FirefoxAccount.kt#L30)

FirefoxAccount represents the authentication state of a client.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `FirefoxAccount(config: `[`ServerConfig`](../-server-config.md)`, persistCallback: `[`PersistCallback`](../-persist-callback.md)`? = null)`<br>Construct a FirefoxAccount from a [Config](#), a clientId, and a redirectUri. |

### Functions

| Name | Summary |
|---|---|
| [authorizeOAuthCode](authorize-o-auth-code.md) | `fun authorizeOAuthCode(clientId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, scopes: `[`Array`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>, state: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, accessType: `[`AccessType`](../../mozilla.components.concept.sync/-access-type/index.md)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?`<br>Provisions a scoped OAuth code for a given [clientId](../../mozilla.components.concept.sync/-o-auth-account/authorize-o-auth-code.md#mozilla.components.concept.sync.OAuthAccount$authorizeOAuthCode(kotlin.String, kotlin.Array((kotlin.String)), kotlin.String, mozilla.components.concept.sync.AccessType)/clientId) and the passed [scopes](../../mozilla.components.concept.sync/-o-auth-account/authorize-o-auth-code.md#mozilla.components.concept.sync.OAuthAccount$authorizeOAuthCode(kotlin.String, kotlin.Array((kotlin.String)), kotlin.String, mozilla.components.concept.sync.AccessType)/scopes). |
| [beginOAuthFlowAsync](begin-o-auth-flow-async.md) | `fun beginOAuthFlowAsync(scopes: `[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): Deferred<`[`AuthFlowUrl`](../../mozilla.components.concept.sync/-auth-flow-url/index.md)`?>`<br>Constructs a URL used to begin the OAuth flow for the requested scopes and keys. |
| [beginPairingFlowAsync](begin-pairing-flow-async.md) | `fun beginPairingFlowAsync(pairingUrl: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, scopes: `[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): Deferred<`[`AuthFlowUrl`](../../mozilla.components.concept.sync/-auth-flow-url/index.md)`?>`<br>Constructs a URL used to begin the pairing flow for the requested scopes and pairingUrl. |
| [checkAuthorizationStatusAsync](check-authorization-status-async.md) | `fun checkAuthorizationStatusAsync(singleScope: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): Deferred<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`?>`<br>This method should be called when a request made with an OAuth token failed with an authentication error. It will re-build cached state and perform a connectivity check. |
| [close](close.md) | `fun close(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [completeOAuthFlowAsync](complete-o-auth-flow-async.md) | `fun completeOAuthFlowAsync(code: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, state: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): Deferred<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>`<br>Authenticates the current account using the [code](../../mozilla.components.concept.sync/-o-auth-account/complete-o-auth-flow-async.md#mozilla.components.concept.sync.OAuthAccount$completeOAuthFlowAsync(kotlin.String, kotlin.String)/code) and [state](../../mozilla.components.concept.sync/-o-auth-account/complete-o-auth-flow-async.md#mozilla.components.concept.sync.OAuthAccount$completeOAuthFlowAsync(kotlin.String, kotlin.String)/state) parameters obtained via the OAuth flow initiated by [beginOAuthFlowAsync](../../mozilla.components.concept.sync/-o-auth-account/begin-o-auth-flow-async.md). |
| [deviceConstellation](device-constellation.md) | `fun deviceConstellation(): `[`DeviceConstellation`](../../mozilla.components.concept.sync/-device-constellation/index.md)<br>Returns the device constellation for the current account |
| [disconnectAsync](disconnect-async.md) | `fun disconnectAsync(): Deferred<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>`<br>Reset internal account state and destroy current device record. Use this when device record is no longer relevant, e.g. while logging out. On success, other devices will no longer see the current device in their device lists. |
| [getAccessTokenAsync](get-access-token-async.md) | `fun getAccessTokenAsync(singleScope: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): Deferred<`[`AccessTokenInfo`](../../mozilla.components.concept.sync/-access-token-info/index.md)`?>`<br>Tries to fetch an access token for the given scope. |
| [getConnectionSuccessURL](get-connection-success-u-r-l.md) | `fun getConnectionSuccessURL(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Fetches the connection success url. |
| [getCurrentDeviceId](get-current-device-id.md) | `fun getCurrentDeviceId(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?`<br>Returns current FxA Device ID for an authenticated account. |
| [getProfileAsync](get-profile-async.md) | `fun getProfileAsync(ignoreCache: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): Deferred<`[`Profile`](../../mozilla.components.concept.sync/-profile/index.md)`?>`<br>Fetches the profile object for the current client either from the existing cached state or from the server (requires the client to have access to the profile scope). |
| [getSessionToken](get-session-token.md) | `fun getSessionToken(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?`<br>Returns session token for an authenticated account. |
| [getTokenServerEndpointURL](get-token-server-endpoint-u-r-l.md) | `fun getTokenServerEndpointURL(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Fetches the token server endpoint, for authentication using the SAML bearer flow. |
| [migrateFromSessionTokenAsync](migrate-from-session-token-async.md) | `fun migrateFromSessionTokenAsync(sessionToken: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, kSync: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, kXCS: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): Deferred<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>`<br>Attempts to migrate from an existing session token without user input |
| [registerPersistenceCallback](register-persistence-callback.md) | `fun registerPersistenceCallback(callback: `[`StatePersistenceCallback`](../../mozilla.components.concept.sync/-state-persistence-callback/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Registers a callback for when the account state gets persisted |
| [toJSONString](to-j-s-o-n-string.md) | `fun toJSONString(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Serializes the current account's authentication state as a JSON string, for persistence in the Android KeyStore/shared preferences. The authentication state can be restored using [FirefoxAccount.fromJSONString](#). |

### Companion Object Functions

| Name | Summary |
|---|---|
| [fromJSONString](from-j-s-o-n-string.md) | `fun fromJSONString(json: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, persistCallback: `[`PersistCallback`](../-persist-callback.md)`? = null): `[`FirefoxAccount`](./index.md)<br>Restores the account's authentication state from a JSON string produced by [FirefoxAccount.toJSONString](to-j-s-o-n-string.md). |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
