/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.app.links

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import java.util.UUID

class AppLinksUseCases(
    private val context: Context,
    browserPackageNames: Set<String>? = null
) {
    val browserPackageNames: Set<String>

    init {
        this.browserPackageNames = browserPackageNames ?: findExcludedPackages()
    }

    private fun getNonBrowserActivities(url: String): List<ResolveInfo> {
        return findActivities(url)
            .filter { !browserPackageNames.contains(it.activityInfo.packageName) }
    }

    private fun findActivities(url: String): List<ResolveInfo> {
        val intent = createBrowsableIntent(url)
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: emptyList()
    }

    private fun createBrowsableIntent(url: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        return intent
    }

    private fun findExcludedPackages(): Set<String> {
        // We generate a URL is not likely to be opened by a native app
        // but will fallback to a browser.
        // In this way, we're looking for only the browsers — including us.
        val randomWebURLString = "https://${UUID.randomUUID()}.net"
        return findActivities(randomWebURLString)
            .map { it.activityInfo.packageName }
            .toHashSet()
    }

    inner class IsAppLinkUseCase internal constructor() {
        fun invoke(url: String): Boolean {
            val available = getNonBrowserActivities(url)

            return available.isNotEmpty()
        }
    }

    inner class OpenAppLinkUseCase internal constructor() {
        fun invoke(url: String) {
            val intent = createBrowsableIntent(url)

            val openInIntent = Intent.createChooser(
                intent,
                context.getString(R.string.mozac_feature_applinks_open_in)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(openInIntent)
        }
    }

    val isAppLink: IsAppLinkUseCase by lazy { IsAppLinkUseCase() }
    val loadUrl: OpenAppLinkUseCase by lazy { OpenAppLinkUseCase() }
}
