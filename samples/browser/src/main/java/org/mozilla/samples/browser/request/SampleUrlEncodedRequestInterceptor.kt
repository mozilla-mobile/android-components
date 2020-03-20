/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.request

import android.content.Context
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.request.RequestInterceptor.InterceptionResponse
import mozilla.components.concept.engine.request.RequestInterceptor.ErrorResponse
import org.mozilla.samples.browser.ext.components

/**
 * Example of a request interceptor that loads error pages with URL encoding (images)
 */
class SampleUrlEncodedRequestInterceptor(val context: Context) : RequestInterceptor {

    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        hasUserGesture: Boolean,
        isSameDomain: Boolean
    ): InterceptionResponse? {
        return when (uri) {
            "sample:about" -> InterceptionResponse.Content("<h1>I am the sample browser</h1>")
            else -> context.components.appLinksInterceptor.onLoadRequest(
                engineSession, uri, hasUserGesture, isSameDomain)
        }
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): ErrorResponse {
        val errorPage = ErrorPages.createUrlEncodedErrorPage(context, errorType, uri)
        return ErrorResponse.Uri(errorPage)
    }
}
