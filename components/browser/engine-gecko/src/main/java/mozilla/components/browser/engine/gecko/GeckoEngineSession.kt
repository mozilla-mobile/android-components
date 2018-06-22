/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.content.Context
import android.util.Log
import mozilla.components.concept.engine.EngineSession
import org.mozilla.geckoview.GeckoSession

/**
 * Gecko-based EngineSession implementation.
 */
class GeckoEngineSession(
    context: Context
) : EngineSession() {

    internal var geckoSession = GeckoSession()

    init {
        geckoSession.open(context)

        geckoSession.navigationDelegate = createNavigationDelegate()
        geckoSession.progressDelegate = createProgressDelegate()
    }

    /**
     * See [EngineSession.loadUrl]
     */
    override fun loadUrl(url: String) {
        geckoSession.loadUri(url)
    }

    /**
     * See [EngineSession.reload]
     */
    override fun reload() {
        geckoSession.reload()
    }

    /**
     * See [EngineSession.goBack]
     */
    override fun goBack() {
        geckoSession.goBack()
    }

    /**
     * See [EngineSession.goForward]
     */
    override fun goForward() {
        geckoSession.goForward()
    }

    /**
     * See [EngineSession.saveState]
     *
     * GeckoView provides a String representing the entire session state. We
     * store this String using a single Map entry with key GECKO_STATE_KEY.

     * See https://bugzilla.mozilla.org/show_bug.cgi?id=1441810 for
     * discussion on sync vs. async, where a decision was made that
     * callers should provide synchronous wrappers, if needed. In case we're
     * asking for the state when persisting, a separate (independent) thread
     * is used so we're not blocking anything else. In case of calling this
     * method from onPause or similar, we also want a synchronous response.
     */
    @Throws(GeckoEngineException::class)
    override fun saveState(): Map<String, Any> {
        Log.d("GeckoEngineSession", "Not implemented: saveState()", RuntimeException())
        return HashMap()
    }

    /**
     * See [EngineSession.restoreState]
     */
    override fun restoreState(state: Map<String, Any>) {
        Log.d("GeckoEngineSession", "Not implemented: restoreState()", RuntimeException())
    }

    /**
     * NavigationDelegate implementation for forwarding callbacks to observers of the session.
     */
    private fun createNavigationDelegate() = object : GeckoSession.NavigationDelegate {
        override fun onNewSession(
            session: GeckoSession?,
            uri: String?,
            response: GeckoSession.Response<GeckoSession>?
        ) = Unit

        override fun onLocationChange(session: GeckoSession?, url: String) {
            notifyObservers { onLocationChange(url) }
        }

        override fun onLoadRequest(session: GeckoSession?, uri: String?, target: Int): Boolean {
            return false
        }

        override fun onCanGoForward(session: GeckoSession?, canGoForward: Boolean) {
            notifyObservers { onNavigationStateChange(canGoForward = canGoForward) }
        }

        override fun onCanGoBack(session: GeckoSession?, canGoBack: Boolean) {
            notifyObservers { onNavigationStateChange(canGoBack = canGoBack) }
        }
    }

    /**
    * ProgressDelegate implementation for forwarding callbacks to observers of the session.
    */
    private fun createProgressDelegate() = object : GeckoSession.ProgressDelegate {
        override fun onSecurityChange(
            session: GeckoSession?,
            securityInfo: GeckoSession.ProgressDelegate.SecurityInformation?
        ) {
            notifyObservers {
                if (securityInfo != null) {
                    onSecurityChange(securityInfo.isSecure, securityInfo.host, securityInfo.issuerOrganization)
                } else {
                    onSecurityChange(false)
                }
            }
        }

        override fun onPageStart(session: GeckoSession?, url: String?) {
            notifyObservers {
                onProgress(PROGRESS_START)
                onLoadingStateChange(true)
            }
        }

        override fun onPageStop(session: GeckoSession?, success: Boolean) {
            if (success) {
                notifyObservers {
                    onProgress(PROGRESS_STOP)
                    onLoadingStateChange(false)
                }
            }
        }
    }

    companion object {
        internal const val PROGRESS_START = 25
        internal const val PROGRESS_STOP = 100
        internal const val GECKO_STATE_KEY = "GECKO_STATE"
    }
}
