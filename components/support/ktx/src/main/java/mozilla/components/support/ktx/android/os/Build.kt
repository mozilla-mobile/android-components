/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.support.ktx.android.os

import android.os.Build.SUPPORTED_ABIS
import android.system.Os
import mozilla.components.support.base.log.logger.Logger

/**
 * Originally taken from [HardwareUtils#isX86System][0].
 *
 * [0]: https://dxr.mozilla.org/mozilla-central/source/mobile/android/geckoview/src/main/java/org/mozilla/gecko/util/HardwareUtils.java
 */
object Build {
    fun isX86System(): Boolean {
        if ("x86" == SUPPORTED_ABIS[0]) {
            return true
        }
        // On some devices we have to look into the kernel release string.
        try {
            return Os.uname().release.contains("-x86_")
        } catch (e: Exception) {
            Logger.warn("Cannot get uname", e)
        }
        return false
    }
}
