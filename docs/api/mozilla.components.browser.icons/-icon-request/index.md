[android-components](../../index.md) / [mozilla.components.browser.icons](../index.md) / [IconRequest](./index.md)

# IconRequest

`data class IconRequest` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/IconRequest.kt#L14)

A request to load an [Icon](../-icon/index.md).

### Types

| Name | Summary |
|---|---|
| [Resource](-resource/index.md) | `data class Resource`<br>An icon resource that can be loaded. |
| [Size](-size/index.md) | `enum class Size`<br>Supported sizes. |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `IconRequest(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, size: `[`Size`](-size/index.md)` = Size.DEFAULT, resources: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Resource`](-resource/index.md)`> = emptyList())`<br>A request to load an [Icon](../-icon/index.md). |

### Properties

| Name | Summary |
|---|---|
| [resources](resources.md) | `val resources: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Resource`](-resource/index.md)`>`<br>An optional list of icon resources to load the icon from. |
| [size](size.md) | `val size: `[`Size`](-size/index.md)<br>The preferred size of the icon that should be loaded. |
| [url](url.md) | `val url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>The URL of the website an icon should be loaded for. |
