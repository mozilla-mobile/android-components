/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.CATEGORY_HOME
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ShortcutManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.decoder.AndroidIconDecoder
import mozilla.components.browser.icons.decoder.ICOIconDecoder
import mozilla.components.browser.icons.extension.toIconRequest
import mozilla.components.browser.icons.generator.DefaultIconGenerator
import mozilla.components.browser.icons.loader.DataUriIconLoader
import mozilla.components.browser.icons.loader.HttpIconLoader
import mozilla.components.browser.icons.loader.MemoryIconLoader
import mozilla.components.browser.icons.preparer.MemoryIconPreparer
import mozilla.components.browser.icons.processor.AdaptiveIconProcessor
import mozilla.components.browser.icons.processor.ColorProcessor
import mozilla.components.browser.icons.processor.MemoryIconProcessor
import mozilla.components.browser.icons.processor.ResizingProcessor
import mozilla.components.browser.icons.utils.IconMemoryCache
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.pwa.WebAppLauncherActivity.Companion.ACTION_PWA_LAUNCHER
import mozilla.components.feature.pwa.ext.hasLargeIcons
import mozilla.components.feature.pwa.ext.installableManifest

private val pwaIconMemoryCache = IconMemoryCache()

const val SHORTCUT_CATEGORY = mozilla.components.feature.customtabs.SHORTCUT_CATEGORY

/**
 * Helper to manage pinned shortcuts for websites.
 *
 * @param httpClient Fetch client used to load website icons.
 * @param storage Storage used to save web app manifests to disk.
 * @param supportWebApps If true, Progressive Web Apps will be pinnable.
 * If false, all web sites will be bookmark shortcuts even if they have a manifest.
 */
