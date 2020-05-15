/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.thumbnails

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.state.state.ContentState
import mozilla.components.concept.engine.EngineView
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.android.content.isOSOnLowMemory
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Feature implementation for automatically taking thumbnails of sites.
 * The feature will take a screenshot when the page finishes loading,
 * and will add it to the [ContentState.thumbnail] property.
 *
 * If the OS is under low memory conditions, the screenshot will be not taken.
 * Ideally, this should be used in conjunction with `SessionManager.onLowMemory` to allow
 * free up some [ContentState.thumbnail] from memory.
 */
class BrowserThumbnails(
    private val context: Context,
    private val engineView: EngineView,
    private val store: BrowserStore
) : LifecycleAwareFeature {

    private var scope: CoroutineScope? = null

    /**
     * Starts observing the selected session to listen for when a session finishes loading.
     */
    override fun start() {
        scope = store.flowScoped { flow ->
            flow.map { it.selectedTab }
                .ifChanged { it?.content?.loading }
                .collect { state ->
                    if (state?.content?.loading == false) {
                        requestScreenshot()
                    }
                }
        }
    }

    @VisibleForTesting
    internal fun requestScreenshot() {
        if (!isLowOnMemory()) {
            engineView.captureThumbnail {
                val bitmap = it ?: return@captureThumbnail
                val tabId = store.state.selectedTabId ?: return@captureThumbnail

                store.dispatch(ContentAction.UpdateThumbnailAction(tabId, bitmap))
            }
        }
    }

    /**
     * Stops observing the selected session.
     */
    override fun stop() {
        scope?.cancel()
    }

    @VisibleForTesting
    internal var testLowMemory = false

    private fun isLowOnMemory() = testLowMemory || context.isOSOnLowMemory()
}
