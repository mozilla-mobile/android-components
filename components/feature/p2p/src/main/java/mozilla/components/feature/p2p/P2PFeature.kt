/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.p2p.internal.P2PController
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.feature.tabs.TabsUseCases

/**
 * Feature implementation for peer-to-peer communication between browsers.
 */
class P2PFeature(
    val view: P2PView,
    private val store: BrowserStore,
    private val useCases: TabsUseCases,
    override val onNeedToRequestPermissions: OnNeedToRequestPermissions,
    private val onClose: (() -> Unit)
) : LifecycleAwareFeature, BackHandler, PermissionsFeature {
    @VisibleForTesting internal var controller = P2PController(store, view, useCases)

    private var session: SessionState? = null

    // LifeCycleAwareFeature implementation

    override fun start() {
        requestNeededPermissions()
    }

    override fun stop() {
        controller.stop()
    }

    // PermissionsFeature implementation

    private var ungrantedPermissions = NearbyConnection.PERMISSIONS.filter {
        ContextCompat.checkSelfPermission(view.asView().context, it) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ungrantedPermissions.isEmpty()) {
                onPermissionsGranted()
            } else {
                onNeedToRequestPermissions(ungrantedPermissions.toTypedArray())
            }
        } else {
            Logger.error("Cannot continue on pre-Marshmallow device")
        }
    }

    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        // Sometimes ungrantedPermissions still shows a recently accepted permission as being not granted,
        // so we need to check grantResults instead.
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onPermissionsGranted()
        } else {
            Logger.error("Cannot continue due to missing permissions $ungrantedPermissions")
        }
    }

    private fun onPermissionsGranted() {
        controller.start()
    }

    // BackHandler implementation
    override fun onBackPressed(): Boolean {
        // Nothing, for now
        return true
    }
}
