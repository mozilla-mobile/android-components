[android-components](../../index.md) / [mozilla.components.browser.tabstray](../index.md) / [TabViewHolder](./index.md)

# TabViewHolder

`class TabViewHolder : ViewHolder` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/tabstray/src/main/java/mozilla/components/browser/tabstray/TabViewHolder.kt#L22)

A RecyclerView ViewHolder implementation for "tab" items.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `TabViewHolder(itemView: <ERROR CLASS>, tabsTray: `[`BrowserTabsTray`](../-browser-tabs-tray/index.md)`)`<br>A RecyclerView ViewHolder implementation for "tab" items. |

### Functions

| Name | Summary |
|---|---|
| [bind](bind.md) | `fun bind(tab: `[`Tab`](../../mozilla.components.concept.tabstray/-tab/index.md)`, isSelected: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`, observable: `[`Observable`](../../mozilla.components.support.base.observer/-observable/index.md)`<`[`Observer`](../../mozilla.components.concept.tabstray/-tabs-tray/-observer/index.md)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Displays the data of the given session and notifies the given observable about events. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
