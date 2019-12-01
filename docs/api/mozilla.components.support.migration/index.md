[android-components](../index.md) / [mozilla.components.support.migration](./index.md)

## Package mozilla.components.support.migration

### Types

| Name | Summary |
|---|---|
| [AbstractMigrationService](-abstract-migration-service/index.md) | `abstract class AbstractMigrationService`<br>Abstract implementation of a background service running a configured [FennecMigrator](-fennec-migrator/index.md). |
| [FennecMigrator](-fennec-migrator/index.md) | `class FennecMigrator`<br>Entrypoint for Fennec data migration. See [Builder](-fennec-migrator/-builder/index.md) for public API. |
| [FennecProfile](-fennec-profile/index.md) | `data class FennecProfile`<br>A profile of "Fennec" (Firefox for Android). |
| [FxaMigrationResult](-fxa-migration-result/index.md) | `sealed class FxaMigrationResult`<br>Result of an FxA migration. |
| [Migration](-migration/index.md) | `sealed class Migration`<br>Supported Fennec migrations and their current versions. |
| [MigrationRun](-migration-run/index.md) | `data class MigrationRun`<br>Results of running a single versioned migration. |
| [Result](-result/index.md) | `sealed class Result<T>`<br>Class representing the result of a successful or failed migration action. |
| [VersionedMigration](-versioned-migration/index.md) | `data class VersionedMigration`<br>Describes a [Migration](-migration/index.md) at a specific version, enforcing in-range version specification. |

### Exceptions

| Name | Summary |
|---|---|
| [FennecMigratorException](-fennec-migrator-exception/index.md) | `sealed class FennecMigratorException : `[`Exception`](https://developer.android.com/reference/java/lang/Exception.html)<br>Exceptions related to Fennec migrations. |
| [FennecProfileException](-fennec-profile-exception/index.md) | `sealed class FennecProfileException : `[`Exception`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/index.html)<br>Exceptions related to Fennec profile migrations. |
| [FxaMigrationException](-fxa-migration-exception/index.md) | `class FxaMigrationException : `[`Exception`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-exception/index.html)<br>Wraps [FxaMigrationResult](-fxa-migration-result/index.md) in an exception so that it can be returned via [Result.Failure](-result/-failure/index.md). |

### Type Aliases

| Name | Summary |
|---|---|
| [MigrationResults](-migration-results.md) | `typealias MigrationResults = `[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`Migration`](-migration/index.md)`, `[`MigrationRun`](-migration-run/index.md)`>`<br>Results of running a set of migrations. |
