/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.findinpage

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.findinpage.internal.FindInPageInteractor
import mozilla.components.feature.findinpage.internal.FindInPagePresenter
import mozilla.components.feature.findinpage.view.FindInPageView
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Feature implementation that will keep a [FindInPageView] in sync with a bound [Session].
 */
class FindInPageFeature(
    sessionManager: SessionManager,
    view: FindInPageView,
    engineView: EngineView,
    private val onClose: (() -> Unit)? = null
) : LifecycleAwareFeature, BackHandler {
    @VisibleForTesting internal var presenter = FindInPagePresenter(view)
    @VisibleForTesting internal var interactor = FindInPageInteractor(this, sessionManager, view, engineView)

    private var session: Session? = null

    override fun start() {
        presenter.start()
        interactor.start()
    }

    override fun stop() {
        presenter.stop()
        interactor.stop()
    }

    /**
     * Binds this feature to the given [Session]. Until unbound the [FindInPageView] will be updated presenting the
     * current "Find in Page" state.
     */
    fun bind(session: Session) {
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
     * Unbinds the feature from a previously bound [Session]. The [FindInPageView] will be cleared and not be updated
     * to present the "Find in Page" state anymore.
     */
    fun unbind() {
        session = null
        presenter.unbind()
        interactor.unbind()
        onClose?.invoke()
    }
}
