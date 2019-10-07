/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.internal

import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.p2p.P2PFeature
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.support.ktx.android.view.hideKeyboard

/**
 * Interactor that implements [P2PView.Listener] and notifies the engine or feature about actions the user
 * performed (e.g. "find next result").
 */
internal class P2PInteractor(
    private val feature: P2PFeature,
    private val view: P2PView,
    private val engineView: EngineView?
) : P2PView.Listener {
    private var engineSession: EngineSession? = null

    fun start() {
        view.listener = this
    }

    fun stop() {
        view.listener = null
    }

    fun bind(session: SessionState) {
        engineSession = session.engineState.engineSession
    }

    override fun onAdvertise() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDiscover() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClose() {
        // We pass this event up to the feature. The feature is responsible for unbinding its sub components and
        // potentially notifying other dependencies.
        feature.unbind()
    }

    fun unbind() {
        engineSession?.clearFindMatches()
        engineSession = null
    }
}
