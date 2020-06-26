/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa.intent

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.Session.Source
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.customtabs.ExternalAppIntentProcessor
import mozilla.components.feature.pwa.ManifestStorage
import mozilla.components.feature.pwa.ext.putWebAppManifest
import mozilla.components.feature.pwa.ext.toCustomTabConfig
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.utils.SafeIntent

/**
 * Processor for intents which trigger actions related to web apps.
 */
class WebAppIntentProcessor(
    activity: Activity,
    sessionManager: SessionManager,
    private val loadUrlUseCase: SessionUseCases.DefaultLoadUrlUseCase,
    private val storage: ManifestStorage
) : ExternalAppIntentProcessor(activity, sessionManager) {

    /**
     * Returns true if this intent should launch a progressive web app.
     */
    override fun matches(intent: SafeIntent) =
        intent.action == ACTION_VIEW_PWA

    /**
     * Processes the given [Intent] by creating a [Session] with a corresponding web app manifest.
     *
     * A custom tab config is also set so a custom tab toolbar can be shown when the user leaves
     * the scope defined in the manifest.
     */
    override fun process(intent: SafeIntent, url: String): Session? {
        val webAppManifest = runBlocking { storage.loadManifest(url) } ?: return null
        val session = existingSession()?.also {
            it.webAppManifest = webAppManifest
            it.customTabConfig = webAppManifest.toCustomTabConfig().copy(taskId = activity.taskId)
        } ?: Session(url, private = false, source = Source.HOME_SCREEN).also {
            it.webAppManifest = webAppManifest
            it.customTabConfig = webAppManifest.toCustomTabConfig().copy(taskId = activity.taskId)

            sessionManager.add(it)
            loadUrlUseCase(url, it, EngineSession.LoadUrlFlags.external())
        }

        intent.unsafe.flags = FLAG_ACTIVITY_NEW_DOCUMENT
        intent.unsafe.putWebAppManifest(webAppManifest)
        return session
    }

    companion object {
        const val ACTION_VIEW_PWA = "mozilla.components.feature.pwa.VIEW_PWA"
    }
}
