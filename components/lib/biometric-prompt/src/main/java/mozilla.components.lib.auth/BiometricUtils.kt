/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.auth

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager

class BiometricUtils {

    @VisibleForTesting
    internal fun getAndroidBiometricManager(context: Context): BiometricManager {
        return BiometricManager.from(context)
    }

    /**
     * Checks if the appropriate SDK version and hardware capabilities are met to use the feature.
     */
    fun canUseFeature(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = getAndroidBiometricManager(context)
            isHardwareAvailable(manager) && isEnrolled(manager)
        } else {
            false
        }
    }

    /**
     * Checks if the hardware requirements are met for using the [BiometricManager].
     */
    @VisibleForTesting
    internal fun isHardwareAvailable(biometricManager: BiometricManager): Boolean {
        val status =
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return status != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE &&
            status != BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
    }

    /**
     * Checks if the user can use the [BiometricManager] and is therefore enrolled.
     */
    @VisibleForTesting
    internal fun isEnrolled(biometricManager: BiometricManager): Boolean {
        val status =
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }
}
