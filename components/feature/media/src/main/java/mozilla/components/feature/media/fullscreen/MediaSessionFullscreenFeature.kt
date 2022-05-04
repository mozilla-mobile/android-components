/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.media.fullscreen

import android.app.Activity
import android.content.pm.ActivityInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

/**
 * Feature that will auto-rotate the device to the correct orientation for the media aspect ratio.
 */
class MediaSessionFullscreenFeature(
    private val activity: Activity,
    private val store: BrowserStore
) : LifecycleAwareFeature {

    private var scope: CoroutineScope? = null

    override fun start() {
        scope = store.flowScoped { flow ->
            flow.map {
                it.tabs + it.customTabs
            }.map { tab ->
                tab.filter { it.mediaSessionState != null && it.mediaSessionState!!.fullscreen }
            }.ifChanged().collect { states ->
                processFullscreen(states)
            }
        }
    }

    @Suppress("SourceLockedOrientationActivity") // We deliberately want to lock the orientation here.
    private fun processFullscreen(sessionStates: List<SessionState>) {
        /* there should only be one fullscreen session */
        val activeState = sessionStates.firstOrNull()
        if (activeState == null || activeState.mediaSessionState?.fullscreen != true) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            return
        }

        when (activeState.mediaSessionState?.elementMetadata?.portrait) {
            true ->
                activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            false ->
                activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    override fun stop() {
        scope?.cancel()
    }
}
