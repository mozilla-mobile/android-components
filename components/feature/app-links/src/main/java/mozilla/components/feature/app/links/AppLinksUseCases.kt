/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.app.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.annotation.VisibleForTesting
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.net.isHttpOrHttps
import java.net.URISyntaxException
import java.util.UUID

private const val EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url"
private const val MARKET_INTENT_URI_PACKAGE_PREFIX = "market://details?id="

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal const val APP_LINKS_CACHE_INTERVAL = 30 * 1000L // 30 seconds

/**
 * These use cases allow for the detection of, and opening of links that other apps have registered
 * an [IntentFilter]s to open.
 *
 * Care is taken to:
 *  * resolve [intent://] links, including [S.browser_fallback_url]
 *  * provide a fallback to the installed marketplace app (e.g. on Google Android, the Play Store).
 *  * open HTTP(S) links with an external app.
 *
 * Since browsers are able to open HTTPS pages, existing browser apps are excluded from the list of
 * apps that trigger a redirect to an external app.
 *
 * @param context Context the feature is associated with.
 * @param launchInApp If {true} then launch app links in third party app(s). Default to false because
 * of security concerns.
 * @param browserPackageNames Set of browser package names installed.
 * @param unguessableWebUrl URL is not likely to be opened by a native app but will fallback to a browser.
 */
