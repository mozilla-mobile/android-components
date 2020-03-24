[android-components](../index.md) / [mozilla.components.service.sync.logins](index.md) / [SyncAuthInvalidException](./-sync-auth-invalid-exception.md)

# SyncAuthInvalidException

`typealias SyncAuthInvalidException = SyncAuthInvalidException` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/sync-logins/src/main/java/mozilla/components/service/sync/logins/SyncableLoginsStorage.kt#L58)

This indicates that the authentication information (e.g. the [SyncUnlockInfo](-sync-unlock-info.md))
provided to [AsyncLoginsStorage.sync](#) is invalid. This often indicates that it's
stale and should be refreshed with FxA (however, care should be taken not to
get into a loop refreshing this information).

