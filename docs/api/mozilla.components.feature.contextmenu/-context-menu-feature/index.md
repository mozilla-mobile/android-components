[android-components](../../index.md) / [mozilla.components.feature.contextmenu](../index.md) / [ContextMenuFeature](./index.md)

# ContextMenuFeature

`class ContextMenuFeature : `[`LifecycleAwareFeature`](../../mozilla.components.support.base.feature/-lifecycle-aware-feature/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/contextmenu/src/main/java/mozilla/components/feature/contextmenu/ContextMenuFeature.kt#L35)

Feature for displaying a context menu after long-pressing web content.

This feature will subscribe to the currently selected [Session](../../mozilla.components.browser.session/-session/index.md) and display the context menu based on
[Session.Observer.onLongPress](../../mozilla.components.browser.session/-session/-observer/on-long-press.md) events. Once the context menu is closed or the user selects an item from the context
menu the related [HitResult](../../mozilla.components.concept.engine/-hit-result/index.md) will be consumed.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ContextMenuFeature(fragmentManager: FragmentManager, sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, candidates: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ContextMenuCandidate`](../-context-menu-candidate/index.md)`>, engineView: `[`EngineView`](../../mozilla.components.concept.engine/-engine-view/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null)`<br>Feature for displaying a context menu after long-pressing web content. |

### Functions

| Name | Summary |
|---|---|
| [start](start.md) | `fun start(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Start observing the selected session and when needed show a context menu. |
| [stop](stop.md) | `fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Stop observing the selected session and do not show any context menus anymore. |
