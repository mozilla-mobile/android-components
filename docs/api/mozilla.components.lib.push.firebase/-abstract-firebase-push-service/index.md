[android-components](../../index.md) / [mozilla.components.lib.push.firebase](../index.md) / [AbstractFirebasePushService](./index.md)

# AbstractFirebasePushService

`abstract class AbstractFirebasePushService : FirebaseMessagingService, `[`PushService`](../../mozilla.components.concept.push/-push-service/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/push-firebase/src/main/java/mozilla/components/lib/push/firebase/AbstractFirebasePushService.kt#L28)

A Firebase Cloud Messaging implementation of the [PushService](../../mozilla.components.concept.push/-push-service/index.md) for Android devices that support Google Play Services.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AbstractFirebasePushService(coroutineContext: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html)` = Dispatchers.IO)`<br>A Firebase Cloud Messaging implementation of the [PushService](../../mozilla.components.concept.push/-push-service/index.md) for Android devices that support Google Play Services. |

### Functions

| Name | Summary |
|---|---|
| [deleteToken](delete-token.md) | `open fun deleteToken(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Removes the Firebase instance ID. This would lead a new token being generated when the service hits the Firebase servers. |
| [isServiceAvailable](is-service-available.md) | `open fun isServiceAvailable(context: <ERROR CLASS>): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>If the push service is support on the device. |
| [onMessageReceived](on-message-received.md) | `open fun onMessageReceived(remoteMessage: RemoteMessage?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [onNewToken](on-new-token.md) | `open fun onNewToken(newToken: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [start](start.md) | `open fun start(context: <ERROR CLASS>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Initializes Firebase and starts the messaging service if not already started and enables auto-start as well. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stops the Firebase messaging service and disables auto-start. |

### Companion Object Properties

| Name | Summary |
|---|---|
| [MESSAGE_KEY_BODY](-m-e-s-s-a-g-e_-k-e-y_-b-o-d-y.md) | `const val MESSAGE_KEY_BODY: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [MESSAGE_KEY_CHANNEL_ID](-m-e-s-s-a-g-e_-k-e-y_-c-h-a-n-n-e-l_-i-d.md) | `const val MESSAGE_KEY_CHANNEL_ID: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [MESSAGE_KEY_CRYPTO_KEY](-m-e-s-s-a-g-e_-k-e-y_-c-r-y-p-t-o_-k-e-y.md) | `const val MESSAGE_KEY_CRYPTO_KEY: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [MESSAGE_KEY_ENCODING](-m-e-s-s-a-g-e_-k-e-y_-e-n-c-o-d-i-n-g.md) | `const val MESSAGE_KEY_ENCODING: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [MESSAGE_KEY_SALT](-m-e-s-s-a-g-e_-k-e-y_-s-a-l-t.md) | `const val MESSAGE_KEY_SALT: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
