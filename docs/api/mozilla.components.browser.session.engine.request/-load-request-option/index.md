[android-components](../../index.md) / [mozilla.components.browser.session.engine.request](../index.md) / [LoadRequestOption](./index.md)

# LoadRequestOption

`enum class LoadRequestOption` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/session/src/main/java/mozilla/components/browser/session/engine/request/LoadRequestOption.kt#L16)

Simple enum class for defining the set of characteristics of a [LoadRequest](#).

Facilities for combining these and testing the resulting bit mask also exist as operators.

This should be generalized, but it's not clear if it will be useful enough to go into [kotlin.support](#).

### Enum Values

| Name | Summary |
|---|---|
| [NONE](-n-o-n-e.md) |  |
| [REDIRECT](-r-e-d-i-r-e-c-t.md) |  |
| [WEB_CONTENT](-w-e-b_-c-o-n-t-e-n-t.md) |  |

### Properties

| Name | Summary |
|---|---|
| [mask](mask.md) | `val mask: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Functions

| Name | Summary |
|---|---|
| [toMask](to-mask.md) | `fun toMask(): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [plus](../plus.md) | `operator infix fun `[`LoadRequestOption`](./index.md)`.plus(other: `[`LoadRequestOption`](./index.md)`): `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |
