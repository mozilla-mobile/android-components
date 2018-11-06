---
layout: page
title: Changelog
permalink: /changelog/
---

# 0.31.0-SNAPSHOT (In Development)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.30.0...v0.31.0),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/33?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.30.1/index)

* Compiled against:
  * Android (SDK: 27, Support Libraries: 27.1.1)
  * Kotlin (Stdlib: 1.2.71, Coroutines: 0.30.2)
  * GeckoView (Nightly: 65.0.20181023100123, Beta: 64.0.20181022150107, Release: 63.0.20181018182531)

# 0.30.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.29.0...v0.30.0),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/32?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.30.0/index)

* Compiled against:
  * Android (SDK: 27, Support Libraries: 27.1.1)
  * Kotlin (Stdlib: 1.2.71, Coroutines: 0.30.2)
  * GeckoView (Nightly: 65.0.20181023100123, Beta: 64.0.20181022150107, Release: 63.0.20181018182531)
* **concept-storage**
  * ⚠️ **These are a breaking API changes**
  * Added a `getSuggestions` method to `HistoryStorage`, which is intended to power search, autocompletion, etc.
  * Added a `cleanup` method to `HistoryStorage`, which is intended to allow signaling to implementations to cleanup any allocated resources.
  * `HistoryStorage` methods `recordVisit` and `recordObservation` are now `suspend`.
  * `HistoryStorage` methods `getVisited()` and `getVisited(uris)` now return `Deferred`.
* 🆕 Added **browser-storage-memory** ✨
  * Added an in-memory implementation of `concept-storage`.
