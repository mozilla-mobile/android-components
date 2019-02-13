[android-components](../../index.md) / [mozilla.components.browser.engine.system](../index.md) / [SystemEngine](./index.md)

# SystemEngine

`class SystemEngine : `[`Engine`](../../mozilla.components.concept.engine/-engine/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-system/src/main/java/mozilla/components/browser/engine/system/SystemEngine.kt#L25)

WebView-based implementation of the Engine interface.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SystemEngine(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, defaultSettings: `[`Settings`](../../mozilla.components.concept.engine/-settings/index.md)` = DefaultSettings())`<br>WebView-based implementation of the Engine interface. |

### Properties

| Name | Summary |
|---|---|
| [settings](settings.md) | `val settings: `[`Settings`](../../mozilla.components.concept.engine/-settings/index.md)<br>See [Engine.settings](../../mozilla.components.concept.engine/-engine/settings.md) |

### Functions

| Name | Summary |
|---|---|
| [createSession](create-session.md) | `fun createSession(private: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`EngineSession`](../../mozilla.components.concept.engine/-engine-session/index.md)<br>Creates a new WebView-based EngineSession implementation. |
| [createSessionState](create-session-state.md) | `fun createSessionState(json: `[`JSONObject`](https://developer.android.com/reference/org/json/JSONObject.html)`): `[`EngineSessionState`](../../mozilla.components.concept.engine/-engine-session-state/index.md)<br>Create a new [EngineSessionState](../../mozilla.components.concept.engine/-engine-session-state/index.md) instance from the serialized JSON representation. |
| [createView](create-view.md) | `fun createView(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, attrs: `[`AttributeSet`](https://developer.android.com/reference/android/util/AttributeSet.html)`?): `[`EngineView`](../../mozilla.components.concept.engine/-engine-view/index.md)<br>Creates a new WebView-based EngineView implementation. |
| [name](name.md) | `fun name(): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>See [Engine.name](../../mozilla.components.concept.engine/-engine/name.md) |
| [speculativeConnect](speculative-connect.md) | `fun speculativeConnect(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Opens a speculative connection to the host of [url](speculative-connect.md#mozilla.components.browser.engine.system.SystemEngine$speculativeConnect(kotlin.String)/url). |

### Companion Object Properties

| Name | Summary |
|---|---|
| [defaultUserAgent](default-user-agent.md) | `var defaultUserAgent: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`?` |
