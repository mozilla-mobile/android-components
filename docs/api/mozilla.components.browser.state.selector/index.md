[android-components](../index.md) / [mozilla.components.browser.state.selector](./index.md)

## Package mozilla.components.browser.state.selector

### Properties

| Name | Summary |
|---|---|
| [normalTabs](normal-tabs.md) | `val `[`BrowserState`](../mozilla.components.browser.state.state/-browser-state/index.md)`.normalTabs: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`TabSessionState`](../mozilla.components.browser.state.state/-tab-session-state/index.md)`>`<br>List of normal (non-private) tabs. |
| [privateTabs](private-tabs.md) | `val `[`BrowserState`](../mozilla.components.browser.state.state/-browser-state/index.md)`.privateTabs: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`TabSessionState`](../mozilla.components.browser.state.state/-tab-session-state/index.md)`>`<br>List of private tabs. |
| [selectedTab](selected-tab.md) | `val `[`BrowserState`](../mozilla.components.browser.state.state/-browser-state/index.md)`.selectedTab: `[`TabSessionState`](../mozilla.components.browser.state.state/-tab-session-state/index.md)`?`<br>The currently selected tab if there's one. |

### Functions

| Name | Summary |
|---|---|
| [findCustomTab](find-custom-tab.md) | `fun `[`BrowserState`](../mozilla.components.browser.state.state/-browser-state/index.md)`.findCustomTab(tabId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`CustomTabSessionState`](../mozilla.components.browser.state.state/-custom-tab-session-state/index.md)`?`<br>Finds and returns the Custom Tab with the given id. Returns null if no matching tab could be found. |
| [findCustomTabOrSelectedTab](find-custom-tab-or-selected-tab.md) | `fun `[`BrowserState`](../mozilla.components.browser.state.state/-browser-state/index.md)`.findCustomTabOrSelectedTab(customTabId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null): `[`SessionState`](../mozilla.components.browser.state.state/-session-state/index.md)`?`<br>Finds and returns the tab with the given id or the selected tab if no id was provided (null). Returns null if no matching tab could be found or if no selected tab exists. |
| [findTab](find-tab.md) | `fun `[`BrowserState`](../mozilla.components.browser.state.state/-browser-state/index.md)`.findTab(tabId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`TabSessionState`](../mozilla.components.browser.state.state/-tab-session-state/index.md)`?`<br>Finds and returns the tab with the given id. Returns null if no matching tab could be found. |
