[android-components](../../index.md) / [mozilla.components.concept.engine.webextension](../index.md) / [WebExtensionRuntime](index.md) / [installWebExtension](./install-web-extension.md)

# installWebExtension

`open fun installWebExtension(id: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, allowContentMessaging: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = true, supportActions: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, onSuccess: (`[`WebExtension`](../-web-extension/index.md)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = { }, onError: (`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, `[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = { _, _ -> }): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/webextension/WebExtensionRuntime.kt#L33)

Installs the provided extension in this engine.

### Parameters

`id` - the unique ID of the extension.

`url` - the url pointing to either a resources path for locating the extension
within the APK file (e.g. resource://android/assets/extensions/my_web_ext) or to a
local (e.g. resource://android/assets/extensions/my_web_ext.xpi) or remote
(e.g. https://addons.mozilla.org/firefox/downloads/file/123/some_web_ext.xpi) XPI file.

`allowContentMessaging` - whether or not the web extension is allowed
to send messages from content scripts, defaults to true.

`supportActions` - whether or not browser and page actions are handled when
received from the web extension, defaults to false.

`onSuccess` - (optional) callback invoked if the extension was installed successfully,
providing access to the [WebExtension](../-web-extension/index.md) object for bi-directional messaging.

`onError` - (optional) callback invoked if there was an error installing the extension.
This callback is invoked with an [UnsupportedOperationException](https://developer.android.com/reference/java/lang/UnsupportedOperationException.html) in case the engine doesn't
have web extension support.