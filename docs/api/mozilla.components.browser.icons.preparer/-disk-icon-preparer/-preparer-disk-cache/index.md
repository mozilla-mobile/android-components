[android-components](../../../index.md) / [mozilla.components.browser.icons.preparer](../../index.md) / [DiskIconPreparer](../index.md) / [PreparerDiskCache](./index.md)

# PreparerDiskCache

`interface PreparerDiskCache` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/preparer/DiskIconPreparer.kt#L17)

### Functions

| Name | Summary |
|---|---|
| [getResources](get-resources.md) | `abstract fun getResources(context: <ERROR CLASS>, request: `[`IconRequest`](../../../mozilla.components.browser.icons/-icon-request/index.md)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Resource`](../../../mozilla.components.browser.icons/-icon-request/-resource/index.md)`>` |

### Inheritors

| Name | Summary |
|---|---|
| [IconDiskCache](../../../mozilla.components.browser.icons.utils/-icon-disk-cache/index.md) | `class IconDiskCache : `[`LoaderDiskCache`](../../../mozilla.components.browser.icons.loader/-disk-icon-loader/-loader-disk-cache/index.md)`, `[`PreparerDiskCache`](./index.md)`, `[`ProcessorDiskCache`](../../../mozilla.components.browser.icons.processor/-disk-icon-processor/-processor-disk-cache/index.md)<br>Caching bitmaps and resource URLs on disk. |
