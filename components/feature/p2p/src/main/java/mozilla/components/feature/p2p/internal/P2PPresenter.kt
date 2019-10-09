/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Presenter that will observe [SessionState] changes and update the view whenever
 * a p2p result occurs.
 */
internal class P2PPresenter(
    private val store: BrowserStore,
    private val view: P2PView
) {
    @Volatile
    internal var session: SessionState? = null

    private var scope: CoroutineScope? = null

    fun start() {
        view.enable()
        /*
        scope = store.flowScoped { flow ->
            flow.mapNotNull { state -> session?.let { state.findTabOrCustomTab(it.id) } }
                .ifChanged { it.content.findResults }
                .collect {
                    val results = it.content.findResults
                    if (results.isNotEmpty()) {
                        view.displayResult(results.last())
                    }
                }
        }
         */
    }

    fun stop() {
        scope?.cancel()
    }

    fun bind(session: SessionState) {
        this.session = session
        view.focus()
    }

    fun unbind() {
        view.clear()
        this.session = null
    }
}
