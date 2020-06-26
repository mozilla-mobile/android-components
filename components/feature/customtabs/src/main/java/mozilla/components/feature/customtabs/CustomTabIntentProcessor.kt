/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.customtabs

import android.app.Activity
import android.content.Intent.ACTION_VIEW
import android.provider.Browser
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.utils.SafeIntent

/**
 * Processor for intents which trigger actions related to custom tabs.
 */
class CustomTabIntentProcessor(
    activity: Activity,
    sessionManager: SessionManager,
    private val loadUrlUseCase: SessionUseCases.DefaultLoadUrlUseCase,
    private val isPrivate: Boolean = false
) : ExternalAppIntentProcessor(activity, sessionManager) {

    override fun matches(intent: SafeIntent): Boolean {
        return intent.action == ACTION_VIEW && isCustomTabIntent(intent)
    }

    @VisibleForTesting
    internal fun getAdditionalHeaders(intent: SafeIntent): Map<String, String>? {
        val pairs = intent.getBundleExtra(Browser.EXTRA_HEADERS)
        return pairs?.keySet()?.map { key ->
            val header = pairs.getString(key)
            if (header != null) {
                key to header
            } else {
                throw IllegalArgumentException("getAdditionalHeaders() intent bundle contains wrong key value pair")
            }
        }?.toMap()
    }

    override fun process(intent: SafeIntent, url: String): Session {
        return existingSession()?.also {
            it.customTabConfig = createCustomTabConfigFromIntent(intent, activity)
        } ?: Session(url, private = isPrivate, source = Session.Source.CUSTOM_TAB).also {
            it.customTabConfig = createCustomTabConfigFromIntent(intent, activity)
            sessionManager.add(it)

            loadUrlUseCase(url, it, EngineSession.LoadUrlFlags.external(), getAdditionalHeaders(intent))
        }
    }
}
