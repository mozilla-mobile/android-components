/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.push

import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import mozilla.appservices.push.BridgeType
import mozilla.appservices.push.PushAPI
import mozilla.appservices.push.PushError
import mozilla.appservices.push.PushManager
import mozilla.appservices.push.PushSubscriptionChanged as SubscriptionChanged
import mozilla.appservices.push.SubscriptionResponse
import java.io.Closeable
import java.util.Locale
import java.util.UUID

typealias PushScope = String
typealias AppServerKey = String

/**
 * An interface that wraps the [PushAPI].
 *
 * This aides in testing and abstracting out the hurdles of initialization checks required before performing actions
 * on the API.
 */
interface PushConnection : Closeable {
    /**
     * Creates a push subscription for the given scope.
     *
     * @return A push subscription.
     */
    suspend fun subscribe(scope: PushScope, appServerKey: AppServerKey? = null): AutoPushSubscription

    /**
     * Un-subscribes a push subscription for the given scope.
     *
     * @return the invocation result if it was successful.
     */
    suspend fun unsubscribe(scope: PushScope): Boolean

    /**
     * Un-subscribes all push subscriptions.
     *
     * @return the invocation result if it was successful.
     */
    suspend fun unsubscribeAll(): Boolean

    /**
     * Updates the registration token to the native Push API if it changes.
     *
     * @return the invocation result if it was successful.
     */
    suspend fun updateToken(token: String): Boolean

    /**
     * Checks validity of current push subscriptions.
     *
     * Implementation notes: This API will change to return the specific subscriptions that have been updated.
     * See: https://github.com/mozilla/application-services/issues/2049
     *
     * @return the list of push subscriptions that were updated for subscribers that should be notified about.
     */
    suspend fun verifyConnection(): List<AutoPushSubscriptionChanged>

    /**
     * Decrypts a received message.
     *
     * @return a pair of the push scope and the decrypted message, respectively, else null if there was no valid client
     * for the message.
     */
    suspend fun decryptMessage(
        channelId: String,
        body: String,
        encoding: String = "",
        salt: String = "",
        cryptoKey: String = ""
    ): Pair<PushScope, ByteArray>?

    /**
     * Checks if the native Push API has already been initialized.
     */
    fun isInitialized(): Boolean
}

/**
 * An implementation of [PushConnection] for the native component using the [PushAPI].
 *
 * Implementation notes: There are few important concepts to note here - for WebPush, the identifier that is required
 * for notifying clients is called a "scope". This can be a site's host URL or an other uniquely identifying string to
 * create a push subscription for that given scope.
 *
 * In the native Rust component the concept is similar, in that we have a unique ID for each subscription that is local
 * to the device, however the ID is called a "channel ID" (or chid), which is a UUID. In the implementation, the scope
 * can also be provided to the native layer, however currently it is just stored in the database but not used in any
 * significant way (this may change in the future).
 *
 * With this in mind, we decided to write our implementation to be a 1-1 mapping of the public API 'scope' to the
 * internal API 'channel ID' by generating a UUID from the scope value. This means that we have a reproducible way to
 * always retrieve the chid when the caller passes the scope to us.
 *
 * Some nuances are that we also need to provide the native API the same scope value along with the
 * scope-based chid value to satisfy the internal API requirements, at the cost of some noticeable duplication of
 * information, so that we can retrieve those values later; see [RustPushConnection.subscribe] and
 * [RustPushConnection.unsubscribe] implementations for details.
 *
 * Another nuance is that, in order to satisfy the API shape of the subscription, we need to query the database for
 * the scope in accordance with decrypting the message. This lets us notify our observers on messages for a particular
 * receiver; see [RustPushConnection.decryptMessage] implementation for details.
 */
