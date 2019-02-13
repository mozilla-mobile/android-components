[android-components](../../index.md) / [mozilla.components.support.base.facts](../index.md) / [FactProcessor](./index.md)

# FactProcessor

`interface FactProcessor` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/base/src/main/java/mozilla/components/support/base/facts/FactProcessor.kt#L10)

A [FactProcessor](./index.md) receives [Fact](../-fact/index.md) instances to process them further.

### Functions

| Name | Summary |
|---|---|
| [process](process.md) | `abstract fun process(fact: `[`Fact`](../-fact/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Passes the given [Fact](../-fact/index.md) to the [FactProcessor](./index.md) for processing. |

### Extension Functions

| Name | Summary |
|---|---|
| [register](../register.md) | `fun `[`FactProcessor`](./index.md)`.register(): `[`Facts`](../-facts/index.md)<br>Registers this [FactProcessor](./index.md) to collect [Fact](../-fact/index.md) instances from the [Facts](../-facts/index.md) singleton. |

### Inheritors

| Name | Summary |
|---|---|
| [LogFactProcessor](../../mozilla.components.support.base.facts.processor/-log-fact-processor/index.md) | `class LogFactProcessor : `[`FactProcessor`](./index.md)<br>A [FactProcessor](./index.md) implementation that prints collected [Fact](../-fact/index.md) instances to the log. |
