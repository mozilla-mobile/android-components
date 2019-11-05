[android-components](../../index.md) / [mozilla.components.browser.icons.processor](../index.md) / [MemoryIconProcessor](./index.md)

# MemoryIconProcessor

`class MemoryIconProcessor : `[`IconProcessor`](../-icon-processor/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/processor/MemoryIconProcessor.kt#L16)

An [IconProcessor](../-icon-processor/index.md) implementation that saves icons in the in-memory cache.

### Types

| Name | Summary |
|---|---|
| [ProcessorMemoryCache](-processor-memory-cache/index.md) | `interface ProcessorMemoryCache` |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `MemoryIconProcessor(cache: `[`ProcessorMemoryCache`](-processor-memory-cache/index.md)`)`<br>An [IconProcessor](../-icon-processor/index.md) implementation that saves icons in the in-memory cache. |

### Functions

| Name | Summary |
|---|---|
| [process](process.md) | `fun process(context: <ERROR CLASS>, request: `[`IconRequest`](../../mozilla.components.browser.icons/-icon-request/index.md)`, resource: `[`Resource`](../../mozilla.components.browser.icons/-icon-request/-resource/index.md)`?, icon: `[`Icon`](../../mozilla.components.browser.icons/-icon/index.md)`, desiredSize: `[`DesiredSize`](../../mozilla.components.browser.icons/-desired-size/index.md)`): `[`Icon`](../../mozilla.components.browser.icons/-icon/index.md) |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