internal class RustPushConnection(
    private val databasePath: String,
    private val senderId: String,
    private val serverHost: String,
    private val socketProtocol: Protocol,
    private val serviceType: ServiceType
) : PushConnection {

    @VisibleForTesting
    internal var api: PushAPI? = null

    @GuardedBy("this")
    override suspend fun subscribe(
        scope: PushScope,
        appServerKey: AppServerKey?
    ): AutoPushSubscription = synchronized(this) {
        val pushApi = api
        check(pushApi != null) { "Rust API is not initiated; updateToken hasn't been called yet." }

        // Generate the channel ID from the scope so that it's reproducible if we need to query for it later.
        val channelId = scope.toChannelId()
        val response = pushApi.subscribe(channelId, scope, appServerKey)

        return response.toPushSubscription(scope, appServerKey)
    }

    @GuardedBy("this")
    override suspend fun unsubscribe(scope: PushScope): Boolean = synchronized(this) {
        val pushApi = api
        check(pushApi != null) { "Rust API is not initiated; updateToken hasn't been called yet." }

        // Generate the channel ID from the scope so that it's reproducible if we need to query for it later.
        val channelId = scope.toChannelId()

        return pushApi.unsubscribe(channelId)
    }

    @GuardedBy("this")
    override suspend fun unsubscribeAll(): Boolean = synchronized(this) {
        val pushApi = api
        check(pushApi != null) { "Rust API is not initiated; updateToken hasn't been called yet." }

        return pushApi.unsubscribeAll()
    }

    @GuardedBy("this")
    override suspend fun updateToken(token: String): Boolean = synchronized(this) {
        val pushApi = api
        if (pushApi == null) {
            api = PushManager(
                senderId = senderId,
                serverHost = serverHost,
                httpProtocol = socketProtocol.asString(),
                bridgeType = serviceType.toBridgeType(),
                registrationId = token,
                databasePath = databasePath
            )
            return true
        }
        // This call will fail if we haven't 'subscribed' yet.
        return try {
            pushApi.update(token)
        } catch (e: PushError) {
            // Once we get GeneralError, let's catch that instead:
            // https://github.com/mozilla/application-services/issues/2541
            val fakeChannelId = "fake".toChannelId()
            // It's possible that we have a race (on a first run) between 'subscribing' and setting a token.
            // 'update' expects that we've called 'subscribe' (which would obtain a 'uaid' from an autopush
            // server), which we need to have in order to call 'update' on the library.
            // In https://github.com/mozilla/application-services/issues/2490 this will be fixed, and we
            // can clean up this work-around.
            pushApi.subscribe(fakeChannelId)

            // If this fails again, give up - it seems like a legit failure that we should re-throw.
            pushApi.update(token)
        }
    }

    @GuardedBy("this")
    override suspend fun verifyConnection(): List<AutoPushSubscriptionChanged> = synchronized(this) {
        val pushApi = api
        check(pushApi != null) { "Rust API is not initiated; updateToken hasn't been called yet." }

        return pushApi.verifyConnection().map { it.toPushSubscriptionChanged() }
    }

    @GuardedBy("this")
    override suspend fun decryptMessage(
        channelId: String,
        body: String,
        encoding: String,
        salt: String,
        cryptoKey: String
    ): Pair<PushScope, ByteArray>? = synchronized(this) {
        val pushApi = api
        check(pushApi != null) { "Rust API is not initiated; updateToken hasn't been called yet." }

        // Query for the scope so that we can notify observers who the decrypted message is for.
        val scope = pushApi.dispatchInfoForChid(channelId)?.scope

        scope?.let {
            val data = pushApi.decrypt(
                channelID = channelId,
                body = body,
                encoding = encoding,
                salt = salt,
                dh = cryptoKey
            )

            return Pair(scope, data)
        }
    }

    @GuardedBy("this")
    override fun close() = synchronized(this) {
        val pushApi = api
        check(pushApi != null) { "Rust API is not initiated; updateToken hasn't been called yet." }

        pushApi.close()
    }

    override fun isInitialized() = api != null
}

/**
 * Helper function to get the corresponding support [BridgeType] from the support set.
 */
@VisibleForTesting
internal fun ServiceType.toBridgeType() = when (this) {
    ServiceType.FCM -> BridgeType.FCM
    ServiceType.ADM -> BridgeType.ADM
}

/**
 * Helper function to convert the [Protocol] into the required value the native implementation requires.
 */
@VisibleForTesting
internal fun Protocol.asString() = name.toLowerCase(Locale.ROOT)

/**
 * A channel ID from the provided scope.
 */
internal fun PushScope.toChannelId() =
    UUID.nameUUIDFromBytes(this.toByteArray()).toString().replace("-", "")

/**
 * A helper to convert the internal data class.
 */
internal fun SubscriptionResponse.toPushSubscription(
    scope: String,
    appServerKey: AppServerKey? = null
): AutoPushSubscription {
    return AutoPushSubscription(
        scope = scope,
        endpoint = subscriptionInfo.endpoint,
        authKey = subscriptionInfo.keys.auth,
        publicKey = subscriptionInfo.keys.p256dh,
        appServerKey = appServerKey
    )
}

/**
 * A helper to convert the internal data class.
 */
internal fun SubscriptionChanged.toPushSubscriptionChanged() = AutoPushSubscriptionChanged(
    scope = scope,
    channelId = channelID
)
