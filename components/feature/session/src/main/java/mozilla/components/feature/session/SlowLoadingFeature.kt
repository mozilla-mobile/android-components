/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.mapNotNull
import mozilla.components.browser.state.selector.findTabOrCustomTabOrSelectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.script.SlowScriptRequest
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Feature implementation that provides a notification when the
 * slowScript callback is called and enables the user to
 * continue or stop running the script.
 *
 * This is where we'd observe state.content.slowLoadingScriptCallback.
 * When it's null -> everything is okay, nothing to do
 * When it's not null -> post a notification for the user to choose next actions:
 *   - slowLoadingScriptCallback.allowSlowLoadingScript()
 *   - slowLoadingScriptCallback.stopSlowLoadingScript()
 *
 * @param sessionId ID of the session running the slow script.
 * @param store Reference to the application's [BrowserStore].
 * @param showNotification Function having [SlowScriptRequest] as parameter used to show
 * the notification with the controls for the callback.
 * @param dismissNotification Function used to dismiss the notification.
 */
class SlowLoadingFeature(
    private val sessionId: String?,
    private val store: BrowserStore,
    private val showNotification: (SlowScriptRequest) -> Unit,
    private val dismissNotification: () -> Unit,
) : LifecycleAwareFeature {

    private val logger = Logger("SlowScript")

    private var scope: CoroutineScope? = null

    override fun start() {
        logger.debug("SlowScriptFeature started.")
        scope = store.flowScoped { flow ->
            flow.mapNotNull { state -> state.findTabOrCustomTabOrSelectedTab(sessionId) }
                .ifChanged { it.content.slowScriptRequest }
                .collect {
                    if (it.content.slowScriptRequest != null) {
                        showNotification(it.content.slowScriptRequest!!)
                    } else {
                        dismissNotification()
                    }
                    logger.debug("SlowScript notification consumed.")
                }
        }
    }

    override fun stop() {
        scope?.cancel()
        dismissNotification()
        logger.debug("SlowScriptFeature stopped.")
    }
}
