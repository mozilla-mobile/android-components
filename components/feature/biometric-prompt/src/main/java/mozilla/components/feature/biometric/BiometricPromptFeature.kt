/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.biometric

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger

/**
 * A [LifecycleAwareFeature] for the Android Biometric API to prompt for user authentication.
 *
 * @param context Android context.
 * @param fragment The fragment on which this feature will live.
 * @param authenticationCallbacks Callbacks for BiometricPrompt.
 */

class BiometricPromptFeature(
    private val context: Context,
    private val fragment: Fragment,
    private val authenticationCallbacks: AuthenticationCallbacks
) : LifecycleAwareFeature {
    private val logger = Logger(javaClass.simpleName)

    @VisibleForTesting
    internal var biometricPrompt: BiometricPrompt? = null

    override fun start() {
        val executor = ContextCompat.getMainExecutor(context)
        biometricPrompt = BiometricPrompt(fragment, executor, PromptCallback())
    }

    override fun stop() {
        biometricPrompt = null
    }

    /**
     * Requests the user for biometric authentication.
     *
     * @param title Adds a title for the authentication prompt.
     * @param subtitle Adds a subtitle for the authentication prompt.
     */
    fun requestAuthentication(
        title: String,
        subtitle: String = ""
    ) {
        val promptInfo: BiometricPrompt.PromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
        biometricPrompt?.authenticate(promptInfo)
    }

    internal inner class PromptCallback : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            logger.error("onAuthenticationError: errorMessage $errString errorCode=$errorCode")
            authenticationCallbacks.onAuthError.invoke(errString.toString())
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            logger.debug("onAuthenticationSucceeded")
            authenticationCallbacks.onAuthSuccess.invoke()
        }

        override fun onAuthenticationFailed() {
            logger.error("onAuthenticationFailed")
            authenticationCallbacks.onAuthFailure.invoke()
        }
    }

    internal fun getAndroidBiometricManager(context: Context): BiometricManager {
        return BiometricManager.from(context)
    }

    /**
     * Checks if the appropriate SDK version and hardware capabilities are met to use the feature.
     */
    fun canUseFeature(context: Context): Boolean {
        return if (SDK_INT >= M) {
            val manager = getAndroidBiometricManager(context)
            isHardwareAvailable(manager) && isEnrolled(manager)
        } else {
            false
        }
    }

    /**
     * Checks if the hardware requirements are met for using the [BiometricManager].
     */
    fun isHardwareAvailable(biometricManager: BiometricManager): Boolean {
        val status = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        return status != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
            status != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    /**
     * Checks if the user can use the [BiometricManager] and is therefore enrolled.
     */
    fun isEnrolled(biometricManager: BiometricManager): Boolean {
        val status = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }
}
