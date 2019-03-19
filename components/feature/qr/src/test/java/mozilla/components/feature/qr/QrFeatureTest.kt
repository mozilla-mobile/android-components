/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr

import android.Manifest.permission.CAMERA
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import org.junit.Assert.assertTrue
import mozilla.components.support.test.robolectric.grantPermission
import mozilla.components.support.test.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class QrFeatureTest {

    private lateinit var feature: QrFeature

    @Test
    fun `it can initialize and scan`() {
        val context = RuntimeEnvironment.application

        feature = QrFeature(context,
                fragmentManager = mockFragmentManager(),
                onNeedToRequestPermissions = {
                },
                onScanResult = { result ->
                    var k = 1
                })

        feature.scan(1)
    }

    @Test
    fun `when a camera permission is not granted needToRequestPermissions and after onPermissionsGranted the download must be triggered`() {
        var needToRequestPermissionCalled = false

        feature.onNeedToRequestPermissions = { needToRequestPermissionCalled = true }

        feature.scan(1)

        assertTrue(needToRequestPermissionCalled)

        grantPermissions()

        feature.onPermissionsResult(1)
    }


    private fun grantPermissions() {
        grantPermission(CAMERA)
    }

    private fun mockFragmentManager(): FragmentManager {
        val fragmentManager: FragmentManager = mock()

        val transaction: FragmentTransaction = mock()
        Mockito.doReturn(transaction).`when`(fragmentManager).beginTransaction()

        return fragmentManager
    }
}