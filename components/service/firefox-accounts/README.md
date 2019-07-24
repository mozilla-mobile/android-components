# [Android Components](../../../README.md) > Service > Firefox Accounts (FxA)

A library for integrating with Firefox Accounts.

## Motivation

The **Firefox Accounts Android Component** provides both low and high level accounts functionality.

At a low level, there is direct interaction with the accounts system:
* Obtain scoped OAuth tokens that can be used to access the user's data in Mozilla-hosted services like Firefox Sync
* Fetch client-side scoped keys needed for end-to-end encryption of that data
* Fetch a user's profile to personalize the application

At a high level, there is an Account Manager:
* Handles account state management and persistence
* Abstracts away OAuth details, handling scopes, token caching, recovery, etc. Application can still specify custom scopes if needed
* Integrates with FxA device management, automatically creating and destroying device records as appropriate
* (optionally) Provides Send Tab integration - allows sending and receiving tabs within the Firefox Account ecosystem
* (optionally) Provides Firefox Sync integration

Sample applications:
* [accounts sample app](https://github.com/mozilla-mobile/android-components/tree/master/samples/firefox-accounts), demonstrates how to use low level APIs
* [sync app](https://github.com/mozilla-mobile/android-components/tree/master/samples/sync), demonstrates a high level accounts integration, complete with syncing multiple data stores

Useful companion components:
* [feature-accounts](https://github.com/mozilla-mobile/android-components/tree/master/components/feature/accounts), provides a `tabs` integration on top of `FxaAccountManager`, to handle display of web sign-in UI.
* [browser-storage-sync](https://github.com/mozilla-mobile/android-components/tree/master/components/browser/storage-sync), provides data storage layers compatible with Firefox Sync.

## Usage
### Setting up the dependency

Use Gradle to download the library from [maven.mozilla.org](https://maven.mozilla.org/) ([Setup repository](../../../README.md#maven-repository)):

```Groovy
implementation "org.mozilla.components:service-firefox-accounts:{latest-version}"
```

### High level APIs, recommended for most applications

Below is an example of how to integrate most of the common functionality exposed by `FxaAccountManager`.
Additionally, see `feature-accounts`

```kotlin
// Make the two "syncable" stores accessible to account manager's sync machinery.
GlobalSyncableStoreProvider.configureStore("history" to historyStorage)
GlobalSyncableStoreProvider.configureStore("bookmarks" to bookmarksStorage)

val accountManager = FxaAccountManager(
    context = this,
    serverConfig = ServerConfig.release(CLIENT_ID, REDIRECT_URL),
    deviceConfig =DeviceConfig(
        name = "Sample app",
        type = DeviceType.MOBILE,
        capabilities = setOf(DeviceCapability.SEND_TAB)
    ),
    syncConfig = SyncConfig(setOf("history", "bookmarks"), syncPeriodInMinutes = 15L)
)

// Observe changes to the account and profile.
accountManager.register(accountObserver, owner = this, autoPause = true)

// Observe sync state changes.
accountManager.registerForSyncEvents(syncObserver, owner = this, autoPause = true)

// Observe incoming device events (e.g. SEND_TAB events from other devices).
accountManager.registerForDeviceEvents(deviceEventsObserver, owner = this, autoPause = true)

// Now that all of the observers we care about are registered, kick off the account manager.
// If we're already authenticated
launch { accountManager.initAsync().await() }

// 'Sync Now' button binding.
findViewById<View>(R.id.buttonSync).setOnClickListener {
    accountManager.syncNowAsync()
}

// 'Sign-in' button binding.
findViewById<View>(R.id.buttonSignIn).setOnClickListener {
    launch {
        val authUrl = accountManager.beginAuthenticationAsync().await()
        authUrl?.let { openWebView(it) }
    }
}

// 'Sign-out' button binding
findViewById<View>(R.id.buttonLogout).setOnClickListener {
    launch {
        accountManager.logoutAsync().await()
    }
}

// 'Disable periodic sync' button binding
findViewById<View>(R.id.disablePeriodicSync).setOnClickListener {
    launch {
        accountManager.setSyncConfigAsync(
            SyncConfig(setOf("history", "bookmarks")
        ).await()
    }
}

// 'Enable periodic sync' button binding
findViewById<View>(R.id.enablePeriodicSync).setOnClickListener {
    launch {
        accountManager.setSyncConfigAsync(
            SyncConfig(setOf("history", "bookmarks"), syncPeriodInMinutes = 60L)
        ).await()
    }
}

// This is expected to be called from the webview/geckoview integration, which intercepts page loads and gets
// 'code' and 'state' out of the 'successful sign-in redirect' url.
fun onLoginComplete(code: String, state: String) {
    launch {
        accountManager.finishAuthenticationAsync(code, state).await()
    }
}

// Observe changes to account state.
val accountObserver = object : AccountObserver {
    override fun onLoggedOut() = launch {
        // handle logging-out in the UI
    }

    override fun onAuthenticationProblems() = launch {
        // prompt user to re-authenticate
    }

    override fun onAuthenticated(account: OAuthAccount) = launch {
        // logged-in successfully; display account details
    }

    override fun onProfileUpdated(profile: Profile) {
        // display ${profile.displayName} and ${profile.email} if desired
    }
}

// Observe changes to sync state.
val syncObserver = object : SyncStatusObserver {
    override fun onStarted() = launch {
        // sync started running; update some UI to indicate this
    }

    override fun onIdle() = launch {
        // sync stopped running; update some UI to indicate this
    }

    override fun onError(error: Exception?) = launch {
        // sync encountered an error; optionally indicate this in the UI
    }
}

// Observe incoming device events.
val deviceEventsObserver = object : DeviceEventsObserver {
    override fun onEvents(events: List<DeviceEvent>) {
        // device received some events; for example, here's how you can process incoming Send Tab events:
        events.filter { it is DeviceEvent.TabReceived }.forEach {
            val tabReceivedEvent = it as DeviceEvent.TabReceived
            val fromDeviceName = tabReceivedEvent.from?.displayName
            showNotification("Tab ${tab.title}, received from: ${fromDisplayName}", tab.url)
        }
    }
}
```

### Low level APIs

First you need some OAuth information. Generate a `client_id`, `redirectUrl` and find out the scopes for your application.
See the [Firefox Account documentation](https://mozilla.github.io/application-services/docs/accounts/welcome.html)
for that.

Once you have the OAuth info, you can start adding `FxAClient` to your Android project.
As part of the OAuth flow your application will be opening up a WebView or a Custom Tab.
Currently the SDK does not provide the WebView, you have to write it yourself.

Create a global `account` object: 

```kotlin
var account: FirefoxAccount? = null
```

You will need to save state for FxA in your app, this example just uses `SharedPreferences`. We suggest using the [Android Keystore]( https://developer.android.com/training/articles/keystore) for this data.
Define variables to help save state for FxA:

```kotlin
val STATE_PREFS_KEY = "fxaAppState"
val STATE_KEY = "fxaState"
```

Then you can write the following:

```kotlin

account = getAuthenticatedAccount()
if (account == null) {
  // Start authentication flow
  val config = Config(CONFIG_URL, CLIENT_ID, REDIRECT_URL)
  // Some helpers such as Config.release(CLIENT_ID, REDIRECT_URL)
  // are also provided for well-known Firefox Accounts servers.
  account = FirefoxAccount(config)
}

fun getAuthenticatedAccount(): FirefoxAccount? {
    val savedJSON = getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE).getString(FXA_STATE_KEY, "")
    return savedJSON?.let {
        try {
            FirefoxAccount.fromJSONString(it)
        } catch (e: FxaException) {
            null
        }
    } ?: null
}
```

The code above checks if you have some existing state for FxA, otherwise it configures it. All asynchronous methods on `FirefoxAccount` are executed on `Dispatchers.IO`'s dedicated thread pool. They return `Deferred` which is Kotlin's non-blocking cancellable Future type.

Once the configuration is available and an account instance was created, the authentication flow can be started:

```kotlin
launch {
    val url = account.beginOAuthFlow(scopes, wantsKeys).await()
    openWebView(url)
}
```

When spawning the WebView, be sure to override the `OnPageStarted` function to intercept the redirect url and fetch the code + state parameters:

```kotlin
override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
    if (url != null && url.startsWith(redirectUrl)) {
        val uri = Uri.parse(url)
        val mCode = uri.getQueryParameter("code")
        val mState = uri.getQueryParameter("state")
        if (mCode != null && mState != null) {
            // Pass the code and state parameters back to your main activity
            listener?.onLoginComplete(mCode, mState, this@LoginFragment)
        }
    }

    super.onPageStarted(view, url, favicon)
}
```

Finally, complete the OAuth flow, retrieve the profile information, then save your login state once you've gotten valid profile information:

```kotlin
launch {
    // Complete authentication flow    
    account.completeOAuthFlow(code, state).await()

    // Display profile information
    val profile = account.getProfile().await()    
    txtView.txt = profile.displayName

    // Persist login state    
    val json = account.toJSONString()
    getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE).edit()
        .putString(FXA_STATE_KEY, json).apply()
}
```

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
