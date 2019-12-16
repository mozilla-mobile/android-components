[android-components](../../index.md) / [mozilla.components.concept.engine](../index.md) / [Engine](index.md) / [disableWebExtension](./disable-web-extension.md)

# disableWebExtension

`open fun disableWebExtension(extension: `[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`, onSuccess: (`[`WebExtension`](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`, onError: (`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = { }): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/Engine.kt#L215)

Disables the provided [WebExtension](../../mozilla.components.concept.engine.webextension/-web-extension/index.md). If the extension is already disabled the [onSuccess](disable-web-extension.md#mozilla.components.concept.engine.Engine$disableWebExtension(mozilla.components.concept.engine.webextension.WebExtension, kotlin.Function1((mozilla.components.concept.engine.webextension.WebExtension, kotlin.Unit)), kotlin.Function1((kotlin.Throwable, kotlin.Unit)))/onSuccess)
callback will be invoked, but this method has no effect on the extension.

### Parameters

`onSuccess` - (optional) callback invoked with the enabled [WebExtension](../../mozilla.components.concept.engine.webextension/-web-extension/index.md)

`onError` - (optional) callback invoked if there was an error disabling
the installed extensions. This callback is invoked with an [UnsupportedOperationException](https://developer.android.com/reference/java/lang/UnsupportedOperationException.html)
in case the engine doesn't have web extension support.