/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.app.links

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.feature.app.links.AppLinksUseCases.Companion.ENGINE_SUPPORTED_SCHEMES

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
 * @param engineSupportedSchemes List of schemes that the engine supports.
 * @param alwaysDeniedSchemes List of schemes that will never be opened in a third-party app even if
 * [interceptLinkClicks] is `true`.
 * @param launchInApp If {true} then launch app links in third party app(s). Default to false because
 * of security concerns.
 * @param useCases These use cases allow for the detection of, and opening of links that other apps
 * have registered to open.
 * @param launchFromInterceptor If {true} then the interceptor will launch the link in third-party apps if available.
 */
class AppLinksInterceptor(
    private val context: Context,
    private val interceptLinkClicks: Boolean = false,
    private val engineSupportedSchemes: Set<String> = ENGINE_SUPPORTED_SCHEMES,
    private val alwaysDeniedSchemes: Set<String> = setOf("javascript", "about"),
    private val launchInApp: () -> Boolean = { false },
    private val useCases: AppLinksUseCases = AppLinksUseCases(context, launchInApp),
    private val launchFromInterceptor: Boolean = false
) : RequestInterceptor {

    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        hasUserGesture: Boolean,
        isSameDomain: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        val uriScheme = Uri.parse(uri).scheme

        val doNotIntercept = when {
            uriScheme == null -> true
            // If request not from user gesture or if we're already on the site,
            // and we're clicking around then let's not go to an external app.
            (!hasUserGesture && engineSupportedSchemes.contains(uriScheme)) || isSameDomain -> true
            // If scheme not in whitelist then follow user preference
            (!interceptLinkClicks || !launchInApp()) &&
                engineSupportedSchemes.contains(uriScheme) -> true
            // Never go to an external app when scheme is in blacklist
            alwaysDeniedSchemes.contains(uriScheme) -> true
            else -> false
        }

        if (doNotIntercept) {
            return null
        }

        val redirect = useCases.interceptedAppLinkRedirect(uri)
        val result = handleRedirect(redirect, uri, hasUserGesture)
        if (redirect.isRedirect()) {
            if (launchFromInterceptor && result is RequestInterceptor.InterceptionResponse.AppIntent) {
                result.appIntent.flags = result.appIntent.flags or Intent.FLAG_ACTIVITY_NEW_TASK
                useCases.openAppLink(result.appIntent)
            }

            return result
        }

        return null
    }

    @SuppressWarnings("ReturnCount")
    @SuppressLint("MissingPermission")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun handleRedirect(
        redirect: AppLinkRedirect,
        uri: String,
        hasUserGesture: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        if (!redirect.hasExternalApp()) {
            redirect.marketplaceIntent?.let {
                return RequestInterceptor.InterceptionResponse.AppIntent(it, uri)
            }

            redirect.fallbackUrl?.let {
                return RequestInterceptor.InterceptionResponse.Url(it)
            }

            return null
        }

        if (!hasUserGesture || !launchInApp()) {
            redirect.fallbackUrl?.let {
                return RequestInterceptor.InterceptionResponse.Url(it)
            }
        }

        redirect.appIntent?.let {
            return RequestInterceptor.InterceptionResponse.AppIntent(it, uri)
        }

        return null
    }
}
