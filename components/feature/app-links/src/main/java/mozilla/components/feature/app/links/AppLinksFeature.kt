/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.app.links

import android.content.Context
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor

class AppLinksFeature(
    private val context: Context
) {
    val useCases = AppLinksUseCases(context)

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
