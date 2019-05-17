[android-components](../../index.md) / [mozilla.components.browser.icons.loader](../index.md) / [IconLoader](./index.md)

# IconLoader

`interface IconLoader` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/icons/src/main/java/mozilla/components/browser/icons/loader/IconLoader.kt#L15)

A loader that can load an icon from an [IconRequest.Resource](../../mozilla.components.browser.icons/-icon-request/-resource/index.md).

### Types

| Name | Summary |
|---|---|
| [Result](-result/index.md) | `sealed class Result` |

### Functions

| Name | Summary |
|---|---|
| [load](load.md) | `abstract fun load(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, request: `[`IconRequest`](../../mozilla.components.browser.icons/-icon-request/index.md)`, resource: `[`Resource`](../../mozilla.components.browser.icons/-icon-request/-resource/index.md)`): `[`Result`](-result/index.md)<br>Tries to load the [IconRequest.Resource](../../mozilla.components.browser.icons/-icon-request/-resource/index.md) for the given [IconRequest](../../mozilla.components.browser.icons/-icon-request/index.md). |

### Inheritors

| Name | Summary |
|---|---|
| [DataUriIconLoader](../-data-uri-icon-loader/index.md) | `class DataUriIconLoader : `[`IconLoader`](./index.md)<br>An [IconLoader](./index.md) implementation that will base64 decode the image bytes from a data:image uri. |
| [DiskIconLoader](../-disk-icon-loader/index.md) | `class DiskIconLoader : `[`IconLoader`](./index.md)<br>[IconLoader](./index.md) implementation that loads icons from a disk cache. |
| [HttpIconLoader](../-http-icon-loader/index.md) | `class HttpIconLoader : `[`IconLoader`](./index.md)<br>[IconLoader](./index.md) implementation that will try to download the icon for resources that point to an http(s) URL. |
| [MemoryIconLoader](../-memory-icon-loader/index.md) | `class MemoryIconLoader : `[`IconLoader`](./index.md)<br>An [IconLoader](./index.md) implementation that loads icons from an in-memory cache. |
