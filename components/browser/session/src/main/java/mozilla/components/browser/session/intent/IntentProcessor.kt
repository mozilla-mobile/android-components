/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.intent

import android.content.Intent

/**
 * Processor for Android intents which should trigger session-related actions.
 */
interface IntentProcessor {

    /**
     * Returns true if this intent processor will handle the intent.
     */
    fun matches(intent: Intent): Boolean

    /**
     * Processes the given [Intent].
     *
     * @param intent The intent to process.
     * @return True if the intent was processed, otherwise false.
     */
    suspend fun process(intent: Intent): Boolean
}
