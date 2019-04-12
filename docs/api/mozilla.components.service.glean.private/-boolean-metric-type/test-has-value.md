[android-components](../../index.md) / [mozilla.components.service.glean.private](../index.md) / [BooleanMetricType](index.md) / [testHasValue](./test-has-value.md)

# testHasValue

`fun testHasValue(pingName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = getStorageNames().first()): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/glean/src/main/java/mozilla/components/service/glean/private/BooleanMetricType.kt#L63)

Tests whether a value is stored for the metric for testing purposes only. This function will
attempt to await the last task (if any) writing to the the metric's storage engine before
returning a value.

### Parameters

`pingName` - represents the name of the ping to retrieve the metric for.  Defaults
    to the either the first value in [defaultStorageDestinations](default-storage-destinations.md) or the first
    value in [sendInPings](send-in-pings.md)

**Return**
true if metric value exists, otherwise false

