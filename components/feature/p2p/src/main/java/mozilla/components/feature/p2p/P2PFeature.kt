/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.p2p.internal.P2PInteractor
import mozilla.components.feature.p2p.internal.P2PPresenter
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.log.logger.Logger

/**
 * Feature implementation that will keep a [P2PView] in sync with a bound [SessionState].
 */
class P2PFeature(
    store: BrowserStore,
    val view: P2PView,
    override val onNeedToRequestPermissions: OnNeedToRequestPermissions,
    private val onClose: (() -> Unit)
) : LifecycleAwareFeature, BackHandler, PermissionsFeature {
    @VisibleForTesting internal var presenter = P2PPresenter(store, view)
    @VisibleForTesting internal var interactor = P2PInteractor(this, view)

    private var session: SessionState? = null

    //LifeCycleAwareFeature

    override fun start() {
        requestNeededPermissions()
    }

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

    // PermissionsFeature
    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        // Sometimes ungrantedPermissions still shows a recently accepted permission as being not granted,
        // so we need to check grantResults instead.
        if (grantResults.all {it == PackageManager.PERMISSION_GRANTED}) {
            onPermissionsGranted()
        } else {
            Logger.error("Cannot continue due to missing permissions $ungrantedPermissions")
        }
    }

    // This is called after all permissions have been granted.
    private fun onPermissionsGranted() {
        interactor.start() // Must start before presenter because it sets the listener
        presenter.start()
    }

    override fun stop() {
        presenter.stop()
        interactor.stop()
    }


    /**
     * Binds this feature to the given [SessionState]. Until unbound the [P2PView] will be
     * updated presenting the current "Find in Page" state.
     */
    fun bind(session: SessionState) {
        this.session = session

        presenter.bind(session)
        interactor.bind(session)
    }

    // BackHandler
    /**
     * Returns true if the back button press was handled and the feature unbound from a session.
     */
    override fun onBackPressed(): Boolean {
        return if (session != null) {
            unbind()
            true
        } else {
            false
        }
    }

    /**
     * Unbinds the feature from a previously bound [SessionState]. The [P2PView] will be
     * cleared and not be updated to present the "Find in Page" state anymore.
     */
    fun unbind() {
        session = null
        presenter.unbind()
        interactor.unbind()
        onClose.invoke()
    }
}