* 🆕 Added **browser-storage-sync** ✨
  * Added an implementation of `concept-storage` which is backed by the Rust Places library provided by [application-services](https://github.com/mozilla/application-services).
* **service-firefox-accounts**:
  * ⚠️ **This is a breaking API change**
  * The `FxaResult` type served as a custom promise-like type to support older versions of Java. We have now removed this type and switched to Kotlin's `Deferred` instead. We've also made sure all required types are `Closeable`:

  ```kotlin
  // Before
  Config.custom(CONFIG_URL).then { config: Config ->
    account = FirefoxAccount(config, CLIENT_ID, REDIRECT_URL)
  }

  // Now
  val account = async {
    Config.custom(CONFIG_URL).await().use { config ->
      FirefoxAccount(config, CLIENT_ID, REDIRECT_URL)
    }
  }
  ```
  In case error handling is needed, the new API will also become easier to work with:
  ```kotlin
  // Before
  account.beginOAuthFlow(scopes, wantsKeys).then({ url ->
    showLoginScreen(url)
  }, { exception ->
    handleException(exception)
  })

  // Now
  async {
      try {
        account.beginOAuthFlow(scopes, wantsKeys).await()
      } catch (e: FxaException) {
        handleException(e)
      }
  }

  ```
* **browser-engine-system, browser-engine-gecko, browser-engine-gecko-beta and browser-engine-gecko-nightly**:
  Adding support for using `SystemEngineView` and `GeckoEngineView` in a `CoordinatorLayout`.
  This allows to create nice transitions like hiding the toolbar when scrolls.
* **browser-session**
  * Fixed an issue where a custom tab `Session?` could get selected after removing the currently selected `Session`.
* **browser-toolbar**:
  * Added TwoStateButton that will change drawables based on the `isEnabled` listener. This is particularly useful for
  having a reload/cancel button.
  ```kotlin
  var isLoading: Boolean // updated by some state change.
  BrowserToolbar.TwoStateButton(
      reloadDrawable,
      "reload button",
      cancelDrawable,
      "cancel button",
      { isLoading }
  ) { /* On-click listener */ }
  ```
  * BrowserToolbar APIs for Button and ToggleButton have also been updated to accept `Drawable` instead of resource IDs:
  ```kotlin
  // Before
  BrowserToolbar.Button(R.drawable.image, "image description") {
    // perform an action on click.
  }

  // Now
  val imageDrawable: Drawable = Drawable()
  BrowserToolbar.Button(imageDrawable, "image description") {
    // perform an action on click.
  }

  // Before
  BrowserToolbar.ToggleButton(
    R.drawable.image,
    R.drawable.image_selected,
    "image description",
    "image selected description") {
    // perform an action on click.
  }

  // Now
  val imageDrawable: Drawable = Drawable()
  val imageSelectedDrawable: Drawable = Drawable()
  BrowserToolbar.ToggleButton(
    imageDrawable,
    imageSelectedDrawable,
    "image description",
    "image selected description") {
    // perform an action on click.
  }
  ```
* **concept-awesomebar**
  * 🆕 New component: An abstract definition of an awesome bar component.
* **browser-awesomebar**
  * 🆕 New component: A customizable [Awesome Bar](https://support.mozilla.org/en-US/kb/awesome-bar-search-firefox-bookmarks-history-tabs) implementation for browsers.A
* **feature-awesomebar**
  * 🆕 New component: A component that connects a [concept-awesomebar](https://github.com/mozilla-mobile/android-components/components/concept/awesomebar/README.md) implementation to a [concept-toolbar](https://github.com/mozilla-mobile/android-components/components/concept/toolbar/README.md) implementation and provides implementations of various suggestion providers.

# 0.29.0

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.28.0...v0.29.0),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/31?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.29.0/index)

* Compiled against:
  * Android (SDK: 27, Support Libraries: 27.1.1)
  * Kotlin (Stdlib: **1.2.71** 🔺, Coroutines: **0.30.2** 🔺)
  * GeckoView (Nightly: **65.0.20181023100123** 🔺, Beta: **64.0.20181022150107** 🔺, Release: **63.0.20181018182531** 🔺)
* **browser-toolbar**:
  * Added new listener to get notified when the user is editing the URL:
  ```kotlin
  toolbar.setOnEditListener(object : Toolbar.OnEditListener {
      override fun onTextChanged(text: String) {
          // Fired whenever the user changes the text in the address bar.
      }

      override fun onStartEditing() {
          // Fired when the toolbar switches to edit mode.
      }

      override fun onStopEditing() {
          // Fired when the toolbar switches back to display mode.
      }
  })
  ```
  * Added new toolbar APIs:
  ```kotlin
  val toolbar = BrowserToolbar(context)
  toolbar.textColor: Int = getColor(R.color.photonRed50)
  toolbar.hintColor: Int = getColor(R.color.photonGreen50)
  toolbar.textSize: Float = 12f
  toolbar.typeface: Typeface = Typeface.createFromFile("fonts/foo.tff")
  ```
  These attributes are also available in XML (except for typeface):

    ```xml
    <mozilla.components.browser.toolbar.BrowserToolbar
      android:id="@+id/toolbar"
      app:browserToolbarTextColor="#ff0000"
      app:browserToolbarHintColor="#00ff00"
      app:browserToolbarTextSize="12sp"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"/>
    ```

  * [API improvement](https://github.com/mozilla-mobile/android-components/issues/772) for more flexibility to create a `BrowserToolbar.Button`,
  and `BrowserToolbar.ToggleButton`, now you can provide a custom padding:
  ```kotlin
  val padding = Padding(start = 16, top = 16, end = 16, bottom = 16)
  val button = BrowserToolbar.Button(mozac_ic_back, "Forward", padding = padding) {}
  var toggle = BrowserToolbar.ToggleButton(mozac_ic_pin, mozac_ic_pin_filled, "Pin", "Unpin", padding = padding) {}
  ```
* **concept-toolbar**:
  * [API improvement](https://github.com/mozilla-mobile/android-components/issues/772) for more flexibility to create a `Toolbar.ActionToggleButton`,
  `Toolbar.ActionButton`, `Toolbar.ActionSpace` and `Toolbar.ActionImage`, now you can provide a custom padding:
  ```kotlin
  val padding = Padding(start = 16, top = 16, end = 16, bottom = 16)
  var toggle = Toolbar.ActionToggleButton(0, mozac_ic_pin_filled, "Pin", "Unpin", padding = padding) {}
  val button = Toolbar.ActionButton(mozac_ic_back, "Forward", padding = padding) {}
  val space = Toolbar.ActionSpace(pxToDp(128), padding = padding)
  val image = Toolbar.ActionImage(brand, padding = padding)
  ```
* **support-base**:
  * A new class add for representing an Android Padding.
    ```kotlin
    val padding = Padding(16, 24, 32, 40)
    val (start, top, end, bottom) = padding
    ```
* **support-ktx**:
  * A new extention function that allows you to set `Padding` object to a `View`.
    ```kotlin
    val padding = Padding(16, 24, 32, 40)
    val view = View(context)
    view.setPadding(padding)
    ```
* **concept-engine**, **browser-engine-system**, **browser-engine-gecko(-beta/nightly)**
  * `RequestInterceptor` was enhanced to support loading an alternative URL.
  :warning: **This is a breaking change for the `RequestInterceptor` method signature!**
  ```kotlin
  // To provide alternative content the new InterceptionResponse.Content type needs to be used
  requestInterceptor = object : RequestInterceptor {
      override fun onLoadRequest(session: EngineSession, uri: String): InterceptionResponse? {
          return when (uri) {
              "sample:about" -> InterceptionResponse.Content("<h1>I am the sample browser</h1>")
              else -> null
          }
      }
  }
  // To provide an alternative URL the new InterceptionResponse.Url type needs to be used
  requestInterceptor = object : RequestInterceptor {
      override fun onLoadRequest(session: EngineSession, uri: String): InterceptionResponse? {
          return when (uri) {
              "sample:about" -> InterceptionResponse.Url("sample:aboutNew")
              else -> null
          }
      }
  }
  ```
* **concept-storage**:
  * Added a new concept for describing an interface for storing browser data. First iteration includes a description of `HistoryStorage`.
* **feature-storage**:
  * Added a first iteration of `feature-storage`, which includes `HistoryTrackingFeature` that ties together `concept-storage` and `concept-engine` and allows engines to track history visits and page meta information. It does so by implementing `HistoryTrackingDelegate` defined by `concept-engine`.
  Before adding a first session to the engine, initialize the history tracking feature:
  ```kotlin
  val historyTrackingFeature = HistoryTrackingFeature(
      components.engine,
      components.historyStorage
  )
  ```
  Once the feature has been initialized, history will be tracked for all subsequently added sessions.
* **sample-browser**:
  * Updated the sample browser to track browsing history using an in-memory history storage implementation (how much is actually tracked in practice depends on which engine is being used. As of this release, only `SystemEngine` provides a full set of necessary APIs).
* **lib-crash**
  * Added option to display additional message in prompt and define the theme to be used:
  ```kotlin
  CrashReporter(
      promptConfiguration = CrashReporter.PromptConfiguration(
        // ..

        // An additional message that will be shown in the prompt
        message = "We are very sorry!"

        // Use a custom theme for the prompt (Extend Theme.Mozac.CrashReporter)
        theme = android.R.style.Theme_Holo_Dialog
      )
      // ..
  ).install(applicationContext)
  ```
  * Showing the crash prompt won't play the default activity animation anymore.
  * Added a new sample app `samples-crash` to show and test crash reporter integration.
* **feature-tabs**:
  * `TabsToolbarFeature` is now adding a `TabCounter` from the `ui-tabcounter` component to the toolbar.
* **lib-jexl**
  * New component for evaluating Javascript Expression Language (JEXL) expressions. This implementation is based on [Mozjexl](https://github.com/mozilla/mozjexl) used at Mozilla, specifically as a part of SHIELD and Normandy. In a future version of Fretboard JEXL will allow more complex rules for experiments. For more see [documentation](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/jexl/README.md).
* **service-telemetry**
  * Added option to send list of experiments in event pings: `Telemetry.recordExperiments(Map<String, Boolean> experiments)`
  * Fixed an issue where `DebugLogClient` didn't use the provided log tag.
* **service-fretboard**
  * Fixed an issue where for some locales a `MissingResourceException` would occur.
* **browser-engine-system**
  * Playback of protected media (DRM) is now granted automatically.
* **browser-engine-gecko**
  * Updated components to follow merge day: (Nightly: 65.0, Beta: 64.0, Release: 63.0)

# 0.28.0

Release date: 2018-10-23

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.27.0...v0.28.0),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/30?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.28.0/index)

⚠️ **Note**: This and upcoming releases are **only** available from *maven.mozilla.org*.

* Compiled against:
  * Android (SDK: 27, Support Libraries: 27.1.1)
  * Kotlin (Stdlib: 1.2.61, Coroutines: 0.23.4)
  * GeckoView
    * Nightly: 64.0.20181004100221
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)
* **concept-engine**
  * Added `HistoryTrackingDelegate` interface for integrating engine implementations with history storage backends. Intended to be used via engine settings.
* **browser-engine**
  * `Download.fileName` cannot be `null` anymore. All engine implementations are guaranteed to return a proposed file name for Downloads now.
* **browser-engine-gecko-**, **browser-engine-system**
  * Added support for `HistoryTrackingDelegate`, if it's specified in engine settings.
* **browser-engine-servo**
  * Added a new experimental *Engine* implementation based on the [Servo Browser Engine](https://servo.org/).
* **browser-session** - basic session hierarchy:
  * Sessions can have a parent `Session` now. A `Session` with a parent will be added *after* the parent `Session`. On removal of a selected `Session` the parent `Session` can be selected automatically if desired:
  ```kotlin
  val parent = Session("https://www.mozilla.org")
  val session = Session("https://www.mozilla.org/en-US/firefox/")

  sessionManager.add(parent)
  sessionManager.add(session, parent = parent)

  sessionManager.remove(session, selectParentIfExists = true)
  ```
* **browser-session** - obtaining an restoring a SessionsSnapshot:
  * It's now possible to request a SessionsSnapshot from the `SessionManager`, which encapsulates currently active sessions, their order and state, and which session is the selected one. Private and Custom Tab sessions are omitted from the snapshot. A new public `restore` method allows restoring a `SessionsSnapshot`.
  ```kotlin
  val snapshot = sessionManager.createSnapshot()
  // ... persist snapshot somewhere, perhaps using the DefaultSessionStorage
  sessionManager.restore(snapshot)
  ```
  * `restore` follows a different observer notification pattern from regular `add` flow. See method documentation for details. A new `onSessionsRestored` notification is now available.
* **browser-session** - new SessionStorage API, new DefaultSessionStorage data format:
  * Coupled with the `SessionManager` changes, the SessionStorage API has been changed to operate over `SessionsSnapshot`. New API no longer operates over a SessionManager, and instead reads/writes snapshots which may used together with the SessionManager (see above). An explicit `clear` method is provided for wiping SessionStorage.
  * `DefaultSessionStorage` now uses a new storage format internally, which allows maintaining session ordering and preserves session parent information.
* **browser-errorpages**
  * Added translation annotations to our error page strings. Translated strings will follow in a future release.
* **service-glean**
  * A new client-side telemetry SDK for collecting metrics and sending them to Mozilla's telemetry service. This component is going to eventually replace `service-telemetry`. The SDK is currently in development and the component is not ready to be used yet.
* **lib-dataprotect**
  * The `Keystore` class and its `encryptBytes()` and `decryptBytes()` methods are now open to simplify mocking in unit tests.
* **ui-tabcounter**
  * The `TabCounter` class is now open and can get extended.
* **feature-downloads**
  * Now you're able to provide a dialog before a download starts and customize it to your wish. Take a look at the [updated docs](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/downloads/README.md).

# 0.27.0

Release date: 2018-10-16

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.26.0...v0.27.0),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/27?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.27.0/index)

