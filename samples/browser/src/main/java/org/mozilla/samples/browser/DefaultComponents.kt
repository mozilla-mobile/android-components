/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.engine.system.SystemEngine
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.WebExtensionBrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuCheckbox
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.memory.InMemoryHistoryStorage
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.addons.AddonManager
import mozilla.components.feature.addons.amo.AddonCollectionProvider
import mozilla.components.feature.addons.update.AddonUpdater
import mozilla.components.feature.addons.update.DefaultAddonUpdater
import mozilla.components.feature.addons.migration.DefaultSupportedAddonsChecker
import mozilla.components.feature.addons.migration.SupportedAddonsChecker
import mozilla.components.feature.app.links.AppLinksInterceptor
import mozilla.components.feature.app.links.AppLinksUseCases
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.customtabs.CustomTabIntentProcessor
import mozilla.components.feature.customtabs.store.CustomTabsServiceStore
import mozilla.components.feature.downloads.DownloadsUseCases
import mozilla.components.feature.intent.processing.TabIntentProcessor
import mozilla.components.feature.media.MediaFeature
import mozilla.components.feature.media.RecordingDevicesNotificationFeature
import mozilla.components.feature.media.state.MediaStateMachine
import mozilla.components.feature.pwa.ManifestStorage
import mozilla.components.feature.pwa.WebAppShortcutManager
import mozilla.components.feature.pwa.WebAppUseCases
import mozilla.components.feature.pwa.intent.TrustedWebActivityIntentProcessor
import mozilla.components.feature.pwa.intent.WebAppIntentProcessor
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.HistoryDelegate
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.webnotifications.WebNotificationFeature
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import org.mozilla.samples.browser.addons.AddonsActivity
import org.mozilla.samples.browser.ext.components
import org.mozilla.samples.browser.integration.FindInPageIntegration
import org.mozilla.samples.browser.request.SampleRequestInterceptor
import java.util.concurrent.TimeUnit

private const val DAY_IN_MINUTES = 24 * 60L

@Suppress("LargeClass")
open class DefaultComponents(private val applicationContext: Context) {
    companion object {
        const val SAMPLE_BROWSER_PREFERENCES = "sample_browser_preferences"
        const val PREF_LAUNCH_EXTERNAL_APP = "sample_browser_launch_external_app"
    }

    val preferences: SharedPreferences =
        applicationContext.getSharedPreferences(SAMPLE_BROWSER_PREFERENCES, Context.MODE_PRIVATE)

    // Engine Settings
    val engineSettings by lazy {
        DefaultSettings().apply {
            historyTrackingDelegate = HistoryDelegate(lazyHistoryStorage)
            requestInterceptor = SampleRequestInterceptor(applicationContext)
            remoteDebuggingEnabled = true
            supportMultipleWindows = true
        }
    }

    val addonUpdater =
        DefaultAddonUpdater(applicationContext, AddonUpdater.Frequency(1, TimeUnit.DAYS))

    // Engine
    open val engine: Engine by lazy {
        SystemEngine(applicationContext, engineSettings)
    }

    open val client: Client by lazy { HttpURLConnectionClient() }

    val icons by lazy { BrowserIcons(applicationContext, client) }

    // Storage
    private val lazyHistoryStorage = lazy { InMemoryHistoryStorage() }
    val historyStorage by lazy { lazyHistoryStorage.value }

    private val sessionStorage by lazy { SessionStorage(applicationContext, engine) }

    val store by lazy { BrowserStore() }

    val customTabsStore by lazy { CustomTabsServiceStore() }

    val sessionManager by lazy {
        SessionManager(engine, store).apply {
            sessionStorage.restore()?.let { snapshot -> restore(snapshot) }

            if (size == 0) {
                add(Session("about:blank"))
            }

            sessionStorage.autoSave(this)
                .periodicallyInForeground(interval = 30, unit = TimeUnit.SECONDS)
                .whenGoingToBackground()
                .whenSessionsChange()

            icons.install(engine, store)

            RecordingDevicesNotificationFeature(applicationContext, sessionManager = this)
                .enable()

            MediaFeature(applicationContext)
                .enable()

            MediaStateMachine.start(this)

            WebNotificationFeature(applicationContext, engine, icons, R.drawable.ic_notification,
                BrowserActivity::class.java)
        }
    }

    val sessionUseCases by lazy { SessionUseCases(sessionManager) }

    // Addons
    val addonManager by lazy {
        AddonManager(store, engine, addonCollectionProvider, addonUpdater)
    }

    val addonCollectionProvider by lazy {
        AddonCollectionProvider(applicationContext, client, maxCacheAgeInMinutes = DAY_IN_MINUTES)
    }

    val supportedAddonsChecker by lazy {
        DefaultSupportedAddonsChecker(applicationContext, SupportedAddonsChecker.Frequency(1, TimeUnit.DAYS))
    }

    // Search
    val searchEngineManager by lazy {
        SearchEngineManager().apply {
            CoroutineScope(Dispatchers.IO).launch {
                loadAsync(applicationContext).await()
            }
        }
    }

    val searchUseCases by lazy { SearchUseCases(applicationContext, searchEngineManager, sessionManager) }
    val defaultSearchUseCase by lazy { { searchTerms: String -> searchUseCases.defaultSearch.invoke(searchTerms) } }
    val appLinksUseCases by lazy { AppLinksUseCases(applicationContext) }

