[android-components](../../index.md) / [mozilla.components.browser.icons.loader](../index.md) / [DiskIconLoader](./index.md)

# DiskIconLoader

`class DiskIconLoader : `[`IconLoader`](../-icon-loader/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/loader/DiskIconLoader.kt#L14)

[IconLoader](../-icon-loader/index.md) implementation that loads icons from a disk cache.

### Types

| Name | Summary |
|---|---|
| [LoaderDiskCache](-loader-disk-cache/index.md) | `interface LoaderDiskCache` |

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `DiskIconLoader(cache: `[`LoaderDiskCache`](-loader-disk-cache/index.md)`)`<br>[IconLoader](../-icon-loader/index.md) implementation that loads icons from a disk cache. |

### Functions

| Name | Summary |
|---|---|
| [load](load.md) | `fun load(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, request: `[`IconRequest`](../../mozilla.components.browser.icons/-icon-request/index.md)`, resource: `[`Resource`](../../mozilla.components.browser.icons/-icon-request/-resource/index.md)`): `[`Result`](../-icon-loader/-result/index.md)<br>Tries to load the [IconRequest.Resource](../../mozilla.components.browser.icons/-icon-request/-resource/index.md) for the given [IconRequest](../../mozilla.components.browser.icons/-icon-request/index.md). |
