/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr

import android.Manifest.permission.CAMERA
import android.content.Context
import android.support.v4.app.FragmentManager
import mozilla.components.support.ktx.android.content.isPermissionGranted

typealias OnScanResult = (result: String) -> Unit
typealias OnNeedToRequestPermissions = (permissions: Array<String>) -> Unit

class QrFeature(
    private val applicationContext: Context,
    var onNeedToRequestPermissions: OnNeedToRequestPermissions = { },
    var onScanResult: OnScanResult = { },
    private val fragmentManager: FragmentManager,
    private val fragment: QrFragment = QrFragment.newInstance()
) {
    fun scan(container: Int): Boolean {
        val listener: QrFragment.OnScanCompleteListener = object: QrFragment.OnScanCompleteListener {
            override fun onScanComplete(result: String) {
                onScanResult(result)
            }
        }

        fragment.setListener(listener)

        return if (applicationContext.isPermissionGranted(CAMERA)) {
            fragmentManager.beginTransaction()
                    .replace(container, fragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss()
            true
        } else {
            onNeedToRequestPermissions(arrayOf(CAMERA))
            false
        }
    }

    fun onPermissionsResult(container: Int) {
        if (applicationContext.isPermissionGranted(CAMERA)) {
            this.scan(container)
        }
    }
}