    val appLinksInterceptor by lazy {
        AppLinksInterceptor(
            applicationContext,
            interceptLinkClicks = true,
            launchInApp = {
                applicationContext.components.preferences.getBoolean(PREF_LAUNCH_EXTERNAL_APP, false)
            },
            launchFromInterceptor = true
        )
    }

    val webAppManifestStorage by lazy { ManifestStorage(applicationContext) }
    val webAppShortcutManager by lazy { WebAppShortcutManager(applicationContext, client, webAppManifestStorage) }
    val webAppUseCases by lazy { WebAppUseCases(applicationContext, sessionManager, webAppShortcutManager) }

    // Intent
    val tabIntentProcessor by lazy {
        TabIntentProcessor(sessionManager, sessionUseCases.loadUrl, searchUseCases.newTabSearch)
    }
    val externalAppIntentProcessors by lazy {
        listOf(
            WebAppIntentProcessor(sessionManager, sessionUseCases.loadUrl, webAppManifestStorage),
            TrustedWebActivityIntentProcessor(
                sessionManager,
                sessionUseCases.loadUrl,
                client,
                applicationContext.packageManager,
                null,
                customTabsStore
            ),
            CustomTabIntentProcessor(sessionManager, sessionUseCases.loadUrl, applicationContext.resources)
        )
    }

    // Menu
    val menuBuilder by lazy { WebExtensionBrowserMenuBuilder(menuItems, store = store) }

    private val menuItems by lazy {
        val items = mutableListOf(
            menuToolbar,
            BrowserMenuHighlightableItem("No Highlight", R.drawable.mozac_ic_share, android.R.color.black,
                highlight = BrowserMenuHighlight.LowPriority(
                    notificationTint = ContextCompat.getColor(applicationContext, android.R.color.holo_green_dark),
                    label = "Highlight"
                )
            ) {
                Toast.makeText(applicationContext, "Highlight", Toast.LENGTH_SHORT).show()
            },
            BrowserMenuImageText("Share", R.drawable.mozac_ic_share, android.R.color.black) {
                Toast.makeText(applicationContext, "Share", Toast.LENGTH_SHORT).show()
            },
            SimpleBrowserMenuItem("Settings") {
                Toast.makeText(applicationContext, "Settings", Toast.LENGTH_SHORT).show()
            },
            SimpleBrowserMenuItem("Add-ons") {
                val intent = Intent(applicationContext, AddonsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                applicationContext.startActivity(intent)
            },
            SimpleBrowserMenuItem("Find In Page") {
                FindInPageIntegration.launch?.invoke()
            },
            BrowserMenuDivider()
        )

        items.add(
            SimpleBrowserMenuItem("Add to homescreen") {
                MainScope().launch {
                    webAppUseCases.addToHomescreen()
                }
            }.apply {
                visible = { webAppUseCases.isPinningSupported() && sessionManager.selectedSession != null }
            }
        )

        items.add(
            SimpleBrowserMenuItem("Open in App") {
                val getRedirect = appLinksUseCases.appLinkRedirect
                sessionManager.selectedSession?.let {
                    val redirect = getRedirect.invoke(it.url)
                    redirect.appIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    appLinksUseCases.openAppLink.invoke(redirect.appIntent)
                }
            }.apply {
                visible = {
                    sessionManager.selectedSession?.let {
                        appLinksUseCases.appLinkRedirect(it.url).hasExternalApp()
                    } ?: false
                }
            }
        )

        items.add(
            SimpleBrowserMenuItem("Clear Data") {
                sessionUseCases.clearData()
            }
        )
        items.add(
            BrowserMenuCheckbox("Request desktop site", {
                sessionManager.selectedSessionOrThrow.desktopMode
            }) { checked ->
                sessionUseCases.requestDesktopSite(checked)
            }.apply {
                visible = { sessionManager.selectedSession != null }
            }
        )
        items.add(
            BrowserMenuCheckbox("Open links in apps", {
                preferences.getBoolean(PREF_LAUNCH_EXTERNAL_APP, false)
            }) { checked ->
                preferences.edit().putBoolean(PREF_LAUNCH_EXTERNAL_APP, checked).apply()
            }
        )

        items
    }

    private val menuToolbar by lazy {
        val forward = BrowserMenuItemToolbar.Button(
                mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
                iconTintColorResource = R.color.photonBlue90,
                contentDescription = "Forward",
                isEnabled = { sessionManager.selectedSession?.canGoForward == true }
        ) {
            sessionUseCases.goForward()
        }

        val refresh = BrowserMenuItemToolbar.Button(
                mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
                iconTintColorResource = R.color.photonBlue90,
                contentDescription = "Refresh",
                isEnabled = { sessionManager.selectedSession?.loading != true }
        ) {
            sessionUseCases.reload()
        }

        val stop = BrowserMenuItemToolbar.Button(
                mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
                iconTintColorResource = R.color.photonBlue90,
                contentDescription = "Stop") {
            sessionUseCases.stopLoading()
        }

        BrowserMenuItemToolbar(listOf(forward, refresh, stop))
    }

    val shippedDomainsProvider by lazy {
        ShippedDomainsProvider().also { it.initialize(applicationContext) }
    }

    val tabsUseCases: TabsUseCases by lazy { TabsUseCases(sessionManager) }
    val downloadsUseCases: DownloadsUseCases by lazy { DownloadsUseCases(store) }
    val contextMenuUseCases: ContextMenuUseCases by lazy { ContextMenuUseCases(sessionManager, store) }
}
