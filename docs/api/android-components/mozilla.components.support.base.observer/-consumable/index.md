[android-components](../../index.md) / [mozilla.components.support.base.observer](../index.md) / [Consumable](./index.md)

# Consumable

`class Consumable<T>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/base/src/main/java/mozilla/components/support/base/observer/Consumable.kt#L10)

A generic wrapper for values that can get consumed.

### Functions

| Name | Summary |
|---|---|
| [consume](consume.md) | `fun consume(consumer: (value: `[`T`](index.md#T)`) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Invokes the given lambda and marks the value as consumed if the lambda returns true. |
| [consumeBy](consume-by.md) | `fun consumeBy(consumers: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<(`[`T`](index.md#T)`) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Invokes the given list of lambdas and marks the value as consumed if at least one lambda returns true. |
| [isConsumed](is-consumed.md) | `fun isConsumed(): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns whether the value was consumed. |

### Companion Object Functions

| Name | Summary |
|---|---|
| [empty](empty.md) | `fun <T> empty(): `[`Consumable`](./index.md)`<`[`T`](empty.md#T)`>`<br>Returns an empty Consumable with not value as if it was consumed already. |
| [from](from.md) | `fun <T> from(value: `[`T`](from.md#T)`): `[`Consumable`](./index.md)`<`[`T`](from.md#T)`>`<br>Creates a new Consumable wrapping the given value. |
| [stream](stream.md) | `fun <T> stream(vararg values: `[`T`](stream.md#T)`): `[`ConsumableStream`](../-consumable-stream/index.md)`<`[`T`](stream.md#T)`>`<br>Creates a new Consumable stream for the provided values. |
