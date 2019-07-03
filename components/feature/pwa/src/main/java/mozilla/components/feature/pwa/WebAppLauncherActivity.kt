/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.support.base.log.logger.Logger

/**
 * This activity is launched by Web App shortcuts on the home screen.
 *
 * Based on the Web App Manifest (display) it will decide whether the app is launched in the browser or in a
 * standalone activity.
 */
class WebAppLauncherActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val logger = Logger("WebAppLauncherActivity")
    private lateinit var storage: ManifestStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = ManifestStorage(this)

        intent.data?.let { startUrl ->
            launch {
                val manifest = loadManifest(startUrl.toString())
                routeManifest(startUrl, manifest)
            }
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun routeManifest(startUrl: Uri, manifest: WebAppManifest?) {
        when (manifest?.display) {
            WebAppManifest.DisplayMode.FULLSCREEN, WebAppManifest.DisplayMode.STANDALONE -> launchWebAppShell(startUrl)

            // We do not implement "minimal-ui" mode. Following the Web App Manifest spec we fallback to
            // using "browser" in this case.
            WebAppManifest.DisplayMode.MINIMAL_UI, WebAppManifest.DisplayMode.BROWSER -> launchBrowser(startUrl)

            // If no manifest is saved for this site, just open the browser.
            null -> launchBrowser(startUrl)
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun launchBrowser(startUrl: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, startUrl).apply {
            `package` = packageName
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.error("Package does not handle VIEW intent. Can't launch browser.")
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun launchWebAppShell(startUrl: Uri) {
        val intent = Intent(AbstractWebAppShellActivity.INTENT_ACTION).apply {
            data = startUrl
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            `package` = packageName
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.error("Packages does not handle AbstractWebAppShellActivity intent. Can't launch web app.", e)
            // Fall back to normal browser
            launchBrowser(startUrl)
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal suspend fun loadManifest(startUrl: String): WebAppManifest? {
        return storage.loadManifest(startUrl)
    }

    companion object {
        const val INTENT_ACTION = "mozilla.components.feature.pwa.PWA_LAUNCHER"
    }
}
