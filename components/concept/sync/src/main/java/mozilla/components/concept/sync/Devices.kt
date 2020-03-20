/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
@file:SuppressWarnings("TooManyFunctions")
package mozilla.components.concept.sync

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Deferred
import mozilla.components.support.base.observer.Observable

/**
 * Describes available interactions with the current device and other devices associated with an [OAuthAccount].
 */
interface DeviceConstellation : Observable<AccountEventsObserver> {
    /**
     * Register current device in the associated [DeviceConstellation].
     *
     * @param name An initial name for the current device. This may be changed via [setDeviceNameAsync].
     * @param type Type of the current device. This can't be changed.
     * @param capabilities A list of capabilities that the current device claims to have.
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun initDeviceAsync(
        name: String,
        type: DeviceType = DeviceType.MOBILE,
        capabilities: Set<DeviceCapability>
    ): Deferred<Boolean>

    /**
     * Ensure that all passed in [capabilities] are configured.
     * This may involve backend service registration, or other work involving network/disc access.
     * @param capabilities A list of capabilities to configure. This is expected to be the same or
     * longer list than what was passed into [initDeviceAsync]. Removing capabilities is currently
     * not supported.
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun ensureCapabilitiesAsync(capabilities: Set<DeviceCapability>): Deferred<Boolean>

    /**
     * Current state of the constellation. May be missing if state was never queried.
     * @return [ConstellationState] describes current and other known devices in the constellation.
     */
    fun state(): ConstellationState?

    /**
     * Allows monitoring state of the device constellation via [DeviceConstellationObserver].
     * Use this to be notified of changes to the current device or other devices.
     */
    fun registerDeviceObserver(observer: DeviceConstellationObserver, owner: LifecycleOwner, autoPause: Boolean)

    /**
     * Set name of the current device.
     * @param name New device name.
     * @param context An application context, used for updating internal caches.
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun setDeviceNameAsync(name: String, context: Context): Deferred<Boolean>

    /**
     * Set a [DevicePushSubscription] for the current device.
     * @param subscription A new [DevicePushSubscription].
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun setDevicePushSubscriptionAsync(subscription: DevicePushSubscription): Deferred<Boolean>

    /**
     * Send a command to a specified device.
     * @param targetDeviceId A device ID of the recipient.
     * @param outgoingCommand An event to send.
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun sendCommandToDeviceAsync(targetDeviceId: String, outgoingCommand: DeviceCommandOutgoing): Deferred<Boolean>

    /**
     * Process a raw event, obtained via a push message or some other out-of-band mechanism.
     * @param payload A raw, plaintext payload to be processed.
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun processRawEventAsync(payload: String): Deferred<Boolean>

    /**
     * Refreshes [ConstellationState]. Registered [DeviceConstellationObserver] observers will be notified.
     *
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun refreshDevicesAsync(): Deferred<Boolean>

    /**
     * Polls for any pending [DeviceCommandIncoming] commands.
     * In case of new commands, registered [AccountEventsObserver] observers will be notified.
     *
     * @return A [Deferred] that will be resolved with a success flag once operation is complete.
     */
    fun pollForCommandsAsync(): Deferred<Boolean>
}

/**
 * Describes current device and other devices in the constellation.
 */
data class ConstellationState(val currentDevice: Device?, val otherDevices: List<Device>)

/**
 * Allows monitoring constellation state.
 */
interface DeviceConstellationObserver {
    fun onDevicesUpdate(constellation: ConstellationState)
}

/**
 * Describes a type of the physical device in the constellation.
 */
enum class DeviceType {
    DESKTOP,
    MOBILE,
    TABLET,
    TV,
    VR,
    UNKNOWN
}

/**
 * Describes an Autopush-compatible push channel subscription.
 */
data class DevicePushSubscription(
    val endpoint: String,
    val publicKey: String,
    val authKey: String
)

/**
 * Capabilities that a [Device] may have.
 */
enum class DeviceCapability {
    SEND_TAB
}

/**
 * Describes a device in the [DeviceConstellation].
 */
data class Device(
    val id: String,
    val displayName: String,
    val deviceType: DeviceType,
    val isCurrentDevice: Boolean,
    val lastAccessTime: Long?,
    val capabilities: List<DeviceCapability>,
    val subscriptionExpired: Boolean,
    val subscription: DevicePushSubscription?
)
