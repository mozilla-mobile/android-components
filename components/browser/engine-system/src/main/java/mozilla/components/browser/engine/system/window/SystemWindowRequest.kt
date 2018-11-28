/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.system.window

import android.os.Message
import android.webkit.WebView
import mozilla.components.browser.engine.system.SystemEngineSession
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.window.WindowRequest

/**
 * WebView-based implementation of [WindowRequest].
 *
 * @property webView the WebView from which the request originated.
 * @property newWebView the WebView to use for opening a new window, may be null for close requests.
 * @property openAsDialog whether or not the window should be opened as a dialog, defaults to false.
 * @property triggeredByUser whether or not the request was triggered by the user, defaults to false.
 * @property resultMsg the message to send to the new WebView, may be null.
 */
class SystemWindowRequest(
    private val webView: WebView,
    private val newWebView: WebView? = null,
    val openAsDialog: Boolean = false,
    val triggeredByUser: Boolean = false,
    private val resultMsg: Message? = null
) : WindowRequest {

    override val url: String = "about:blank"

    override fun prepare(engineSession: EngineSession) {
        (engineSession as SystemEngineSession).webView = newWebView
    }

    override fun start() {
        val message = resultMsg
        val transport = message?.obj as? WebView.WebViewTransport
        transport?.let {
            it.webView = newWebView
            message.sendToTarget()
        }
    }
}
