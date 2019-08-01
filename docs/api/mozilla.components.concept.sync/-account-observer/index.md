[android-components](../../index.md) / [mozilla.components.concept.sync](../index.md) / [AccountObserver](./index.md)

# AccountObserver

`interface AccountObserver` [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/concept/sync/src/main/java/mozilla/components/concept/sync/OAuthAccount.kt#L55)

Observer interface which lets its users monitor account state changes and major events.

### Functions

| Name | Summary |
|---|---|
| [onAuthenticated](on-authenticated.md) | `abstract fun onAuthenticated(account: `[`OAuthAccount`](../-o-auth-account/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Account was successfully authenticated. |
| [onAuthenticationProblems](on-authentication-problems.md) | `abstract fun onAuthenticationProblems(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Account needs to be re-authenticated (e.g. due to a password change). |
| [onLoggedOut](on-logged-out.md) | `abstract fun onLoggedOut(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Account just got logged out. |
| [onProfileUpdated](on-profile-updated.md) | `abstract fun onProfileUpdated(profile: `[`Profile`](../-profile/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Account's profile is now available. |
