---
layout: page
title: Changelog
permalink: /changelog/
---

# 21.0.0-SNAPSHOT (In Development)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v20.0.0...master)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/81?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/master/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/master/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/master/buildSrc/src/main/java/Config.kt)

* **feature-downloads**
  * Added `tryAgain` which can be called on the feature in order to restart a failed download.

# 20.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v19.0.0...v20.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/80?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v20.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v20.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v20.0.0/buildSrc/src/main/java/Config.kt)

* **browser-session**, **feature-customtabs**, **feature-session**, **feature-tabs**
  *  ⚠️ **This is a breaking change**: The `WindowFeature` and `CustomTabWindowFeature` components have been migrated to `browser-state` from `browser-session`. Therefore creating these features now requires a `BrowserStore` instance (instead of a `SessionManager` instance). The `windowRequest` properties have been removed `Session` so window requests can now only be observed on a `BrowserStore` from the `browser-state` component. In addition, `WindowFeature` was moved from `feature-session` to `feature-tabs` because it now makes use of our `TabsUseCases` and this would otherwise cause a dependency cycle.

* **feature-downloads**
  * Added ability to pause, resume, cancel, and try again on a download through the `DownloadNotification`.
  * Added support for multiple, continuous downloads.
  * Added size of the file to the `DownloadNotification`.
  * Added open file functionality to the `DownloadNotification`.
    * Note: you must add a `FileProvider` to your manifest as well as `file_paths.xml`. See SampleBrowser for an example.
    * To open .apk files, you must still add the permission `android.permission.INSTALL_PACKAGES` to your manifest.
  * Improved visuals of `SimpleDownloadDialogFragment` to better match `SitePermissionsDialogFragment`.
    * `SimpleDownloadDialogFragment` can similarly be themed by using `PromptsStyling` properties.
  * Recreated download notification channel with lower importance for Android O+ so that the notification is not audibly intrusive.

* **feature-webnotifications**
  * Adds feature implementation for configuring and displaying web notifications to the user
  ```Kotlin
  WebNotificationFeature(
      applicationContext, engine, icons, R.mipmap.ic_launcher, BrowserActivity::class.java
  )
  ```

* **service-glean**
   * Bumped the Glean SDK version to 19.1.0. This fixes a startup crash on Android SDK 22 devices due to missing `stderr`.

* **concept-engine**
  * Adds support for WebPush abstraction to the Engine.
  * Adds support for WebShare abstraction as a PromptRequest.
  
* **engine-gecko-nightly**
  * Adds support for WebPush in GeckoEngine.

* **support-webextensions**
  * Adds support for sending messages to background pages and scripts in WebExtensions.

* **service-firefox-accounts**
  * Adds `authorizeOAuthCode` method for generating scoped OAuth codes.

* **feature-push**
  * ⚠️ The `AutoPushFeature` now throws when reaching exceptions in the native layer that are unrecoverable.

* **feature-prompts**
  * Adds support for Web Share API using `ShareDelegate`.
  
* **experiments**
  * Fixes a crash when the app version or the experiment's version specifiers are not in the expected format.

# 19.0.1

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v19.0.0...v19.0.1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v19.0.1/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v19.0.1/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v19.0.1/buildSrc/src/main/java/Config.kt)

* **service-glean**
   * Bumped the Glean SDK version to 19.1.0. This fixes a startup crash on Android SDK 22 devices due to missing `stderr`.

# 19.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v18.0.0...v19.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/79?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v19.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v19.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v19.0.0/buildSrc/src/main/java/Config.kt)

* **browser-toolbar**
  * ⚠️ **This is a breaking change**: Refactored the internals to use `ConstraintLayout`. As part of this change the public API was simplified and unused methods/properties have been removed.

* **feature-accounts**
  * Add new `FxaPushSupportFeature` for some underlying support when connecting push and fxa accounts together.

* **browser-state**
  * Added `externalAppType` to `CustomTabConfig` to indicate how the session is being used.

* **service-glean**
   * The Rust implementation of the Glean SDK is now being used.
   * ⚠️ **This is a breaking change**: the `GleanDebugActivity` is no longer exposed from service-glean. Users need to use the one in `mozilla.telemetry.glean.debug.GleanDebugActivity` from the `adb` command line.

* **lib-push-firebase**
   * Fixes a potential bug where we receive a message for another push service that we cannot process.

* **feature-privatemode**
  * Added new feature for private browsing mode.
  * Added `SecureWindowFeature` to prevent screenshots in private browsing mode.

* **browser-engine-gecko**, **browser-engine-gecko-beta**, **browser-engine-gecko-nightly**
  * **Merge day!**
    * `browser-engine-gecko-release`: GeckoView 71.0
    * `browser-engine-gecko-beta`: GeckoView 71.0
    * `browser-engine-gecko-nightly`: GeckoView 72.0

* **feature-push**
  * The `AutoPushFeature` now checks (once every 24 hours) to verify and renew push subscriptions if expired after a cold boot.

# 18.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v17.0.0...v18.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/78?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v18.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v18.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v18.0.0/buildSrc/src/main/java/Config.kt)

* **browser-menu**
  * Adds the ability to create a BrowserMenuCategory, a menu item that defines a category for other menu items

* **concept-engine**
  * Adds the setting `forceUserScalableContent`.

* **engine-gecko-nightly**
  * Implements the setting `forceUserScalableContent`.

* **feature-prompts**
  * Deprecated `PromptFeature` constructor that has parameters for both `Activity` and `Fragment`. Use the constructors that just take either one instead.
  * Changed `sessionId` parameter name to `customTabId`.

# 17.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v16.0.0...v17.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/77?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v17.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v17.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v17.0.0/buildSrc/src/main/java/Config.kt)

* **feature-contextmenu**
  * The "Save Image" context menu item will no longer prompt before downloading the image.

* **concept-engine**
  * Added `WebAppManifest.ShareTarget` data class.

* **lib-crash**
  * Now supports sending caught exceptions.  Use the 'submitCaughtException()' to send caught exceptions if the underlying crash reporter service supports it.
  ```Kotlin
  val job = crashReporter.submitCaughtException(e)
  ```

