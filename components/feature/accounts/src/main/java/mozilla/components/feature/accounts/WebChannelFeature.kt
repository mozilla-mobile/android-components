/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.accounts

import android.content.Context
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.session.SelectionAwareSessionObserver
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import java.util.WeakHashMap

/**
 * Feature implementation that provides Firefox Accounts WebChannel support.
 * This feature is implemented as a web extension and
 * needs to be installed prior to use (see [WebChannelViewFeature.install]).
 *
 * @property context a reference to the context.
 * @property engine a reference to the application's browser engine.
 * @property sessionManager a reference to the application's [SessionManager].
 */
@Suppress("TooManyFunctions")
class WebChannelFeature(
    private val context: Context,
    private val engine: Engine,
    private val sessionManager: SessionManager
) : SelectionAwareSessionObserver(sessionManager), LifecycleAwareFeature {

    override fun start() {
        observeSelected()

        registerContentMessageHandler(activeSession)

        if (WebChannelFeature.installedWebExt == null) {
            WebChannelFeature.install(engine)
        }
    }

    override fun stop() {
        super.stop()
    }

    override fun onSessionSelected(session: Session) {
        super.onSessionSelected(session)
    }

    override fun onSessionAdded(session: Session) {
        registerContentMessageHandler(session)
    }

    override fun onSessionRemoved(session: Session) {
        ports.remove(sessionManager.getEngineSession(session))
    }

    private class WebChannelViewContentMessageHandler(
        private val engineSession: EngineSession
    ) : MessageHandler {
        override fun onPortConnected(port: Port) {
            ports[port.engineSession] = port
        }

        override fun onPortDisconnected(port: Port) {
            ports.remove(port.engineSession)
        }

        override fun onPortMessage(message: Any, port: Port) {
            if (message is JSONObject) {
                logger.debug(message.toString())

                val messageObj = message.getJSONObject("message")
                val command = messageObj.get("command")

                val messageId = messageObj.optString("messageId", "")
                when (command) {
                    COMMAND_CAN_LINK_ACCOUNT -> sendLinkResponse(messageId, engineSession)
                    COMMAND_OAUTH_LOGIN -> receiveLogin(messageObj)
                }
            }
        }
    }

    private fun registerContentMessageHandler(session: Session?) {
        if (session == null) {
            return
        }

        val engineSession = sessionManager.getOrCreateEngineSession(session)
        val messageHandler = WebChannelViewContentMessageHandler(engineSession)
        registerMessageHandler(engineSession, messageHandler)
    }

    @VisibleForTesting
    companion object {
        private val logger = Logger("mozac-fxawebchannel")

        internal const val WEB_CHANNEL_EXTENSION_ID = "mozacWebchannel"
        internal const val WEB_CHANNEL_EXTENSION_URL = "resource://android/assets/extensions/fxawebchannel/"

        // Constants for incoming messages from the WebExtension.
        // Possible messages: https://github.com/mozilla/fxa/blob/8701348cdd79dbdc9879b2b4a55a23a135a32bc1/packages/fxa-content-server/docs/relier-communication-protocols/fx-fxawebchannel.md
        internal const val CHANNEL_ID = "account_updates"
        internal const val COMMAND_CAN_LINK_ACCOUNT = "fxaccounts:can_link_account"
        internal const val COMMAND_OAUTH_LOGIN = "fxaccounts:oauth_login"

        @Volatile
        internal var installedWebExt: WebExtension? = null

        @Volatile
        private var registerContentMessageHandler: (WebExtension) -> Unit? = { }

        internal var ports = WeakHashMap<EngineSession, Port>()

        /**
         * Installs the WebChannel web extension in the provided engine.
         *
         * @param engine a reference to the application's browser engine.
         */
        fun install(engine: Engine) {
            engine.installWebExtension(WEB_CHANNEL_EXTENSION_ID, WEB_CHANNEL_EXTENSION_URL,
                    onSuccess = {
                        logger.debug("Installed extension: ${it.id}")
                        registerContentMessageHandler(it)
                        installedWebExt = it
                    },
                    onError = { ext, throwable ->
                        logger.error("Failed to install extension: $ext", throwable)
                    }
            )
        }

        fun registerMessageHandler(session: EngineSession, messageHandler: MessageHandler) {
            registerContentMessageHandler = {
                if (!it.hasContentMessageHandler(session, WEB_CHANNEL_EXTENSION_ID)) {
                    it.registerContentMessageHandler(session, WEB_CHANNEL_EXTENSION_ID, messageHandler)
                }
            }

            installedWebExt?.let { registerContentMessageHandler(it) }
        }

        private fun sendLinkResponse(messageId: String, engineSession: EngineSession) {
            val statusData = JSONObject()
            statusData.put("ok", true)

            val statusMessage = JSONObject()
            statusMessage.put("messageId", messageId)
            statusMessage.put("command", COMMAND_CAN_LINK_ACCOUNT)
            statusMessage.put("data", statusData)

            val status = JSONObject()
            status.put("id", CHANNEL_ID)
            status.put("message", statusMessage)

            sendContentMessage(status, engineSession)
        }

        private fun receiveLogin(messageObj: JSONObject) {
            logger.info(messageObj.toString())
        }

        private fun sendContentMessage(msg: Any, engineSession: EngineSession) {
            val port = ports[engineSession]
            port?.postMessage(msg) ?: logger.error("No port connected for provided session. Message $msg not sent.")
        }
    }
}
