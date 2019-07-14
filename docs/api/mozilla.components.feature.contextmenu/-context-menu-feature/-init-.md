[android-components](../../index.md) / [mozilla.components.feature.contextmenu](../index.md) / [ContextMenuFeature](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`ContextMenuFeature(fragmentManager: FragmentManager, sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, candidates: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`ContextMenuCandidate`](../-context-menu-candidate/index.md)`>, engineView: `[`EngineView`](../../mozilla.components.concept.engine/-engine-view/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null)`

Feature for displaying a context menu after long-pressing web content.

This feature will subscribe to the currently selected [Session](../../mozilla.components.browser.session/-session/index.md) and display the context menu based on
[Session.Observer.onLongPress](../../mozilla.components.browser.session/-session/-observer/on-long-press.md) events. Once the context menu is closed or the user selects an item from the context
menu the related [HitResult](../../mozilla.components.concept.engine/-hit-result/index.md) will be consumed.

