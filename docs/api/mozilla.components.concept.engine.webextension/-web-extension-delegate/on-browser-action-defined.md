[android-components](../../index.md) / [mozilla.components.concept.engine.webextension](../index.md) / [WebExtensionDelegate](index.md) / [onBrowserActionDefined](./on-browser-action-defined.md)

# onBrowserActionDefined

`open fun onBrowserActionDefined(webExtension: `[`WebExtension`](../-web-extension/index.md)`, action: `[`BrowserAction`](../-browser-action/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/webextension/WebExtensionDelegate.kt#L73)

Invoked when a web extension defines a browser action. To listen for session-specific
overrides of [BrowserAction](../-browser-action/index.md)s and other action-specific events (e.g. opening a popup)
see [WebExtension.registerActionHandler](../-web-extension/register-action-handler.md).

### Parameters

`webExtension` - The [WebExtension](../-web-extension/index.md) defining the browser action.

`action` - the defined [BrowserAction](../-browser-action/index.md).