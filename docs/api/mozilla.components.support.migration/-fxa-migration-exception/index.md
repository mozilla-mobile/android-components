[android-components](../../index.md) / [mozilla.components.support.migration](../index.md) / [FxaMigrationException](./index.md)

# FxaMigrationException

`class FxaMigrationException : `[`Exception`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/support/migration/src/main/java/mozilla/components/support/migration/FennecFxaMigration.kt#L43)

Wraps [FxaMigrationResult](../-fxa-migration-result/index.md) in an exception so that it can be returned via [Result.Failure](../-result/-failure/index.md).

PII note - be careful to not log this exception, as it may contain personal information (wrapped in a JSONException).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `FxaMigrationException(failure: `[`Failure`](../-fxa-migration-result/-failure/index.md)`)`<br>Wraps [FxaMigrationResult](../-fxa-migration-result/index.md) in an exception so that it can be returned via [Result.Failure](../-result/-failure/index.md). |

### Properties

| Name | Summary |
|---|---|
| [failure](failure.md) | `val failure: `[`Failure`](../-fxa-migration-result/-failure/index.md)<br>Wrapped [FxaMigrationResult](../-fxa-migration-result/index.md) indicating exact failure reason. |

### Extension Functions

| Name | Summary |
|---|---|
| [loadResourceAsString](../../mozilla.components.support.test.file/kotlin.-any/load-resource-as-string.md) | `fun `[`Any`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)`.loadResourceAsString(path: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Loads a file from the resources folder and returns its content as a string object. |
