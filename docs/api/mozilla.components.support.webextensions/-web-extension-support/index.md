[android-components](../../index.md) / [mozilla.components.support.webextensions](../index.md) / [WebExtensionSupport](./index.md)

# WebExtensionSupport

`object WebExtensionSupport` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/webextensions/src/main/java/mozilla/components/support/webextensions/WebExtensionSupport.kt#L33)

Provides functionality to make sure web extension related events in the
[Engine](../../mozilla.components.concept.engine/-engine/index.md) are reflected in the browser state by dispatching the
corresponding actions to the [BrowserStore](../../mozilla.components.browser.state.store/-browser-store/index.md).

### Properties

| Name | Summary |
|---|---|
| [installedExtensions](installed-extensions.md) | `val installedExtensions: `[`ConcurrentHashMap`](https://developer.android.com/reference/java/util/concurrent/ConcurrentHashMap.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`>` |

### Functions

| Name | Summary |
|---|---|
| [awaitInitialization](await-initialization.md) | `suspend fun awaitInitialization(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Awaits for completion of the initialization process (completes when the state of all installed extensions is known). |
| [initialize](initialize.md) | `fun initialize(engine: `[`Engine`](../../mozilla.components.concept.engine/-engine/index.md)`, store: `[`BrowserStore`](../../mozilla.components.browser.state.store/-browser-store/index.md)`, onNewTabOverride: (`[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`?, `[`EngineSession`](../../mozilla.components.concept.engine/-engine-session/index.md)`, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`) -> `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = null, onCloseTabOverride: (`[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`?, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = null, onSelectTabOverride: (`[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`?, `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = null): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Registers a listener for web extension related events on the provided [Engine](../../mozilla.components.concept.engine/-engine/index.md) and reacts by dispatching the corresponding actions to the provided [BrowserStore](../../mozilla.components.browser.state.store/-browser-store/index.md). |
| [markExtensionAsUpdated](mark-extension-as-updated.md) | `fun markExtensionAsUpdated(store: `[`BrowserStore`](../../mozilla.components.browser.state.store/-browser-store/index.md)`, updatedExtension: `[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Marks the provided [updatedExtension](mark-extension-as-updated.md#mozilla.components.support.webextensions.WebExtensionSupport$markExtensionAsUpdated(mozilla.components.browser.state.store.BrowserStore, mozilla.components.concept.engine.webextension.WebExtension)/updatedExtension) as updated in the [store](mark-extension-as-updated.md#mozilla.components.support.webextensions.WebExtensionSupport$markExtensionAsUpdated(mozilla.components.browser.state.store.BrowserStore, mozilla.components.concept.engine.webextension.WebExtension)/store). |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
