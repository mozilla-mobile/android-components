[android-components](../../index.md) / [mozilla.components.browser.toolbar](../index.md) / [AsyncFilterListener](./index.md)

# AsyncFilterListener

`class AsyncFilterListener : `[`OnFilterListener`](../../mozilla.components.ui.autocomplete/-on-filter-listener.md)`, CoroutineScope` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/toolbar/src/main/java/mozilla/components/browser/toolbar/BrowserToolbar.kt#L594)

Wraps [filter](#) execution in a coroutine context, cancelling prior executions on every invocation.
[coroutineContext](coroutine-context.md) must be of type that doesn't propagate cancellation of its children upwards.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `AsyncFilterListener(urlView: `[`AutocompleteView`](../../mozilla.components.ui.autocomplete/-autocomplete-view/index.md)`, coroutineContext: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html)`, filter: suspend (`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`AutocompleteDelegate`](../../mozilla.components.concept.toolbar/-autocomplete-delegate/index.md)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`, uiContext: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html)` = Dispatchers.Main)`<br>Wraps [filter](#) execution in a coroutine context, cancelling prior executions on every invocation. [coroutineContext](coroutine-context.md) must be of type that doesn't propagate cancellation of its children upwards. |

### Properties

| Name | Summary |
|---|---|
| [coroutineContext](coroutine-context.md) | `val coroutineContext: `[`CoroutineContext`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html) |

### Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | `fun invoke(text: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
