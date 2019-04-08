/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.lib.push.firebase

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mozilla.components.concept.push.EncryptedPushMessage
import mozilla.components.concept.push.Error
import mozilla.components.concept.push.PushProvider
import mozilla.components.concept.push.PushService

abstract class AbstractFirebasePushService : FirebaseMessagingService(), PushService {

    init {
        // TODO When we can expose start/stop of the service to be controlled by the application, we can remove this.
        // See: https://github.com/mozilla-mobile/android-components/issues/2603
        start()
    }

    final override fun start() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
    }

    override fun onNewToken(newToken: String) {
        PushProvider.requireInstance.onNewToken(newToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        remoteMessage?.let {
            val message = EncryptedPushMessage(
                it.data.getValue("chid"),
                it.data.getValue("body"),
                it.data.getValue("con"),
                it.data["enc"],
                it.data["cryptokey"]
            )
            PushProvider.requireInstance.onMessageReceived(message)
        }
    }

    final override fun stop() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = false
    }

    companion object {

        /**
         * A helper method that allows you to check if the device is supported by Google Play Services in order to
         * receive push notifications from Firebase Cloud Messaging.
         */
        fun isAvailable(context: Context): Boolean {
            val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (result != ConnectionResult.SUCCESS) {
                PushProvider.requireInstance.onError(Error.ServiceUnavailable(
                    "This device does not support GCM! isGooglePlayServicesAvailable returned: $result")
                )
            }
            return result == ConnectionResult.SUCCESS
        }
    }
}