* **engine**, **engine-gecko-nightly**, **engine-gecko-beta**, **engine-gecko**
  * ⚠️ **This is a breaking change**: Renamed `WebExtensionTabDelegate` to `WebExtensionDelegate` to handle various web extensions related engine events:
  ```kotlin
  GeckoEngine(applicationContext, engineSettings).also {
    it.registerWebExtensionDelegate(object : WebExtensionDelegate {
        override fun onNewTab(webExtension: WebExtension?, url: String, engineSession: EngineSession) {
          sessionManager.add(Session(url), true, engineSession)
        }
    })
  }
  ```
  * ⚠️ **This is a breaking change**: Redirect source and target flags are now passed to history tracking delegates. As part of this change, `HistoryTrackingDelegate.onVisited()` receives a new `PageVisit` data class as its second argument, specifying the `visitType` and `redirectSource`. For more details, please see [PR #4268](https://github.com/mozilla-mobile/android-components/pull/4268).

* **support-webextensions**
  * Added functionality to make sure web extension related events in the engine are reflected in the browser state / store. Instead of attaching a `WebExtensionDelegate` to the engine, and manually reacting to all events, it's now possible to initialize `WebExtensionSupport`, which provides overridable default behaviour for all web extension related engine events:
  ```kotlin
  // Makes sure web extension related events (e.g. an extension is installed, or opens a new tab) are dispatched to the browser store.
  WebExtensionSupport.initialize(components.engine, components.store)

  // If dispatching to the browser store is not desired, all actions / behaviour can be overridden:
  WebExtensionSupport.initialize(components.engine, components.store, onNewTabOverride = {
        _, engineSession, url -> components.sessionManager.add(Session(url), true, engineSession)
  })
  ```

* **browser-menu**
   * Fixes background ripple of Switch in BrowserMenuImageSwitch

# 16.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v15.0.0...v16.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/76?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v16.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v16.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v16.0.0/buildSrc/src/main/java/Config.kt)

* **feature-awesomebar**
  * ⚠️ **This is a breaking change**: `AwesomeBar.Suggestion` now directly takes a Bitmap for the icon param rather than a Unit.

* **feature-pwa**
  * ⚠️ **This is a breaking change**: Intent sent from the `WebAppShortcutManager` now require the consumption of the `SHORTCUT_CATEGORY` in your manifest

* **feature-customtabs**
  * 'CustomTabIntentProcessor' can create private sessions now.

* **browser-session**, **browser-state**, **feature-prompts**
  *  ⚠️ **This is a breaking change**: The `feature-prompts` component has been migrated to `browser-state` from `browser-session`. Therefore creating a `PromptFeature` requires a `BrowserStore` instance (instead of a `SessionManager` instance). The `promptRequest` property has been removed `Session`. Prompt requests can now only be observed on a `BrowserStore` from the `browser-state` component.

* **tooling-detekt**
  * Published detekt rules for internal use. Check module documentation for detailed ruleset description.

* **feature-intent**
  * Added support for NFC tag intents to `TabIntentProcessor`.

* **firefox-accounts**, **service-fretboard**
  * ⚠️ **This is a breaking change**: Due to migration to WorkManager v2.2.0, some classes like `WorkManagerSyncScheduler` and `WorkManagerSyncDispatcher` now expects a `Context` in their constructors.

* **engine**, **engine-gecko-nightly** and **engine-gecko-beta**
  * Added `WebExtensionsTabsDelegate` to support `browser.tabs.create()` in web extensions.
  ```kotlin
  GeckoEngine(applicationContext, engineSettings).also {
    it.registerWebExtensionTabDelegate(object : WebExtensionTabDelegate {
        override fun onNewTab(webExtension: WebExtension?, url: String, engineSession: EngineSession) {
          sessionManager.add(Session(url), true, engineSession)
        }
    })
  }
  ```

# 15.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v14.0.0...v15.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/75?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v15.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v15.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v15.0.0/buildSrc/src/main/java/Config.kt)

* **browser-session**, **browser-state**, **feature-contextmenu**, **feature-downloads**
  * * ⚠️ **This is a breaking change**: Removed the `download` property from `Session`. Downloads can now only be observed on a `BrowserState` from the `browser-state` component. Therefore `ContextMenuUseCases` and `DownloadsUseCases` now require a `BrowserStore` instance.

* **support-ktx**
  * Adds `Resources.Theme.resolveAttribute(Int)` to quickly get a resource ID from a theme.
  * Adds `Context.getColorFromAttr` to get a color int from an attribute.

* **feature-customtabs**
  * Added `CustomTabWindowFeature` to handle windows inside custom tabs, PWAs, and TWAs.

* **feature-tab-collections**

  * Behavior change: In a collection List<TabEntity> is now ordered descending by creation date (newest tab in a collection on top)
* **feature-session**, **engine-gecko-nightly** and **engine-gecko-beta**
  * Added api to manage the tracking protection exception list, any session added to the list will be ignored and the the current tracking policy will not be applied.

  ```kotlin
    val useCase = TrackingProtectionUseCases(sessionManager,engine)

    useCase.addException(session)

    useCase.removeException(session)

    useCase.removeAllExceptions()

    useCase.containsException(session){ contains ->
        // contains indicates if this session is on the exception list.
    }

    useCase.fetchExceptions { exceptions ->
        // exceptions is a list of all the origins that are in the exception list.
    }
  ```

* **support-sync-telemetry**
  * 🆕 New component containing building blocks for sync telemetry.

* **concept-sync**, **services-firefox-accounts**
  ⚠️ **This is a breaking change**
  * Internal implementation of sync changed. Most visible change is that clients are now allowed to change which sync engines are enabled and disabled.
  * `FxaAccountManager#syncNowAsync` takes an instance of a `reason` instead of `startup` boolean flag.
  * `SyncEnginesStorage` is introduced, allowing applications to read and update enabled/disabled state configured `SyncEngine`s.
  * `SyncEngine` is no longer an `enum class`, but a `sealed class` instead. e.g. `SyncEngine.HISTORY` is now `SyncEngine.History`.
  * `DeviceConstellation#setDeviceNameAsync` now takes a `context` in addition to new `name`.
  * `FxaAuthData` now takes an optional `declinedEngines` set of SyncEngines.

# 14.0.1

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v14.0.0...v14.0.1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v14.0.1/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v14.0.1/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v14.0.1/buildSrc/src/main/java/Config.kt)

