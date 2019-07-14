[android-components](../../index.md) / [mozilla.components.browser.engine.servo](../index.md) / [ServoEngineSession](index.md) / [loadUrl](./load-url.md)

# loadUrl

`fun loadUrl(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, flags: `[`LoadUrlFlags`](../../mozilla.components.concept.engine/-engine-session/-load-url-flags/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/engine-servo/src/main/java/mozilla/components/browser/engine/servo/ServoEngineSession.kt#L77)

Overrides [EngineSession.loadUrl](../../mozilla.components.concept.engine/-engine-session/load-url.md)

Loads the given URL.

### Parameters

`url` - the url to load.

`flags` - the [LoadUrlFlags](../../mozilla.components.concept.engine/-engine-session/-load-url-flags/index.md) to use when loading the provider url.