* Compiled against:
  * Android (SDK: 27, Support Libraries: 27.1.1)
  * Kotlin (Stdlib: 1.2.61, Coroutines: 0.23.4)
  * GeckoView
    * Nightly: **64.0.20181004100221** 🔺
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)
* **browser-engine-system**
  * Fixed a bug where `SystemEngineSession#exitFullScreenMode` didn't invoke the internal callback to exit the fullscreen mode.
  * A new field `defaultUserAgent` was added to `SystemEngine` for testing purposes. This is to circumvent calls to `WebSettings.getDefaultUserAgent` which fails with a `NullPointerException` in Robolectric. If the `SystemEngine` is used in Robolectric tests the following code will be needed:
    ```kotlin
    @Before
    fun setup() {
        SystemEngine.defaultUserAgent = "test-ua-string"
    }
    ```
* **browser-engine-gecko-nightly**:
  * Enabled [Java 8 support](https://developer.android.com/studio/write/java8-support) to meet upstream [GeckoView requirements](https://mail.mozilla.org/pipermail/mobile-firefox-dev/2018-September/002411.html). Apps using this component need to enable Java 8 support as well:
  ```Groovy
  android {
    ...
    compileOptions {
      sourceCompatibility JavaVersion.VERSION_1_8
      targetCompatibility JavaVersion.VERSION_1_8
    }
  }
  ```
* **browser-search**
  * Fixed an issue where a locale change at runtime would not update the search engines.
* **browser-session**:
  * Added reusable functionality for observing sessions, which also support observering the currently selected session, even if it changes.
  ```kotlin
  class MyFeaturePresenter(
      private val sessionManager: SessionManager
  ) : SelectionAwareSessionObserver(sessionManager) {

      fun start() {
          // Always observe changes to the selected session even if the selection changes
          super.observeSelected()

          // To observe changes to a specific session the following method can be used:
          // super.observeFixed(session)
      }

      override fun onUrlChanged(session: Session, url: String) {
          // URL of selected session changed
      }

      override fun onProgress(session: Session, progress: Int) {
         // Progress of selected session changed
      }

      // More observer functions...
  }
  ```
* **browser-errorpages**
  * Added more detailed documentation in the README.
* **feature-downloads**
  * A new components for apps that want to process downloads, for more examples take a look at [here](https://github.com/mozilla-mobile/android-components/blob/master/components/feature/downloads/README.md).
* **lib-crash**
  * A new generic crash reporter component that can report crashes to multiple services ([documentation](https://github.com/mozilla-mobile/android-components/blob/master/components/lib/crash/README.md)).
* **support-ktx**
  * Added new helper method to run a block of code with a different StrictMode policy:
  ```kotlin
  StrictMode.allowThreadDiskReads().resetAfter {
    // In this block disk reads are not triggering a strict mode violation
  }
  ```
  * Added a new helper for checking if you have permission to do something or not:
  ```kotlin
    var isGranted = context.isPermissionGranted(INTERNET)
    if (isGranted) {
        //You can proceed
    } else {
        //Request permission
    }
  ```
* **support-test**
  * Added a new helper for granting permissions in  Robolectric tests:
  ```kotlin
     val context = RuntimeEnvironment.application
     var isGranted = context.isPermissionGranted(INTERNET)

     assertFalse(isGranted) //False permission is not granted yet.

     grantPermission(INTERNET) // Now you have permission.

     isGranted = context.isPermissionGranted(INTERNET)

     assertTrue(isGranted) // True :D
  ```

# 0.26.0

Release date: 2018-10-05

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.25.1...v0.26.0),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/26?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.26.0/index)

* Compiled against:
  * Android (SDK: 27, Support Libraries: 27.1.1)
  * Kotlin (Stdlib: 1.2.61, Coroutines: 0.23.4)
  * GeckoView
    * Nightly: 64.0.20180905100117
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)

* ⚠️ **Releases are now getting published on [maven.mozilla.org](http://maven.mozilla.org/?prefix=maven2/org/mozilla/components/)**.
  * Additionally all artifacts published now use an artifact name that matches the gradle module name (e.g. `browser-toolbar` instead of just `toolbar`).
  * All artifcats are published with the group id `org.mozilla.components` (`org.mozilla.photon` is not being used anymore).
  * For a smooth transition all artifacts still get published on JCenter with the old group ids and artifact ids. In the near future releases will only be published on maven.mozilla.org. Old releases will remain on JCenter and not get removed.
* **browser-domains**
  * Removed `microsoftonline.com` from the global and localized domain lists. No content is being served from that domain. Only subdomains like `login.microsoftonline.com` are used.
* **browser-errorpages**
  * Added error page support for multiple error types.
    ```kotlin
    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType, // This used to be an Int
        uri: String?
    ): RequestInterceptor.ErrorResponse? {
        // Create an error page.
        val errorPage = ErrorPages.createErrorPage(context, errorType)
        // Return it to the request interceptor to take care of default error cases.
        return RequestInterceptor.ErrorResponse(errorPage)
    }
    ```
  * :warning: **This is a breaking change for the `RequestInterceptor#onErrorRequest` method signature!**
* **browser-engine-***
  * Added a setting for enabling remote debugging.
  * Creating an `Engine` requires a `Context` now.
      ```kotlin
       val geckoEngine = GeckoEngine(context)
       val systemEngine = SystemEngine(context)
      ```
* **browser-engine-system**
  * The user agent string now defaults to WebView's default, if not provided, and to the user's default, if provided. It can also be read and changed:
    ```kotlin
    // Using WebView's default
    val engine = SystemEngine(context)

    // Using customized WebView default
    val engine = SystemEngine(context)
    engine.settings.userAgentString = buildUserAgentString(engine.settings.userAgentString)

    // Using custom default
    val engine = SystemEngine(context, DefaultSettings(userAgentString = "foo"))
    ```
  * The tracking protection policy can now be set, both as a default and at any time later.
    ```kotlin
    // Set the default tracking protection policy
    val engine = SystemEngine(context, DefaultSettings(
      trackingProtectionPolicy = TrackingProtectionPolicy.all())
    )

    // Change the tracking protection policy
    engine.settings.trackingProtectionPolicy = TrackingProtectionPolicy.select(
      TrackingProtectionPolicy.AD,
      TrackingProtectionPolicy.SOCIAL
    )
    ```
* **browser-engine-gecko(-*)**
  * Creating a `GeckoEngine` requires a `Context` now. Providing a `GeckoRuntime` is now optional.
* **browser-session**
  * Fixed an issue that caused a Custom Tab `Session` to get selected if it is the first session getting added.
  * `Observer` instances that get attached to a `LifecycleOwner` can now automatically pause and resume observing whenever the lifecycle pauses and resumes. This behavior is off by default and can be enabled by using the `autoPause` parameter when registering the `Observer`.
    ```kotlin
    sessionManager.register(
        observer = object : SessionManager.Observer {
            // ...
        },
        owner = lifecycleOwner,
        autoPause = true
    )
    ```
  * Added an optional callback to provide a default `Session` whenever  `SessionManager` is empty:
    ```kotlin
    val sessionManager = SessionManager(
        engine,
        defaultSession = { Session("https://www.mozilla.org") }
    )
    ```
* **service-telemetry**
  * Added `Telemetry.getClientId()` to let consumers read the client ID.
  * `Telemetry.recordSessionEnd()` now takes an optional callback to be executed upon failure - instead of throwing `IllegalStateException`.
* **service-fretboard**
  * Added `ValuesProvider.getClientId()` to let consumers specify the client ID to be used for bucketing the client. By default fretboard will generate and save an internal UUID used for bucketing. By specifying the client ID consumers can use the same ID for telemetry and bucketing.
  * Update jobs scheduled with `WorkManagerSyncScheduler` will now automatically retry if the configuration couldn't get updated.
  * The update interval of `WorkManagerSyncScheduler` can now be configured.
  * Fixed an issue when reading a corrupt experiments file from disk.
  * Added a workaround for HttpURLConnection throwing ArrayIndexOutOfBoundsException.
* **ui-autocomplete**
  * Fixed an issue causing desyncs between the soft keyboard and `InlineAutocompleteEditText`.
* **samples-firefox-accounts**
  * Showcasing new pairing flow which allows connecting new devices to existing accounts using a QR code.

# 0.25.1 (2018-09-27)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.25...v0.25.1),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/28?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.25.1/index)

* Compiled against:
  * Android
    * SDK: 27
    * Support Libraries: 27.1.1
  * Kotlin
    * Standard library: 1.2.61
    * Coroutines: 0.23.4
  * GeckoView
    * Nightly: 64.0.20180905100117
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)

* **browser-engine-system**: Fixed a `NullPointerException` in `SystemEngineSession.captureThumbnail()`.

# 0.25 (2018-09-26)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.24...v0.25),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/25?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.25/index)

* Compiled against:
  * Android
    * SDK: 27
    * Support Libraries: 27.1.1
  * Kotlin
    * Standard library: 1.2.61
    * Coroutines: 0.23.4
  * GeckoView
    * Nightly: 64.0.20180905100117
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)

