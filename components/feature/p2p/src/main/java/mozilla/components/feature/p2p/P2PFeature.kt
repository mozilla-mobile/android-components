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
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.p2p.internal.P2PInteractor
import mozilla.components.feature.p2p.internal.P2PPresenter
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.log.logger.Logger
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Feature implementation that will keep a [P2PView] in sync with a bound [SessionState].
 */
class P2PFeature(
    store: BrowserStore,
    val view: P2PView,
    //engineView: EngineView,
    override var onNeedToRequestPermissions: OnNeedToRequestPermissions = { }
) : LifecycleAwareFeature, BackHandler, PermissionsFeature {
    @VisibleForTesting internal var presenter = P2PPresenter(store, view)
    @VisibleForTesting internal var interactor = P2PInteractor(this, view /*,engineView*/)

    var onClose: (() -> Unit)? = null
    private var session: SessionState? = null

    //LifeCycleAwareFeature

    override fun start() {
        requestNeededPermissions()
    }

    private val ungrantedPermissions = NearbyConnection.PERMISSIONS.filter {
        ContextCompat.checkSelfPermission(view.asView().context, it) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestNeededPermissions() {
        try {
            throw Exception("for debugging purposes")
        } catch (e: java.lang.Exception) {
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            Logger.error("Stack trace: ${sw.toString()}")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ungrantedPermissions.isEmpty()) {
                Logger.error("All permissions granted already, so no need to request.")
                onPermissionsGranted()
            } else {
                Logger.error("Desired permissions: $ungrantedPermissions")
                Logger.error("About to request permissions")
                onNeedToRequestPermissions(ungrantedPermissions.toTypedArray())
            }
        } else {
            Logger.error("Cannot continue on pre-Marshmallow device")
        }
    }

    // PermissionsFeature
    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        Logger.error("After requesting ${permissions.size} permissions: " +
            permissions.indices.map { "${permissions[it]}: ${grantResults[it]}"}
                .joinToString(", "))
        if (ungrantedPermissions.isEmpty()) {
            Logger.error("All permissions were granted")
            onPermissionsGranted()
        } else {
            Logger.error("Cannot proceed due to missing permissions")
        }
    }

    // This is called after all permissions have been granted.
    private fun onPermissionsGranted() {
        presenter.start()
        interactor.start()
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
        onClose?.invoke()
    }
}
