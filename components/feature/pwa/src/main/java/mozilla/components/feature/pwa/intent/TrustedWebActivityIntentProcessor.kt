/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa.intent

import android.app.Activity
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsService.RELATION_HANDLE_ALL_URLS
import androidx.browser.customtabs.CustomTabsSessionToken
import androidx.browser.trusted.TrustedWebActivityIntentBuilder.EXTRA_ADDITIONAL_TRUSTED_ORIGINS
import androidx.core.net.toUri
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.state.ExternalAppType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.customtabs.ExternalAppIntentProcessor
import mozilla.components.feature.customtabs.createCustomTabConfigFromIntent
import mozilla.components.feature.customtabs.feature.OriginVerifierFeature
import mozilla.components.feature.customtabs.isTrustedWebActivityIntent
import mozilla.components.feature.customtabs.store.CustomTabsServiceStore
import mozilla.components.feature.pwa.ext.toOrigin
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.service.digitalassetlinks.RelationChecker
import mozilla.components.support.utils.SafeIntent

/**
 * Processor for intents which open Trusted Web Activities.
 */
class TrustedWebActivityIntentProcessor(
    activity: Activity,
    sessionManager: SessionManager,
    private val loadUrlUseCase: SessionUseCases.DefaultLoadUrlUseCase,
    packageManager: PackageManager,
    relationChecker: RelationChecker,
    private val store: CustomTabsServiceStore
) : ExternalAppIntentProcessor(activity, sessionManager) {

    private val verifier = OriginVerifierFeature(packageManager, relationChecker) { store.dispatch(it) }
    private val scope = MainScope()

    override fun matches(intent: SafeIntent) =
        intent.action == ACTION_VIEW && isTrustedWebActivityIntent(intent)

    override fun process(intent: SafeIntent, url: String): Session {
        val customTabConfig = createCustomTabConfigFromIntent(intent, activity)
            .copy(externalAppType = ExternalAppType.TRUSTED_WEB_ACTIVITY)

        val session = existingSession()?.also {
            it.customTabConfig = customTabConfig
        } ?: Session(url, private = false, source = Session.Source.HOME_SCREEN).also {
            it.customTabConfig = customTabConfig

            sessionManager.add(it)
            loadUrlUseCase(url, it, EngineSession.LoadUrlFlags.external())
        }

        customTabConfig.sessionToken?.let { token ->
            val origin = listOfNotNull(intent.data?.toOrigin())
            val additionalOrigins = intent
                .getStringArrayListExtra(EXTRA_ADDITIONAL_TRUSTED_ORIGINS)
                .orEmpty()
                .mapNotNull { it.toUri().toOrigin() }

            // Launch verification separately so the intent processing isn't held up
            scope.launch {
                verify(token, origin + additionalOrigins)
            }
        }

        return session
    }

    private suspend fun verify(token: CustomTabsSessionToken, origins: List<Uri>) {
        val tabState = store.state.tabs[token] ?: return
        origins.map { origin ->
            scope.async {
                verifier.verify(tabState, token, RELATION_HANDLE_ALL_URLS, origin)
            }
        }.awaitAll()
    }
}