* ⚠️ **This is the last release compiled against Android SDK 27. Upcoming releases of the components will require Android SDK 28**.
* **service-fretboard**:
  * Fixed a bug in `FlatFileExperimentStorage` that caused updated experiment configurations not being saved to disk.
  * Added [WorkManager](https://developer.android.com/reference/kotlin/androidx/work/WorkManager) implementation for updating experiment configurations in the background (See ``WorkManagerSyncScheduler``).
  * `Experiment.id` is not accessible by component consumers anymore.
* **browser-engine-system**:
  * URL changes are now reported earlier; when the URL of the main frame changes.
  * Fixed an issue where fullscreen mode would only take up part of the screen.
  * Fixed a crash that could happen when loading invalid URLs.
  * `RequestInterceptor.onErrorRequest()` can return custom error page content to be displayed now (the original URL that caused the error will be preserved).
* **feature-intent**: New component providing intent processing functionality (Code moved from *feature-session*).
* **support-utils**: `DownloadUtils.guessFileName()` will replace extension in the URL with the MIME type file extension if needed (`http://example.com/file.aspx` + `image/jpeg` -> `file.jpg`).

# 0.24 (2018-09-21)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.23...v0.24),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/24?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.24/index)

* Compiled against:
  * Android
    * SDK: 27
    * Support Libraries: 27.1.1
  * Kotlin
    * Standard library: 1.2.61
    * Coroutines: 0.23.4
  * GeckoView
    * Nightly: 64.0.20180905100117
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)

* **dataprotect**:
  * Added a component using AndroidKeyStore to protect user data.
  ```kotlin
  // Create a Keystore and generate a key
  val keystore: Keystore = Keystore("samples-dataprotect")
  keystore.generateKey()

  // Encrypt data
  val plainText = "plain text data".toByteArray(StandardCharsets.UTF_8)
  val encrypted = keystore.encryptBytes(plain)

  // Decrypt data
  val samePlainText = keystore.decryptBytes(encrypted)
  ```
