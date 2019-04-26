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

    private val config = Config(context.getSharedPreferences("mozac_feature_reader_view", Context.MODE_PRIVATE), this)
    private val controlsPresenter = ReaderViewControlsPresenter(controlsView, config)
    private val controlsInteractor = ReaderViewControlsInteractor(controlsView, config)

    class Config(val prefs: SharedPreferences, val readerViewFeature: ReaderViewFeature) {
        enum class FontType(val value: String) { SANSSERIF("sans-serif"), SERIF("serif") }
        enum class ColorScheme { LIGHT, SEPIA, DARK }

        var colorScheme by Delegates.observable(ColorScheme.valueOf(prefs.getString(COLOR_SCHEME_KEY, "LIGHT")!!)) {
            _, old, new ->
                if (old != new) {
                    val message = JSONObject().put(ACTION_MESSAGE_KEY, ACTION_SET_COLOR_SCHEME).put(ACTION_VALUE, new)
                    readerViewFeature.sendContentMessage(message)
                    prefs.edit().putString(COLOR_SCHEME_KEY, new.name).commit()
                }
        }

        var fontType by Delegates.observable(FontType.valueOf(prefs.getString(FONT_TYPE_KEY, "SERIF")!!)) {
            _, old, new ->
                if (old != new) {
                    val message = JSONObject().put(ACTION_MESSAGE_KEY, ACTION_SET_FONT_TYPE).put(ACTION_VALUE, new.name)
                    readerViewFeature.sendContentMessage(message)
                    prefs.edit().putString(FONT_TYPE_KEY, new.name).commit()
                }
        }

        @Suppress("MagicNumber")
        var fontSize by Delegates.observable(prefs.getInt(FONT_SIZE_KEY, 3)) { _, old, new ->
            if (old != new) {
                val message = JSONObject().put(ACTION_MESSAGE_KEY, ACTION_CHANGE_FONT_SIZE).put(ACTION_VALUE, new - old)
                readerViewFeature.sendContentMessage(message)
                prefs.edit().putInt(FONT_SIZE_KEY, new).commit()
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
        activeSession?.let {
            if (it.readerMode) {
                hideReaderView()
                return true
            }
        }
        return false
    }

    override fun onSessionSelected(session: Session) {
        registerContentMessageHandler(activeSession)
        checkReaderable()
        super.onSessionSelected(session)
    }

    override fun onSessionRemoved(session: Session) {
        ports.remove(sessionManager.getEngineSession(session))
    }

    override fun onUrlChanged(session: Session, url: String) {
        session.readerMode = false
        checkReaderable()
    }

    override fun onLoadingStateChanged(session: Session, loading: Boolean) {
        // If the page was refreshed and reader mode was turned on before,
        // make sure it is still turned on.
        if (!loading && activeSession?.readerMode == true) {
            showReaderView()
        }
    }

    override fun onReaderableStateUpdated(session: Session, readerable: Boolean) {
        onReaderViewAvailableChange(readerable)
    }

    fun showReaderView() {
        activeSession?.let {
            val config = JSONObject()
                .put(ACTION_VALUE_FONT_SIZE, config.fontSize)
                .put(ACTION_VALUE_FONT_TYPE, config.fontType.name.toLowerCase())
                .put(ACTION_VALUE_COLOR_SCHEME, config.colorScheme.name.toLowerCase())

            val message = JSONObject()
                .put(ACTION_MESSAGE_KEY, ACTION_SHOW)
                .put(ACTION_VALUE, config)

            sendContentMessage(message, it)
            it.readerMode = true
        }
    }

    fun hideReaderView() {
        activeSession?.let {
            it.readerMode = false
            // We will re-determine if the original page is readerable when it's loaded.
            it.readerable = false
            sendContentMessage(JSONObject().put(ACTION_MESSAGE_KEY, ACTION_HIDE), it)
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
                sendContentMessage(JSONObject().put(ACTION_MESSAGE_KEY, ACTION_CHECK_READERABLE), it)
            }
        }
    }

    private fun sendContentMessage(msg: Any, session: Session? = activeSession) {
        session?.let {
            val port = ports[sessionManager.getEngineSession(session)]
            port?.postMessage(msg) ?: Logger.error("No port connected for the provided session")
        }
    }

    @VisibleForTesting
    companion object {
        internal const val READER_VIEW_EXTENSION_ID = "mozacReaderview"
        internal const val READER_VIEW_EXTENSION_URL = "resource://android/assets/extensions/readerview/"

        internal const val ACTION_MESSAGE_KEY = "action"

        internal const val ACTION_SHOW = "show"
        internal const val ACTION_HIDE = "hide"
        internal const val ACTION_CHECK_READERABLE = "checkReaderable"
        internal const val ACTION_SET_COLOR_SCHEME = "setColorScheme"
        internal const val ACTION_CHANGE_FONT_SIZE = "changeFontSize"
        internal const val ACTION_SET_FONT_TYPE = "setFontType"

        internal const val ACTION_VALUE = "value"
        internal const val ACTION_VALUE_FONT_SIZE = "fontSize"
        internal const val ACTION_VALUE_FONT_TYPE = "fontType"
        internal const val ACTION_VALUE_COLOR_SCHEME = "colorScheme"

        internal const val READERABLE_RESPONSE_MESSAGE_KEY = "readerable"

        @Volatile
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
