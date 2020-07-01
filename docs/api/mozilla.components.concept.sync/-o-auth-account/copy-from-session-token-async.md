[android-components](../../index.md) / [mozilla.components.concept.sync](../index.md) / [OAuthAccount](index.md) / [copyFromSessionTokenAsync](./copy-from-session-token-async.md)

# copyFromSessionTokenAsync

`abstract fun copyFromSessionTokenAsync(sessionToken: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, kSync: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, kXCS: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): Deferred<<ERROR CLASS>?>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/sync/src/main/java/mozilla/components/concept/sync/OAuthAccount.kt#L213)

Attempts to migrate from an existing session token without user input.
New session token will be created.

### Parameters

`sessionToken` - token string to use for login

`kSync` - sync string for login

`kXCS` - XCS string for login

**Return**
JSON object with the result of the migration or 'null' if it failed.
For up-to-date schema, see underlying implementation in https://github.com/mozilla/application-services/blob/v0.49.0/components/fxa-client/src/migrator.rs#L10
At the moment, it's just "{total_duration: long}".

