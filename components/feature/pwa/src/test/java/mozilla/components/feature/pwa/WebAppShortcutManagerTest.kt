/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.manifest.Size
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.pwa.WebAppLauncherActivity.Companion.ACTION_PWA_LAUNCHER
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.initMocks
import org.robolectric.util.ReflectionHelpers.setStaticField
import kotlin.reflect.jvm.javaField

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class WebAppShortcutManagerTest {
    private lateinit var context: Context
    @Mock private lateinit var httpClient: Client
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var shortcutManager: ShortcutManager
    @Mock private lateinit var storage: ManifestStorage
    @Mock private lateinit var icons: BrowserIcons
    private lateinit var manager: WebAppShortcutManager
    private val baseManifest = WebAppManifest(
        name = "Demo",
        startUrl = "https://example.com"
    )

    @Before
    fun setup() {
        setSdkInt(0)
        initMocks(this)
        context = spy(testContext)

        doReturn(packageManager).`when`(context).packageManager
        doReturn(shortcutManager).`when`(context).getSystemService(ShortcutManager::class.java)
        doReturn("").`when`(context).getString(R.string.mozac_feature_pwa_default_shortcut_label)

        manager = spy(WebAppShortcutManager(context, httpClient, storage))
        doReturn(icons).`when`(manager).icons
    }

    @After
    fun teardown() = setSdkInt(0)

    @Test
    fun `requestPinShortcut no-op if pinning unsupported`() = runBlockingTest {
        val manifest = baseManifest.copy(
            display = WebAppManifest.DisplayMode.STANDALONE,
            icons = listOf(WebAppManifest.Icon(
                src = "https://example.com/icon.png",
                sizes = listOf(Size(192, 192))
            ))
        )
        val session = buildInstallableSession(manifest)
        `when`(packageManager.queryBroadcastReceivers(any(), anyInt())).thenReturn(emptyList())

        manager.requestPinShortcut(context, session)
        verify(manager, never()).buildWebAppShortcut(context, manifest)

        setSdkInt(Build.VERSION_CODES.O)
        `when`(shortcutManager.isRequestPinShortcutSupported).thenReturn(false)
        clearInvocations(manager)

        manager.requestPinShortcut(context, session)
        verify(manager, never()).buildWebAppShortcut(context, manifest)
    }

    @Test
    fun `requestPinShortcut won't make a PWA icon if the session is not installable`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val manifest = baseManifest.copy(
            display = WebAppManifest.DisplayMode.STANDALONE,
            icons = emptyList() // no icons
        )
        val session = buildInstallableSession(manifest)
        val shortcutCompat: ShortcutInfoCompat = mock()
        `when`(shortcutManager.isRequestPinShortcutSupported).thenReturn(true)
        doReturn(shortcutCompat).`when`(manager).buildBasicShortcut(context, session)

        manager.requestPinShortcut(context, session)
        verify(manager, never()).buildWebAppShortcut(context, manifest)
        verify(manager).buildBasicShortcut(context, session)
    }

    @Test
    fun `requestPinShortcut pins PWA shortcut`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val manifest = baseManifest.copy(
            display = WebAppManifest.DisplayMode.STANDALONE,
            icons = listOf(WebAppManifest.Icon(
                src = "https://example.com/icon.png",
                sizes = listOf(Size(192, 192))
            ))
        )
        val session = buildInstallableSession(manifest)
        val shortcutCompat: ShortcutInfoCompat = mock()
        `when`(shortcutManager.isRequestPinShortcutSupported).thenReturn(true)
        doReturn(shortcutCompat).`when`(manager).buildWebAppShortcut(context, manifest)

        manager.requestPinShortcut(context, session)
        verify(manager).buildWebAppShortcut(context, manifest)
        verify(shortcutManager).requestPinShortcut(any(), any())
    }

    @Test
    fun `requestPinShortcut pins basic shortcut`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val session: Session = mock()
        `when`(session.securityInfo).thenReturn(Session.SecurityInfo(secure = true))
        val shortcutCompat: ShortcutInfoCompat = mock()
        `when`(shortcutManager.isRequestPinShortcutSupported).thenReturn(true)
        doReturn(shortcutCompat).`when`(manager).buildBasicShortcut(context, session)

        manager.requestPinShortcut(context, session)
        verify(manager).buildBasicShortcut(context, session)
        verify(shortcutManager).requestPinShortcut(any(), any())
    }

    @Test
    fun `buildBasicShortcut uses manifest short name as label by default`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val session = Session("https://mozilla.org")
        session.title = "Internet for people, not profit — Mozilla"
        session.webAppManifest = WebAppManifest(
            name = "Mozilla",
            shortName = "Moz",
            startUrl = "https://mozilla.org"
        )
        val shortcut = manager.buildBasicShortcut(context, session)

        assertEquals("Moz", shortcut.shortLabel)
    }

    @Test
    fun `buildBasicShortcut uses manifest name as label by default`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val session = Session("https://mozilla.org")
        session.title = "Internet for people, not profit — Mozilla"
        session.webAppManifest = WebAppManifest(name = "Mozilla", startUrl = "https://mozilla.org")
        val shortcut = manager.buildBasicShortcut(context, session)

        assertEquals("Mozilla", shortcut.shortLabel)
    }

    @Test
    fun `buildBasicShortcut uses session title as label if there is no manifest`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val expectedTitle = "Internet for people, not profit — Mozilla"
        val session = Session("https://mozilla.org")
        session.title = expectedTitle
        val shortcut = manager.buildBasicShortcut(context, session)

        assertEquals(expectedTitle, shortcut.shortLabel)
    }

    @Test
    fun `buildBasicShortcut can create a shortcut with a custom name`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.O)
        val title = "Internet for people, not profit — Mozilla"
        val expectedName = "Mozilla"
        val session = Session("https://mozilla.org")
        session.title = title
        val shortcut = manager.buildBasicShortcut(context, session, expectedName)

        assertEquals(expectedName, shortcut.shortLabel)
    }

    @Test
    fun `updateShortcuts no-op`() = runBlockingTest {
        val manifests = listOf(baseManifest)
        doReturn(null).`when`(manager).buildWebAppShortcut(context, manifests[0])

        manager.updateShortcuts(context, manifests)
        verify(manager, never()).buildWebAppShortcut(context, manifests[0])
        verify(shortcutManager, never()).updateShortcuts(any())

        setSdkInt(Build.VERSION_CODES.N_MR1)
        manager.updateShortcuts(context, manifests)
        verify(shortcutManager).updateShortcuts(emptyList())
    }

    @Test
    fun `updateShortcuts updates list of existing shortcuts`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.N_MR1)
        val manifests = listOf(baseManifest)
        val shortcutCompat: ShortcutInfoCompat = mock()
        val shortcut: ShortcutInfo = mock()
        doReturn(shortcutCompat).`when`(manager).buildWebAppShortcut(context, manifests[0])
        doReturn(shortcut).`when`(shortcutCompat).toShortcutInfo()

        manager.updateShortcuts(context, manifests)
        verify(shortcutManager).updateShortcuts(listOf(shortcut))
    }

    @Test
    fun `buildWebAppShortcut builds shortcut and saves manifest`() = runBlockingTest {
        doReturn(mock<IconCompat>()).`when`(manager).buildIconFromManifest(baseManifest)

        val shortcut = manager.buildWebAppShortcut(context, baseManifest)!!
        val intent = shortcut.intent

        verify(storage).saveManifest(baseManifest)

        assertEquals("https://example.com", shortcut.id)
        assertEquals("Demo", shortcut.longLabel)
        assertEquals("Demo", shortcut.shortLabel)
        assertEquals(ACTION_PWA_LAUNCHER, intent.action)
        assertEquals("https://example.com".toUri(), intent.data)
    }

    @Test
    fun `buildWebAppShortcut builds shortcut with short name`() = runBlockingTest {
        val manifest = WebAppManifest(name = "Demo Demo", shortName = "DD", startUrl = "https://example.com")
        doReturn(mock<IconCompat>()).`when`(manager).buildIconFromManifest(manifest)

        val shortcut = manager.buildWebAppShortcut(context, manifest)!!

        assertEquals("https://example.com", shortcut.id)
        assertEquals("Demo Demo", shortcut.longLabel)
        assertEquals("DD", shortcut.shortLabel)
    }

    @Test
    fun `findShortcut returns shortcut`() {
        assertNull(manager.findShortcut(context, "https://mozilla.org"))

        setSdkInt(Build.VERSION_CODES.N_MR1)
        val exampleShortcut = mock<ShortcutInfo>().apply {
            `when`(id).thenReturn("https://example.com")
        }
        `when`(shortcutManager.pinnedShortcuts).thenReturn(listOf(exampleShortcut))

        assertNull(manager.findShortcut(context, "https://mozilla.org"))

        val mozShortcut = mock<ShortcutInfo>().apply {
            `when`(id).thenReturn("https://mozilla.org")
        }
        `when`(shortcutManager.pinnedShortcuts).thenReturn(listOf(mozShortcut, exampleShortcut))

        assertEquals(mozShortcut, manager.findShortcut(context, "https://mozilla.org"))
    }

    @Test
    fun `checking unknown url returns uninstalled state`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.N_MR1)

        val url = "https://mozilla.org"
        val currentTime = System.currentTimeMillis()

        whenever(storage.hasRecentManifest(url, currentTime))
                .thenReturn(false)

        val installState = manager.getWebAppInstallState(url, currentTime)

        assertEquals(WebAppShortcutManager.WebAppInstallState.NotInstalled, installState)
    }

    @Test
    fun `checking a known url returns installed state`() = runBlockingTest {
        setSdkInt(Build.VERSION_CODES.N_MR1)

        val url = "https://mozilla.org/pwa/"
        val currentTime = System.currentTimeMillis()

        whenever(storage.hasRecentManifest(url, currentTime))
                .thenReturn(true)

        val installState = manager.getWebAppInstallState(url, currentTime)

        assertEquals(WebAppShortcutManager.WebAppInstallState.Installed, installState)
    }

    private fun setSdkInt(sdkVersion: Int) {
        setStaticField(Build.VERSION::SDK_INT.javaField, sdkVersion)
    }

    private fun buildInstallableSession(manifest: WebAppManifest): Session {
        val session: Session = mock()
        `when`(session.webAppManifest).thenReturn(manifest)
        `when`(session.url).thenReturn(manifest.startUrl)
        `when`(session.securityInfo).thenReturn(Session.SecurityInfo(secure = true))
        return session
    }
}
