/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.content.Context
import mozilla.components.service.glean.config.Configuration

/**
 * Enumeration of different metric lifetimes.
 */
enum class Lifetime {
    /**
    * The metric is reset with each sent ping
    */
    Ping,
    /**
    * The metric is reset on application restart
    */
    Application,
    /**
    * The metric is reset with each user profile
    */
    User
}

@Suppress("UNUSED_PARAMETER")
open class GleanInternalAPI internal constructor () {
    @Suppress("UNUSED_PARAMETER")
    fun initialize(
        applicationContext: Context,
        configuration: Configuration = Configuration()
    ) {
    }

    @Suppress("UNUSED_PARAMETER")
    fun setUploadEnabled(enabled: Boolean) {
    }

    fun getUploadEnabled(): Boolean {
        return false
    }

    @Suppress("UNUSED_PARAMETER")
    fun setExperimentActive(
        experimentId: String,
        branch: String,
        extra: Map<String, String>? = null
    ) {
    }

    @Suppress("UNUSED_PARAMETER")
    fun setExperimentInactive(experimentId: String) {
    }

    @Suppress("UNUSED_PARAMETER")
    fun handleEvent(pingEvent: Glean.PingEvent) {
    }
}

object Glean : GleanInternalAPI() {
    /**
    * Enumeration of different metric lifetimes.
    */
    enum class PingEvent {
        /**
        * When the application goes into the background
        */
        Background,
        /**
        * A periodic event to send the default ping
        */
        Default
    }
}
