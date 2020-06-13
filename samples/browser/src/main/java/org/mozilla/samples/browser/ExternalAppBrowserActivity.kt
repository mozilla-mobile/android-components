/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import androidx.fragment.app.Fragment
import mozilla.components.feature.pwa.ext.getWebAppManifest
import org.mozilla.samples.browser.ext.components

/**
 * Activity that holds the [BrowserFragment] that is launched within an external app,
 * such as custom tabs and progressive web apps.
 */
class ExternalAppBrowserActivity : BrowserActivity() {

    override fun createBrowserFragment(sessionId: String?): Fragment {
        return if (sessionId != null) {
            val manifest = intent.getWebAppManifest()

            ensureSessionIsReleased(sessionId)

            ExternalAppBrowserFragment.create(
                sessionId,
                manifest = manifest
            )
        } else {
            // Fall back to browser fragment
            super.createBrowserFragment(sessionId)
        }
    }

    private fun ensureSessionIsReleased(sessionId: String) {
        val session = components.sessionManager.findSessionById(sessionId) ?: return
        val engineSession = components.sessionManager.getOrCreateEngineSession(session)

        engineSession.releaseFromView()
    }
}