* **concept-engine**: Enhanced settings to cover most common WebView settings.
* **browser-engine-system**:
  * `SystemEngineSession` now provides a way to capture a screenshot of the actual content of the web page just by calling `captureThumbnail`
* **browser-session**:
  * `Session` exposes a new property called `thumbnail` and its internal observer also exposes a new listener `onThumbnailChanged`.

  ```Kotlin
  session.register(object : Session.Observer {
      fun onThumbnailChanged(session: Session, bitmap: Bitmap?) {
              // Do Something
      }
  })
  ```

  * `SessionManager` lets you notify it when the OS is under low memory condition by calling to its new function `onLowMemory`.

* **browser-tabstray**:

   * Now on `BrowserTabsTray` every tab gets is own thumbnail :)

* **support-ktx**:

   * Now you can easily query if the OS is under low memory conditions, just by using `isOSOnLowMemory()` extention function on `Context`.

  ```Kotlin
  val shouldReduceMemoryUsage = context.isOSOnLowMemory()

  if (shouldReduceMemoryUsage) {
      //Deallocate some heavy objects
  }
  ```

  * `View.dp` is now`Resource.pxtoDp`.

  ```Kotlin
  // Before
  toolbar.dp(104)

  // Now
  toolbar.resources.pxToDp(104)
  ```
* **samples-browser**:
   * Updated to show the new features related to tab thumbnails. Be aware that this feature is only available for `systemEngine` and you have to switch to the build variant `systemEngine*`.

# 0.23 (2018-09-13)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.22...v0.23),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/23?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.23/index)

* Compiled against:
  * Android
    * SDK: 27
    * Support Libraries: 27.1.1
  * Kotlin
    * Standard library: 1.2.61
    * Coroutines: 0.23.4
  * GeckoView
    * Nightly: 64.0.20180905100117
    * Beta: 63.0b3 (0269319281578bff4e01d77a21350bf91ba08620)
    * Release: 62.0 (9cbae12a3fff404ed2c12070ad475424d0ae869f)

* Added initial documentation for the browser-session component: https://github.com/mozilla-mobile/android-components/blob/master/components/browser/session/README.md
* **sync-logins**: New component for integrating with Firefox Sync (for Logins). A sample app showcasing this new functionality can be found at: https://github.com/mozilla-mobile/android-components/tree/master/samples/sync-logins
* **browser-engine-***:
  * Added support for fullscreen mode and the ability to exit it programmatically if needed.
  ```Kotlin
  session.register(object : Session.Observer {
      fun onFullScreenChange(enabled: Boolean) {
          if (enabled) {
              // ..
              sessionManager.getEngineSession().exitFullScreenMode()
          }
      }
  })
  ```
* **concept-engine**, **browser-engine-system**, **browser-engine-gecko(-beta/nightly)**:
  * We've extended support for intercepting requests to also include intercepting of errors
  ```Kotlin
  val interceptor = object : RequestInterceptor {
    override fun onErrorRequest(
      session: EngineSession,
      errorCode: Int,
      uri: String?
    ) {
      engineSession.loadData("<html><body>Couldn't load $uri!</body></html>")
    }
  }
  // GeckoEngine (beta/nightly) and SystemEngine support request interceptors.
  GeckoEngine(runtime, DefaultSettings(requestInterceptor = interceptor))
  ```
* **browser-engine-system**:
    * Added functionality to clear all browsing data
    ```Kotlin
    sessionManager.getEngineSession().clearData()
    ```
    * `onNavigationStateChange` is now called earlier (when the title of a web page is available) to allow for faster toolbar updates.
* **feature-session**: Added support for processing `ACTION_SEND` intents (`ACTION_VIEW` was already supported)

  ```Kotlin
  // Triggering a search if the provided EXTRA_TEXT is not a URL
  val searchHandler: TextSearchHandler = { searchTerm, session ->
       searchUseCases.defaultSearch.invoke(searchTerm, session)
  }

  // Handles both ACTION_VIEW and ACTION_SEND intents
  val intentProcessor = SessionIntentProcessor(
      sessionUseCases, sessionManager, textSearchHandler = searchHandler
  )
  intentProcessor.process(intent)
  ```
* Replaced some miscellaneous uses of Java 8 `forEach` with Kotlin's for consistency and backward-compatibility.
* Various bug fixes (see [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.22...v0.23) for details).

# 0.22 (2018-09-07)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.21...v0.22),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/22?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.22/index)

* Compiled against:
  * Android
    * SDK: 27
    * Support Libraries: 27.1.1
  * Kotlin
    * Standard library: 1.2.61
    * Coroutines: 0.23.4
  * GeckoView
    * Nightly: **64.0.20180905100117** 🔺
    * Beta: **63.0b3** (0269319281578bff4e01d77a21350bf91ba08620) 🔺
    * Release: **62.0** (9cbae12a3fff404ed2c12070ad475424d0ae869f) 🔺

* We now provide aggregated API docs. The docs for this release are hosted at: https://mozilla-mobile.github.io/android-components/api/0.22
* **browser-engine-***:
  * EngineView now exposes lifecycle methods with default implementations. A `LifecycleObserver` implementation is provided which forwards events to EngineView instances.
  ```Kotlin
  lifecycle.addObserver(EngineView.LifecycleObserver(view))
  ```
  * Added engine setting for blocking web fonts:
  ```Kotlin
  GeckoEngine(runtime, DefaultSettings(webFontsEnabled = false))
  ```
  * `setDesktopMode()` was renamed to `toggleDesktopMode()`.
* **browser-engine-system**: The `X-Requested-With` header is now cleared (set to an empty String).
* **browser-session**: Desktop mode can be observed now:
  ```Kotlin
  session.register(object : Session.Observer {
      fun onDesktopModeChange(enabled: Boolean) {
          // ..
      }
  })
  ```
* **service-fretboard**:
  * `Fretboard` now has synchronous methods for adding and clearing overrides: `setOverrideNow()`, `clearOverrideNow`, `clearAllOverridesNow`.
  * Access to `Experiment.id` is now deprecated and is scheduled to be removed in a future release (target: 0.24). The `id` is an implementation detail of the underlying storage service and was not meant to be exposed to apps.
* **ui-tabcounter**: Due to a packaging error previous releases of this component didn't contain any compiled code. This is the first usable release of the component.


# 0.21 (2018-08-31)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.20...v0.21),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/21?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.21/index)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library **1.2.61** 🔺
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: **63.0.20180830111743** 🔺
    * Beta: **62.0b21** (7ce198bb7ce027d450af3f69a609896671adfab8) 🔺
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **concept-engine**, **engine-system**, **engine-gecko**: Added API to set default session configuration e.g. to enable tracking protection for all sessions by default.
    ```Kotlin
    // DefaultSettings can be set on GeckoEngine and SystemEngine.
    GeckoEngine(runtime, DefaultSettings(
        trackingProtectionPolicy = TrackingProtectionPolicy.all(),
        javascriptEnabled = false))
    ```