class AppLinksUseCases(
    private val context: Context,
    private val launchInApp: () -> Boolean = { false },
    browserPackageNames: Set<String>? = null,
    unguessableWebUrl: String = "https://${UUID.randomUUID()}.net",
    private val alwaysDeniedSchemes: Set<String> = setOf("file")
) {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val browserPackageNames: Lazy<Set<String>>

    init {
        this.browserPackageNames = lazy { browserPackageNames ?: findExcludedPackages(unguessableWebUrl) }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun findActivities(intent: Intent): List<ResolveInfo> {
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: emptyList()
    }

    private fun findDefaultActivity(intent: Intent): ResolveInfo? {
        return context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    private fun safeParseUri(uri: String, flags: Int): Intent? {
        return try {
            Intent.parseUri(uri, flags)
        } catch (e: URISyntaxException) {
            null
        }
    }

    private fun findExcludedPackages(randomWebURLString: String): Set<String> {
        val intent = safeParseUri(randomWebURLString, 0) ?: return emptySet()
        // We generate a URL is not likely to be opened by a native app
        // but will fallback to a browser.
        // In this way, we're looking for only the browsers — including us.
        return findActivities(intent.addCategory(Intent.CATEGORY_BROWSABLE))
            .map { it.activityInfo.packageName }
            .toHashSet()
    }

    /**
     * Parse a URL and check if it can be handled by an app elsewhere on the Android device.
     * If that app is not available, then a market place intent is also provided.
     *
     * It will also provide a fallback.
     *
     * @param includeHttpAppLinks If {true} then test URLs that start with {http} and {https}.
     * @param ignoreDefaultBrowser If {true} then do not offer an app link if the user has
     * selected this browser as default previously. This is only applicable if {includeHttpAppLinks}
     * is true.
     * @param includeInstallAppFallback If {true} then offer an app-link to the installed market app
     * if no web fallback is available.
     */
    @Suppress("ComplexMethod")
    inner class GetAppLinkRedirect internal constructor(
        private val includeHttpAppLinks: Boolean = false,
        private val ignoreDefaultBrowser: Boolean = false,
        private val includeInstallAppFallback: Boolean = false
    ) {
        operator fun invoke(url: String): AppLinkRedirect {
            val urlHash = (url + includeHttpAppLinks + ignoreDefaultBrowser + includeHttpAppLinks).hashCode()
            val currentTimeStamp = System.currentTimeMillis()
            // since redirectCache is mutable, get the latest
            val cache = redirectCache
            if (cache != null && urlHash == cache.cachedUrlHash &&
                currentTimeStamp <= cache.cacheTimeStamp + APP_LINKS_CACHE_INTERVAL) {
                return cache.cachedAppLinkRedirect
            }

            val redirectData = createBrowsableIntents(url)
            val isAppIntentHttpOrHttps = redirectData.appIntent?.data?.isHttpOrHttps ?: false

            val appIntent = when {
                redirectData.resolveInfo == null -> null
                includeHttpAppLinks && (ignoreDefaultBrowser ||
                    (redirectData.appIntent != null && isDefaultBrowser(redirectData.appIntent))) -> null
                includeHttpAppLinks && isAppIntentHttpOrHttps -> redirectData.appIntent
                !launchInApp() && ENGINE_SUPPORTED_SCHEMES.contains(Uri.parse(url).scheme) -> null
                else -> redirectData.appIntent
            }

            val fallbackUrl = when {
                redirectData.fallbackIntent?.data?.isHttpOrHttps == true ->
                    redirectData.fallbackIntent.dataString
                else -> null
            }

            // no need to check marketplace intent since it is only set if a package is set in the intent
            val appLinkRedirect = AppLinkRedirect(appIntent, fallbackUrl, redirectData.marketplaceIntent)
            redirectCache = AppLinkRedirectCache(currentTimeStamp, urlHash, appLinkRedirect)
            return appLinkRedirect
        }

        private fun isDefaultBrowser(intent: Intent) =
            findDefaultActivity(intent)?.activityInfo?.packageName == context.packageName

        private fun getNonBrowserActivities(intent: Intent): List<ResolveInfo> {
            return findActivities(intent)
                .map { it.activityInfo.packageName to it }
                .filter { !browserPackageNames.value.contains(it.first) || intent.`package` == it.first }
                .map { it.second }
        }

        private fun createBrowsableIntents(url: String): RedirectData {
            val intent = safeParseUri(url, 0)
            if (intent != null && intent.action == Intent.ACTION_VIEW) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                intent.selector = null
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val fallbackIntent = intent?.getStringExtra(EXTRA_BROWSER_FALLBACK_URL)?.let {
                Intent.parseUri(it, 0)
            }

            val marketplaceIntent = intent?.`package`?.let {
                if (includeInstallAppFallback) {
                    Intent.parseUri(MARKET_INTENT_URI_PACKAGE_PREFIX + it, 0)
                } else {
                    null
                }
            }

            if (marketplaceIntent != null) {
                marketplaceIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val resolveInfoList = intent?.let {
                getNonBrowserActivities(it)
            }
            val resolveInfo = resolveInfoList?.firstOrNull()

            // only target intent for specific app if only one non browser app is found
            if (resolveInfoList?.count() == 1) {
                resolveInfo?.let {
                    intent.`package` = it.activityInfo?.packageName
                }
            }

            val appIntent = when {
                intent?.data == null -> null
                alwaysDeniedSchemes.contains(intent.data?.scheme) -> null
                else -> intent
            }

            return RedirectData(appIntent, fallbackIntent, marketplaceIntent, resolveInfo)
        }
    }

    /**
     * Open an external app with the redirect created by the [GetAppLinkRedirect].
     *
     * This does not do any additional UI other than the chooser that Android may provide the user.
     */
    inner class OpenAppLinkRedirect internal constructor(
        private val context: Context
    ) {
        operator fun invoke(
            appIntent: Intent?,
            launchInNewTask: Boolean = true,
            failedToLaunchAction: () -> Unit = {}
        ) {
            appIntent?.let {
                try {
                    val scheme = appIntent.data?.scheme
                    if (scheme != null && alwaysDeniedSchemes.contains(scheme)) {
                        return
                    }

                    if (launchInNewTask) {
                        it.flags = it.flags or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(it)
                } catch (e: ActivityNotFoundException) {
                    failedToLaunchAction()
                    Logger.error("failed to start third party app activity", e)
                }
            }
        }
    }

    val openAppLink: OpenAppLinkRedirect by lazy { OpenAppLinkRedirect(context) }
    val interceptedAppLinkRedirect: GetAppLinkRedirect by lazy {
        GetAppLinkRedirect(
            includeHttpAppLinks = false,
            includeInstallAppFallback = true
        )
    }
    val appLinkRedirect: GetAppLinkRedirect by lazy {
        GetAppLinkRedirect(
            includeHttpAppLinks = true,
            ignoreDefaultBrowser = false,
            includeInstallAppFallback = false
        )
    }
    val appLinkRedirectIncludeInstall: GetAppLinkRedirect by lazy {
        GetAppLinkRedirect(
            includeHttpAppLinks = true,
            ignoreDefaultBrowser = false,
            includeInstallAppFallback = true
        )
    }
    private data class RedirectData(
        val appIntent: Intent? = null,
        val fallbackIntent: Intent? = null,
        val marketplaceIntent: Intent? = null,
        val resolveInfo: ResolveInfo? = null
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal data class AppLinkRedirectCache(
        var cacheTimeStamp: Long,
        var cachedUrlHash: Int,
        var cachedAppLinkRedirect: AppLinkRedirect
    )

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal var redirectCache: AppLinkRedirectCache? = null

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        // list of scheme from https://searchfox.org/mozilla-central/source/netwerk/build/components.conf
        internal val ENGINE_SUPPORTED_SCHEMES: Set<String> = setOf("about", "data", "file", "ftp", "http",
            "https", "moz-extension", "moz-safe-about", "resource", "view-source", "ws", "wss")
    }
}
