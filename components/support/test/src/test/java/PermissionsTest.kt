/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.test.robolectric

import android.Manifest.permission.INTERNET
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.ktx.android.content.isPermissionGranted
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionsTest {

    @Test
    fun `after call grantPermission this permission must be granted `() {
        var isGranted = testContext.isPermissionGranted(INTERNET)
        assertFalse(isGranted)

        grantPermission(INTERNET)
        isGranted = testContext.isPermissionGranted(INTERNET)

        assertTrue(isGranted)
    }
}
