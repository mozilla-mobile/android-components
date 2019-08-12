[android-components](../../index.md) / [mozilla.components.feature.customtabs](../index.md) / [AbstractCustomTabsService](./index.md)

# AbstractCustomTabsService

`abstract class AbstractCustomTabsService : CustomTabsService` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/customtabs/src/main/java/mozilla/components/feature/customtabs/AbstractCustomTabsService.kt#L31)

[Service](#) providing Custom Tabs related functionality.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AbstractCustomTabsService()`<br>[Service](#) providing Custom Tabs related functionality. |

### Properties

| Name | Summary |
|---|---|
| [engine](engine.md) | `abstract val engine: `[`Engine`](../../mozilla.components.concept.engine/-engine/index.md) |
| [httpClient](http-client.md) | `open val httpClient: `[`Client`](../../mozilla.components.concept.fetch/-client/index.md)`?` |

### Functions

| Name | Summary |
|---|---|
| [extraCommand](extra-command.md) | `open fun extraCommand(commandName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?, args: <ERROR CLASS>?): <ERROR CLASS>?` |
| [mayLaunchUrl](may-launch-url.md) | `open fun mayLaunchUrl(sessionToken: CustomTabsSessionToken?, url: <ERROR CLASS>?, extras: <ERROR CLASS>?, otherLikelyBundles: `[`MutableList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)`<<ERROR CLASS>>?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [newSession](new-session.md) | `open fun newSession(sessionToken: CustomTabsSessionToken?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [onDestroy](on-destroy.md) | `open fun onDestroy(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [postMessage](post-message.md) | `open fun postMessage(sessionToken: CustomTabsSessionToken?, message: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?, extras: <ERROR CLASS>?): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
| [requestPostMessageChannel](request-post-message-channel.md) | `open fun requestPostMessageChannel(sessionToken: CustomTabsSessionToken?, postMessageOrigin: <ERROR CLASS>?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [updateVisuals](update-visuals.md) | `open fun updateVisuals(sessionToken: CustomTabsSessionToken?, bundle: <ERROR CLASS>?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [validateRelationship](validate-relationship.md) | `open fun validateRelationship(sessionToken: CustomTabsSessionToken?, relation: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, origin: <ERROR CLASS>, extras: <ERROR CLASS>?): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [warmup](warmup.md) | `open fun warmup(flags: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
