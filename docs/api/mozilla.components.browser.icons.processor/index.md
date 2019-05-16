[android-components](../index.md) / [mozilla.components.browser.icons.processor](./index.md)

## Package mozilla.components.browser.icons.processor

### Types

| Name | Summary |
|---|---|
| [DiskIconProcessor](-disk-icon-processor/index.md) | `class DiskIconProcessor : `[`IconProcessor`](-icon-processor/index.md)<br>[IconProcessor](-icon-processor/index.md) implementation that saves icons in the disk cache. |
| [IconProcessor](-icon-processor/index.md) | `interface IconProcessor`<br>An [IconProcessor](-icon-processor/index.md) implementation receives the [Icon](../mozilla.components.browser.icons/-icon/index.md) with the [IconRequest](../mozilla.components.browser.icons/-icon-request/index.md) and [IconRequest.Resource](../mozilla.components.browser.icons/-icon-request/-resource/index.md) after the icon was loaded. The [IconProcessor](-icon-processor/index.md) has the option to rewrite a loaded [Icon](../mozilla.components.browser.icons/-icon/index.md) and return a new instance. |
| [MemoryIconProcessor](-memory-icon-processor/index.md) | `class MemoryIconProcessor : `[`IconProcessor`](-icon-processor/index.md)<br>An [IconProcessor](-icon-processor/index.md) implementation that saves icons in the in-memory cache. |
