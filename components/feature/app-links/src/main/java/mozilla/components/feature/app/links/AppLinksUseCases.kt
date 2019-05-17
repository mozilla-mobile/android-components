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
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentManager
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.app.links.RedirectDialogFragment.Companion.FRAGMENT_TAG
import mozilla.components.support.ktx.android.net.isHttpOrHttps
import java.util.UUID

private const val EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url"
private const val MARKET_INTENT_URI_PACKAGE_PREFIX = "market://details?id="

class AppLinksUseCases(
    private val context: Context,
    browserPackageNames: Set<String>? = null,
    private val fragmentManager: FragmentManager,
    private val dialog: RedirectDialogFragment
) {
    @VisibleForTesting
    val browserPackageNames: Set<String>

    init {
        this.browserPackageNames = browserPackageNames ?: findExcludedPackages()
    }

    private fun findActivities(intent: Intent): List<ResolveInfo> {
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: emptyList()
    }

    private fun findExcludedPackages(): Set<String> {
        // We generate a URL is not likely to be opened by a native app
        // but will fallback to a browser.
        // In this way, we're looking for only the browsers — including us.
        val randomWebURLString = "https://${UUID.randomUUID()}.net"

        return findActivities(Intent.parseUri(randomWebURLString, 0))
            .map { it.activityInfo.packageName }
            .toHashSet()
    }

    inner class GetAppLinkConfig internal constructor() {
        fun invoke(url: String): AppLinkRedirect {
            val intents = createBrowsableIntents(url)
            val appIntent = intents.firstOrNull {
                getNonBrowserActivities(it).isNotEmpty()
            }

            val webUrls = intents.mapNotNull {
                if (it.data?.isHttpOrHttps == true) it.dataString else null
            }

            val webUrl = webUrls.firstOrNull { it != url } ?: webUrls.firstOrNull()

            return AppLinkRedirect(appIntent, webUrl, webUrl != url)
        }

        private fun getNonBrowserActivities(intent: Intent): List<ResolveInfo> {
            return findActivities(intent)
                .filter { !browserPackageNames.contains(it.activityInfo.packageName) }
        }

        private fun createBrowsableIntents(url: String): List<Intent> {
            val intent = Intent.parseUri(url, 0)

            if (intent.action == Intent.ACTION_VIEW) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
            }

            return when (intent.data?.isHttpOrHttps) {
                null -> emptyList()
                true -> listOf(intent)
                false -> {
                    val fallback = intent.getStringExtra(EXTRA_BROWSER_FALLBACK_URL)?.let {
                        createBrowsableIntents(it)
                    } ?: emptyList()

                    val marketplaceIntent = intent.`package`?.let {
                        Intent.parseUri(MARKET_INTENT_URI_PACKAGE_PREFIX + it, 0)
                    }

                    return listOf(intent) + fallback + listOfNotNull(marketplaceIntent)
                }
            }
        }
    }

    class OpenAppLinkUseCase internal constructor(
        private val context: Context,
        private val fragmentManager: FragmentManager,
        private val dialog: RedirectDialogFragment
    ) {
        fun invoke(redirect: AppLinkRedirect, session: EngineSession): Boolean {
            val intent = redirect.appIntent ?: return false

            dialog.setAppLinkRedirect(redirect)
            dialog.onConfirmRedirect = {
                val openInIntent = Intent.createChooser(
                    intent,
                    context.getString(R.string.mozac_feature_applinks_open_in)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(openInIntent)
            }

            dialog.onDismiss(object : DialogInterface {
                override fun dismiss() {
                    redirect.webUrl?.let {
                        session.loadUrl(it)
                    }
                }

                override fun cancel() {
                    dismiss()
                }
            })

            dialog.show(fragmentManager, FRAGMENT_TAG)

            // TODO check that the user has made a choice.
            // if the user has made a choice, we should wait until it comes back.
            // if the user has cancelled, we should try the fallback.

            return true
        }
    }

    val appLinkRedirect: GetAppLinkConfig by lazy { GetAppLinkConfig() }
    val loadUrl: OpenAppLinkUseCase by lazy { OpenAppLinkUseCase(context, fragmentManager, dialog) }
}
