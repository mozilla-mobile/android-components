package org.mozilla.samples.browser.integration

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.view.View
import androidx.core.app.ActivityCompat

import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.p2p.P2PFeature
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature

class P2PIntegration(
    private val store: BrowserStore,
    private val view: P2PView,
    engineView: EngineView,
    private val feature: P2PFeature
) : LifecycleAwareFeature, BackHandler {

    override fun start() {
        feature.start()
        feature.onClose = ::onClose
        launch = this::launch
    }

    override fun stop() {
        feature.stop()
        launch = null
    }

    override fun onBackPressed(): Boolean {
        return feature.onBackPressed()
    }

    private fun onClose() {
        view.asView().visibility = View.GONE
    }

    private fun launch() {
        val session = store.state.selectedTab ?: return

        view.asView().visibility = View.VISIBLE
        feature.bind(session)
    }

    companion object {
        var launch: (() -> Unit)? = null
            private set
    }
}
