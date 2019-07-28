[android-components](../../index.md) / [mozilla.components.browser.state.action](../index.md) / [ContentAction](./index.md)

# ContentAction

`sealed class ContentAction : `[`BrowserAction`](../-browser-action.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/action/BrowserAction.kt#L117)

[BrowserAction](../-browser-action.md) implementations related to updating the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) of a single [SessionState](../../mozilla.components.browser.state.state/-session-state/index.md) inside
[BrowserState](../../mozilla.components.browser.state.state/-browser-state/index.md).

### Types

| Name | Summary |
|---|---|
| [RemoveIconAction](-remove-icon-action/index.md) | `data class RemoveIconAction : `[`ContentAction`](./index.md)<br>Removes the icon of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-remove-icon-action/session-id.md). |
| [RemoveThumbnailAction](-remove-thumbnail-action/index.md) | `data class RemoveThumbnailAction : `[`ContentAction`](./index.md)<br>Removes the thumbnail of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-remove-thumbnail-action/session-id.md). |
| [UpdateIconAction](-update-icon-action/index.md) | `data class UpdateIconAction : `[`ContentAction`](./index.md)<br>Updates the icon of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-icon-action/session-id.md). |
| [UpdateLoadingStateAction](-update-loading-state-action/index.md) | `data class UpdateLoadingStateAction : `[`ContentAction`](./index.md)<br>Updates the loading state of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-loading-state-action/session-id.md). |
| [UpdateProgressAction](-update-progress-action/index.md) | `data class UpdateProgressAction : `[`ContentAction`](./index.md)<br>Updates the progress of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-progress-action/session-id.md). |
| [UpdateSearchTermsAction](-update-search-terms-action/index.md) | `data class UpdateSearchTermsAction : `[`ContentAction`](./index.md)<br>Updates the search terms of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-search-terms-action/session-id.md). |
| [UpdateSecurityInfo](-update-security-info/index.md) | `data class UpdateSecurityInfo : `[`ContentAction`](./index.md)<br>Updates the [SecurityInfoState](../../mozilla.components.browser.state.state/-security-info-state/index.md) of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-security-info/session-id.md). |
| [UpdateThumbnailAction](-update-thumbnail-action/index.md) | `data class UpdateThumbnailAction : `[`ContentAction`](./index.md)<br>Updates the thumbnail of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-thumbnail-action/session-id.md). |
| [UpdateTitleAction](-update-title-action/index.md) | `data class UpdateTitleAction : `[`ContentAction`](./index.md)<br>Updates the title of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-title-action/session-id.md). |
| [UpdateUrlAction](-update-url-action/index.md) | `data class UpdateUrlAction : `[`ContentAction`](./index.md)<br>Updates the URL of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-url-action/session-id.md). |

### Inheritors

| Name | Summary |
|---|---|
| [RemoveIconAction](-remove-icon-action/index.md) | `data class RemoveIconAction : `[`ContentAction`](./index.md)<br>Removes the icon of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-remove-icon-action/session-id.md). |
| [RemoveThumbnailAction](-remove-thumbnail-action/index.md) | `data class RemoveThumbnailAction : `[`ContentAction`](./index.md)<br>Removes the thumbnail of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-remove-thumbnail-action/session-id.md). |
| [UpdateIconAction](-update-icon-action/index.md) | `data class UpdateIconAction : `[`ContentAction`](./index.md)<br>Updates the icon of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-icon-action/session-id.md). |
| [UpdateLoadingStateAction](-update-loading-state-action/index.md) | `data class UpdateLoadingStateAction : `[`ContentAction`](./index.md)<br>Updates the loading state of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-loading-state-action/session-id.md). |
| [UpdateProgressAction](-update-progress-action/index.md) | `data class UpdateProgressAction : `[`ContentAction`](./index.md)<br>Updates the progress of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-progress-action/session-id.md). |
| [UpdateSearchTermsAction](-update-search-terms-action/index.md) | `data class UpdateSearchTermsAction : `[`ContentAction`](./index.md)<br>Updates the search terms of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-search-terms-action/session-id.md). |
| [UpdateSecurityInfo](-update-security-info/index.md) | `data class UpdateSecurityInfo : `[`ContentAction`](./index.md)<br>Updates the [SecurityInfoState](../../mozilla.components.browser.state.state/-security-info-state/index.md) of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-security-info/session-id.md). |
| [UpdateThumbnailAction](-update-thumbnail-action/index.md) | `data class UpdateThumbnailAction : `[`ContentAction`](./index.md)<br>Updates the thumbnail of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-thumbnail-action/session-id.md). |
| [UpdateTitleAction](-update-title-action/index.md) | `data class UpdateTitleAction : `[`ContentAction`](./index.md)<br>Updates the title of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-title-action/session-id.md). |
| [UpdateUrlAction](-update-url-action/index.md) | `data class UpdateUrlAction : `[`ContentAction`](./index.md)<br>Updates the URL of the [ContentState](../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](-update-url-action/session-id.md). |
