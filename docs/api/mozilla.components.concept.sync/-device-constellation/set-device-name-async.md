[android-components](../../index.md) / [mozilla.components.concept.sync](../index.md) / [DeviceConstellation](index.md) / [setDeviceNameAsync](./set-device-name-async.md)

# setDeviceNameAsync

`abstract fun setDeviceNameAsync(name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, context: <ERROR CLASS>): Deferred<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/sync/src/main/java/mozilla/components/concept/sync/Devices.kt#L58)

Set name of the current device.

### Parameters

`name` - New device name.

`context` - An application context, used for updating internal caches.

**Return**
A [Deferred](#) that will be resolved with a success flag once operation is complete.

