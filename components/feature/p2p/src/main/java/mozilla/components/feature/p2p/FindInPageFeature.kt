/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.findinpage.internal.P2PInteractor
import mozilla.components.feature.findinpage.internal.P2PPresenter
import mozilla.components.feature.findinpage.view.P2PView
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Feature implementation that will keep a [P2PView] in sync with a bound [SessionState].
 */
class P2PFeature(
    store: BrowserStore,
    view: P2PView,
    engineView: EngineView,
    private val onClose: (() -> Unit)? = null
) : LifecycleAwareFeature, BackHandler {
    @VisibleForTesting internal var presenter = P2PPresenter(store, view)
    @VisibleForTesting internal var interactor = P2PInteractor(this, view, engineView)

    private var session: SessionState? = null

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
