[android-components](../../index.md) / [mozilla.components.service.sync.logins](../index.md) / [GeckoLoginStorageDelegate](index.md) / [getPersistenceOperation](./get-persistence-operation.md)

# getPersistenceOperation

`fun getPersistenceOperation(newLogin: `[`Login`](../../mozilla.components.concept.storage/-login/index.md)`, savedLogin: `[`Login`](../../mozilla.components.concept.storage/-login/index.md)`?): `[`Operation`](../-operation/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/sync-logins/src/main/java/mozilla/components/service/sync/logins/GeckoLoginStorageDelegate.kt#L102)

Returns whether an existing login record should be UPDATED or a new one [CREATE](#)d, based
on the saved [Login](../../mozilla.components.concept.storage/-login/index.md) and new [Login](../../mozilla.components.concept.storage/-login/index.md).

