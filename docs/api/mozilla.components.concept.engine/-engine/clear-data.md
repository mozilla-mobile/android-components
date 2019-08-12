[android-components](../../index.md) / [mozilla.components.concept.engine](../index.md) / [Engine](index.md) / [clearData](./clear-data.md)

# clearData

`open fun clearData(data: `[`BrowsingData`](-browsing-data/index.md)` = BrowsingData.all(), host: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, onSuccess: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = { }, onError: (`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`) -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)` = { }): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/engine/src/main/java/mozilla/components/concept/engine/Engine.kt#L139)

Clears browsing data stored by the engine.

### Parameters

`data` - the type of data that should be cleared, defaults to all.

`host` - (optional) name of the host for which data should be cleared. If
omitted data will be cleared for all hosts.

`onSuccess` - (optional) callback invoked if the data was cleared successfully.

`onError` - (optional) callback invoked if clearing the data caused an exception.