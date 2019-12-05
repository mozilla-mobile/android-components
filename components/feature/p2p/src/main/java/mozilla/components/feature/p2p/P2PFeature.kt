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
import mozilla.components.feature.p2p.internal.P2PController
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

/**
 * Feature implementation for peer-to-peer communication between browsers.
 */
class P2PFeature(
    private val view: P2PView,
    private val store: BrowserStore,
    private val engine: Engine,
    private val thunk: () -> NearbyConnection,
    private val tabsUseCases: TabsUseCases,
    private val sessionUseCases: SessionUseCases,
    private val sessionManager: SessionManager,
    override val onNeedToRequestPermissions: OnNeedToRequestPermissions,
    private val onClose: (() -> Unit)
) : SelectionAwareSessionObserver(sessionManager), LifecycleAwareFeature, PermissionsFeature {
    private val logger = Logger("P2PFeature")
    @VisibleForTesting
    internal var controller: P2PController? = null
    @VisibleForTesting
    internal lateinit var extensionController: WebExtensionController

    // LifeCycleAwareFeature implementation

    override fun start() {
        requestNeededPermissions()
    }

    override fun stop() {
        super.stop()
        controller?.stop()
    }

    // PermissionsFeature implementation

    private var ungrantedPermissions =
        (NearbyConnection.PERMISSIONS + LOCAL_PERMISSIONS).filter {
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
            Logger.error("Cannot continue on pre-Marshmallow device")
        }
    }

    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        // Sometimes ungrantedPermissions still shows a recently accepted permission as being
        // not granted, so we need to check grantResults instead.
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onPermissionsGranted()
        } else {
            Logger.error("Cannot continue due to missing permissions $ungrantedPermissions")
        }
    }

    private fun onPermissionsGranted() {
        startExtension()
    }

    // Communication with the web extension

    private fun startExtension() {
        observeSelected() // this sets actionSession

        extensionController = WebExtensionController(P2P_EXTENSION_ID, P2P_EXTENSION_URL)
        registerP2PContentMessageHandler()
        extensionController.install(engine)

        controller =
            P2PController(store, thunk, view, tabsUseCases, sessionUseCases, P2PFeatureSender(), onClose)
        controller?.start()
    }

    @VisibleForTesting
    internal fun registerP2PContentMessageHandler(session: Session? = activeSession) {
        if (session == null) {
            return
        }

        val engineSession = sessionManager.getOrCreateEngineSession(session)
        val messageHandler = P2PContentMessageHandler(session)
        extensionController.registerContentMessageHandler(engineSession, messageHandler)
    }

    private inner class P2PContentMessageHandler(
        private val session: Session
    ) : MessageHandler {
        override fun onPortMessage(message: Any, port: Port) {
            if (message is String) {
                controller?.onPageReadyToSend(message)
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
            activeSession?.let {
                extensionController.sendContentMessage(
                    json,
                    sessionManager.getOrCreateEngineSession(it)
                )
            }
        }
    }

    @VisibleForTesting
    companion object {
        private const val P2P_EXTENSION_ID = "mozacP2P"
        private const val P2P_EXTENSION_URL = "resource://android/assets/extensions/p2p/"

        // Write incoming pages to file system and loadUrl() to display them.
        // Surprisingly, that is faster than passing the page to loadData().
        private val LOCAL_PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Constants for building messages sent to the web extension.
        private const val ACTION_MESSAGE_KEY = "action"
        private const val ACTION_GET_HTML = "get_html" // request the page's HTML
    }
}
