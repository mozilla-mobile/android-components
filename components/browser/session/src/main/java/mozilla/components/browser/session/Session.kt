/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session

import mozilla.components.browser.session.engine.EngineSessionHolder
import mozilla.components.browser.session.tab.CustomTabConfig
import mozilla.components.concept.engine.HitResult
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import java.util.UUID
import kotlin.properties.Delegates

/**
 * Value type that represents the state of a browser session. Changes can be observed.
 */
@Suppress("TooManyFunctions")
class Session(
    initialUrl: String,
    val private: Boolean = false,
    val source: Source = Source.NONE,
    val id: String = UUID.randomUUID().toString(),
    delegate: Observable<Session.Observer> = ObserverRegistry()
) : Observable<Session.Observer> by delegate {
    /**
     * Holder for keeping a reference to an engine session and its observer to update this session
     * object.
     */
    internal val engineSessionHolder = EngineSessionHolder()

    /**
     * Interface to be implemented by classes that want to observe a session.
     */
    interface Observer {
        fun onUrlChanged(session: Session, url: String) = Unit
        fun onTitleChanged(session: Session, title: String) = Unit
        fun onProgress(session: Session, progress: Int) = Unit
        fun onLoadingStateChanged(session: Session, loading: Boolean) = Unit
        fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) = Unit
        fun onSearch(session: Session, searchTerms: String) = Unit
        fun onSecurityChanged(session: Session, securityInfo: SecurityInfo) = Unit
        fun onCustomTabConfigChanged(session: Session, customTabConfig: CustomTabConfig?) = Unit
        fun onDownload(session: Session, download: Download): Boolean = false
        fun onTrackerBlockingEnabledChanged(session: Session, blockingEnabled: Boolean) = Unit
        fun onTrackerBlocked(session: Session, blocked: String, all: List<String>) = Unit
        fun onLongPress(session: Session, hitResult: HitResult): Boolean = false
        fun onFindResult(session: Session, result: FindResult) = Unit
        fun onDesktopModeChanged(session: Session, enabled: Boolean) = Unit
    }

    /**
     * A value type holding security information for a Session.
     *
     * @property secure true if the session is currently pointed to a URL with
     * a valid SSL certificate, otherwise false.
     * @property host domain for which the SSL certificate was issued.
     * @property issuer name of the certificate authority who issued the SSL certificate.
     */
    data class SecurityInfo(val secure: Boolean = false, val host: String = "", val issuer: String = "")

    /**
     * Represents the origin of a session to describe how and why it was created.
     */
    enum class Source {
        /**
         * Created to handle an ACTION_SEND (share) intent
         */
        ACTION_SEND,

        /**
         * Created to handle an ACTION_VIEW intent
         */
        ACTION_VIEW,

        /**
         * Created to handle a CustomTabs intent
         */
        CUSTOM_TAB,

        /**
         * User interacted with the home screen
         */
        HOME_SCREEN,

        /**
         * User interacted with a menu
         */
        MENU,

        /**
         * User opened a new tab
         */
        NEW_TAB,

        /**
         * Default value and for testing purposes
         */
        NONE,

        /**
         * Default value and for testing purposes
         */
        TEXT_SELECTION,

        /**
         * User entered a URL or search term
         */
        USER_ENTERED
    }

    /**
     * A value type representing a result of a "find in page" operation.
     *
     * @property activeMatchOrdinal the zero-based ordinal of the currently selected match.
     * @property numberOfMatches the match count
     * @property isDoneCounting true if the find operation has completed, otherwise false.
     */
    data class FindResult(val activeMatchOrdinal: Int, val numberOfMatches: Int, val isDoneCounting: Boolean)

    /**
     * The currently loading or loaded URL.
     */
    var url: String by Delegates.observable(initialUrl) {
        _, old, new -> notifyObservers(old, new) { onUrlChanged(this@Session, new) }
    }

    /**
     * The title of the currently displayed website changed.
     */
    var title: String by Delegates.observable("") {
        _, old, new -> notifyObservers(old, new) { onTitleChanged(this@Session, new) }
    }

    /**
     * The progress loading the current URL.
     */
    var progress: Int by Delegates.observable(0) {
        _, old, new -> notifyObservers(old, new) { onProgress(this@Session, new) }
    }

    /**
     * Loading state, true if this session's url is currently loading, otherwise false.
     */
    var loading: Boolean by Delegates.observable(false) {
        _, old, new -> notifyObservers(old, new) { onLoadingStateChanged(this@Session, new) }
    }

    /**
     * Navigation state, true if there's an history item to go back to, otherwise false.
     */
    var canGoBack: Boolean by Delegates.observable(false) {
        _, old, new -> notifyObservers(old, new) { onNavigationStateChanged(this@Session, new, canGoForward) }
    }

    /**
     * Navigation state, true if there's an history item to go forward to, otherwise false.
     */
    var canGoForward: Boolean by Delegates.observable(false) {
        _, old, new -> notifyObservers(old, new) { onNavigationStateChanged(this@Session, canGoBack, new) }
    }

    /**
     * The currently / last used search terms.
     */
    var searchTerms: String by Delegates.observable("") {
        _, _, new -> notifyObservers { if (!new.isEmpty()) onSearch(this@Session, new) }
    }

    /**
     * Security information indicating whether or not the current session is
     * for a secure URL, as well as the host and SSL certificate authority, if applicable.
     */
    var securityInfo: SecurityInfo by Delegates.observable(SecurityInfo()) {
        _, old, new -> notifyObservers(old, new) { onSecurityChanged(this@Session, new) }
    }

    /**
     * Configuration data in case this session is used for a Custom Tab.
     */
    var customTabConfig: CustomTabConfig? by Delegates.observable<CustomTabConfig?>(null) {
        _, _, new -> notifyObservers { onCustomTabConfigChanged(this@Session, new) }
    }

    /**
     * Last download request if it wasn't consumed by at least one observer.
     */
    var download: Consumable<Download> by Delegates.vetoable(Consumable.empty()) { _, _, download ->
        val consumers = wrapConsumers<Download> { onDownload(this@Session, it) }
        !download.consumeBy(consumers)
    }

    /**
     * Tracker blocking state, true if blocking trackers is enabled, otherwise false.
     */
    var trackerBlockingEnabled: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onTrackerBlockingEnabledChanged(this@Session, trackerBlockingEnabled) }
    }

    /**
     * List of URIs that have been blocked in this session.
     */
    var trackersBlocked: List<String> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) {
            if (new.isNotEmpty()) {
                onTrackerBlocked(this@Session, trackersBlocked.last(), trackersBlocked)
            }
        }
    }

    /**
     * List of results of that latest "find in page" operation.
     */
    var findResults: List<FindResult> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) {
            if (new.isNotEmpty()) {
                onFindResult(this@Session, findResults.last())
            }
        }
    }

    /**
     * The target of the latest long click operation.
     */
    var hitResult: Consumable<HitResult> by Delegates.vetoable(Consumable.empty()) { _, _, result ->
        val consumers = wrapConsumers<HitResult> { onLongPress(this@Session, it) }
        !result.consumeBy(consumers)
    }

    /**
     * Desktop Mode state, true if the desktop mode is requested, otherwise false.
     */
    var desktopMode: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) {
            onDesktopModeChanged(this@Session, new)
        }
    }

    /**
     * Returns whether or not this session is used for a Custom Tab.
     */
    fun isCustomTabSession() = customTabConfig != null

    /**
     * Helper method to notify observers.
     */
    private fun notifyObservers(old: Any, new: Any, block: Observer.() -> Unit) {
        if (old != new) {
            notifyObservers(block)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Session
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
