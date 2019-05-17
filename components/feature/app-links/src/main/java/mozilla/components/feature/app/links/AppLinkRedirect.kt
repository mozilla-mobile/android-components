/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.app.links

import android.content.Intent

data class AppLinkRedirect(
    val appIntent: Intent?,
    val webUrl: String?,
    val isFallback: Boolean
) {
    fun hasExternalApp() = appIntent != null

    fun hasFallback() = webUrl != null && isFallback

    fun isRedirect() = hasExternalApp() || hasFallback()
}
