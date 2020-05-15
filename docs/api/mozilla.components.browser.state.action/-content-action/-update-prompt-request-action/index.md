[android-components](../../../index.md) / [mozilla.components.browser.state.action](../../index.md) / [ContentAction](../index.md) / [UpdatePromptRequestAction](./index.md)

# UpdatePromptRequestAction

`data class UpdatePromptRequestAction : `[`ContentAction`](../index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/action/BrowserAction.kt#L214)

Updates the [PromptRequest](../../../mozilla.components.concept.engine.prompt/-prompt-request/index.md) of the [ContentState](../../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](session-id.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `UpdatePromptRequestAction(sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, promptRequest: `[`PromptRequest`](../../../mozilla.components.concept.engine.prompt/-prompt-request/index.md)`)`<br>Updates the [PromptRequest](../../../mozilla.components.concept.engine.prompt/-prompt-request/index.md) of the [ContentState](../../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](session-id.md). |

### Properties

| Name | Summary |
|---|---|
| [promptRequest](prompt-request.md) | `val promptRequest: `[`PromptRequest`](../../../mozilla.components.concept.engine.prompt/-prompt-request/index.md) |
| [sessionId](session-id.md) | `val sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