class WebAppShortcutManager(
    context: Context,
    httpClient: Client,
    private val storage: ManifestStorage,
    internal val supportWebApps: Boolean = true
) {

    @VisibleForTesting
    internal val icons = webAppIcons(context, httpClient)

    private val fallbackLabel = context.getString(R.string.mozac_feature_pwa_default_shortcut_label)

    /**
     * Request to create a new shortcut on the home screen.
     * @param context The current context.
     * @param session The session to create the shortcut for.
     * @param overrideShortcutName (optional) The name of the shortcut. Ignored for PWAs.
     */
    suspend fun requestPinShortcut(
        context: Context,
        session: Session,
        overrideShortcutName: String? = null
    ) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            val manifest = session.installableManifest()
            val shortcut = if (supportWebApps && manifest != null) {
                buildWebAppShortcut(context, manifest)
            } else {
                buildBasicShortcut(context, session, overrideShortcutName)
            }

            if (shortcut != null) {
                val intent = Intent(ACTION_MAIN).apply {
                    addCategory(CATEGORY_HOME)
                    flags = FLAG_ACTIVITY_NEW_TASK
                }
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT)
                val intentSender = pendingIntent.intentSender

                ShortcutManagerCompat.requestPinShortcut(context, shortcut, intentSender)
            }
        }
    }

    /**
     * Update existing PWA shortcuts with the latest info from web app manifests.
     *
     * Devices before 7.1 do not allow shortcuts to be dynamically updated,
     * so this method will do nothing.
     */
    suspend fun updateShortcuts(context: Context, manifests: List<WebAppManifest>) {
        if (SDK_INT >= VERSION_CODES.N_MR1) {
            context.getSystemService<ShortcutManager>()?.apply {
                val shortcuts = manifests.mapNotNull { buildWebAppShortcut(context, it)?.toShortcutInfo() }
                updateShortcuts(shortcuts)
            }
        }
    }

    /**
     * Create a new basic pinned website shortcut using info from the session.
     * Consuming `SHORTCUT_CATEGORY` in `AndroidManifest` is required for the package to be launched
     */
    suspend fun buildBasicShortcut(
        context: Context,
        session: Session,
        overrideShortcutName: String? = null
    ): ShortcutInfoCompat {
        val shortcutIntent = Intent(Intent.ACTION_VIEW, session.url.toUri()).apply {
            addCategory(SHORTCUT_CATEGORY)
            `package` = context.packageName
        }

        val manifest = session.webAppManifest
        val shortLabel = overrideShortcutName
            ?: manifest?.shortName
            ?: manifest?.name
            ?: session.title

        val builder = ShortcutInfoCompat.Builder(context, session.url)
            .setShortLabel(shortLabel.ifBlank { fallbackLabel })
            .setIntent(shortcutIntent)

        val icon = if (manifest != null && manifest.hasLargeIcons()) {
            buildIconFromManifest(manifest)
        } else {
            session.icon?.let { IconCompat.createWithBitmap(it) }
        }
        icon?.let {
            builder.setIcon(it)
        }

        return builder.build()
    }

    /**
     * Create a new Progressive Web App shortcut using a web app manifest.
     */
    suspend fun buildWebAppShortcut(
        context: Context,
        manifest: WebAppManifest
    ): ShortcutInfoCompat? {
        val shortcutIntent = Intent(context, WebAppLauncherActivity::class.java).apply {
            action = ACTION_PWA_LAUNCHER
            data = manifest.startUrl.toUri()
            flags = FLAG_ACTIVITY_NEW_DOCUMENT
            `package` = context.packageName
        }

        val shortLabel = manifest.shortName ?: manifest.name
        storage.saveManifest(manifest)

        return ShortcutInfoCompat.Builder(context, manifest.startUrl)
            .setLongLabel(manifest.name)
            .setShortLabel(shortLabel.ifBlank { fallbackLabel })
            .setIcon(buildIconFromManifest(manifest))
            .setIntent(shortcutIntent)
            .build()
    }

    @VisibleForTesting
    internal suspend fun buildIconFromManifest(manifest: WebAppManifest): IconCompat {
        val request = manifest.toIconRequest()
        val icon = icons.loadIcon(request).await()
        return if (icon.maskable) {
            IconCompat.createWithAdaptiveBitmap(icon.bitmap)
        } else {
            IconCompat.createWithBitmap(icon.bitmap)
        }
    }

    /**
     * Finds the shortcut associated with the given startUrl.
     * This method can be used to check if a web app was added to the homescreen.
     */
    fun findShortcut(context: Context, startUrl: String) =
        if (SDK_INT >= VERSION_CODES.N_MR1) {
            context.getSystemService<ShortcutManager>()?.pinnedShortcuts?.find { it.id == startUrl }
        } else {
            null
        }

    /**
     * Uninstalls a set of PWAs from the user's device by disabling their
     * shortcuts and removing the associated manifest data.
     *
     * @param startUrls List of manifest startUrls to remove.
     * @param disabledMessage Message to display when a disable shortcut is tapped.
     */
    suspend fun uninstallShortcuts(context: Context, startUrls: List<String>, disabledMessage: String? = null) {
        if (SDK_INT >= VERSION_CODES.N_MR1) {
            context.getSystemService<ShortcutManager>()?.disableShortcuts(startUrls, disabledMessage)
        }
        storage.removeManifests(startUrls)
    }

    /**
     * Checks if there is a currently installed web app to which this URL belongs.
     *
     * @param url url that is covered by the scope of a web app installed by the user
     * @param currentTime the curent time in milliseconds
     */
    suspend fun getWebAppInstallState(url: String, currentTime: Long = System.currentTimeMillis()): WebAppInstallState {
        if (storage.hasRecentManifest(url, currentTime = currentTime)) {
            return WebAppInstallState.Installed
        }

        return WebAppInstallState.NotInstalled
    }

    /**
     * Updates the usedAt timestamp of the web app this url is associated with.
     *
     * @param manifest the manifest to update
     */
    suspend fun reportWebAppUsed(manifest: WebAppManifest): Unit? {
        return storage.updateManifestUsedAt(manifest)
    }

    /**
     * Possible install states of a Web App.
     */
    enum class WebAppInstallState {
        NotInstalled,
        Installed
    }
}

/**
 * Creates custom version of [BrowserIcons] for loading web app icons.
 *
 * This version has its own cache to avoid affecting tab icons.
 */
private fun webAppIcons(
    context: Context,
    httpClient: Client
) = BrowserIcons(
    context = context,
    httpClient = httpClient,
    generator = DefaultIconGenerator(cornerRadiusDimen = null),
    preparers = listOf(
        MemoryIconPreparer(pwaIconMemoryCache)
    ),
    loaders = listOf(
        MemoryIconLoader(pwaIconMemoryCache),
        HttpIconLoader(httpClient),
        DataUriIconLoader()
    ),
    decoders = listOf(
        AndroidIconDecoder(),
        ICOIconDecoder()
    ),
    processors = listOf(
        MemoryIconProcessor(pwaIconMemoryCache),
        ResizingProcessor(),
        ColorProcessor(),
        AdaptiveIconProcessor()
    )
)
