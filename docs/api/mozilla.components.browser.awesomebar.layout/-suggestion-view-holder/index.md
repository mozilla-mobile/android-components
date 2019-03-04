[android-components](../../index.md) / [mozilla.components.browser.awesomebar.layout](../index.md) / [SuggestionViewHolder](./index.md)

# SuggestionViewHolder

`abstract class SuggestionViewHolder` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/awesomebar/src/main/java/mozilla/components/browser/awesomebar/layout/SuggestionViewHolder.kt#L9)

A view holder implementation for displaying an [AwesomeBar.Suggestion](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `SuggestionViewHolder(view: `[`View`](https://developer.android.com/reference/android/view/View.html)`)`<br>A view holder implementation for displaying an [AwesomeBar.Suggestion](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion/index.md). |

### Properties

| Name | Summary |
|---|---|
| [view](view.md) | `val view: `[`View`](https://developer.android.com/reference/android/view/View.html) |

### Functions

| Name | Summary |
|---|---|
| [bind](bind.md) | `abstract fun bind(suggestion: `[`Suggestion`](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion/index.md)`, selectionListener: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Binds the views in the holder to the [AwesomeBar.Suggestion](../../mozilla.components.concept.awesomebar/-awesome-bar/-suggestion/index.md). |
