[android-components](../../index.md) / [mozilla.components.browser.engine.gecko.webextension](../index.md) / [GeckoPort](index.md) / [postMessage](./post-message.md)

# postMessage

`fun postMessage(message: `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-gecko-nightly/src/main/java/mozilla/components/browser/engine/gecko/webextension/GeckoWebExtension.kt#L101)

Overrides [Port.postMessage](../../mozilla.components.concept.engine.webextension/-port/post-message.md)

Sends a message to this port.

### Parameters

`message` - the message to send, either a primitive type
or a org.json.JSONObject.