[android-components](../../index.md) / [mozilla.components.browser.state.action](../index.md) / [CustomTabListAction](./index.md)

# CustomTabListAction

`sealed class CustomTabListAction : `[`BrowserAction`](../-browser-action.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/state/src/main/java/mozilla/components/browser/state/action/BrowserAction.kt#L112)

[BrowserAction](../-browser-action.md) implementations related to updating [BrowserState.customTabs](../../mozilla.components.browser.state.state/-browser-state/custom-tabs.md).

### Types

| Name | Summary |
|---|---|
| [AddCustomTabAction](-add-custom-tab-action/index.md) | `data class AddCustomTabAction : `[`CustomTabListAction`](./index.md)<br>Adds a new [CustomTabSessionState](../../mozilla.components.browser.state.state/-custom-tab-session-state/index.md) to [BrowserState.customTabs](../../mozilla.components.browser.state.state/-browser-state/custom-tabs.md). |
| [RemoveAllCustomTabsAction](-remove-all-custom-tabs-action.md) | `object RemoveAllCustomTabsAction : `[`CustomTabListAction`](./index.md)<br>Removes all custom tabs [TabSessionState](../../mozilla.components.browser.state.state/-tab-session-state/index.md)s. |
| [RemoveCustomTabAction](-remove-custom-tab-action/index.md) | `data class RemoveCustomTabAction : `[`CustomTabListAction`](./index.md)<br>Removes an existing [CustomTabSessionState](../../mozilla.components.browser.state.state/-custom-tab-session-state/index.md) to [BrowserState.customTabs](../../mozilla.components.browser.state.state/-browser-state/custom-tabs.md). |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |

### Inheritors

| Name | Summary |
|---|---|
| [AddCustomTabAction](-add-custom-tab-action/index.md) | `data class AddCustomTabAction : `[`CustomTabListAction`](./index.md)<br>Adds a new [CustomTabSessionState](../../mozilla.components.browser.state.state/-custom-tab-session-state/index.md) to [BrowserState.customTabs](../../mozilla.components.browser.state.state/-browser-state/custom-tabs.md). |
| [RemoveAllCustomTabsAction](-remove-all-custom-tabs-action.md) | `object RemoveAllCustomTabsAction : `[`CustomTabListAction`](./index.md)<br>Removes all custom tabs [TabSessionState](../../mozilla.components.browser.state.state/-tab-session-state/index.md)s. |
| [RemoveCustomTabAction](-remove-custom-tab-action/index.md) | `data class RemoveCustomTabAction : `[`CustomTabListAction`](./index.md)<br>Removes an existing [CustomTabSessionState](../../mozilla.components.browser.state.state/-custom-tab-session-state/index.md) to [BrowserState.customTabs](../../mozilla.components.browser.state.state/-browser-state/custom-tabs.md). |
