/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.p2p.internal.P2PInteractor
import mozilla.components.feature.p2p.internal.P2PPresenter
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature

/**
 * Feature implementation that will keep a [P2PView] in sync with a bound [SessionState].
 */
class P2PFeature(
    store: BrowserStore,
    view: P2PView,
    engineView: EngineView,
    override var onNeedToRequestPermissions: OnNeedToRequestPermissions = { }
) : LifecycleAwareFeature, BackHandler, PermissionsFeature {
    @VisibleForTesting internal var presenter = P2PPresenter(store, view)
    @VisibleForTesting internal var interactor = P2PInteractor(this, view, engineView)

    var onClose: (() -> Unit)? = null
    private var session: SessionState? = null

    //LifeCycleAwareFeature

    override fun start() {
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

    // PermissionsFeature
    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
