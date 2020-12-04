/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("LongParameterList")

package mozilla.components.feature.accounts.push

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.base.crash.Breadcrumb
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.push.exceptions.SubscriptionException
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.ConstellationState
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.DeviceConstellationObserver
import mozilla.components.concept.sync.DevicePushSubscription
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.feature.accounts.push.ext.redactPartialUri
import mozilla.components.feature.push.AutoPushFeature
import mozilla.components.feature.push.AutoPushSubscription
import mozilla.components.feature.push.PushScope
import mozilla.components.concept.sync.AccountObserver as SyncAccountObserver
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.manager.ext.withConstellation
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.utils.SharedPreferencesCache
import org.json.JSONObject
import java.util.UUID

internal const val PREFERENCE_NAME = "mozac_feature_accounts_push"
internal const val PREF_LAST_VERIFIED = "last_verified_push_subscription"
internal const val PREF_FXA_SCOPE = "fxa_push_scope"

/**
 * A feature used for supporting FxA and push integration where needed. One of the main functions is when FxA notifies
 * the device during a sync, that it's unable to reach the device via push messaging; triggering a push
 * registration renewal.
 *
 * @param context The application Android context.
 * @param accountManager The FxaAccountManager.
 * @param pushFeature The [AutoPushFeature] if that is setup for observing push events.
 * @param crashReporter Instance of `CrashReporting` to record unexpected caught exceptions.
 * @param owner the lifecycle owner for the observer. Defaults to [ProcessLifecycleOwner].
 * @param autoPause whether to stop notifying the observer during onPause lifecycle events.
 * Defaults to false so that observers are always notified.
 */
class FxaPushSupportFeature(
    private val context: Context,
    accountManager: FxaAccountManager,
    pushFeature: AutoPushFeature,
    private val crashReporter: CrashReporting? = null,
    owner: LifecycleOwner = ProcessLifecycleOwner.get(),
    autoPause: Boolean = false
) {

    /**
     * A unique scope for the FxA push subscription that is generated once and stored in SharedPreferences.
     *
     * This scope is randomly generated and unique to the account on that particular device.
     */
    private val fxaPushScope: String by lazy {
        val prefs = preference(context)

        // Generate a unique scope if one doesn't exist.
        val randomUuid = UUID.randomUUID().toString().replace("-", "")

        // Return a scope in the format example: "fxa_push_scope_a62d5f27c9d74af4996d057f0e0e9c38"
        val scope = PUSH_SCOPE_PREFIX + randomUuid

        if (!prefs.contains(PREF_FXA_SCOPE)) {
            prefs.edit().putString(PREF_FXA_SCOPE, scope).apply()

            return@lazy scope
        }

        // The default string is non-null, so we can safely cast.
        prefs.getString(PREF_FXA_SCOPE, scope) as String
    }

    init {
        val autoPushObserver = AutoPushObserver(accountManager, pushFeature, fxaPushScope)

        val accountObserver = AccountObserver(
            context,
            pushFeature,
            fxaPushScope,
            crashReporter,
            owner,
            autoPause
        )

        accountManager.register(accountObserver)

        pushFeature.register(autoPushObserver, owner, autoPause)
    }

    companion object {
        const val PUSH_SCOPE_PREFIX = "fxa_push_scope_"
    }
}

/**
 * An [FxaAccountManager] observer to know when an account has been added, so we can begin observing the device
 * constellation.
 */
internal class AccountObserver(
    private val context: Context,
    private val push: AutoPushFeature,
    private val fxaPushScope: String,
    private val crashReporter: CrashReporting?,
    private val lifecycleOwner: LifecycleOwner,
    private val autoPause: Boolean
) : SyncAccountObserver {

    private val logger = Logger(AccountObserver::class.java.simpleName)
    private val verificationDelegate = VerificationDelegate(context, push.config.disableRateLimit)

    override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {

        val constellationObserver = ConstellationObserver(
            context = context,
            push = push,
            scope = fxaPushScope,
            account = account,
            verifier = verificationDelegate,
            crashReporter = crashReporter
        )

        // We need a new subscription only when we have a new account.
        // The subscription is removed when an account logs out.
        if (authType != AuthType.Existing && authType != AuthType.Recovered) {
            logger.debug("Subscribing for FxaPushScope ($fxaPushScope) events.")

            push.subscribe(
                scope = fxaPushScope,
                onSubscribeError = { e ->
                    crashReporter?.recordCrashBreadcrumb(Breadcrumb("Subscribing to FxA push failed at login."))
                    logger.info("Subscribing to FxA push failed at login.", e)
                },
                onSubscribe = { subscription ->
                    logger.info("Created a new subscription: $subscription")
                    CoroutineScope(Dispatchers.Main).launch {
                        account.deviceConstellation().setDevicePushSubscription(subscription.into())
                    }
                })
        }

        // NB: can we just expose registerDeviceObserver on account manager?
        // registration could happen after onDevicesUpdate has been called, without having to tie this
        // into the account "auth lifecycle".
        // See https://github.com/mozilla-mobile/android-components/issues/8766
        account.deviceConstellation().registerDeviceObserver(constellationObserver, lifecycleOwner, autoPause)
    }

    override fun onLoggedOut() {
        logger.debug("Un-subscribing for FxA scope $fxaPushScope events.")

        push.unsubscribe(fxaPushScope)

        // Delete cached value of last verified timestamp and scope when we log out.
        preference(context).edit()
            .remove(PREF_LAST_VERIFIED)
            .apply()
    }
}

