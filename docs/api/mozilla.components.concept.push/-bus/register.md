[android-components](../../index.md) / [mozilla.components.concept.push](../index.md) / [Bus](index.md) / [register](./register.md)

# register

`abstract fun register(type: `[`T`](index.md#T)`, observer: `[`Observer`](-observer/index.md)`<`[`T`](index.md#T)`, `[`M`](index.md#M)`>): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/push/src/main/java/mozilla/components/concept/push/Bus.kt#L38)

Registers an observer to get notified about events.

### Parameters

`observer` - The observer to be notified for the type.