* **concept-engine**, **engine-system**, **engine-gecko-beta/nightly**:
  * Added support for intercepting request and injecting custom content. This can be used for internal pages (e.g. *focus:about*, *firefox:home*) and error pages.
    ```Kotlin
    // GeckoEngine (beta/nightly) and SystemEngine support request interceptors.
    GeckoEngine(runtime, DefaultSettings(
        requestInterceptor = object : RequestInterceptor {
            override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
                return when (uri) {
                    "sample:about" -> RequestInterceptor.InterceptionResponse("<h1>I am the sample browser</h1>")
                    else -> null
               }
           }
       }
    )
    ```
  * Added APIs to support "find in page".
    ```Kotlin
        // Finds and highlights all occurrences of "hello"
        engineSession.findAll("hello")

        // Finds and highlights the next or previous match
        engineSession.findNext(forward = true)

        // Clears the highlighted results
        engineSession.clearFindMatches()

        // The current state of "Find in page" can be observed on a Session object:
        session.register(object : Session.Observer {
            fun onFindResult(session: Session, result: FindResult) {
                // ...
            }
        })
    ```
* **browser-engine-gecko-nightly**: Added option to enable/disable desktop mode ("Request desktop site").
    ```Kotlin
        engineSession.setDesktopMode(true, reload = true)
    ```
* **browser-engine-gecko(-nightly/beta)**: Added API for observing long presses on web content (links, audio, videos, images, phone numbers, geo locations, email addresses).
    ```Kotlin
        session.register(object : Session.Observer {
            fun onLongPress(session: Session, hitResult: HitResult): Boolean {
                // HitResult is a sealed class representing the different types of content that can be long pressed.
                // ...

                // Returning true will "consume" the event. If no observer consumes the event then it will be
                // set on the Session object to be consumed at a later time.
                return true
            }
        })
    ```
