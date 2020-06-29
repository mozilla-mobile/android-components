/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.webnotifications

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.icons.Icon.Source
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webnotifications.WebNotification
import mozilla.components.feature.sitepermissions.SitePermissions
import mozilla.components.feature.sitepermissions.SitePermissions.Status
import mozilla.components.feature.sitepermissions.SitePermissionsStorage
import mozilla.components.support.test.any
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class WebNotificationFeatureTest {
    private val context = spy(testContext)
    private val browserIcons: BrowserIcons = mock()
    private val icon: Icon = mock()
    private val engine: Engine = mock()
    private val notificationManager: NotificationManager = mock()
    private val permissionsStorage: SitePermissionsStorage = mock()

    private val testNotification = WebNotification(
        "Mozilla",
        "mozilla.org",
        "Notification body",
        "mozilla.org",
        "https://mozilla.org/image.ico",
        "rtl",
        "en",
        false
    )

    @Before
    fun setup() {
        `when`(context.getSystemService(NotificationManager::class.java)).thenReturn(notificationManager)
        `when`(icon.source).thenReturn(Source.GENERATOR) // to no-op the browser icons call.
        `when`(browserIcons.loadIcon(any())).thenReturn(CompletableDeferred(icon))
    }

    @Test
    fun `register web notification delegate`() {
        doNothing().`when`(engine).registerWebNotificationDelegate(any())
        doNothing().`when`(notificationManager).createNotificationChannel(any())

        WebNotificationFeature(context, engine, browserIcons, android.R.drawable.ic_dialog_alert, mock(), null)

        verify(engine).registerWebNotificationDelegate(any())
    }

    @Test
    fun `engine notifies to cancel notification`() {
        val webNotification: WebNotification = mock()
        val feature =
            WebNotificationFeature(context, engine, browserIcons, android.R.drawable.ic_dialog_alert, mock(), null)

        `when`(webNotification.tag).thenReturn("testTag")

        feature.onCloseNotification(webNotification)

        verify(notificationManager).cancel("testTag", NOTIFICATION_ID)
    }

    @Test
    fun `engine notifies to show notification`() = runBlockingTest {
        val feature = WebNotificationFeature(
            context,
            engine,
            browserIcons,
            android.R.drawable.ic_dialog_alert,
            permissionsStorage,
            null,
            coroutineContext
        )
        val permission = SitePermissions(origin = "mozilla.org", notification = Status.ALLOWED, savedAt = 0)

        `when`(permissionsStorage.findSitePermissionsBy(any())).thenReturn(permission)

        feature.onShowNotification(testNotification)

        verify(notificationManager).notify(eq(testNotification.tag), eq(NOTIFICATION_ID), any())
    }

    @Test
    fun `notification ignored if permissions are not allowed`() = runBlockingTest {
        val feature = WebNotificationFeature(
            context,
            engine,
            browserIcons,
            android.R.drawable.ic_dialog_alert,
            permissionsStorage,
            null,
            coroutineContext
        )

        // No permissions found.

        feature.onShowNotification(testNotification)

        verify(notificationManager, never()).notify(eq(testNotification.tag), eq(NOTIFICATION_ID), any())

        // When explicitly denied.

        val permission = SitePermissions(origin = "mozilla.org", notification = Status.BLOCKED, savedAt = 0)
        `when`(permissionsStorage.findSitePermissionsBy(any())).thenReturn(permission)

        feature.onShowNotification(testNotification)

        verify(notificationManager, never()).notify(eq(testNotification.tag), eq(NOTIFICATION_ID), any())
    }
}
