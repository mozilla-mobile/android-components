[android-components](../index.md) / [mozilla.components.feature.toolbar](./index.md)

## Package mozilla.components.feature.toolbar

### Types

| Name | Summary |
|---|---|
| [ToolbarAutocompleteFeature](-toolbar-autocomplete-feature/index.md) | `class ToolbarAutocompleteFeature`<br>Feature implementation for connecting a toolbar with a list of autocomplete providers. |
| [ToolbarFeature](-toolbar-feature/index.md) | `class ToolbarFeature : `[`LifecycleAwareFeature`](../mozilla.components.support.base.feature/-lifecycle-aware-feature/index.md)`, `[`BackHandler`](../mozilla.components.support.base.feature/-back-handler/index.md)<br>Feature implementation for connecting a toolbar implementation with the session module. |
| [ToolbarInteractor](-toolbar-interactor/index.md) | `class ToolbarInteractor`<br>Connects a toolbar instance to the browser engine via use cases |
| [ToolbarPresenter](-toolbar-presenter/index.md) | `class ToolbarPresenter`<br>Presenter implementation for a toolbar implementation in order to update the toolbar whenever the state of the selected session or web extensions changes. |
| [WebExtensionToolbarAction](-web-extension-toolbar-action/index.md) | `open class WebExtensionToolbarAction : `[`Action`](../mozilla.components.concept.toolbar/-toolbar/-action/index.md)<br>An action button that represents an web extension item to be added to the toolbar. |

### Type Aliases

| Name | Summary |
|---|---|
| [SearchUseCase](-search-use-case.md) | `typealias SearchUseCase = (`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>A function representing the search use case, accepting the search terms as string. |
| [WebExtensionBrowserAction](-web-extension-browser-action.md) | `typealias WebExtensionBrowserAction = `[`BrowserAction`](../mozilla.components.concept.engine.webextension/-browser-action/index.md) |
