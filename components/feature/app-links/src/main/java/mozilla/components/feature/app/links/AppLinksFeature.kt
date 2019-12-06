/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.app.links

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.support.ktx.android.net.hostWithoutCommonPrefixes

/**
 * This feature implements use cases for detecting and handling redirects to external apps. The user
 * is asked to confirm her intention before leaving the app. These include the Android Intents,
 * custom schemes and support for [Intent.CATEGORY_BROWSABLE] `http(s)` URLs.
 *
 * In the case of Android Intents that are not installed, and with no fallback, the user is prompted
 * to search the installed market place.
 *
 * It provides use cases to detect and open links openable in third party non-browser apps.
 *
 * It requires: a [Context].
 *
 * A [Boolean] flag is provided at construction to allow the feature and use cases to be landed without
 * adjoining UI. The UI will be activated in https://github.com/mozilla-mobile/android-components/issues/2974
 * and https://github.com/mozilla-mobile/android-components/issues/2975.
 *
 * @param context Context the feature is associated with.
 * @param interceptLinkClicks If {true} then intercept link clicks.
 * @param alwaysAllowedSchemes List of schemes that will always be allowed to be opened in a third-party
 * app even if [interceptLinkClicks] is `false`.
 * @param alwaysDeniedSchemes List of schemes that will never be opened in a third-party app even if
 * [interceptLinkClicks] is `true`.
 * @param fragmentManager FragmentManager for interacting with fragments.
 * @param launchInApp If {true} then launch app links in third party app(s). Default to false because
 * of security concerns.
 * @param useCases These use cases allow for the detection of, and opening of links that other apps
 * have registered to open.
 */
class AppLinksFeature(
    private val context: Context,
    private val interceptLinkClicks: Boolean = false,
    private val alwaysAllowedSchemes: Set<String> = setOf("mailto", "market", "sms", "tel"),
    private val alwaysDeniedSchemes: Set<String> = setOf("javascript", "about"),
    private val launchInApp: () -> Boolean = { false },
    private val useCases: AppLinksUseCases = AppLinksUseCases(context, launchInApp)
) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val interceptor = object : RequestInterceptor {
        override fun onLoadRequest(
            engineSession: EngineSession,
            privateMode: Boolean,
            currentUrl: String?,
            uri: String,
            hasUserGesture: Boolean
        ): RequestInterceptor.InterceptionResponse? {
            // If request not from user gesture or if we're already on the site,
            // and we're clicking around then let's not go to an external app.
            if (!hasUserGesture || uri.hostname() == currentUrl?.hostname()) {
                return null
            }

            val redirect = useCases.interceptedAppLinkRedirect(uri)
            if (redirect.isRedirect()) {
                return handleRedirect(redirect, privateMode)
            }

            return null
        }
    }

    @SuppressWarnings("ReturnCount")
    @SuppressLint("MissingPermission")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun handleRedirect(
        redirect: AppLinkRedirect,
        privateMode: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        if (!redirect.hasExternalApp()) {
            redirect.marketplaceIntent?.let {
                return RequestInterceptor.InterceptionResponse.AppIntent(context, it)
            }

            return handleFallback(redirect)
        }

        redirect.appIntent?.data?.scheme?.let { scheme ->
            if ((!interceptLinkClicks && !alwaysAllowedSchemes.contains(scheme)) ||
                alwaysDeniedSchemes.contains(scheme)) {
                return null
            }
        }

        if (!privateMode) {
            redirect.appIntent?.let {
                return RequestInterceptor.InterceptionResponse.AppIntent(context, it)
            }
        }

        return null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun handleFallback(
        redirect: AppLinkRedirect
    ): RequestInterceptor.InterceptionResponse? {
        redirect.fallbackUrl?.let {
            return RequestInterceptor.InterceptionResponse.Url(it)
        }

        return null
    }

    private fun String.hostname() =
        if (URLUtil.isValidUrl(this)) {
            Uri.parse(this).hostWithoutCommonPrefixes
        } else {
            null
        }
}
