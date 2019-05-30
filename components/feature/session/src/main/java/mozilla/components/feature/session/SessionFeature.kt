/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.findSessionByIdOrSelected
import mozilla.components.concept.engine.EngineView
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Feature implementation for connecting the engine module with the session module.
 *
 * @param sessionManager Session Manager to pass back press events to.
 * @param goBackUseCase Use Case to navigate back in the history.
 * @param engineView Reference to the engine view.
 * @param sessionId ID of specific session to observe.
 */
class SessionFeature(
    private val sessionManager: SessionManager,
    private val goBackUseCase: SessionUseCases.GoBackUseCase,
    engineView: EngineView,
    private val sessionId: String? = null
) : LifecycleAwareFeature, BackHandler {
    /**
     * @deprecated Pass goBackUseCase directly
     */
    constructor(
        sessionManager: SessionManager,
        sessionUseCases: SessionUseCases,
        engineView: EngineView,
        sessionId: String? = null
    ) : this(sessionManager, sessionUseCases.goBack, engineView, sessionId)

    internal val presenter = EngineViewPresenter(sessionManager, engineView, sessionId)

    /**
     * Start feature: App is in the foreground.
     */
    override fun start() {
        presenter.start()
    }

    /**
     * Handler for back pressed events in activities that use this feature.
     *
     * @return true if the event was handled, otherwise false.
     */
    override fun onBackPressed(): Boolean {
        val session = sessionManager.findSessionByIdOrSelected(sessionId)

        if (session?.canGoBack == true) {
            goBackUseCase.invoke(session)
            return true
        }

        return false
    }

    /**
     * Stop feature: App is in the background.
     */
    override fun stop() {
        presenter.stop()
    }
}
