[android-components](../../index.md) / [mozilla.components.feature.app.links](../index.md) / [AppLinksFeature](index.md) / [&lt;init&gt;](./-init-.md)

# &lt;init&gt;

`AppLinksFeature(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`, sessionManager: `[`SessionManager`](../../mozilla.components.browser.session/-session-manager/index.md)`, sessionId: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null, interceptLinkClicks: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)` = true, fragmentManager: FragmentManager? = null, dialog: `[`RedirectDialogFragment`](../-redirect-dialog-fragment/index.md)` = SimpleRedirectDialogFragment.newInstance(), useCases: `[`AppLinksUseCases`](../-app-links-use-cases/index.md)` = AppLinksUseCases(context))`

This feature implements use cases for detecting and handling redirects to external apps. The user
is asked to confirm her intention before leaving the app. These include the Android Intents,
custom schemes and support for [Intent.CATEGORY_BROWSABLE](https://developer.android.com/reference/android/content/Intent.html#CATEGORY_BROWSABLE) `http(s)` URLs.

In the case of Android Intents that are not installed, and with no fallback, the user is prompted
to search the installed market place.

It provides use cases to detect and open links openable in third party non-browser apps.

It provides a [RequestInterceptor](../../mozilla.components.concept.engine.request/-request-interceptor/index.md) to do the detection and asking of consent.

It requires: a [Context](https://developer.android.com/reference/android/content/Context.html), and a [FragmentManager](#).

A [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) flag is provided at construction to allow the feature and use cases to be landed without
adjoining UI. The UI will be activated in https://github.com/mozilla-mobile/android-components/issues/2974
and https://github.com/mozilla-mobile/android-components/issues/2975.

