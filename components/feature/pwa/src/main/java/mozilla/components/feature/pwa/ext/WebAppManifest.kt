/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa.ext

import android.app.ActivityManager.TaskDescription
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.net.toUri
import mozilla.components.browser.session.tab.CustomTabConfig
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.support.utils.ColorUtils.isDark

/**
 * Creates a [TaskDescription] for the activity manager based on the manifest.
 *
 * Since the web app icon is provided dynamically by the web site, we can't provide a resource ID.
 * Instead we use the deprecated constructor.
 */
@Suppress("Deprecation")
fun WebAppManifest.toTaskDescription(icon: Bitmap?) =
    TaskDescription(name, icon, themeColor ?: 0)

/**
 * Creates a [CustomTabConfig] that styles a custom tab toolbar to match the manifest theme.
 */
fun WebAppManifest.toCustomTabConfig() =
    CustomTabConfig(
        id = startUrl,
        toolbarColor = themeColor,
        navigationBarColor = backgroundColor?.let {
            if (isDark(it)) Color.BLACK else Color.WHITE
        },
        closeButtonIcon = null,
        enableUrlbarHiding = true,
        actionButtonConfig = null,
        showShareMenuItem = true,
        menuItems = emptyList()
    )

/**
 * Returns the scope of the manifest as a [Uri] for use
 * with [mozilla.components.feature.pwa.feature.WebAppHideToolbarFeature].
 *
 * Null is returned when the scope should be ignored, such as with display: minimal-ui,
 * where the toolbar should always be displayed.
 */
fun WebAppManifest.getTrustedScope(): Uri? {
    return when (display) {
        WebAppManifest.DisplayMode.FULLSCREEN,
        WebAppManifest.DisplayMode.STANDALONE -> (scope ?: startUrl).toUri()

        WebAppManifest.DisplayMode.MINIMAL_UI,
        WebAppManifest.DisplayMode.BROWSER -> null
    }
}
