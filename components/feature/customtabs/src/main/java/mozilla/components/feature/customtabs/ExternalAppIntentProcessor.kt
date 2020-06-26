/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.customtabs

import android.app.Activity
import android.content.Intent
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.intent.ext.putSessionId
import mozilla.components.feature.intent.processing.IntentProcessor
import mozilla.components.support.utils.SafeIntent

/**
 * Base class for intent processors used to handle intents from external apps,
 * such as custom tabs, PWAs, and Trusted Web Activities.
 */
abstract class ExternalAppIntentProcessor(
    protected val activity: Activity,
    protected val sessionManager: SessionManager
) : IntentProcessor {

    /**
     * Returns true if this intent should be handled by this processor.
     */
    protected abstract fun matches(intent: SafeIntent): Boolean

    /**
     * Processes the given [intent] by building a browsing session.
     */
    protected abstract fun process(intent: SafeIntent, url: String): Session?

    /**
     * Returns an existing session that matches this activity task ID.
     */
    protected fun existingSession() =
        sessionManager.all.find { it.customTabConfig?.taskId == activity.taskId }

    final override fun process(intent: Intent): Boolean {
        val safeIntent = SafeIntent(intent)
        val url = safeIntent.dataString

        if (!url.isNullOrEmpty() && matches(safeIntent)) {
            process(safeIntent, url)?.let { session ->
                intent.putSessionId(session.id)
                return true
            }
        }

        return false
    }
}
