[android-components](../../index.md) / [mozilla.components.support.base.feature](../index.md) / [BackHandler](./index.md)

# BackHandler

`interface BackHandler` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/base/src/main/java/mozilla/components/support/base/feature/BackHandler.kt#L10)

Generic interface for fragments, features and other components that want to handle 'back' button presses.

### Functions

| Name | Summary |
|---|---|
| [onBackPressed](on-back-pressed.md) | `abstract fun onBackPressed(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Called when this [BackHandler](./index.md) gets the option to handle the user pressing the back key. |

### Inheritors

| Name | Summary |
|---|---|
| [CustomTabsToolbarFeature](../../mozilla.components.feature.customtabs/-custom-tabs-toolbar-feature/index.md) | `class CustomTabsToolbarFeature : `[`LifecycleAwareFeature`](../-lifecycle-aware-feature/index.md)`, `[`BackHandler`](./index.md)<br>Initializes and resets the Toolbar for a Custom Tab based on the CustomTabConfig. |
| [FindInPageFeature](../../mozilla.components.feature.findinpage/-find-in-page-feature/index.md) | `class FindInPageFeature : `[`LifecycleAwareFeature`](../-lifecycle-aware-feature/index.md)`, `[`BackHandler`](./index.md)<br>Feature implementation that will keep a [FindInPageView](../../mozilla.components.feature.findinpage.view/-find-in-page-view/index.md) in sync with a bound [Session](../../mozilla.components.browser.session/-session/index.md). |
| [FullScreenFeature](../../mozilla.components.feature.session/-full-screen-feature/index.md) | `open class FullScreenFeature : `[`SelectionAwareSessionObserver`](../../mozilla.components.browser.session/-selection-aware-session-observer/index.md)`, `[`LifecycleAwareFeature`](../-lifecycle-aware-feature/index.md)`, `[`BackHandler`](./index.md)<br>Feature implementation for handling fullscreen mode (exiting and back button presses). |
| [SessionFeature](../../mozilla.components.feature.session/-session-feature/index.md) | `class SessionFeature : `[`LifecycleAwareFeature`](../-lifecycle-aware-feature/index.md)`, `[`BackHandler`](./index.md)<br>Feature implementation for connecting the engine module with the session module. |
| [ToolbarFeature](../../mozilla.components.feature.toolbar/-toolbar-feature/index.md) | `class ToolbarFeature : `[`LifecycleAwareFeature`](../-lifecycle-aware-feature/index.md)`, `[`BackHandler`](./index.md)<br>Feature implementation for connecting a toolbar implementation with the session module. |