* **lib-dataprotect**: New component to protect local user data using the [Android keystore system](https://developer.android.com/training/articles/keystore). This component doesn't contain any code in this release. In the next sprints the Lockbox team will move code from the [prototype implementation](https://github.com/linuxwolf/android-dataprotect) to the component.
* **support-testing**: New helper test function to assert that a code block throws an exception:
    ```Kotlin
    expectException(IllegalStateException::class) {
        // Do something that should throw IllegalStateException..
    }
    ```

# 0.20 (2018-08-24)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.19.1...v0.20),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/19?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.20/index)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.60
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: **63.0.20180820100132** 🔺
    * Beta: 62.0b15 (7ce198bb7ce027d450af3f69a609896671adfab8)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* GeckoView Nightly dependencies are now pulled in from *maven.mozilla.org*.
* **engine-system**: Added tracking protection functionality.
* **concept-engine**, **browser-session**, **feature-session**: Added support for private browsing mode.
* **concept-engine**, **engine-gecko**, **engine-system**: Added support for modifying engine and engine session settings.

# 0.19.1 (2018-08-20)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.19...v0.19.1),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/20?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.19.1/index)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.60
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180810100129 (2018.08.10, d999fb858fb2c007c5be4af72bce419c63c69b8e)
    * Beta: 62.0b15 (7ce198bb7ce027d450af3f69a609896671adfab8)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **browser-toolbar**: Replaced `ui-progress` component with default [Android Progress Bar](https://developer.android.com/reference/android/widget/ProgressBar) to fix CPU usage problems.
* **ui-progress**: Reduced high CPU usage when idling and not animating.


# 0.19 (2018-08-17)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.18...v0.19),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/18?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.19/index)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.60
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180810100129 (2018.08.10, d999fb858fb2c007c5be4af72bce419c63c69b8e)
    * Beta: 62.0b15 (7ce198bb7ce027d450af3f69a609896671adfab8)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **concept-engine**, **engine-system**, **engine-gecko**: Added new API to load data and HTML directly (without loading a URL). Added the ability to stop loading a page.
* **ui-autocomplete**: Fixed a bug that caused soft keyboards and the InlineAutocompleteEditText component to desync.
* **service-firefox-accounts**: Added JNA-specific proguard rules so consumers of this library don't have to add them to their app (see https://github.com/java-native-access/jna/blob/master/www/FrequentlyAskedQuestions.md#jna-on-android for details). Underlying libfxa_client.so no longer depends on versioned .so names. All required dependencies are now statically linked which simplified our dependency setup as well.

# 0.18 (2018-08-10)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.17...v0.18),
[Milestone](https://github.com/mozilla-mobile/android-components/milestone/16?closed=1),
[API reference](https://mozilla-mobile.github.io/android-components/api/0.18/index)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.60
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: **63.0.20180810100129** (2018.08.10, d999fb858fb2c007c5be4af72bce419c63c69b8e) 🔺
    * Beta: **62.0b15** (7ce198bb7ce027d450af3f69a609896671adfab8) 🔺
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **engine-gecko-beta**: Since the [Load Progress Tracking API](https://bugzilla.mozilla.org/show_bug.cgi?id=1437988) was uplifted to GeckoView Beta  _engine-gecko-beta_ now reports progress via `EngineSession.Observer.onProgress()`.
* **service-fretboard**: KintoExperimentSource can now validate the signature of the downloaded experiments configuration (`validateSignature` flag). This ensures that the configuration was signed by Mozilla and was not modified by a bad actor. For now the `validateSignature` flag is off by default until this has been tested in production. Various bugfixes and refactorings.
* **service-firefox-accounts**: JNA native libraries are no longer part of the AAR and instead referenced as a dependency. This avoids duplication when multiple libraries depend on JNA.
* **ui-tabcounter**: New UI component - A button that shows the current tab count and can animate state changes. Extracted from Firefox Rocket.
* API references for every release are now generated and hosted online: [https://mozilla-mobile.github.io/android-components/reference/](https://mozilla-mobile.github.io/android-components/reference/)
* Documentation and more is now hosted at: [https://mozilla-mobile.github.io/android-components/](https://mozilla-mobile.github.io/android-components/). More content coming soon.
* **tooling-lint**: New (internal-only) component containing custom lint rules.


# 0.17 (2018-08-03)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library **1.2.60** 🔺
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: **63.0.20180801100114** (2018.08.01, af6a7edf0069549543f2fba6a8ee3ea251b20829) 🔺
    * Beta: **62.0b13** (dd92dec96711e60a8c6a49ebe584fa23a453a292) 🔺
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **support-base**: New base component containing small building blocks for other components. Added a [simple logging API](https://github.com/mozilla-mobile/android-components/blob/master/components/support/base/README.md) that allows components to log messages/exceptions but lets the consuming app decide what gets logged and how.
* **support-utils**: Some classes have been moved to the new _support-base_ component.
* **service-fretboard**: ⚠️ Breaking change: `ExperimentDescriptor` instances now operate on the experiment name instead of the ID.
* **ui-icons**: Added new icons (used in _Firefox Focus_ UI refresh): `mozac_ic_arrowhead_down`, `mozac_ic_arrowhead_up`, `mozac_ic_check`, `mozac_ic_device_desktop`, `mozac_ic_mozilla`, `mozac_ic_open_in`, `mozac_ic_reorder`.
* **service-firefox-accounts**: Added [documentation](https://github.com/mozilla-mobile/android-components/blob/master/components/service/firefox-accounts/README.md).
* **service-fretboard**: Updated [documentation](https://github.com/mozilla-mobile/android-components/blob/master/components/service/fretboard/README.md).
* **browser-toolbar**: Fixed an issue where the toolbar content disappeared if a padding value was set on the toolbar.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.16.1...v0.17), [Milestone](https://github.com/mozilla-mobile/android-components/milestone/15?closed=1)

# 0.16.1 (2018-07-26)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.51
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180724100046 (2018.07.24, 1e5fa52a612e8985e12212d1950a732954e00e45)
    * Beta: 62.0b9 (d7ab2f3df0840cdb8557659afd46f61afa310379)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **service-telemetry**: Allow up to 200 extras in event pings.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.16...v0.16.1), [Milestone](https://github.com/mozilla-mobile/android-components/milestone/17?closed=1)

# 0.16 (2018-07-25)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.51
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180724100046 (2018.07.24, 1e5fa52a612e8985e12212d1950a732954e00e45)
    * Beta: 62.0b9 (d7ab2f3df0840cdb8557659afd46f61afa310379)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **service-fretboard**: Experiments can now be filtered by release channel. Added helper method to get list of active experiments.
* **service-telemetry**: Added option to report active experiments in the core ping.
* **service-firefox-accounts**, **sample-firefox-accounts**: libjnidispatch.so is no longer in the tree but automatically fetched from tagged GitHub releases at build-time. Upgraded to fxa-rust-client library 0.2.1. Renmaed armeabi directory to armeabi-v7a.
* **browser-session**, **concept-engine**: Exposed website title and tracking protection in session and made observable.
* **browser-toolbar**: Fixed bug that prevented the toolbar from being displayed at the bottom of the screen. Fixed animation problem when multiple buttons animated at the same time.
* Various bugfixes and refactorings (see commits below for details)
* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.15...v0.16), [Milestone](https://github.com/mozilla-mobile/android-components/milestone/14?closed=1)

# 0.15 (2018-07-20)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.51
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180704100138 (2018.07.04, 1c235a552c32ba6c97e6030c497c49f72c7d48a8)
    * Beta: 62.0b5 (801112336847960bbb9a018695cf09ea437dc137)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **service-firefox-accounts**, **sample-firefox-accounts**: Added authentication flow using WebView. Introduced functionality to persist and restore FxA state in shared preferences to keep users signed in between applications restarts. Increased test coverage for library.
* **service-fretboard**: New component for segmenting users in order to run A/B tests and rollout features gradually.
* **browser-session**: Refactored session observer to provide session object and changed values to simplify observer implementations. Add source (origin) information to Session.
* **browser-search**: Introduced new functionality to retrieve search suggestions.
* **engine-system**, **engine-gecko**, **browser-session**: Exposed downloads in engine components and made them consumable from browser session.
* **engine-gecko**: Added optimization to ignore initial loads of about:blank.

* Various bugfixes and refactorings (see commits below for details)
* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.14...v0.15), [Milestone](https://github.com/mozilla-mobile/android-components/milestone/13?closed=1)

# 0.14 (2018-07-13)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.51
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180704100138 (2018.07.04, 1c235a552c32ba6c97e6030c497c49f72c7d48a8)
    * Beta: 62.0b5 (801112336847960bbb9a018695cf09ea437dc137)
    * Release: 61.0 (785d242a5b01d5f1094882aa2144d8e5e2791e06)

* **support-test**: A new component with helpers for testing components.
* **browser-session**: New method `SessionManager.removeSessions()` for removing all sessions *except custom tab sessions*. `SessionManager.selectedSession` is now nullable. `SessionManager.selectedSessionOrThrow` can be used in apps that will always have at least one selected session and that do not want to deal with a nullable type.
* **feature-sessions**: `SessionIntentProcessor` can now be configured to open new tabs for incoming [Intents](https://developer.android.com/reference/android/content/Intent).
* **ui-icons**: Mirrored `mozac_ic_pin` and `mozac_ic_pin_filled` icons.
* **service-firefox-accounts**: Renamed the component from *service-fxa* for clarity. Introduced `FxaResult.whenComplete()` to be called when the `FxaResult` and the whole chain of `then` calls is completed with a value. Synchronized blocks invoking Rust calls.
* Various bugfixes and refactorings (see commits below for details)
* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.13...v0.14), [Milestone](https://github.com/mozilla-mobile/android-components/milestone/12?closed=1)

# 0.13 (2018-07-06)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.51
  * Kotlin coroutines 0.23.4
  * GeckoView
    * Nightly: 63.0.20180704100138 (2018.07.04, 1c235a552c32ba6c97e6030c497c49f72c7d48a8)
    * Beta: 62.0b5
    * Release: 61.0

* **service-fxa**, **samples-fxa**: Various improvements to FxA component API (made calls asynchronous and introduced error handling)
* **browser-toolbar**: Added functionality to observer focus changes (`setOnEditFocusChangeListener`)
* **concept-tabstray**, **browser-tabstray**, **features-tabs**: New components to provide browser tabs functionality
* **sample-browser**: Updated to support multiple tabs

* **API changes**:
  * InlineAutocompleteEditText: `onAutocomplete` was renamed to `applyAutocompleteResult`
  * Toolbar: `setOnUrlChangeListener` was renamed to `setOnUrlCommitListener`

* Various bugfixes and refactorings (see commits below for details)
* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.12...v0.13)

# 0.12 (2018-06-29)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.50
  * Kotlin coroutines 0.23.3
  * GeckoView Nightly
    * date: 2018.06.27
    * version: 63.0.20180627100018
    * revision: 1c235a552c32ba6c97e6030c497c49f72c7d48a8

* **service-fxa**, **samples-fxa**: Added new library/component for integrating with Firefox Accounts, and a sample app to demo its usage
* **samples-browser**: Moved all browser behaviour into standalone fragment
* Various bugfixes and refactorings (see commits below for details)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.11...v0.12)

# 0.11 (2018-06-22)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.06.21
    * version: 62.0.20180621100051
    * revision: e834d23a292972ab4250a8be00e6740c43e41db2

* **feature-session**, **browser-session**: Added functionality to process CustomTabsIntent.
* **engine-gecko**: Created separate engine-gecko variants/modules for nightly/beta/release channels.
* **browser-toolbar**: Added support for setting autocomplete filter.
* Various refactorings (see commits below for details)

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.10...v0.11)

# 0.10 (2018-06-14)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.05.16
    * version: 62.0.20180516100458
    * revision: dedd25bfd2794eaba95225361f82c701e49c9339

* **browser-session**: Added Custom Tabs configuration to session. Added new functionality that allows attaching a lifecycle owner to session observers so that observer can automatically be unregistered when the associated lifecycle ends.
* **service-telemetry**: Updated createdTimestamp and createdDate fields for mobile-metrics ping

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.9...v0.10)

# 0.9 (2018-06-06)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.05.16
    * version: 62.0.20180516100458
    * revision: dedd25bfd2794eaba95225361f82c701e49c9339

* **feature-session**, **engine-gecko**, **engine-system**: Added functionality and API to save/restore engine session state and made sure it's persisted by default (using `DefaultSessionStorage`)
* **concept-toolbar**: Use "AppCompat" versions of ImageButton and ImageView. Add `notifyListener` parameter to `setSelected` and `toggle` to specify whether or not listeners should be invoked.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.8...v0.9)

# 0.8 (2018-05-30)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.05.16
    * version: 62.0.20180516100458
    * revision: dedd25bfd2794eaba95225361f82c701e49c9339

* **browser-session**, **engine-gecko**, **engine-system**: Added SSL information and secure state to session, and made it observable.
* **browser-toolbar**: Introduced page, browser and navigation actions and allow for them to be dynamically shown, hidden and updated. Added ability to specify custom behaviour for clicks on URL in display mode. Added support for custom background actions. Enabled layout transitions by default.
* **service-telemetry**: Added new mobile-metrics ping type.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.7...v0.8)

# 0.7 (2018-05-24)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.05.16
    * version: 62.0.20180516100458
    * revision: dedd25bfd2794eaba95225361f82c701e49c9339

* **browser-toolbar**: Added support for dynamic actions. Made site security indicator optional. Added support for overriding default height and padding.
* **feature-session**: Added new use case implementation to support reloading URLs. Fixed bugs when restoring sessions from storage. Use `AtomicFile` for `DefaultSessionStorage`.
* **feature-search**: New component - Connects an (concept) engine implementation with the browser search module and provides search related use case implementations e.g. searching using the default provider.
* **support-ktx**: Added extension method to check if a `String` represents a URL.
* **samples-browser**: Added default search integration using the new feature-search component.
* **samples-toolbar**: New sample app - Shows how to customize the browser-toolbar component.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.6...v0.7)

# 0.6 (2018-05-16)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.05.16
    * version: 62.0.20180516100458
    * revision: dedd25bfd2794eaba95225361f82c701e49c9339

* **browser-menu**: New component - A generic menu with customizable items for browser toolbars.
* **concept-session-storage**: New component - Abstraction layer for hiding the actual session storage implementation.
* **feature-session**: Added `DefaultSessionStorage` which is used if no other implementation of `SessionStorage` (from the new concept module) is provided. Introduced a new `SessionProvider` type which simplifies the API for use cases and components and removed the `SessionMapping` type as it's no longer needed.
* **support-ktx**: Added extension methods to `View` for checking visibility (`View.isVisible`, `View.isInvisible` and `View.isGone`).
* **samples-browser**: Use new browser menu component and switch to Gecko as default engine.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.5.1...v0.6)

# 0.5.1 (2018-05-03)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.41
  * Kotlin coroutines 0.22.5
  * GeckoView Nightly
    * date: 2018.04.10
    * version: 61.0.20180410100334
    * revision: a8061a09cd7064a8783ca9e67979d77fb52e001e

* **browser-domains**: Simplified API of `DomainAutoCompleteProvider` which now uses a dedicated result type instead of a callback and typealias.
* **browser-toolbar**: Added various enhancements to support edit and display mode and to navigate back/forward.
* **feature-session**: Added `SessionIntentProcessor` which provides reuseable functionality to handle incoming intents.
* **sample-browser**: Sample application now handles the device back button and reacts to incoming (ACTION_VIEW) intents.
* **support-ktx**: Added extension methods to `View` for converting dp to pixels (`View.dp`), showing and hiding the keyboard (`View.showKeyboard` and `View.hideKeyboard`).
* **service-telemetry**: New component - A generic library for generating and sending telemetry pings from Android applications to Mozilla's telemetry service.
* **ui-icons**: New component - A collection of often used browser icons.
* **ui-progress**: New component - An animated progress bar following the Photon Design System.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.4...v0.5)

# 0.5 (2018-05-02)

_Due to a packaging bug this release is not usable. Please use 0.5.1 instead._

# 0.4 (2018-04-19)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.31
  * Kotlin coroutines 0.22.5

* **browser-search**: New module - Search plugins and companion code to load, parse and use them.
* **browser-domains**: Auto-completion of full URLs (instead of just domains) is now supported.
* **ui-colors** module (org.mozilla.photon:colors) now includes all photon colors.
* **ui-fonts**: New module - Convenience accessor for fonts used by Mozilla.
* Multiple (Java/Kotlin) package names have been changed to match the naming of the module. Module names usually follow the template "$group-$name" and package names now follow the same scheme: "mozilla.components.$group.$name". For example the code of the "browser-toolbar" module now lives in the "mozilla.components.browser.toolbar" package. The group and artifacts Ids in Maven/Gradle have not been changed at this time.

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.3...v0.4)

# 0.3 (2018-04-05)

* Compiled against:
  * Android support libraries 27.1.1
  * Kotlin Standard library 1.2.30
  * Kotlin coroutines 0.19.3

* New component: **ui-autocomplete** - A set of components to provide autocomplete functionality. **InlineAutocompleteEditText** is a Kotlin version of the inline autocomplete widget we have been using in Firefox for Android and Focus/Klar for Android.
* New component: **browser-domains** - Localized and customizable domain lists for auto-completion in browsers.
* New components (Planning phase; Not for consumption yet): engine, engine-gecko, session, toolbar

* [Commits](https://github.com/mozilla-mobile/android-components/compare/v0.2.2...v0.3)

# 0.2.2 (2018-03-27)

* Compiled against:
  * Android support libraries 27.1.0
  * Kotlin Standard library 1.2.30

* First release with synchronized version numbers.

