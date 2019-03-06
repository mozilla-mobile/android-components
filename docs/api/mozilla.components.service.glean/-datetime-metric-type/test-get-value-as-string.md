[android-components](../../index.md) / [mozilla.components.service.glean](../index.md) / [DatetimeMetricType](index.md) / [testGetValueAsString](./test-get-value-as-string.md)

# testGetValueAsString

`fun testGetValueAsString(pingName: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = getStorageNames().first()): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/glean/src/main/java/mozilla/components/service/glean/DatetimeMetricType.kt#L112)

Returns the string representation of the stored value for testing purposes only. This
function will attempt to await the last task (if any) writing to the the metric's storage
engine before returning a value.

### Parameters

`pingName` - represents the name of the ping to retrieve the metric for.  Defaults
    to the either the first value in [defaultStorageDestinations](default-storage-destinations.md) or the first
    value in [sendInPings](send-in-pings.md)

### Exceptions

`NullPointerException` - if no value is stored

**Return**
value of the stored metric

