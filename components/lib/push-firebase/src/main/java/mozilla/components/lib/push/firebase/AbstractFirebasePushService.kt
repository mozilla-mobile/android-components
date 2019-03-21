/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.lib.push.firebase

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mozilla.components.concept.push.Push
import mozilla.components.concept.push.PushMessage
import mozilla.components.concept.push.PushService

abstract class AbstractFirebasePushService : FirebaseMessagingService(), PushService {

    init {
//        val result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(baseContext)
//        if (result != ConnectionResult.SUCCESS) {
//            throw Exception("NeedsGooglePlayServicesException isn't available on this device.")
//        }
    }

    override fun start() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
    }

    override fun onNewToken(newToken: String) {
        Push.requireInstance.onNewToken(newToken)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        remoteMessage?.let {
            val message = PushMessage(it.data.toString())
            Push.requireInstance.onMessageReceived(message)
        }
    }

    override fun stop() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = false
    }

    override fun forceRegistrationRenewal() {
        stop()
        FirebaseInstanceId.getInstance().deleteInstanceId()
        start()
    }
}