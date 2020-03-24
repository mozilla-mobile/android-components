[android-components](../../index.md) / [mozilla.components.concept.sync](../index.md) / [OAuthAccount](index.md) / [disconnectAsync](./disconnect-async.md)

# disconnectAsync

`abstract fun disconnectAsync(): Deferred<`[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/sync/src/main/java/mozilla/components/concept/sync/OAuthAccount.kt#L242)

Reset internal account state and destroy current device record.
Use this when device record is no longer relevant, e.g. while logging out. On success, other
devices will no longer see the current device in their device lists.

**Return**
A [Deferred](#) that will be resolved with a success flag once operation is complete.
Failure indicates that we may have failed to destroy current device record. Nothing to do for
the consumer; device record will be cleaned up eventually via TTL.

