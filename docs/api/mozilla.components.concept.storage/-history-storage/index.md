[android-components](../../index.md) / [mozilla.components.concept.storage](../index.md) / [HistoryStorage](./index.md)

# HistoryStorage

`interface HistoryStorage : `[`Storage`](../-storage/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/storage/src/main/java/mozilla/components/concept/storage/HistoryStorage.kt#L11)

An interface which defines read/write methods for history data.

### Functions

| Name | Summary |
|---|---|
| [deleteEverything](delete-everything.md) | `abstract suspend fun deleteEverything(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Remove all locally stored data. |
| [deleteVisitsBetween](delete-visits-between.md) | `abstract suspend fun deleteVisitsBetween(startTime: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, endTime: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Remove history visits in an inclusive range from [startTime](delete-visits-between.md#mozilla.components.concept.storage.HistoryStorage$deleteVisitsBetween(kotlin.Long, kotlin.Long)/startTime) to [endTime](delete-visits-between.md#mozilla.components.concept.storage.HistoryStorage$deleteVisitsBetween(kotlin.Long, kotlin.Long)/endTime). |
| [deleteVisitsFor](delete-visits-for.md) | `abstract suspend fun deleteVisitsFor(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Remove all history visits for a given [url](delete-visits-for.md#mozilla.components.concept.storage.HistoryStorage$deleteVisitsFor(kotlin.String)/url). |
| [deleteVisitsSince](delete-visits-since.md) | `abstract suspend fun deleteVisitsSince(since: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Remove history visits in an inclusive range from [since](delete-visits-since.md#mozilla.components.concept.storage.HistoryStorage$deleteVisitsSince(kotlin.Long)/since) to now. |
| [getAutocompleteSuggestion](get-autocomplete-suggestion.md) | `abstract fun getAutocompleteSuggestion(query: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`HistoryAutocompleteResult`](../-history-autocomplete-result/index.md)`?`<br>Retrieves domain suggestions which best match the [query](get-autocomplete-suggestion.md#mozilla.components.concept.storage.HistoryStorage$getAutocompleteSuggestion(kotlin.String)/query). |
| [getDetailedVisits](get-detailed-visits.md) | `abstract suspend fun getDetailedVisits(start: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, end: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)` = Long.MAX_VALUE): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`VisitInfo`](../-visit-info/index.md)`>`<br>Retrieves detailed information about all visits that occurred in the given time range. |
| [getSuggestions](get-suggestions.md) | `abstract fun getSuggestions(query: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, limit: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`SearchResult`](../-search-result/index.md)`>`<br>Retrieves suggestions matching the [query](get-suggestions.md#mozilla.components.concept.storage.HistoryStorage$getSuggestions(kotlin.String, kotlin.Int)/query). |
| [getVisited](get-visited.md) | `abstract suspend fun getVisited(uris: `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>`<br>Maps a list of page URIs to a list of booleans indicating if each URI was visited.`abstract suspend fun getVisited(): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`>`<br>Retrieves a list of all visited pages. |
| [prune](prune.md) | `abstract suspend fun prune(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Prune history storage, removing stale history. |
| [recordObservation](record-observation.md) | `abstract suspend fun recordObservation(uri: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, observation: `[`PageObservation`](../-page-observation/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Records an observation about a page. |
| [recordVisit](record-visit.md) | `abstract suspend fun recordVisit(uri: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, visitType: `[`VisitType`](../-visit-type/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Records a visit to a page. |

### Inherited Functions

| Name | Summary |
|---|---|
| [cleanup](../-storage/cleanup.md) | `abstract fun cleanup(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Cleans up background work and database connections |
| [runMaintenance](../-storage/run-maintenance.md) | `abstract suspend fun runMaintenance(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Runs internal database maintenance tasks |

### Inheritors

| Name | Summary |
|---|---|
| [InMemoryHistoryStorage](../../mozilla.components.browser.storage.memory/-in-memory-history-storage/index.md) | `class InMemoryHistoryStorage : `[`HistoryStorage`](./index.md)<br>An in-memory implementation of [mozilla.components.concept.storage.HistoryStorage](./index.md). |
| [PlacesHistoryStorage](../../mozilla.components.browser.storage.sync/-places-history-storage/index.md) | `open class PlacesHistoryStorage : `[`PlacesStorage`](../../mozilla.components.browser.storage.sync/-places-storage/index.md)`, `[`HistoryStorage`](./index.md)`, `[`SyncableStore`](../../mozilla.components.concept.sync/-syncable-store/index.md)<br>Implementation of the [HistoryStorage](./index.md) which is backed by a Rust Places lib via [PlacesApi](#). |
