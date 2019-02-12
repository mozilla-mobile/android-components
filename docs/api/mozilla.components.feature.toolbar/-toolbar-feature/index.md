[android-components](../../index.md) / [mozilla.components.feature.toolbar](../index.md) / [ToolbarFeature](./index.md)

# ToolbarFeature

`class ToolbarFeature : `[`LifecycleAwareFeature`](../../mozilla.components.support.base.feature/-lifecycle-aware-feature/index.md)`, `[`BackHandler`](../../mozilla.components.support.base.feature/-back-handler/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/toolbar/src/main/java/mozilla/components/feature/toolbar/ToolbarFeature.kt#L22)

Feature implementation for connecting a toolbar implementation with the session module.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ToolbarFeature(toolbar: `[`Toolbar`](../../mozilla.components.concept.toolbar/-toolbar/index.md)`, sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, loadUrlUseCase: `[`LoadUrlUseCase`](../../mozilla.components.feature.session/-session-use-cases/-load-url-use-case/index.md)`, searchUseCase: `[`SearchUseCase`](../-search-use-case.md)`? = null, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null)`<br>Feature implementation for connecting a toolbar implementation with the session module. |

### Properties

| Name | Summary |
|---|---|
| [toolbar](toolbar.md) | `val toolbar: `[`Toolbar`](../../mozilla.components.concept.toolbar/-toolbar/index.md) |

### Functions

| Name | Summary |
|---|---|
| [onBackPressed](on-back-pressed.md) | `fun onBackPressed(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Handler for back pressed events in activities that use this feature. |
| [start](start.md) | `fun start(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Start feature: App is in the foreground. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop feature: App is in the background. |