* **feature-collections**
  * Fixed [#4514](https://github.com/mozilla-mobile/android-components/issues/4514): Do not restore parent tab ID for collections.

* **service-glean**
  *  PR [#4511](https://github.com/mozilla-mobile/android-components/pull/4511/): Always set 'wasMigrated' to false in the Glean SDK.

# 14.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v13.0.0...v14.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/74?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v14.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v14.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v14.0.0/buildSrc/src/main/java/Config.kt)

* **feature-customtabs**
  * Now the color of the tracking protection icon adapts to color of the toolbar.

* **feature-session**, **engine-gecko-nightly** and **engine-gecko-beta**
  * Added a way to exposes the same amount of trackers as Firefox desktop has in it tracking protection panel via TrackingProtectionUseCases.

  ```kotlin
    val useCase = TrackingProtectionUseCases(sessionManager,engine)
    useCase.fetchTrackingLogs(
        session,
        onSuccess = { trackersLog ->
            // A list of all the tracker logger for this session
        },
        onError = { throwable ->
            //A throwable indication what went wrong
        }
    )
  ```

* **browser-toolbar**
  * Resized icons on the toolbar see [#4490](https://github.com/mozilla-mobile/android-components/issues/4490) for more information.
  * Added a way to customize the color of the tracking protection icon via BrowserToolbar.
  ```kotlin
  val toolbar = BrowserToolbar(context)
  toolbar.trackingProtectionColor = Color.BLUE
  ```

* **All components**
  * Increased `androidx.browser` version to `1.2.0-alpha07`.

* **feature-media**
  * Playback will now be stopped and the media notification will get removed if the app's task is getting removed (app is swiped away in task switcher).

* **feature-pwa**
  * Adds `WebAppHideToolbarFeature.onToolbarVisibilityChange` to be notified when the toolbar is shown or hidden.

* **engine-gecko-nightly**
  * Added the ability to exfiltrate Gecko categorical histograms.

* **support-webextensions**
  * 🆕 New component containing building blocks for features implemented as web extensions.

* **lib-push-amazon**
  * Fixed usage of cache version of registration ID in situations when app data is deleted.

* **tools-detekt**
  * New (internal-only) component with custom detekt rules.

* **service-glean**
  * ⚠ **This is a breaking change**: Glean.initialize() must be called on the main thread.

# 13.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v12.0.0...v13.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/73?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v13.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v13.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v13.0.0/buildSrc/src/main/java/Config.kt)

* **All components**
  * Updated Kotlin version from `1.3.40` to `1.3.50`.
  * Updated Kotlin Coroutine library version from `1.2.2` to `1.3.0`.

* **browser-session**
  * Clear session icon only if URL host changes.

* **feature-pwa**
  * Adds `WebAppUseCases.isInstallable` to check if the current session can be installed as a Progressive Web App.

* **feature-downloads**
  *  ⚠️ **This is a breaking change**: The `feature-downloads` component has been migrated to `browser-state` from `browser-session`. Therefore creating a `DownloadsFeature` requires a `BrowserStore` instance (instead of a `SessionManager` instance) and a `DownloadsUseCases` instance now.

* **feature-contextmenu**
  *  ⚠️ **This is a breaking change**: The `feature-contextmenu` component has been migrated to `browser-state` from `browser-session`. Therefore creating a `ContextMenuFeature` requires a `BrowserStore` instance (instead of a `SessionManager` instance) and a `ContextMenuUseCases` instance now.

* **service-glean**
  * ⚠️ **This is a breaking change**: applications need to use `ConceptFetchHttpUploader` for overriding the ping uploading mechanism instead of directly using `concept-fetch` implementations.

* **feature-tabs**
  * ⚠️ **This is a breaking change**: Methods that have been accepting a parent `Session` parameter now expect the parent id (`String`).

* **browser-menu**
   * Adds the ability to create a BrowserMenuImageSwitch, a BrowserMenuSwitch with icon

* **feature-accounts**
  * Added ability to configure FxaWebChannelFeature with a set of `FxaCapability`. Currently there's just one: `CHOOSE_WHAT_TO_SYNC`. It defaults to `false`, so if you want "choose what to sync" selection during auth flows, please specify it.

# 12.0.1

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v12.0.0...v12.0.1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v12.0.1/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v12.0.1/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v12.0.1/buildSrc/src/main/java/Config.kt)

* **lib-push-amazon**
  * Fixed [#4448](https://github.com/mozilla-mobile/android-components/issues/4458): Clearing app data does not reset the registration ID.

# 12.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v11.0.0...v12.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/72?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v12.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v12.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v12.0.0/buildSrc/src/main/java/Config.kt)

* **browser-engine-gecko-nightly**, **browser-engine-gecko-beta** and **browser-engine-gecko**
  * The `TrackingProtectionPolicy.recommended()` and `TrackingProtectionPolicy.strict()`  policies are now aligned with standard and strict (respectively) policies on FireFox desktop, for more details see the [issue #4349](https://github.com/mozilla-mobile/android-components/issues/4349).

* **browser-engine-gecko-nightly** and **browser-engine-gecko-beta**
  * The `TrackingProtectionPolicy.select` function now allows you to indicate if `strictSocialTrackingProtection` should be activated or not. When it is active blocks trackers from the social-tracking-protection-digest256 list, for more details take a look at the [issue #4320](https://github.com/mozilla-mobile/android-components/issues/4320)
  ```kotlin
  val policy = TrackingProtectionPolicy.select(
    strictSocialTrackingProtection = true
  )
  ```

* **context-menu**
  * Exposed title tag from GV in HitResult. Fixes [#1444]. If title is null or blank the src value is returned for title.

* **browser-engine-gecko**, **browser-engine-gecko-beta**, **browser-engine-gecko-nightly**
  * **Merge day!**
    * `browser-engine-gecko-release`: GeckoView 69.0
    * `browser-engine-gecko-beta`: GeckoView 70.0
    * `browser-engine-gecko-nightly`: GeckoView 71.0

* **feature-toolbar**
  *  ⚠️ **This is a breaking change**: The `feature-toolbar` component has been migrated to `browser-state` from `browser-session`. Therefore creating a `ToolbarFeature` requires a `BrowserStore` instance instead of a `SessionManager` instance now.

* **lib-crash**
  * Now supports Breadcrumbs.  Use the 'recordCrashBreadcrumb()' to record Breadcrumbs if the underlying crash reporter service supports it.
  ```Kotlin
  crashReporter.recordCrashBreadcrumb(
      CrashBreadcrumb("Settings button clicked", data, "UI", Level.INFO, Type.USER)
  )
  ```

* **browser-engine-gecko-nightly** and **browser-engine-gecko-beta**
  * The `TrackingProtectionPolicy.strict()` now blocks trackers from the social-tracking-protection-digest256 list, for more details take a look at the [issue #4213](https://github.com/mozilla-mobile/android-components/issues/4213)

* **browser-session**
  *  ⚠️ **This is a breaking change**: `getSessionId` and `EXTRA_SESSION_ID` has moved to the `feature-intent` component.

* **feature-intent**
  *  ⚠️ **This is a breaking change**: `TabIntentProcessor` has moved to the `processing` sub-package, but is still in the same component.

* **browser-engine-gecko**
  * Like with the nightly and beta flavor previously this component now has a hard dependency on the new [universal GeckoView build](https://bugzilla.mozilla.org/show_bug.cgi?id=1508976) that is no longer architecture specific (ARM, x86, ..). With that apps no longer need to specify the GeckoView dependency themselves and synchronize the used version with Android Components. Additionally apps can now make use of [APK splits](https://developer.android.com/studio/build/configure-apk-splits) or [Android App Bundles (AAB)](https://developer.android.com/guide/app-bundle).

* **browser-engine-servo**
  * ❌ We removed the `browser-engine-servo` component since it was not being maintained, updated and used.

* **concept-sync**, **service-firefox-accounts**
  * ⚠️ **This is a breaking change**:
  * `SyncConfig`'s `syncableStores` has been renamed to `supportedEngines`, expressed via new enum type `SyncEngine`.
  * `begin*` OAuthAccount methods now return an `AuthFlowUrl`, which encapsulates an OAuth state identifier.
  * `AccountObserver:onAuthenticated` method now has `authType` parameter (instead of `newAccount`), which describes in detail what caused an authentication.
  * `GlobalSyncableStoreProvider.configureStore` now takes a pair of `Pair<SyncEngine, SyncableStore>`, instead of allowing arbitrary string names for engines.
  * `GlobalSyncableStoreProvider.getStore` is no longer part of the public API.

* **feature-push**
  * Added more logging into `AutoPushFeature` to aid in debugging in release builds.

* **support-ktx**
  * Added variant of `Flow.ifChanged()` that takes a mapping function in order to filter items where the mapped value has not changed.

* **feature-pwa**
  * Adds the ability to create a basic shortcut with a custom label

* **browser-engine-gecko-nightly**
  * Adds support for exposing Gecko scalars through the Glean SDK. See [bug 1579365](https://bugzilla.mozilla.org/show_bug.cgi?id=1579365) for details.

* **support-utils**
  * `Intent.asForegroundServicePendingIntent(Context)` extension method to create pending intent for the service that will play nicely with background execution limitations introduced in Android O (e.g. foreground service).

* **concept-sync**
  * ⚠️ **This is a breaking change**: `action` param of `AuthType.OtherExternal` is now optional. Missing `action` indicates that we really don't know what external authType we've hit.

# 11.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v10.0.0...v11.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/71?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v11.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v11.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v11.0.0/buildSrc/src/main/java/Config.kt)

* **browser-icons**
  * Ensures icons are not cached on the disk in private sessions.

* **browser-menu**
  * Added `startImage` to Highlight of HighlightableMenuItem, which allows changing the startImage in addition to the endImage when highlighted
  * Highlight properties of HighlightableMenuItem `startImage` and `endImage` are now both optional

* **lib-state**
  * Added `Store` extensions to observe `State` using Kotlin's `Flow` API: `Store.flow()`, `Store.flowScoped()`.

* **support-ktx**
  * Added property delegates to work with `SharedPreferences`.
  * Added `Flow.ifChanged()` operator for filtering a `Flow` based on whether a value has changed from the previous one (e.g. `A, A, B, C, A -> A, B, C, A`).

* **feature-customtabs**
  * Added `CustomTabsServiceStore` to track custom tab data in `AbstractCustomTabsService`.

* **feature-pwa**
  * Added support for hiding the toolbar in a Trusted Web Activity.
  * Added `TrustedWebActivityIntentProcessor` to process TWA intents.
  * Added `CustomTabState.trustedOrigins` extension method to turn the verification state of a custom tab into a list of origins.
  * Added `WebAppHideToolbarFeature.onTrustedScopesChange` to change the trusted scopes after the feature is created.

* **service-telemetry**
  * This component is now deprecated. Please use the [Glean SDK](https://mozilla.github.io/glean/book/index.html) instead. This library will not be removed until all projects using it start using the Glean SDK.

* **browser-session**, **feature-intent**
  * ⚠️ **This is a breaking change**: Moved `Intent` related code from `browser-session` to `feature-intent`.

* **feature-media**
  * The `Intent` launched from the media notification now has its action set to `MediaFeature.ACTION_SWITCH_TAB`. In addition to that the extra `MediaFeature.EXTRA_TAB_ID` contains the id of the tab the media notification is displayed for.

# 10.0.1

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v10.0.0...v10.0.1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v10.0.1/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v10.0.1/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v10.0.1/buildSrc/src/main/java/Config.kt)

* **browser-menu**
  * Added `startImage` to Highlight of HighlightableMenuItem, which allows changing the startImage in addition to the endImage when highlighted
  * Highlight properties of HighlightableMenuItem `startImage` and `endImage` are now both optional

* Imported latest state of translations.

# 10.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v9.0.0...v10.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/69?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v10.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v10.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v10.0.0/buildSrc/src/main/java/Config.kt)

* **feature-toolbar**
  * Toolbar Menu is now closed on exiting the app.

* **support-test-appservices**
  * 🆕 New component for synchronizing Application Services' unit testing dependencies used in Android Components.

* **service-location**
  * Added `RegionSearchLocalizationProvider` - A `SearchLocalizationProvider` implementation that uses a `MozillaLocationService` instance to do a region lookup via GeoIP.
  * ⚠️ **This is a breaking change**: An implementation of `SearchLocalizationProvider` now returns a `SearchLocalization` data class instead of multiple properties.

* **service-glean**
  * ⚠️ **This is a breaking change**: `Glean.handleBackgroundEvent` is now an internal API.
  * Added a `QuantityMetricType` (for internal use by Gecko metrics only).

* **browser-engine-gecko(-beta/nightly)**, **concept-engine**
  * Added simplified `Media.state` derived from `Media.playbackState` events.

* **lib-push-adm**, **lib-push-firebase**, **concept-push**
  * Added `isServiceAvailable` to signify if the push service is supported on the device.

* **concept-engine**
  * Added `WebNotification` data class for the web notifications API.

* **browser-engine-system**
  * Fixed issue [4191](https://github.com/mozilla-mobile/android-components/issues/4191) where the `recommended()` tracking category was not getting applied for `SystemEngine`.

* **concept-engine**, **browser-engine-gecko-nightly** and **browser-engine-gecko-beta**:
  * ⚠️ **This is a breaking change**: `TrackingProtectionPolicy` does not have a `safeBrowsingCategories` anymore, Safe Browsing is now a separate setting on the Engine level. To change the default value of `SafeBrowsingPolicy.RECOMMENDED` you have set it through `engine.settings.safeBrowsingPolicy`.
  * This decouples the tracking protection API and safe browsing from each other so you can change the tracking protection policy without affecting your safe browsing policy as described in this issue [#4190](https://github.com/mozilla-mobile/android-components/issues/4190).
  * ⚠️ **Alert for SystemEngine consumers**: The Safe Browsing API is not yet supported on this engine, this will be covered on [#4206](https://github.com/mozilla-mobile/android-components/issues/4206). If you use this API you will get a `UnsupportedSettingException`, however you can use a manifest tag to activate it.

  ```xml
    <manifest>
    <application>
        <meta-data android:name="android.webkit.WebView.EnableSafeBrowsing"
                   android:value="true" />
        ...
     </application>
    </manifest>
  ```

# 9.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v8.0.0...v9.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/68?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v9.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v9.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v9.0.0/buildSrc/src/main/java/Config.kt)

* **browser-menu**
  * Updated the styling of the menu to not have padding on top or bottom. Also modified size of `BrowserMenuItemToolbar` to match `BrowserToolbar`'s height

* **feature-media**
  * Do not display title/url/icon of website in media notification if website is opened in private mode.

* **concept-engine** and **browser-session**
  * ⚠️ **This is a breaking change**: `TrackingProtectionPolicy` removes the `categories` property to expose two new ones `trackingCategories: Array<AntiTrackingCategory>` and `safeBrowsingCategories: Array<SafeBrowsingCategory>` to separate the tracking protection categories from the safe browsing ones.
  * ⚠️ **This is a breaking change**: `TrackingProtectionPolicy.all()` has been replaced by `TrackingProtectionPolicy.strict()` to have similar naming conventions with GeckoView api.
  * ⚠️ **This is a breaking change**: `Tracker#categories` has been replaced by `Tracker#trackingCategories` and `Tracker#cookiePolicies` to better report blocked content see [#4098](https://github.com/mozilla-mobile/android-components/issues/4098).
  * Added: `Session#trackersLoaded` A list of `Tracker`s that could be blocked but has been loaded in this session.
  * Added: `Session#Observer#onTrackerLoaded` Notifies that a tracker that could be blocked has been loaded.

* **browser-toolbar**
  * HTTP sites are now marked as insecure with a broken padlock icon, rather than a globe icon. Apps can revert to the globe icon by using a custom `BrowserToolbar.siteSecurityIcon`.

* **service-firefox-accounts**, **concept-sync**
  * `FxaAccountManager`, if configured with `DeviceCapability.SEND_TAB`, will now automatically refresh device constellation state and poll for device events during initialization and login.
  * `FxaAccountManager.syncNowAsync` can now receive a `debounce` parameter, allowing consumers to specify debounce behaviour of their sync requests.
  * ⚠️ **This is a breaking change:**
  * Removed public methods from `DeviceConstellation` and its implementation in `FxaDeviceConstellation`: `fetchAllDevicesAsync`, `startPeriodicRefresh`, `stopPeriodicRefresh`.
  * `DeviceConstellation#refreshDeviceStateAsync` was renamed to `refreshDevicesAsync`: no longer polls for device events, only updates device states (e.g. new devices, name changes)
  * `pollForEventsAsync` no longer returns the events. Use the observer API instead:
  ```kotlin
  val deviceConstellation = autheneticatedAccount()?.deviceConstellation() ?: return
  deviceConstellation.registerDeviceObserver(
    object: DeviceEventsObserver {
      override fun onEvents(events: List<DeviceEvent>) {
          // Process device events here.
      }
    }, lifecycleOwner, false)
  // Poll for events.
  deviceConstellation.pollForEventsAsync().await()
  ```

* **browser-session**
  * Removed deprecated `CustomTabConfig` helpers. Use the equivalent methods in **feature-customtabs** instead.

* **support-ktx**
  * Removed deprecated methods that have equivalents in Android KTX.

* **concept-sync**, **service-firefox-account**
  * ⚠️ **This is a breaking change**
  * In `OAuthAccount` (and by extension, `FirefoxAccount`) `beginOAuthFlowAsync` no longer need to specify `wantsKeys` parameter; it's automatically inferred from the requested `scopes`.
  * Three new device types now available: `tablet`, `tv`, `vr`.

# 8.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v7.0.0...v8.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/67?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v8.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v8.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v8.0.0/buildSrc/src/main/java/Config.kt)

* **service-glean**
  * Timing distributions now use a functional bucketing algorithm that does not require fixed limits to be defined up front.

* **support-android-test**
  * Added `WebserverRule` - a junit rule that will run a webserver during tests serving content from assets in the test package ([#3893](https://github.com/mozilla-mobile/android-components/issues/3893)).

* **browser-engine-gecko-nightly**
  * The component now exposes an implementation of the Gecko runtime telemetry delegate, `glean.GeckoAdapter`, which can be used to collect Gecko metrics with the Glean SDK.

* **browser-engine-gecko-beta**
  * The component now handles situations where the Android system kills the content process (without killing the main app process) in order to reclaim resources. In those situations the component will automatically recover and restore the last known state of those sessions.

* **browser-toolbar**
  * Changed `BrowserToolbar.siteSecurityColor` to use no icon color filter when the color is set to `Color.TRANSPARENT`.
  * Added `BrowserToolbar.siteSecurityIcon` to use custom security icons with multiple colors in the toolbar.

* **feature-sendtab**
  * Added a `SendTabFeature` that observes account device events with optional support for push notifications.

  ```kotlin
  SendTabFeature(
    context,
    accountManager,
    pushFeature, // optional
    pushService // optional; if you want the service to also be started/stopped based on account changes.
    onTabsReceiver = { from, tabs -> /* Do cool things here! */ }
  )
  ```

* **feature-media**
  * `MediaFeature` is no longer showing a notification for playing media with a very short duration.
  * Lowererd priority of media notification channel to avoid the media notification makign any sounds itself.

# 7.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v6.0.2...v7.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/66?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v7.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v7.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v7.0.0/buildSrc/src/main/java/Config.kt)

* **browser-menu**
  * ⚠️ **This is a breaking change**: `BrowserMenuHighlightableItem` now has a ripple effect and includes an example of how to pass in a drawable properly to also include a ripple when highlighted

* **feature-accounts**
    * ⚠️ **This is a breaking change**:
    * The `FirefoxAccountsAuthFeature` no longer needs an `TabsUseCases`, instead is taking a lambda to
      allow applications to decide which action should be taken. This fixes [#2438](https://github.com/mozilla-mobile/android-components/issues/2438) and [#3272](https://github.com/mozilla-mobile/android-components/issues/3272).

    ```kotlin
     val feature = FirefoxAccountsAuthFeature(
         accountManager,
         redirectUrl
     ) { context, authUrl ->
        // passed-in context allows easily opening new activities for handling urls.
        tabsUseCases.addTab(authUrl)
     }

     // ... elsewhere, in the UI code, handling click on button "Sign In":
     components.feature.beginAuthentication(activityContext)
    ```

* **browser-engine-gecko-nightly**
  * Now supports window requests. A new tab will be opened for `target="_blank"` links and `window.open` calls.

* **browser-icons**
  * Handles low-memory scenarios by reducing memory footprint.

* **feature-app-links**
  * Fixed [#3944](https://github.com/mozilla-mobile/android-components/issues/3944) causing third-party apps being opened when links with a `javascript` scheme are clicked.

* **feature-session**
  * ⚠️ **This is a breaking change**:
  * The `WindowFeature` no longer needs an engine. It can now be created using just:
  ```kotlin
     val windowFeature = WindowFeature(components.sessionManager)
  ```

* **feature-pwa**
  * Added full support for pinning websites to the home screen.
  * Added full support for Progressive Web Apps, which can be pinned and open in their own window.

* **service-glean**
  * Fixed a bug in`TimeSpanMetricType` that prevented multiple consecutive `start()`/`stop()` calls. This resulted in the `glean.baseline.duration` being missing from most [`baseline`](https://mozilla.github.io/glean/book/user/pings/baseline.html) pings.

* **service-firefox-accounts**
  * ⚠️ **This is a breaking change**: `AccountObserver.onAuthenticated` now helps observers distinguish when an account is a new authenticated account one with a second `newAccount` boolean parameter.

* **concept-sync**, **service-firefox-accounts**:
  * ⚠️ **This is a breaking change**: Added `OAuthAccount@disconnectAsync`, which replaced `DeviceConstellation@destroyCurrentDeviceAsync`.

* **lib-crash**
  * ⚠️ **Known issue**: Sending a crash using the `MozillaSocorroService` with GeckoView 69.0 or 68.0, will lead to a `NoSuchMethodError` when using this particular version of android components. See [#4052](https://github.com/mozilla-mobile/android-components/issues/4052).

# 6.0.2

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v6.0.1...v6.0.2)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v6.0.2/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v6.0.2/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v6.0.2/buildSrc/src/main/java/Config.kt)

* **service-glean**
  * Fixed a bug in`TimeSpanMetricType` that prevented multiple consecutive `start()`/`stop()` calls. This resulted in the `glean.baseline.duration` being missing from most [`baseline`](https://mozilla.github.io/glean/book/user/pings/baseline.html) pings.

# 6.0.1

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v6.0.0...v6.0.1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v6.0.1/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v6.0.1/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v6.0.1/buildSrc/src/main/java/Config.kt)

* **feature-app-links**
  * Fixed [#3944](https://github.com/mozilla-mobile/android-components/issues/3944) causing third-party apps being opened when links with a `javascript` scheme are clicked.

* Imported latest state of translations.

# 6.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v5.0.0...v6.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/65?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v6.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v6.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v6.0.0/buildSrc/src/main/java/Config.kt)

* **support-utils**
  * Fixed [#3871](https://github.com/mozilla-mobile/android-components/issues/3871) autocomplete incorrectly fills urls that contains a port number.

* **feature-readerview**
  * Fixed [#3864](https://github.com/mozilla-mobile/android-components/issues/3864) now minus and plus buttons have the same size on reader view.

* **browser-engine-gecko-nightly**
  * The component now handles situations where the Android system kills the content process (without killing the main app process) in order to reclaim resources. In those situations the component will automatically recover and restore the last known state of those sessions.
  * Now supports window requests. A new tab will be opened for `target="_blank"` links and `window.open` calls.

* **service-location**
  * 🆕 A new component for accessing Mozilla's and other location services.

* **feature-prompts**
  * Improved month picker UI, now we have the same widget as Fennec.

* **support-ktx**
  * Deprecated `ViewGroup.forEach` in favour of Android Core KTX.
  * Deprecated `Map.toBundle()` in favour of Android Core KTX `bundleOf`.

* **lib-state**
  * Migrated `Store.broadcastChannel()` to `Store.channel()`returning a `ReceiveChannel` that can be read by only one receiver. Broadcast channels have a more complicated lifetime that is not needed in most use cases. For multiple receivers multiple channels can be created from the `Store` or Kotlin's `ReceiveChannel.broadcast()` extension method can be used.

* **support-android-test**
  * Added `LeakDetectionRule` to install LeakCanary when running instrumented tests. If a leak is found the test will fail and the test report will contain the leak trace.

* **lib-push-amazon**
  * 🆕 Added a new component for Amazon Device Messaging push support.

* **browser-icons**
  * Changed the maximum size for decoded icons. Icons are now scaled to the target size to save memory.

* **service-firefox-account**
 * Added `isSyncActive(): Boolean` method to `FxaAccountManager`

* **feature-customtabs**
  * `CustomTabsToolbarFeature` now optionally takes `Window` as a parameter. It will update the status bar color to match the toolbar color.
  * Custom tabs can now style the navigation bar using `CustomTabsConfig.navigationBarColor`.

* **feature-sendtab**
  * 🆕 New component for send tab use cases.

  ```kotlin
    val sendTabUseCases = SendTabUseCases(accountManager)

    // Send to a particular device
    sendTabUseCases.sendToDeviceAsync("1234", TabData("Mozilla", "https://mozilla.org"))

    // Send to all devices
    sendTabUseCases.sendToAllAsync(TabData("Mozilla", "https://mozilla.org"))

    // Send multiple tabs to devices works too..
    sendTabUseCases.sendToDeviceAsync("1234", listof(tab1, tab2))
    sendTabUseCases.sendToAllAsync(listof(tab1, tab2))
  ```

* **support-ktx**
  * Added `Collection.crossProduct` to retrieve the cartesian product of two `Collections`.

* **service-glean**
  * ⚠️ **This is a breaking change**: `Glean.enableTestingMode` is now `internal`. Tests can use the `GleanTestRule` to enable testing mode. [Updated docs available here](https://mozilla.github.io/glean/book/user/testing-metrics.html).

* **feature-push**
  * Added default arguments when registering for subscriptions/messages.

# 5.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v4.0.0...v5.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/64?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v5.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v5.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v5.0.0/buildSrc/src/main/java/Config.kt)

* **All components**
  * Increased `compileSdkVersion` to 29 (Android Q)

* **feature-tab**
  * ⚠️ **This is a breaking change**: Now `TabsUseCases.SelectTabUseCase` is an interface, if you want to rely on its previous behavior you could keep using `TabsUseCases.selectTab` or use `TabsUseCases.DefaultSelectTabUseCase`.

* **feature-awesomebar**
  * `SessionSuggestionProvider` now have a new parameter `excludeSelectedSession`, to ignore the selected session on the suggestions.

* **concept-engine** and **browser-session**
  * ⚠️ **This is a breaking change**: Function signature changed from `Session.Observer.onTrackerBlocked(session: Session, blocked: String, all: List<String>) = Unit` to `Session.Observer.onTrackerBlocked(session: Session, tracker: Tracker, all: List<Tracker>) = Unit`
  * ⚠️ **This is a breaking change**: Function signature changed from `EngineSession.Observer.onTrackerBlocked(url: String) = Unit` to `EngineSession.Observer.onTrackerBlocked(tracker: Tracker) = Unit`
  * Added: To provide more details about a blocked content, we introduced a new class called `Tracker` this contains information like the `url` and `categories` of the `Tracker`. Among the categories we have `Ad`, `Analytic`, `Social`,`Cryptomining`, `Fingerprinting` and `Content`.

* **browser-icons**
  * Added `BrowserIcons.loadIntoView` to automatically load an icon into an `ImageView`.

* **browser-session**
  * Added `IntentProcessor` interface to represent a class that processes intents to create sessions.
  * Deprecated `CustomTabConfig.isCustomTabIntent` and `CustomTabConfig.createFromIntent`. Use `isCustomTabIntent` and `createFromCustomTabIntent` in feature-customtabs instead.

* **feature-customtabs**
  * Added `CustomTabIntentProcessor` to create custom tab sessions from intents.
  * Added `isCustomTabIntent` to check if an intent is for creating custom tabs.
  * Added `createCustomTabConfigFromIntent` to create a `CustomTabConfig` from a custom tab intent.

* **feature-downloads**
  * `FetchDownloadManager` now determines the filename during the download, resulting in more accurate filenames.

* **feature-intent**
  * Deprecated `IntentProcessor` class and moved some of its code to the new `TabIntentProcessor`.

* **feature-push**
  * Updated the default autopush service endpoint to `updates.push.services.mozilla.com`.

* **service-glean**
  * Hyphens `-` are now allowed in labels for metrics.  See [1566764](https://bugzilla.mozilla.org/show_bug.cgi?id=1566764).
  * ⚠️ **This is a breaking change**: Timespan values are returned in their configured time unit in the testing API.

* **lib-state**
  * Added ability to pause/resume observing a `Store` via `pause()` and `resume()` methods on the subscription
  * When using `observeManually` the returned `Subscription` is in paused state by default.
  * When binding a subscription to a `LifecycleOwner` then this subscription will automatically paused and resumed based on whether the lifecycle is in STARTED state.
  * When binding a subscription to a `View` then this subscription will be paused until the `View` gets attached.
  * Added `Store.broadcastChannel()` to observe state from a coroutine sequentially ordered.
  * Added helpers to process states coming from a `Store` sequentially via `Fragment.consumeFrom(Store)` and `View.consumeFrom(Store)`.

* **support-ktx**
  * ⚠️ **This is a breaking behavior change**: `JSONArray.mapNotNull` is now an inline function, changing the behavior of the `return` keyword within its lambda.
  * Added `View.toScope()` to create a `CoroutineScope` that is active as long as the `View` is attached. Once the `View` gets detached the `CoroutineScope` gets cancelled automatically.  By default coroutines dispatched on the created [CoroutineScope] run on the main dispatcher

* **concept-push**, **lib-push-firebase**, **feature-push**
  * Added `deleteToken` to the PushService interface.
  * Added the implementation for it to Firebase Push implementation.
  * Added `forceRegistrationRenewal` to the AutopushFeature for situations where our current registration token may be invalid for us to use.

* **service-firefox-accounts**
  * Added `AccountMigration`, which may be used to query trusted FxA Auth providers and automatically sign-in into available accounts.

# 4.0.1

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v4.0.0...v4.0.1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v4.0.1/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v4.0.1/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v4.0.1/buildSrc/src/main/java/Config.kt)

* **service-glean**
  * Hyphens `-` are now allowed in labels for metrics.  See [1566764](https://bugzilla.mozilla.org/show_bug.cgi?id=1566764).

* Imported latest state of translations.

* **support-rusthttp**
  * ⚠️ **This is a breaking change**: The application-services (FxA, sync, push) code now will send HTTP requests through a kotlin-provided HTTP stack in all configurations, however it requires configuration at startup. This may be done via the neq `support-rusthttp` component as follows:

  ```kotlin
  import mozilla.components.support.rusthttp.RustHttpConfig
  // Note: other implementions of `Client` from concept-fetch are fine as well.
  import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
  // some point before calling rust code that makes HTTP requests.
  RustHttpConfig.setClient(lazy { HttpURLConnectionClient() })
  ```

  * Note that code which uses a custom megazord **must** call this after initializing the megazord.


# 4.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v3.0.0...v4.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/63?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v4.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v4.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v4.0.0/buildSrc/src/main/java/Config.kt)

* **browser-engine-gecko**, **browser-engine-gecko-beta**, **browser-engine-gecko-nightly**
  * **Merge day!**
    * `browser-engine-gecko-release`: GeckoView 68.0
    * `browser-engine-gecko-beta`: GeckoView 69.0
    * `browser-engine-gecko-nightly`: GeckoView 70.0

* **browser-engine-gecko-beta**
  * Like with the nightly flavor previously (0.55.0) this component now has a hard dependency on the new [universal GeckoView build](https://bugzilla.mozilla.org/show_bug.cgi?id=1508976) that is no longer architecture specific (ARM, x86, ..). With that apps no longer need to specify the GeckoView dependency themselves and synchronize the used version with Android Components. Additionally apps can now make use of [APK splits](https://developer.android.com/studio/build/configure-apk-splits) or [Android App Bundles (AAB)](https://developer.android.com/guide/app-bundle).

* **feature-media**
  * Added `MediaNotificationFeature` - a feature implementation to show an ongoing notification (keeping the app process alive) while web content is playing media.

* **feature-downloads**
  * Added custom notification icon for `FetchDownloadManager`.

* **feature-app-links**
  * Added whitelist for schemes of URLs to open with an external app. This defaults to `mailto`, `market`, `sms` and `tel`.

* **feature-accounts**
  * ⚠️ **This is a breaking change**: Public API for interacting with `FxaAccountManager` and sync changes
  * `FxaAccountManager` now has a new, simplified public API.
  * `BackgroundSyncManager` is longer exists; sync functionality exposed directly via `FxaAccountManager`.
  * See component's [README](https://github.com/mozilla-mobile/android-components/blob/master/components/service/firefox-accounts/README.md) for detailed description of the new API.
  * As part of these changes, token caching issue has been fixed. See [#3579](https://github.com/mozilla-mobile/android-components/pull/3579) for details.

* **concept-engine**, **browser-engine-gecko(-beta/nightly)**.
  * Added `TrackingProtectionPolicy.CookiePolicy` to indicate how cookies should behave for a given `TrackingProtectionPolicy`.
  * Now `TrackingProtectionPolicy.select` allows you to specify a `TrackingProtectionPolicy.CookiePolicy`, if not specified, `TrackingProtectionPolicy.CookiePolicy.ACCEPT_NON_TRACKERS` will be used.
  * Behavior change: Now `TrackingProtectionPolicy.none()` will get assigned a `TrackingProtectionPolicy.CookiePolicy.ACCEPT_ALL`, and both `TrackingProtectionPolicy.all()` and `TrackingProtectionPolicy.recommended()` will have a `TrackingProtectionPolicy.CookiePolicy.ACCEPT_NON_TRACKERS`.

* **concept-engine**, **browser-engine-system**
  * Added `useWideViewPort` in `Settings` to support the viewport HTML meta tag or if a wide viewport should be used. (Only affects `SystemEngineSession`)

* **browser-session**
  * Added `SessionManager.add(List<Session>)` to add a list of `Session`s to the `SessionManager`.

* **feature-tab-collections**
  * ⚠️ **These are breaking changes below**:
  * `Tab.restore()` now returns a `Session` instead of a `SessionManager.Snapshot`
  * `TabCollection.restore()` and `TabCollection.restoreSubset()` now return a `List<Session>` instead of a `SessionManager.Snapshot`

* **support-ktx**
  * Added `onNextGlobalLayout` to add a `ViewTreeObserver.OnGlobalLayoutListener` that is only called once.

# 3.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v2.0.0...v3.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/62?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v3.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v3.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v3.0.0/buildSrc/src/main/java/Config.kt)

* **feature-prompts**
  * Improved file picker prompt by displaying the option to use the camera to capture images,
    microphone to record audio, or video camera to capture a video.
  * The color picker has been redesigned based on Firefox for Android (Fennec).

* **feature-pwa**
  * Added preliminary support for pinning websites to the home screen.

* **browser-search**
  * Loading search engines should no longer deadlock on devices with 1-2 CPUs

* **concept-engine**, **browser-engine-gecko(-beta/nightly)**, **browser-engine-system**
  * Added `EngineView.release()` to manually release an `EngineSession` that is currently being rendered by the `EngineView`. Usually an app does not need to call `release()` manually since `EngineView` takes care of releasing the `EngineSession` on specific lifecycle events. However sometimes the app wants to release an `EngineSession` to immediately render it on another `EngineView`; e.g. when transforming a Custom Tab into a regular browser tab.

* **browser-session**
  * ⚠️ **This is a breaking change**: Removed "default session" behavior from `SessionManager`. This feature was never used by any app except the sample browser.

* **feature-downloads**
  * Added `FetchDownloadManager`, an alternate download manager that uses a fetch `Client` instead of the native Android `DownloadManager`.

* **support-ktx**
  * Deprecated `String.toUri()` in favour of Android Core KTX.
  * Deprecated `View.isGone` and `View.isInvisible` in favour of Android Core KTX.
  * Added `putCompoundDrawablesRelative` and `putCompoundDrawablesRelativeWithIntrinsicBounds`, aliases of `setCompoundDrawablesRelative` that use Kotlin named and default arguments.

# 2.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v1.0.0...v2.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/61?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v2.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v2.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v2.0.0/buildSrc/src/main/java/Config.kt)


* **browser-toolbar**
  * Adds `focus()` which provides a hook for calling `editMode.focus()` to focus the edit mode `urlView`

* **browser-awesomebar**
  * Updated `DefaultSuggestionViewHolder` to have a style more consistent with Fenix mocks.
  * Fixed a bug with `InlineAutocompleteEditText` where the cursor would disappear if a user cleared an suggested URL.

* **lib-state**
  * A new component for maintaining application, screen or component state via a redux-style `Store`. This component provides the architectural foundation for the `browser-state` component (in development).

* **feature-downloads**
  * `onDownloadCompleted` no longer receives the download object and ID.

* **support-ktx**
  * Deprecated `Resource.pxToDp`.
  * Added `Int.dpToPx` to convert from density independent pixels to an int representing screen pixels.
  * Added `Int.dpToFloat` to convert from density independent pixels to a float representing screen pixels.

* **support-ktx**
  * Added `Context.isScreenReaderEnabled` extension to check if TalkBack service is enabled.

* **browser-icons**
  * The component now ships with the [tippy-top-sites](https://github.com/mozilla/tippy-top-sites) top 200 list for looking up icon resources.

* **concept-engine**, **browser-engine-gecko(-beta/nightly)**, **feature-session**, **feature-tabs**
  * Added to support for specifying additional flags when loading URLs. This can be done using the engine session directly, as well as via use cases:

  ```kotlin
  // Bypass cache
  sessionManager.getEngineSession().loadUrl(url, LoadUrlFlags.select(LoadUrlFlags.BYPASS_CACHE))

  // Bypass cache and proxy
  sessionUseCases.loadUrl.invoke(url, LoadUrlFlags.select(LoadUrlFlags.BYPASS_CACHE, LoadUrlFlags.BYPASS_PROXY))
  ```

# 1.0.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.56.0...v1.0.0)
* [Milestone](https://github.com/mozilla-mobile/android-components/milestone/60?closed=1)
* [Dependencies](https://github.com/mozilla-mobile/android-components/blob/v1.0.0/buildSrc/src/main/java/Dependencies.kt)
* [Gecko](https://github.com/mozilla-mobile/android-components/blob/v1.0.0/buildSrc/src/main/java/Gecko.kt)
* [Configuration](https://github.com/mozilla-mobile/android-components/blob/v1.0.0/buildSrc/src/main/java/Config.kt)

* 🛑 Removed deprecated components (See [blog posting](https://mozac.org/2019/05/23/deprecation.html)):
  * feature-session-bundling
  * ui-progress
  * ui-doorhanger

* **concept-engine**, **browser-engine-gecko(-beta/nightly)**, **browser-engine-system**
  * Added `Engine.version` property (`EngineVersion`) for printing and comparing the version of the used engine.

* **browser-menu**
  * Added `endOfMenuAlwaysVisible` property/parameter to `BrowserMenuBuilder` constructor and to `BrowserMenu.show` function.
    When is set to true makes sure the bottom of the menu is always visible, this allows use cases like [#3211](https://github.com/mozilla-mobile/android-components/issues/3211).
  * Added `onDimiss` parameter to `BrowserMenu.show` function, called when the menu is dismissed.
  * Changed `BrowserMenuHighlightableItem` constructor to allow for dynamically toggling the highlight with `invalidate()`.

* **browser-toolbar**
  * Added highlight effect to the overflow menu button when a highlighted `BrowserMenuHighlightableItem` is present.

* **feature-tab-collections**
  * Tabs can now be restored without restoring the ID of the `Session` by using the `restoreSessionId` flag. An app may
    prefer to use new IDs if it expects sessions to get restored multiple times - otherwise breaking the promise of a
    unique ID.

* **browser-search**
  * Added `getProvidedDefaultSearchEngine` to `SearchEngineManager` to return the provided default search engine or the first
    search engine if the default is not set. This allows use cases like [#3344](https://github.com/mozilla-mobile/android-components/issues/3344).

* **feature-tab-collections**
  * Behavior change: `TabCollection` instances returned by `TabCollectionStorage` are now ordered by the last time they have been updated (instead of the time they have been created).

* **lib-crash**
  * [Restrictions to background activity starts](https://developer.android.com/preview/privacy/background-activity-starts) in Android Q+ make it impossible to launch the crash reporter prompt after certain crashes. In those situations the library will show a "crash notification" instead. Clicking on the notification will launch the crash reporter prompt allowing the user to submit a crash report.

----

For older versions see the [changelog archive]({{ site.baseurl }}/changelog/archive).
