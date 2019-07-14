/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.accounts

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.service.fxa.manager.FxaAccountManager
import kotlin.coroutines.CoroutineContext

/**
 * Ties together an account manager with a session manager/tabs implementation, facilitating an
 * authentication flow.
 */
class FirefoxAccountsAuthFeature(
    private val accountManager: FxaAccountManager,
    private val tabsUseCases: TabsUseCases,
    private val redirectUrl: String,
    private val coroutineContext: CoroutineContext = Dispatchers.Main
) {
    fun beginAuthentication() {
        beginAuthenticationAsync {
            accountManager.beginAuthenticationAsync().await()
        }
    }

    fun beginPairingAuthentication(pairingUrl: String) {
        beginAuthenticationAsync {
            accountManager.beginAuthenticationAsync(pairingUrl).await()
        }
    }

    private fun beginAuthenticationAsync(beginAuthentication: suspend () -> String?) {
        CoroutineScope(coroutineContext).launch {
            // FIXME return a fallback URL provided by Config...
            // https://github.com/mozilla-mobile/android-components/issues/2496
            val authUrl = beginAuthentication() ?: "https://accounts.firefox.com/signin"

            // TODO
            // We may fail to obtain an authentication URL, for example due to transient network errors.
            // If that happens, open up a fallback URL in order to present some kind of a "no network"
            // UI to the user.
            // It's possible that the underlying problem will go away by the time the tab actually
            // loads, resulting in a confusing experience.
            tabsUseCases.addTab.invoke(authUrl)
        }
    }

    val interceptor = object : RequestInterceptor {
        override fun onLoadRequest(session: EngineSession, uri: String): RequestInterceptor.InterceptionResponse? {
            if (uri.startsWith(redirectUrl)) {
                val parsedUri = Uri.parse(uri)
                val code = parsedUri.getQueryParameter("code")

                if (code != null) {
                    val state = parsedUri.getQueryParameter("state") as String

                    // Notify the state machine about our success.
                    accountManager.finishAuthenticationAsync(code, state)

                    return RequestInterceptor.InterceptionResponse.Url(redirectUrl)
                }
            }

            return null
        }
    }
}
