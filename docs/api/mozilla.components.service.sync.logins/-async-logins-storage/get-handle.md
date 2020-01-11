[android-components](../../index.md) / [mozilla.components.service.sync.logins](../index.md) / [AsyncLoginsStorage](index.md) / [getHandle](./get-handle.md)

# getHandle

`abstract fun getHandle(): `[`Long`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/sync-logins/src/main/java/mozilla/components/service/sync/logins/AsyncLoginsStorage.kt#L279)

This should be removed. See: https://github.com/mozilla/application-services/issues/1877

Note: handles do not remain valid after locking / unlocking the logins database.

**Return**
raw internal handle that could be used for referencing underlying logins database.
Use it with SyncManager.

