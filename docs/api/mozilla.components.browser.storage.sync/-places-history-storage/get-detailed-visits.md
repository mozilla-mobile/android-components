[android-components](../../index.md) / [mozilla.components.browser.storage.sync](../index.md) / [PlacesHistoryStorage](index.md) / [getDetailedVisits](./get-detailed-visits.md)

# getDetailedVisits

`open suspend fun getDetailedVisits(start: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`, end: `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`VisitInfo`](../../mozilla.components.concept.storage/-visit-info/index.md)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/storage-sync/src/main/java/mozilla/components/browser/storage/sync/PlacesHistoryStorage.kt#L77)

Overrides [HistoryStorage.getDetailedVisits](../../mozilla.components.concept.storage/-history-storage/get-detailed-visits.md)

Retrieves detailed information about all visits that occurred in the given time range.

### Parameters

`start` - The (inclusive) start time to bound the query.

`end` - The (inclusive) end time to bound the query.

**Return**
A list of all visits within the specified range, described by [VisitInfo](../../mozilla.components.concept.storage/-visit-info/index.md).

