/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import mozilla.components.browser.session.SelectionAwareSessionObserver
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.feature.p2p.internal.P2PInteractor
import mozilla.components.feature.p2p.internal.P2PPresenter
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.webextensions.WebExtensionController
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Feature implementation for peer-to-peer communication between browsers.
 */
class P2PFeature(
    private val view: P2PView,
    private val store: BrowserStore,
    private val engine: Engine,
    private val connectionProvider: () -> NearbyConnection,
    private val tabsUseCases: TabsUseCases,
    private val sessionUseCases: SessionUseCases,
    private val sessionManager: SessionManager,
    override val onNeedToRequestPermissions: OnNeedToRequestPermissions,
    private val onClose: (() -> Unit)
) : SelectionAwareSessionObserver(sessionManager), LifecycleAwareFeature, PermissionsFeature {
    private val logger = Logger("P2PFeature")

    @VisibleForTesting
    internal var interactor: P2PInteractor? = null

    @VisibleForTesting
    internal var presenter: P2PPresenter? = null

    @VisibleForTesting
    internal var extensionController: WebExtensionController? = null

    // LifeCycleAwareFeature implementation

    override fun start() {
        requestNeededPermissions()
    }

    override fun stop() {
        super.stop()
        interactor?.stop()
        presenter?.stop()
    }

    // PermissionsFeature implementation

    private val ungrantedPermissions
        get() = (NearbyConnection.PERMISSIONS + LOCAL_PERMISSIONS).filter {
            ContextCompat.checkSelfPermission(
                view.asView().context,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ungrantedPermissions.isEmpty()) {
                onPermissionsGranted()
            } else {
                onNeedToRequestPermissions(ungrantedPermissions.toTypedArray())
            }
        } else {
            logger.error("Cannot continue on pre-Marshmallow device")
        }
    }

    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        // Sometimes ungrantedPermissions still shows a recently accepted permission as being
        // not granted, so we need to check grantResults instead.
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onPermissionsGranted()
        } else {
            logger.error("Cannot continue due to missing permissions $ungrantedPermissions")
        }
    }

    private fun onPermissionsGranted() {
        startExtension()
    }

    // Communication with the web extension

    private fun startExtension() {
        observeSelected() // this sets actionSession

        extensionController = WebExtensionController(P2P_EXTENSION_ID, P2P_EXTENSION_URL, P2P_MESSAGING_ID)
        registerP2PContentMessageHandler()
        extensionController?.install(engine)

        val outgoingMessages = ConcurrentHashMap<Long, Char>()
        interactor = P2PInteractor(
            store,
            view,
            tabsUseCases,
            sessionUseCases,
            P2PFeatureSender(),
            onClose,
            connectionProvider,
            outgoingMessages
        ).apply { start() }
        presenter = P2PPresenter(connectionProvider, view, outgoingMessages).apply { start() }
    }

    @VisibleForTesting
    internal fun registerP2PContentMessageHandler(session: Session? = activeSession) {
        if (session == null) {
            return
        }

        // TODO: Action for registering content message handler? Do we really need that?
        /*
        store.dispatch(EngineAction.CreateEngineSessionAction(
            session.id,
            sideEffect = { engineSession ->
                val messageHandler = P2PContentMessageHandler()
                extensionController?.registerContentMessageHandler(engineSession, messageHandler)
            }
        ))
         */
    }

    private inner class P2PContentMessageHandler : MessageHandler {
        override fun onPortMessage(message: Any, port: Port) {
            if (message is String) {
                interactor?.onPageReadyToSend(message)
            } else {
                logger.error("P2PC message is not a string.")
            }
            super.onPortMessage(message, port)
        }
    }

    /**
     * A class able to request an encoding of the current web page.
     */
    inner class P2PFeatureSender {
        /**
         * Requests an encoding of the current web page suitable for sending to another device.
         */
        fun requestHtml() {
            sendMessage(JSONObject().put(ACTION_MESSAGE_KEY, ACTION_GET_HTML))
        }

        private fun sendMessage(json: JSONObject) {
            val session = activeSession ?: return

            // TODO: Action for sending content message. Do we really need that?
            json.hashCode()
            session.hashCode()
            /*
            store.dispatch(EngineAction.CreateEngineSessionAction(
                session.id,
                sideEffect = { engineSession ->
                    extensionController?.sendContentMessage(json, engineSession)
                }
            ))
             */
        }
    }

    @VisibleForTesting
    companion object {
        @VisibleForTesting
        internal const val P2P_EXTENSION_ID = "p2p@mozac.org"

        @VisibleForTesting
        internal const val P2P_MESSAGING_ID = "mozacP2P"

        @VisibleForTesting
        internal const val P2P_EXTENSION_URL = "resource://android/assets/extensions/p2p/"

        // Write incoming pages to file system and loadUrl() to display them.
        // Surprisingly, that is faster than passing the page to loadData().
        @VisibleForTesting
        internal val LOCAL_PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Constants for building messages sent to the web extension.
        private const val ACTION_MESSAGE_KEY = "action"
        private const val ACTION_GET_HTML = "get_html" // request the page's HTML
    }
}
