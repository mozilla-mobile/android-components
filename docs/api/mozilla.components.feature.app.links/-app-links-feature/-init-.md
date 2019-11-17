[android-components](../../index.md) / [mozilla.components.feature.app.links](../index.md) / [AppLinksFeature](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`AppLinksFeature(context: <ERROR CLASS>, sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, interceptLinkClicks: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = false, alwaysAllowedSchemes: `[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`> = setOf("mailto", "market", "sms", "tel"), alwaysDeniedSchemes: `[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)`<`[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`> = setOf("javascript", "about"), fragmentManager: FragmentManager? = null, dialog: `[`RedirectDialogFragment`](../-redirect-dialog-fragment/index.md)` = SimpleRedirectDialogFragment.newInstance(), launchInApp: () -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = { false }, useCases: `[`AppLinksUseCases`](../-app-links-use-cases/index.md)` = AppLinksUseCases(context, launchInApp))`

This feature implements use cases for detecting and handling redirects to external apps. The user
is asked to confirm her intention before leaving the app. These include the Android Intents,
custom schemes and support for [Intent.CATEGORY_BROWSABLE](#) `http(s)` URLs.

In the case of Android Intents that are not installed, and with no fallback, the user is prompted
to search the installed market place.

It provides use cases to detect and open links openable in third party non-browser apps.

It requires: a [Context](#), and a [FragmentManager](#).

A [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) flag is provided at construction to allow the feature and use cases to be landed without
adjoining UI. The UI will be activated in https://github.com/mozilla-mobile/android-components/issues/2974
and https://github.com/mozilla-mobile/android-components/issues/2975.

### Parameters

`context` - Context the feature is associated with.

`sessionManager` - Provides access to a centralized registry of all active sessions.

`sessionId` - the session ID to observe.

`interceptLinkClicks` - If {true} then intercept link clicks.

`alwaysAllowedSchemes` - List of schemes that will always be allowed to be opened in a third-party
app even if [interceptLinkClicks](#) is `false`.

`alwaysDeniedSchemes` - List of schemes that will never be opened in a third-party app even if
[interceptLinkClicks](#) is `true`.

`fragmentManager` - FragmentManager for interacting with fragments.

`dialog` - The dialog for redirect.

`launchInApp` - If {true} then launch app links in third party app(s). Default to false because
of security concerns.

`useCases` - These use cases allow for the detection of, and opening of links that other apps
have registered to open.