/**
 * A [DeviceConstellation] observer to know when we should notify the push feature to begin the registration renewal
 * when notified by the FxA server. See [Device.subscriptionExpired].
 */
internal class ConstellationObserver(
    context: Context,
    private val push: AutoPushFeature,
    private val scope: String,
    private val account: OAuthAccount,
    private val verifier: VerificationDelegate = VerificationDelegate(context),
    private val crashReporter: CrashReporting?
) : DeviceConstellationObserver {

    private val logger = Logger(ConstellationObserver::class.java.simpleName)

    override fun onDevicesUpdate(constellation: ConstellationState) {
        logger.info("onDevicesUpdate triggered.")
        val updateSubscription = constellation.currentDevice?.let {
            it.subscription == null || it.subscriptionExpired
        } ?: false

        // If our subscription has not expired, we do nothing.
        // If our last check was recent (see: PERIODIC_INTERVAL_MILLISECONDS), we do nothing.
        val allowedToRenew = verifier.allowedToRenew()
        if (!updateSubscription || !allowedToRenew) {
            logger.info(
                "Short-circuiting onDevicesUpdate: " +
                    "updateSubscription($updateSubscription), allowedToRenew($allowedToRenew)"
            )
            return
        } else {
            logger.info("Proceeding to renew registration")
        }

        logger.info("Our push subscription either doesn't exist or is expired; re-subscribing.")
        push.unsubscribe(
            scope = scope,
            onUnsubscribeError = ::onUnsubscribeError,
            onUnsubscribe = ::onUnsubscribeResult
        )
        push.subscribe(
            scope = scope,
            onSubscribeError = ::onSubscribeError,
            onSubscribe = { onSubscribe(constellation, it) }
        )

        logger.info("Incrementing verifier")
        logger.info("Verifier state before: timestamp=${verifier.innerTimestamp}, count=${verifier.innerCount}")
        verifier.increment()
        logger.info("Verifier state after: timestamp=${verifier.innerTimestamp}, count=${verifier.innerCount}")
    }

    internal fun onSubscribe(constellation: ConstellationState, subscription: AutoPushSubscription) {

        logger.info("Created a new subscription: $subscription")

        val oldEndpoint = constellation.currentDevice?.subscription?.endpoint
        if (subscription.endpoint == oldEndpoint) {
            val exception = SubscriptionException(
                "New push endpoint matches existing one",
                Throwable(
                    "Endpoint: ${subscription.endpoint.redactPartialUri()}"
                )
            )

            logger.warn("Push endpoints match!", exception)

            crashReporter?.submitCaughtException(exception)
        }

        CoroutineScope(Dispatchers.Main).launch {
            account.deviceConstellation().setDevicePushSubscription(subscription.into())
        }
    }

    internal fun onSubscribeError(e: Exception) {
        val errorMessage = "Re-subscribing failed; FxA push events will not be received."

        logger.warn(errorMessage, e)
        crashReporter?.submitCaughtException(SubscriptionException(errorMessage, e))
    }

    internal fun onUnsubscribeError(e: Exception) {
        val errorMessage = "Un-subscribing to failed FxA push after subscriptionExpired"

        logger.warn(errorMessage, e)
        crashReporter?.recordCrashBreadcrumb(
            Breadcrumb(
                category = ConstellationObserver::class.java.simpleName,
                message = errorMessage,
                data = mapOf(
                    "exception" to e.javaClass.name,
                    "message" to e.message.orEmpty()
                )
            )
        )
    }

    private fun onUnsubscribeResult(success: Boolean) {
        logger.info("Un-subscribing successful: $success")
        if (success) {
            logger.info("Subscribe call should give you a new endpoint.")
        }
    }
}

/**
 * An [AutoPushFeature] observer to handle [FxaAccountManager] subscriptions and push events.
 */
