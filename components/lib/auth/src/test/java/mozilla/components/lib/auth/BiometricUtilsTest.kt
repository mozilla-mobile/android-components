/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.auth

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class BiometricUtilsTest {

    @Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
    @Test
    fun `canUseFeature checks for SDK compatible`() {
        Assert.assertFalse(testContext.canUseBiometricFeature())
    }
}
