[android-components](../index.md) / [mozilla.components.service.fxa](./index.md)

## Package mozilla.components.service.fxa

### Types

| Name | Summary |
|---|---|
| [DeviceConfig](-device-config/index.md) | `data class DeviceConfig`<br>Configuration for the current device. |
| [FirefoxAccount](-firefox-account/index.md) | `class FirefoxAccount : `[`OAuthAccount`](../mozilla.components.concept.sync/-o-auth-account/index.md)<br>FirefoxAccount represents the authentication state of a client. |
| [FxaAuthData](-fxa-auth-data/index.md) | `data class FxaAuthData`<br>Captures basic OAuth authentication data (code, state) and any additional data FxA passes along. |
| [FxaDeviceConstellation](-fxa-device-constellation/index.md) | `class FxaDeviceConstellation : `[`DeviceConstellation`](../mozilla.components.concept.sync/-device-constellation/index.md)`, `[`Observable`](../mozilla.components.support.base.observer/-observable/index.md)`<`[`AccountEventsObserver`](../mozilla.components.concept.sync/-account-events-observer/index.md)`>`<br>Provides an implementation of [DeviceConstellation](../mozilla.components.concept.sync/-device-constellation/index.md) backed by a [FirefoxAccount](#). |
| [FxaDeviceSettingsCache](-fxa-device-settings-cache/index.md) | `class FxaDeviceSettingsCache : `[`SharedPreferencesCache`](../mozilla.components.support.base.utils/-shared-preferences-cache/index.md)`<DeviceSettings>`<br>A thin wrapper around [SharedPreferences](#) which knows how to serialize/deserialize [DeviceSettings](#). |
| [SyncAuthInfoCache](-sync-auth-info-cache/index.md) | `class SyncAuthInfoCache : `[`SharedPreferencesCache`](../mozilla.components.support.base.utils/-shared-preferences-cache/index.md)`<`[`SyncAuthInfo`](../mozilla.components.concept.sync/-sync-auth-info/index.md)`>`<br>A thin wrapper around [SharedPreferences](#) which knows how to serialize/deserialize [SyncAuthInfo](../mozilla.components.concept.sync/-sync-auth-info/index.md). |
| [SyncConfig](-sync-config/index.md) | `data class SyncConfig`<br>Configuration for sync. |
| [SyncEngine](-sync-engine/index.md) | `sealed class SyncEngine`<br>Describes possible sync engines that device can support. |

### Type Aliases

| Name | Summary |
|---|---|
| [FxaException](-fxa-exception.md) | `typealias FxaException = FxaException`<br>High-level exception class for the exceptions thrown in the Rust library. |
| [FxaNetworkException](-fxa-network-exception.md) | `typealias FxaNetworkException = Network`<br>Thrown on a network error. |
| [FxaPanicException](-fxa-panic-exception.md) | `typealias FxaPanicException = Panic`<br>Thrown when the Rust library hits an assertion or panic (this is always a bug). |
| [FxaUnauthorizedException](-fxa-unauthorized-exception.md) | `typealias FxaUnauthorizedException = Unauthorized`<br>Thrown when the operation requires additional authorization. |
| [FxaUnspecifiedException](-fxa-unspecified-exception.md) | `typealias FxaUnspecifiedException = Unspecified`<br>Thrown when the Rust library hits an unexpected error that isn't a panic. This may indicate library misuse, network errors, etc. |
| [PersistCallback](-persist-callback.md) | `typealias PersistCallback = PersistCallback` |
| [Server](-server.md) | `typealias Server = Server` |
| [ServerConfig](-server-config.md) | `typealias ServerConfig = Config` |

### Extensions for External Classes

| Name | Summary |
|---|---|
| [kotlin.String](kotlin.-string/index.md) |  |
| [mozilla.appservices.fxaclient.AccessTokenInfo](mozilla.appservices.fxaclient.-access-token-info/index.md) |  |
| [mozilla.appservices.fxaclient.AccountEvent](mozilla.appservices.fxaclient.-account-event/index.md) |  |
| [mozilla.appservices.fxaclient.Device](mozilla.appservices.fxaclient.-device/index.md) |  |
| [mozilla.appservices.fxaclient.Device.Capability](mozilla.appservices.fxaclient.-device.-capability/index.md) |  |
| [mozilla.appservices.fxaclient.Device.PushSubscription](mozilla.appservices.fxaclient.-device.-push-subscription/index.md) |  |
| [mozilla.appservices.fxaclient.IncomingDeviceCommand](mozilla.appservices.fxaclient.-incoming-device-command/index.md) |  |
| [mozilla.appservices.fxaclient.IncomingDeviceCommand.TabReceived](mozilla.appservices.fxaclient.-incoming-device-command.-tab-received/index.md) |  |
| [mozilla.appservices.fxaclient.MigrationState](mozilla.appservices.fxaclient.-migration-state/index.md) |  |
| [mozilla.appservices.fxaclient.Profile](mozilla.appservices.fxaclient.-profile/index.md) |  |
| [mozilla.appservices.fxaclient.ScopedKey](mozilla.appservices.fxaclient.-scoped-key/index.md) |  |
| [mozilla.appservices.fxaclient.TabHistoryEntry](mozilla.appservices.fxaclient.-tab-history-entry/index.md) |  |

### Properties

| Name | Summary |
|---|---|
| [FXA_STATE_KEY](-f-x-a_-s-t-a-t-e_-k-e-y.md) | `const val FXA_STATE_KEY: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [FXA_STATE_PREFS_KEY](-f-x-a_-s-t-a-t-e_-p-r-e-f-s_-k-e-y.md) | `const val FXA_STATE_PREFS_KEY: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Functions

| Name | Summary |
|---|---|
| [asSyncAuthInfo](as-sync-auth-info.md) | `fun `[`AccessTokenInfo`](../mozilla.components.concept.sync/-access-token-info/index.md)`.asSyncAuthInfo(tokenServerUrl: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`SyncAuthInfo`](../mozilla.components.concept.sync/-sync-auth-info/index.md)<br>Converts a generic [AccessTokenInfo](#) into a Firefox Sync-friendly [SyncAuthInfo](../mozilla.components.concept.sync/-sync-auth-info/index.md) instance which may be used for data synchronization. |
| [handleFxaExceptions](handle-fxa-exceptions.md) | `suspend fun <T> handleFxaExceptions(logger: `[`Logger`](../mozilla.components.support.base.log.logger/-logger/index.md)`, operation: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: suspend () -> `[`T`](handle-fxa-exceptions.md#T)`, postHandleAuthErrorBlock: (`[`FxaUnauthorizedException`](-fxa-unauthorized-exception.md)`) -> `[`T`](handle-fxa-exceptions.md#T)`, handleErrorBlock: (`[`FxaException`](-fxa-exception.md)`) -> `[`T`](handle-fxa-exceptions.md#T)`): `[`T`](handle-fxa-exceptions.md#T)<br>Runs a provided lambda, and if that throws non-panic, non-auth FxA exception, runs [handleErrorBlock](handle-fxa-exceptions.md#mozilla.components.service.fxa$handleFxaExceptions(mozilla.components.support.base.log.logger.Logger, kotlin.String, kotlin.SuspendFunction0((mozilla.components.service.fxa.handleFxaExceptions.T)), kotlin.Function1((mozilla.appservices.fxaclient.FxaException.Unauthorized, mozilla.components.service.fxa.handleFxaExceptions.T)), kotlin.Function1((mozilla.appservices.fxaclient.FxaException, mozilla.components.service.fxa.handleFxaExceptions.T)))/handleErrorBlock). If that lambda throws an FxA auth exception, notifies [authErrorRegistry](#), and runs [postHandleAuthErrorBlock](handle-fxa-exceptions.md#mozilla.components.service.fxa$handleFxaExceptions(mozilla.components.support.base.log.logger.Logger, kotlin.String, kotlin.SuspendFunction0((mozilla.components.service.fxa.handleFxaExceptions.T)), kotlin.Function1((mozilla.appservices.fxaclient.FxaException.Unauthorized, mozilla.components.service.fxa.handleFxaExceptions.T)), kotlin.Function1((mozilla.appservices.fxaclient.FxaException, mozilla.components.service.fxa.handleFxaExceptions.T)))/postHandleAuthErrorBlock).`suspend fun <T> handleFxaExceptions(logger: `[`Logger`](../mozilla.components.support.base.log.logger/-logger/index.md)`, operation: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, default: (`[`FxaException`](-fxa-exception.md)`) -> `[`T`](handle-fxa-exceptions.md#T)`, block: suspend () -> `[`T`](handle-fxa-exceptions.md#T)`): `[`T`](handle-fxa-exceptions.md#T)<br>Helper method that handles [FxaException](-fxa-exception.md) and allows specifying a lazy default value via [default](handle-fxa-exceptions.md#mozilla.components.service.fxa$handleFxaExceptions(mozilla.components.support.base.log.logger.Logger, kotlin.String, kotlin.Function1((mozilla.appservices.fxaclient.FxaException, mozilla.components.service.fxa.handleFxaExceptions.T)), kotlin.SuspendFunction0((mozilla.components.service.fxa.handleFxaExceptions.T)))/default) block for use in case of errors. Execution is wrapped in log statements.`suspend fun handleFxaExceptions(logger: `[`Logger`](../mozilla.components.support.base.log.logger/-logger/index.md)`, operation: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Helper method that handles [FxaException](-fxa-exception.md) and returns a [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) success flag as a result. |
| [into](into.md) | `fun `[`DeviceType`](../mozilla.components.concept.sync/-device-type/index.md)`.into(): Type`<br>`fun `[`DeviceCapability`](../mozilla.components.concept.sync/-device-capability/index.md)`.into(): Capability`<br>`fun `[`DevicePushSubscription`](../mozilla.components.concept.sync/-device-push-subscription/index.md)`.into(): PushSubscription`<br>`fun `[`Device`](../mozilla.components.concept.sync/-device/index.md)`.into(): Device`<br>`fun `[`TabData`](../mozilla.components.concept.sync/-tab-data/index.md)`.into(): TabHistoryEntry` |
| [intoSyncType](into-sync-type.md) | `fun `[`DeviceType`](../mozilla.components.concept.sync/-device-type/index.md)`.intoSyncType(): DeviceType`<br>FxA and Sync libraries both define a "DeviceType", so we get to have even more cruft. |
