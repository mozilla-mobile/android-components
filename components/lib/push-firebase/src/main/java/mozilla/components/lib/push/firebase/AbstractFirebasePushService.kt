/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.lib.push.firebase

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mozilla.components.concept.push.EncryptedPushMessage
import mozilla.components.concept.push.PushProvider
import mozilla.components.concept.push.PushService

abstract class AbstractFirebasePushService : FirebaseMessagingService(), PushService {

    init {
        start() // Allow the app to choose when to start? This is harder to allow than it looks..
    }

    final override fun start() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
    }

    override fun onNewToken(newToken: String) {
        PushProvider.requireInstance.onNewToken(newToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        remoteMessage?.let {
            val message = EncryptedPushMessage.invoke(
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

    final override fun forceRegistrationRenewal() {
        stop()
        FirebaseInstanceId.getInstance().deleteInstanceId()
        start()
    }

    final override fun isAvailable(context: Context): Boolean {
        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return result == ConnectionResult.SUCCESS
    }
}
