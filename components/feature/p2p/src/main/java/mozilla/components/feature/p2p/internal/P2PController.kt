/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.internal

import android.content.Context
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.p2p.P2PFeature
import mozilla.components.feature.p2p.R
import mozilla.components.feature.p2p.view.P2PBar
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.lib.nearby.NearbyConnection.ConnectionState
import mozilla.components.lib.nearby.NearbyConnectionObserver
import mozilla.components.support.base.log.logger.Logger
import java.io.File
import java.io.FileOutputStream

/**
 * Controller that mediates between [P2PView] and [NearbyConnection].
 */
@Suppress("TooManyFunctions")
internal class P2PController(
    private val store: BrowserStore,
    private val thunk: () -> NearbyConnection,
    private val view: P2PView,
    private val tabsUseCases: TabsUseCases,
    private val sessionUseCases: SessionUseCases,
    private val sender: P2PFeature.P2PFeatureSender,
    private val onClose: (() -> Unit)
) : P2PView.Listener {
    private val logger = Logger("P2PController")
    private val outgoingMessages = mutableMapOf<Long, Char>()

    private val observer = object : NearbyConnectionObserver {
        @Synchronized
        override fun onStateUpdated(connectionState: ConnectionState) {
            savedConnectionState = connectionState
            updateState(connectionState)
            when (connectionState) {
                is ConnectionState.Authenticating -> {
                    view.authenticate(
                        connectionState.neighborId,
                        connectionState.neighborName,
                        connectionState.token
                    )
                }
                is ConnectionState.ReadyToSend -> view.readyToSend()
                is ConnectionState.Failure -> view.failure(connectionState.message)
                is ConnectionState.Isolated -> view.clear()
            }
        }

        private fun updateState(connectionState: ConnectionState) {
            view.updateStatus(
                when (connectionState) {
                    is ConnectionState.Isolated -> R.string.mozac_feature_p2p_isolated
                    is ConnectionState.Advertising -> R.string.mozac_feature_p2p_advertising
                    is ConnectionState.Discovering -> R.string.mozac_feature_p2p_discovering
                    is ConnectionState.Initiating -> R.string.mozac_feature_p2p_initiating
                    is ConnectionState.Authenticating -> R.string.mozac_feature_p2p_authenticating
                    is ConnectionState.Connecting -> R.string.mozac_feature_p2p_connecting
                    // The user might find it clearer to be notified that the device is connected
                    // than that it is ready to send (it is also ready to receive).
                    is ConnectionState.ReadyToSend -> R.string.mozac_feature_p2p_connected
                    is ConnectionState.Sending -> R.string.mozac_feature_p2p_sending
                    is ConnectionState.Failure -> R.string.mozac_feature_p2p_failure
                }
            )
        }

        override fun onMessageDelivered(payloadId: Long) {
            outgoingMessages.get(payloadId)?.let {
                view.reportSendComplete(
                    if (it == URL_INDICATOR) {
                        R.string.mozac_feature_p2p_url_sent
                    } else {
                        R.string.mozac_feature_p2p_page_sent
                    }
                )
                // Is it better to remove entries for delivered messages from the map
                // or to leave them in (which is more functional-style)?
                outgoingMessages.remove(payloadId)
            } ?: run {
                logger.error("Sent message id was not recognized")
            }
        }

        override fun onMessageReceived(neighborId: String, neighborName: String?, message: String) {
            if (message.length > 1) {
                when (message[0]) {
                    HTML_INDICATOR -> view.receivePage(
                        neighborId, neighborName, message.substring(1)
                    )
                    URL_INDICATOR -> view.receiveUrl(
                        neighborId, neighborName, message.substring(1)
                    )
                    else -> reportError("Cannot parse incoming message $message")
                }
            } else {
                reportError("Trivial message received: '$message'")
            }
        }
    }

    fun start() {
        view.listener = this
        if (nearbyConnection == null || savedConnectionState == null) {
            nearbyConnection = thunk()
            savedConnectionState = null
        } else {
            // If a connection already existed, update the UI to reflect its status.
            when (savedConnectionState) {
                is ConnectionState.Isolated -> view.initializeButtons(true, false)
                is ConnectionState.Advertising,
                is ConnectionState.Discovering,
                is ConnectionState.Initiating,
                is ConnectionState.Authenticating,
                is ConnectionState.Connecting -> view.initializeButtons(false, false)
                is ConnectionState.ReadyToSend -> view.initializeButtons(false, true)
                is ConnectionState.Failure -> view.initializeButtons(false, false)
            }
        }
        nearbyConnection?.register(observer, view as P2PBar)
    }

    fun stop() {
        nearbyConnection?.unregisterObservers()
    }

    @Synchronized
    private fun reportError(msg: String) {
        Logger.error(msg)
        view.failure(msg)
    }

    private inline fun <reified T : ConnectionState> cast() =
        (savedConnectionState as? T).also {
            if (it == null) {
                reportError("savedConnection was expected to be type ${T::class} but is $savedConnectionState")
            }
        }

    // P2PView.Listener implementation

    override fun onAdvertise() {
        nearbyConnection?.startAdvertising()
    }

    override fun onDiscover() {
        nearbyConnection?.startDiscovering()
    }

    override fun onAccept(token: String) {
        cast<ConnectionState.Authenticating>()?.accept()
    }

    override fun onReject(token: String) {
        cast<ConnectionState.Authenticating>()?.reject()
    }

    override fun onSetUrl(url: String, newTab: Boolean) {
        if (newTab) {
            tabsUseCases.addTab(url)
        } else {
            sessionUseCases.loadUrl(url)
        }
    }

    override fun onReset() {
        nearbyConnection?.disconnect()
    }

    override fun onSendPage() {
        if (cast<ConnectionState.ReadyToSend>() != null) {
            sender.requestHtml()
        }
    }

    private fun sendMessage(indicator: Char, message: String) {
        if (cast<ConnectionState.ReadyToSend>() != null) {
            when (val messageId = nearbyConnection?.sendMessage("$indicator$message")) {
                null -> reportError("Unable to send message: sendMessage() returns null")
                else -> outgoingMessages.put(messageId, indicator)
            }
        }
    }

    fun onPageReadyToSend(page: String) {
        sendMessage(HTML_INDICATOR, page)
    }

    override fun onSendUrl() {
        store.state.selectedTab?.content?.url?.let {
            sendMessage(URL_INDICATOR, it)
        }
    }

    @Suppress("MaxLineLength")
    override fun onLoadData(context: Context, data: String, newTab: Boolean) {
        // Store data in file to work around Mozilla bug 1598481, which makes
        // loading a page from memory extremely inefficient. Instead, we will
        // write it to a file and then load a URL referencing that file.
        val file = File.createTempFile("moz", ".html")
        FileOutputStream(file).use {
            it.write(data.toByteArray())
        }
        file.deleteOnExit()
        onSetUrl("file:${file.path}", newTab)
    }

    override fun onCloseToolbar() {
        onClose()
    }

    companion object {
        // Used for message headers and for hash table outgoingMessages
        const val URL_INDICATOR = 'U'
        const val HTML_INDICATOR = 'H'

        private var savedConnectionState: ConnectionState? = null
        private var nearbyConnection: NearbyConnection? = null
    }
}
