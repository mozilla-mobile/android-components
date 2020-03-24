[android-components](../../index.md) / [mozilla.components.browser.state.state](../index.md) / [TabSessionState](./index.md)

# TabSessionState

`data class TabSessionState : `[`SessionState`](../-session-state/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/state/TabSessionState.kt#L24)

Value type that represents the state of a tab (private or normal).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `TabSessionState(id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = UUID.randomUUID().toString(), content: `[`ContentState`](../-content-state/index.md)`, trackingProtection: `[`TrackingProtectionState`](../-tracking-protection-state/index.md)` = TrackingProtectionState(), engineState: `[`EngineState`](../-engine-state/index.md)` = EngineState(), parentId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, extensionState: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`WebExtensionState`](../-web-extension-state/index.md)`> = emptyMap(), readerState: `[`ReaderState`](../-reader-state/index.md)` = ReaderState())`<br>Value type that represents the state of a tab (private or normal). |

### Properties

| Name | Summary |
|---|---|
| [content](content.md) | `val content: `[`ContentState`](../-content-state/index.md)<br>the [ContentState](../-content-state/index.md) of this tab. |
| [engineState](engine-state.md) | `val engineState: `[`EngineState`](../-engine-state/index.md)<br>the [EngineState](../-engine-state/index.md) of this session. |
| [extensionState](extension-state.md) | `val extensionState: `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`WebExtensionState`](../-web-extension-state/index.md)`>`<br>a map of web extension ids to extensions, that contains the overridden values for this tab. |
| [id](id.md) | `val id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>the ID of this tab and session. |
| [parentId](parent-id.md) | `val parentId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?`<br>the parent ID of this tab or null if this tab has no parent. The parent tab is usually the tab that initiated opening this tab (e.g. the user clicked a link with target="_blank" or selected "open in new tab" or a "window.open" was triggered). |
| [readerState](reader-state.md) | `val readerState: `[`ReaderState`](../-reader-state/index.md)<br>the [ReaderState](../-reader-state/index.md) of this tab. |
| [trackingProtection](tracking-protection.md) | `val trackingProtection: `[`TrackingProtectionState`](../-tracking-protection-state/index.md)<br>the [TrackingProtectionState](../-tracking-protection-state/index.md) of this tab. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
