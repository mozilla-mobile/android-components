/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.webnotifications

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon.Source
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.icons.IconRequest.Size
import mozilla.components.concept.engine.webnotifications.WebNotification

internal class NativeNotificationBridge(
    private val icons: BrowserIcons,
    @DrawableRes private val smallIcon: Int
) {
    companion object {
        private const val EXTRA_ON_CLICK = "mozac.feature.webnotifications.generic.onclick"
    }

    /**
     * Create a system [Notification] from this [WebNotification].
     */
    @Suppress("LongParameterList")
    suspend fun convertToAndroidNotification(
        notification: WebNotification,
        context: Context,
        channelId: String,
        activityClass: Class<out Activity>?,
        requestId: Int
    ): Notification {
        val builder = if (SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("Deprecation")
            Notification.Builder(context)
        }

        with(notification) {
            activityClass?.let {
                val intent = Intent(context, activityClass).apply {
                    putExtra(EXTRA_ON_CLICK, tag)
                }

                PendingIntent.getActivity(context, requestId, intent, 0).apply {
                    builder.setContentIntent(this)
                }
            }

            builder.setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setShowWhen(true)
                .setWhen(timestamp)
                .setAutoCancel(true)

            loadIcon(iconUrl?.toUri(), Size.DEFAULT)?.let { iconBitmap ->
                builder.setLargeIcon(iconBitmap)
            }
        }

        return builder.build()
    }

    /**
     * Load an icon for a notification.
     */
    private suspend fun loadIcon(url: Uri?, size: Size): Bitmap? {
        url ?: return null
        val icon = icons.loadIcon(IconRequest(
            url = url.toString(),
            size = size,
            resources = listOf(IconRequest.Resource(
                url = url.toString(),
                type = IconRequest.Resource.Type.MANIFEST_ICON
            ))
        )).await()

        return if (icon.source == Source.GENERATOR) null else icon.bitmap
    }
}
