/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.app.links

import android.content.Context
import androidx.fragment.app.FragmentManager
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor

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
 * It provides a [RequestInterceptor] to do the detection and asking of consent.
 *
 * It requires: a [Context], and a [FragmentManager].
 */
class AppLinksFeature(
    context: Context,
    fragmentManager: FragmentManager,
    dialog: RedirectDialogFragment = SimpleRedirectDialogFragment.newInstance()
) {
    val useCases = AppLinksUseCases(context, null, fragmentManager, dialog)

    val interceptor = object : RequestInterceptor {
        override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
            val redirect = useCases.appLinkRedirect.invoke(uri)
            return if (redirect.hasExternalApp()) {
                useCases.loadUrl.invoke(redirect, session)
                null
            } else {
                redirect.webUrl?.let { RequestInterceptor.InterceptionResponse.Url(it) }
            }
        }
    }
}
