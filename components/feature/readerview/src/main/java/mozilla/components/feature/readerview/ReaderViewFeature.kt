/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.readerview

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.VisibleForTesting
import mozilla.components.browser.session.SelectionAwareSessionObserver
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.feature.readerview.internal.ReaderViewControlsInteractor
import mozilla.components.feature.readerview.internal.ReaderViewControlsPresenter
import mozilla.components.feature.readerview.view.ReaderViewControlsView
import mozilla.components.support.base.feature.BackHandler
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import java.lang.IllegalStateException
import java.util.WeakHashMap
import kotlin.properties.Delegates

typealias OnReaderViewAvailableChange = (available: Boolean) -> Unit

/**
 * Feature implementation that provides a reader view for the selected
 * session. This feature is implemented as a web extension and
 * needs to be installed prior to use (see [ReaderViewFeature.install]).
 *
 * @property context a reference to the context.
 * @property engine a reference to the application's browser engine.
 * @property sessionManager a reference to the application's [SessionManager].
 * @property onReaderViewAvailableChange a callback invoked to indicate whether
 * or not reader view is available for the page loaded by the currently selected
 * session. The callback will be invoked when a page is loaded or refreshed,
 * on any navigation (back or forward), and when the selected session
 * changes.
 */
@Suppress("TooManyFunctions")
class ReaderViewFeature(
    private val context: Context,
    private val engine: Engine,
    private val sessionManager: SessionManager,
    controlsView: ReaderViewControlsView,
    private val onReaderViewAvailableChange: OnReaderViewAvailableChange = { }
) : SelectionAwareSessionObserver(sessionManager), LifecycleAwareFeature, BackHandler {

    private val config = Config(context.getSharedPreferences("mozac_feature_reader_view", Context.MODE_PRIVATE))
    private val controlsPresenter = ReaderViewControlsPresenter(controlsView, config)
    private val controlsInteractor = ReaderViewControlsInteractor(controlsView, config)

    class Config(prefs: SharedPreferences) {
        enum class FontType { SANS_SERIF, SERIF }
        enum class ColorScheme { LIGHT, SEPIA, DARK }

        var colorScheme by Delegates.observable(ColorScheme.valueOf(prefs.getString(COLOR_SCHEME_KEY, "LIGHT")!!)) {
            _, old, new -> saveAndSendMessage(old, new, COLOR_SCHEME_KEY)
        }

        @Suppress("MagicNumber")
        var fontSize by Delegates.observable(prefs.getInt(FONT_SIZE_KEY, 3)) {
            _, old, new -> saveAndSendMessage(old, new, FONT_SIZE_KEY)
        }

        var fontType by Delegates.observable(FontType.valueOf(prefs.getString(FONT_TYPE_KEY, "SANS_SERIF")!!)) {
            _, old, new -> saveAndSendMessage(old, new, FONT_TYPE_KEY)
        }

        @Suppress("UNUSED_PARAMETER")
        private fun saveAndSendMessage(old: Any, new: Any, key: String) {
            if (old != new) {
                // TODO save shared preference
                // TODO send message to reader view web extension
            }
        }

        companion object {
            const val COLOR_SCHEME_KEY = "mozac-readerview-colorscheme"
            const val FONT_TYPE_KEY = "mozac-readerview-fonttype"
            const val FONT_SIZE_KEY = "mozac-readerview-fontsize"
        }
    }

    override fun start() {
        observeSelected()

        registerContentMessageHandler(activeSession)

        if (ReaderViewFeature.installedWebExt == null) {
            ReaderViewFeature.install(engine)
        }

        checkReaderable()

        controlsInteractor.start()
    }

    private fun registerContentMessageHandler(session: Session?) {
        if (session == null) {
            return
        }

        val messageHandler = object : MessageHandler {
            override fun onPortConnected(port: Port) {
                ports[port.engineSession] = port
                checkReaderable()
            }

            override fun onPortDisconnected(port: Port) {
                ports.remove(port.engineSession)
            }

            override fun onPortMessage(message: Any, port: Port) {
                if (message is JSONObject) {
                    activeSession?.readerable = message.optBoolean(READERABLE_RESPONSE_MESSAGE_KEY, false)
                }
            }
        }

        registerMessageHandler(sessionManager.getOrCreateEngineSession(session), messageHandler)
    }

    override fun stop() {
        controlsInteractor.stop()
        super.stop()
    }

    override fun onBackPressed(): Boolean {
        // TODO send message to exit reader view (-> see ReaderView.hide())
        return true
    }

    override fun onSessionSelected(session: Session) {
        // TODO restore selected state of whether the controls are open or not
        registerContentMessageHandler(activeSession)
        checkReaderable()
        super.onSessionSelected(session)
    }

    override fun onSessionRemoved(session: Session) {
        ports.remove(sessionManager.getEngineSession(session))
    }

    override fun onUrlChanged(session: Session, url: String) {
        checkReaderable()
    }

    override fun onReaderableStateUpdated(session: Session, readerable: Boolean) {
        onReaderViewAvailableChange(readerable)
    }

    fun showReaderView() {
        activeSession?.let {
            sendMessage(JSONObject().put(ACTION_MESSAGE_KEY, ACTION_SHOW), it)
        }
    }

    fun hideReaderView() {
        activeSession?.let {
            sendMessage(JSONObject().put(ACTION_MESSAGE_KEY, ACTION_HIDE), it)
        }
    }

    /**
     * Show ReaderView appearance controls.
     */
    fun showControls() {
        controlsPresenter.show()
    }

    /**
     * Hide ReaderView appearance controls.
     */
    fun hideControls() {
        controlsPresenter.hide()
    }

    internal fun checkReaderable() {
        activeSession?.let {
            if (ports.containsKey(sessionManager.getEngineSession(it))) {
                sendMessage(JSONObject().put(ACTION_MESSAGE_KEY, ACTION_CHECK_READERABLE), it)
            }
        }
    }

    private fun sendMessage(msg: Any, session: Session) {
        val port = ports[sessionManager.getEngineSession(session)]
        port?.postMessage(msg) ?: throw IllegalStateException("No port connected for the provided session")
    }

    companion object {
        @VisibleForTesting
        internal const val READER_VIEW_EXTENSION_ID = "mozacReaderview"

        @VisibleForTesting
        internal const val READER_VIEW_EXTENSION_URL = "resource://android/assets/extensions/readerview/"

        @VisibleForTesting
        internal const val ACTION_MESSAGE_KEY = "action"

        @VisibleForTesting
        internal const val ACTION_SHOW = "show"

        @VisibleForTesting
        internal const val ACTION_HIDE = "hide"

        @VisibleForTesting
        internal const val ACTION_CHECK_READERABLE = "checkReaderable"

        @VisibleForTesting
        internal const val READERABLE_RESPONSE_MESSAGE_KEY = "readerable"

        @Volatile
        @VisibleForTesting
        internal var installedWebExt: WebExtension? = null

        @Volatile
        private var registerContentMessageHandler: (WebExtension) -> Unit? = { }

        internal var ports = WeakHashMap<EngineSession, Port>()

        /**
         * Installs the readerview web extension in the provided engine.
         *
         * @param engine a reference to the application's browser engine.
         */
        fun install(engine: Engine) {
            engine.installWebExtension(READER_VIEW_EXTENSION_ID, READER_VIEW_EXTENSION_URL,
                onSuccess = {
                    Logger.debug("Installed extension: ${it.id}")
                    registerContentMessageHandler(it)
                    installedWebExt = it
                },
                onError = { ext, throwable ->
                    Logger.error("Failed to install extension: $ext", throwable)
                }
            )
        }

        fun registerMessageHandler(session: EngineSession, messageHandler: MessageHandler) {
            registerContentMessageHandler = {
                if (!it.hasContentMessageHandler(session, READER_VIEW_EXTENSION_ID)) {
                    it.registerContentMessageHandler(session, READER_VIEW_EXTENSION_ID, messageHandler)
                }
            }

            installedWebExt?.let { registerContentMessageHandler(it) }
        }
    }
}
