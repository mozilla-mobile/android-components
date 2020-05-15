[android-components](../../../index.md) / [mozilla.components.browser.state.action](../../index.md) / [ContentAction](../index.md) / [ConsumePromptRequestAction](./index.md)

# ConsumePromptRequestAction

`data class ConsumePromptRequestAction : `[`ContentAction`](../index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/action/BrowserAction.kt#L219)

Removes the [PromptRequest](../../../mozilla.components.concept.engine.prompt/-prompt-request/index.md) of the [ContentState](../../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](session-id.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `ConsumePromptRequestAction(sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`)`<br>Removes the [PromptRequest](../../../mozilla.components.concept.engine.prompt/-prompt-request/index.md) of the [ContentState](../../../mozilla.components.browser.state.state/-content-state/index.md) with the given [sessionId](session-id.md). |

### Properties

| Name | Summary |
|---|---|
| [sessionId](session-id.md) | `val sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
