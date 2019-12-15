[android-components](../../index.md) / [mozilla.components.concept.engine.webextension](../index.md) / [WebExtensionDelegate](./index.md)

# WebExtensionDelegate

`interface WebExtensionDelegate` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/webextension/WebExtensionDelegate.kt#L14)

Notifies applications or other components of engine events related to web
extensions e.g. an extension was installed, or an extension wants to open
a new tab.

### Functions

| Name | Summary |
|---|---|
| [onBrowserActionDefined](on-browser-action-defined.md) | `open fun onBrowserActionDefined(webExtension: `[`WebExtension`](../-web-extension/index.md)`, action: `[`BrowserAction`](../-browser-action/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension defines a browser action. To listen for session-specific overrides of [BrowserAction](../-browser-action/index.md)s and other action-specific events (e.g. opening a popup) see [WebExtension.registerActionHandler](../-web-extension/register-action-handler.md). |
| [onCloseTab](on-close-tab.md) | `open fun onCloseTab(webExtension: `[`WebExtension`](../-web-extension/index.md)`?, engineSession: `[`EngineSession`](../../mozilla.components.concept.engine/-engine-session/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Invoked when a web extension attempts to close a tab via browser.tabs.remove. |
| [onDisabled](on-disabled.md) | `open fun onDisabled(webExtension: `[`WebExtension`](../-web-extension/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension was disabled successfully. |
| [onEnabled](on-enabled.md) | `open fun onEnabled(webExtension: `[`WebExtension`](../-web-extension/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension was enabled successfully. |
| [onInstallPermissionRequest](on-install-permission-request.md) | `open fun onInstallPermissionRequest(webExtension: `[`WebExtension`](../-web-extension/index.md)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Invoked during installation of a [WebExtension](../-web-extension/index.md) to confirm the required permissions. |
| [onInstalled](on-installed.md) | `open fun onInstalled(webExtension: `[`WebExtension`](../-web-extension/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension was installed successfully. |
| [onNewTab](on-new-tab.md) | `open fun onNewTab(webExtension: `[`WebExtension`](../-web-extension/index.md)`?, url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, engineSession: `[`EngineSession`](../../mozilla.components.concept.engine/-engine-session/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension attempts to open a new tab via browser.tabs.create. |
| [onToggleBrowserActionPopup](on-toggle-browser-action-popup.md) | `open fun onToggleBrowserActionPopup(webExtension: `[`WebExtension`](../-web-extension/index.md)`, engineSession: `[`EngineSession`](../../mozilla.components.concept.engine/-engine-session/index.md)`, action: `[`BrowserAction`](../-browser-action/index.md)`): `[`EngineSession`](../../mozilla.components.concept.engine/-engine-session/index.md)`?`<br>Invoked when a browser action wants to toggle a popup view. |
| [onUninstalled](on-uninstalled.md) | `open fun onUninstalled(webExtension: `[`WebExtension`](../-web-extension/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension was uninstalled successfully. |
| [onUpdatePermissionRequest](on-update-permission-request.md) | `open fun onUpdatePermissionRequest(current: `[`WebExtension`](../-web-extension/index.md)`, updated: `[`WebExtension`](../-web-extension/index.md)`, onPermissionsGranted: (`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Invoked when a web extension has changed its permissions while trying to update to a new version. This requires user interaction as the updated extension will not be installed, until the user grants the new permissions. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