internal class AutoPushObserver(
    private val accountManager: FxaAccountManager,
    private val pushFeature: AutoPushFeature,
    private val fxaPushScope: String
) : AutoPushFeature.Observer {
    private val logger = Logger(AutoPushObserver::class.java.simpleName)

    override fun onMessageReceived(scope: String, message: ByteArray?) {
        if (scope != fxaPushScope) {
            return
        }

        logger.info("Received new push message for $scope")

        // Ignore push messages that do not have data.
        val rawEvent = message ?: return

        accountManager.withConstellation {
            CoroutineScope(Dispatchers.Main).launch {
                processRawEvent(String(rawEvent))
            }
        }
    }

    override fun onSubscriptionChanged(scope: PushScope) {
        if (scope != fxaPushScope) {
            return
        }

        logger.info("Our sync push scope ($scope) has expired. Re-subscribing..")

        pushFeature.subscribe(fxaPushScope) { subscription ->
            val account = accountManager.authenticatedAccount()

            if (account == null) {
                logger.info("We don't have any account to pass the push subscription to.")
                return@subscribe
            }

            CoroutineScope(Dispatchers.Main).launch {
                account.deviceConstellation().setDevicePushSubscription(subscription.into())
            }
        }
    }
}

/**
 * A helper that rate limits how often we should notify our servers to renew push registration. For debugging, we
 * can override this rate-limit check by enabling the [disableRateLimit] flag.
 *
 * Implementation notes: This saves the timestamp of our renewal and the number of times we have renewed our
 * registration within the [PERIODIC_INTERVAL_MILLISECONDS] interval of time.
 */
internal class VerificationDelegate(
    context: Context,
    private val disableRateLimit: Boolean = false
) : SharedPreferencesCache<VerificationState>(context) {
    override val logger: Logger = Logger(VerificationDelegate::class.java.simpleName)
    override val cacheKey: String = PREF_LAST_VERIFIED
    override val cacheName: String = PREFERENCE_NAME

    override fun VerificationState.toJSON() =
        JSONObject().apply {
            put(KEY_TIMESTAMP, timestamp)
            put(KEY_TOTAL_COUNT, totalCount)
        }

    override fun fromJSON(obj: JSONObject) =
        VerificationState(
            obj.getLong(KEY_TIMESTAMP),
            obj.getInt(KEY_TOTAL_COUNT)
        )

    @VisibleForTesting
    internal var innerCount: Int = 0

    @VisibleForTesting
    internal var innerTimestamp: Long = System.currentTimeMillis()

    init {
        getCached()?.let { cache ->
            innerTimestamp = cache.timestamp
            innerCount = cache.totalCount
        }
    }

    /**
     * Checks whether we're within our rate limiting constraints.
     */
    fun allowedToRenew(): Boolean {
        logger.info("Allowed to renew?")

        if (disableRateLimit) {
            logger.info("Rate limit override is enabled - allowed to renew!")
            return true
        }

        // within time frame
        val currentTime = System.currentTimeMillis()
        if ((currentTime - innerTimestamp) >= PERIODIC_INTERVAL_MILLISECONDS) {
            logger.info("Resetting. currentTime($currentTime) - $innerTimestamp < $PERIODIC_INTERVAL_MILLISECONDS")
            reset()
        } else {
            logger.info("No need to reset inner timestamp and count.")
        }

        // within interval counter
        if (innerCount > MAX_REQUEST_IN_INTERVAL) {
            logger.info("Not allowed: innerCount($innerCount) > $MAX_REQUEST_IN_INTERVAL")
            return false
        }

        logger.info("Allowed to renew!")
        return true
    }

    /**
     * Should be called whenever a successful invocation has taken place and we want to record it.
     */
    fun increment() {
        logger.info("Incrementing verification state.")
        val count = innerCount + 1

        setToCache(VerificationState(innerTimestamp, count))

        innerCount = count
    }

    private fun reset() {
        logger.info("Resetting verification state.")
        val timestamp = System.currentTimeMillis()
        innerCount = 0
        innerTimestamp = timestamp

        setToCache(VerificationState(timestamp, 0))
    }

    companion object {
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_TOTAL_COUNT = "totalCount"

        internal const val PERIODIC_INTERVAL_MILLISECONDS = 24 * 60 * 60 * 1000L // 24 hours
        internal const val MAX_REQUEST_IN_INTERVAL = 500 // 500 requests in 24 hours
    }
}

internal data class VerificationState(val timestamp: Long, val totalCount: Int)

@VisibleForTesting
internal fun preference(context: Context) = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

internal fun AutoPushSubscription.into() = DevicePushSubscription(
    endpoint = this.endpoint,
    publicKey = this.publicKey,
    authKey = this.authKey
)
