[android-components](../../index.md) / [mozilla.components.service.fxa.manager](../index.md) / [FxaAccountManager](index.md) / [signInWithShareableAccountAsync](./sign-in-with-shareable-account-async.md)

# signInWithShareableAccountAsync

`fun signInWithShareableAccountAsync(fromAccount: `[`ShareableAccount`](../../mozilla.components.service.fxa.sharing/-shareable-account/index.md)`, reuseSessionToken: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false): Deferred<`[`SignInWithShareableAccountResult`](../-sign-in-with-shareable-account-result/index.md)`>` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/service/firefox-accounts/src/main/java/mozilla/components/service/fxa/manager/FxaAccountManager.kt#L258)

Uses a provided [fromAccount](sign-in-with-shareable-account-async.md#mozilla.components.service.fxa.manager.FxaAccountManager$signInWithShareableAccountAsync(mozilla.components.service.fxa.sharing.ShareableAccount, kotlin.Boolean)/fromAccount) to sign-in into a corresponding FxA account without any required
user input. Once sign-in completes, any registered [AccountObserver.onAuthenticated](../../mozilla.components.concept.sync/-account-observer/on-authenticated.md) listeners
will be notified and [authenticatedAccount](authenticated-account.md) will refer to the new account.
This may fail in case of network errors, or if provided credentials are not valid.

### Parameters

`reuseSessionToken` - Whether or not to reuse existing session token (which is part of the [ShareableAccount](../../mozilla.components.service.fxa.sharing/-shareable-account/index.md).

**Return**
A deferred boolean flag indicating success (if true) of the sign-in operation.

