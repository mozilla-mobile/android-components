/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session

import android.content.Intent
import android.graphics.Bitmap
import mozilla.components.browser.session.engine.EngineSessionHolder
import mozilla.components.browser.session.engine.request.LaunchIntentMetadata
import mozilla.components.browser.session.engine.request.LoadRequestMetadata
import mozilla.components.browser.session.engine.request.LoadRequestOption
import mozilla.components.browser.session.ext.syncDispatch
import mozilla.components.browser.session.ext.toFindResultState
import mozilla.components.browser.session.ext.toSecurityInfoState
import mozilla.components.browser.session.ext.toTabSessionState
import mozilla.components.browser.state.action.ContentAction.AddFindResultAction
import mozilla.components.browser.state.action.ContentAction.ClearFindResultsAction
import mozilla.components.browser.state.action.ContentAction.ConsumeHitResultAction
import mozilla.components.browser.state.action.ContentAction.FullScreenChangedAction
import mozilla.components.browser.state.action.ContentAction.RemoveThumbnailAction
import mozilla.components.browser.state.action.ContentAction.RemoveWebAppManifestAction
import mozilla.components.browser.state.action.ContentAction.UpdateBackNavigationStateAction
import mozilla.components.browser.state.action.ContentAction.UpdateForwardNavigationStateAction
import mozilla.components.browser.state.action.ContentAction.UpdateHitResultAction
import mozilla.components.browser.state.action.ContentAction.UpdateLoadingStateAction
import mozilla.components.browser.state.action.ContentAction.UpdateProgressAction
import mozilla.components.browser.state.action.ContentAction.UpdateSearchTermsAction
import mozilla.components.browser.state.action.ContentAction.UpdateSecurityInfoAction
import mozilla.components.browser.state.action.ContentAction.UpdateThumbnailAction
import mozilla.components.browser.state.action.ContentAction.UpdateTitleAction
import mozilla.components.browser.state.action.ContentAction.UpdateUrlAction
import mozilla.components.browser.state.action.ContentAction.UpdateWebAppManifestAction
import mozilla.components.browser.state.action.ContentAction.ViewportFitChangedAction
import mozilla.components.browser.state.action.CustomTabListAction.RemoveCustomTabAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction.AddTabAction
import mozilla.components.browser.state.action.TrackingProtectionAction
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.state.CustomTabConfig
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.HitResult
import mozilla.components.concept.engine.content.blocking.Tracker
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.concept.engine.media.RecordingDevice
import mozilla.components.concept.engine.permission.PermissionRequest
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
    val contextId: String? = null,
    delegate: Observable<Observer> = ObserverRegistry()
) : Observable<Session.Observer> by delegate {
    /**
     * Holder for keeping a reference to an engine session and its observer to update this session
     * object.
     */
    internal val engineSessionHolder = EngineSessionHolder()

    // For migration purposes every `Session` has a reference to the `BrowserStore` (if used) in order to dispatch
    // actions to it when the `Session` changes.
    internal var store: BrowserStore? = null

    /**
     * Id of parent session, usually refer to the session which created this one. The clue to indicate if this session
     * is terminated, which target we should go back.
     */
    internal var parentId: String? = null

    /**
     * Interface to be implemented by classes that want to observe a session.
     */
    interface Observer {
        fun onUrlChanged(session: Session, url: String) = Unit
        fun onTitleChanged(session: Session, title: String) = Unit
        fun onProgress(session: Session, progress: Int) = Unit
        fun onLoadingStateChanged(session: Session, loading: Boolean) = Unit
        fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) = Unit
        fun onLoadRequest(
            session: Session,
            url: String,
            triggeredByRedirect: Boolean,
            triggeredByWebContent: Boolean
        ) = Unit
        fun onSearch(session: Session, searchTerms: String) = Unit
        fun onSecurityChanged(session: Session, securityInfo: SecurityInfo) = Unit
        fun onCustomTabConfigChanged(session: Session, customTabConfig: CustomTabConfig?) = Unit
        fun onWebAppManifestChanged(session: Session, manifest: WebAppManifest?) = Unit
        fun onTrackerBlockingEnabledChanged(session: Session, blockingEnabled: Boolean) = Unit
        fun onTrackerBlocked(session: Session, tracker: Tracker, all: List<Tracker>) = Unit
        fun onTrackerLoaded(session: Session, tracker: Tracker, all: List<Tracker>) = Unit
        fun onLongPress(session: Session, hitResult: HitResult): Boolean = false
        fun onFindResult(session: Session, result: FindResult) = Unit
        fun onDesktopModeChanged(session: Session, enabled: Boolean) = Unit
        fun onFullScreenChanged(session: Session, enabled: Boolean) = Unit
        /**
         * @param layoutInDisplayCutoutMode value of defined in https://developer.android.com/reference/android/view/WindowManager.LayoutParams#layoutInDisplayCutoutMode
         */
        fun onMetaViewportFitChanged(session: Session, layoutInDisplayCutoutMode: Int) = Unit
        fun onThumbnailChanged(session: Session, bitmap: Bitmap?) = Unit
        fun onContentPermissionRequested(session: Session, permissionRequest: PermissionRequest): Boolean = false
        fun onAppPermissionRequested(session: Session, permissionRequest: PermissionRequest): Boolean = false
        fun onCrashStateChanged(session: Session, crashed: Boolean) = Unit
        fun onRecordingDevicesChanged(session: Session, devices: List<RecordingDevice>) = Unit
        fun onLaunchIntentRequest(session: Session, url: String, appIntent: Intent?) = Unit
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
         * Created to handle an ACTION_SEARCH and ACTION_WEB_SEARCH intent
         */
        ACTION_SEARCH,

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
        USER_ENTERED,

        /**
         * This session was restored
         */
        RESTORED
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
    var url: String by Delegates.observable(initialUrl) { _, old, new ->
        if (notifyObservers(old, new) { onUrlChanged(this@Session, new) }) {
            store?.syncDispatch(UpdateUrlAction(id, new))
        }
    }

    /**
     * The title of the currently displayed website changed.
     */
    var title: String by Delegates.observable("") { _, old, new ->
        if (notifyObservers(old, new) { onTitleChanged(this@Session, new) }) {
            store?.syncDispatch(UpdateTitleAction(id, new))
        }
    }

    /**
     * The progress loading the current URL.
     */
    var progress: Int by Delegates.observable(0) { _, old, new ->
        if (notifyObservers(old, new) { onProgress(this@Session, new) }) {
            store?.syncDispatch(UpdateProgressAction(id, new))
        }
    }

    /**
     * Loading state, true if this session's url is currently loading, otherwise false.
     */
    var loading: Boolean by Delegates.observable(false) { _, old, new ->
        if (notifyObservers(old, new) { onLoadingStateChanged(this@Session, new) }) {
            store?.syncDispatch(UpdateLoadingStateAction(id, new))
        }
    }

    /**
     * Navigation state, true if there's an history item to go back to, otherwise false.
     */
    var canGoBack: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onNavigationStateChanged(this@Session, new, canGoForward) }
        store?.syncDispatch(UpdateBackNavigationStateAction(id, canGoBack))
    }

    /**
     * Navigation state, true if there's an history item to go forward to, otherwise false.
     */
    var canGoForward: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onNavigationStateChanged(this@Session, canGoBack, new) }
        store?.syncDispatch(UpdateForwardNavigationStateAction(id, canGoForward))
    }

    /**
     * The currently / last used search terms (or an empty string).
     */
    var searchTerms: String by Delegates.observable("") { _, _, new ->
        notifyObservers { onSearch(this@Session, new) }
        store?.syncDispatch(UpdateSearchTermsAction(id, new))
    }

    /**
     * Set when a load request is received, indicating if the request came from web content, or via a redirect.
     */
    var loadRequestMetadata: LoadRequestMetadata by Delegates.observable(LoadRequestMetadata.blank) { _, _, new ->
        notifyObservers {
            onLoadRequest(
                this@Session,
                new.url,
                new.isSet(LoadRequestOption.REDIRECT),
                new.isSet(LoadRequestOption.WEB_CONTENT)
            )
        }
    }

    /**
     * Set when a launch intent is received.
     */
    var launchIntentMetadata: LaunchIntentMetadata by Delegates.observable(LaunchIntentMetadata.blank) { _, _, new ->
        notifyObservers {
            onLaunchIntentRequest(
                this@Session,
                new.url,
                new.appIntent
            )
        }
    }

    /**
     * Security information indicating whether or not the current session is
     * for a secure URL, as well as the host and SSL certificate authority, if applicable.
     */
    var securityInfo: SecurityInfo by Delegates.observable(SecurityInfo()) { _, old, new ->
        notifyObservers(old, new) { onSecurityChanged(this@Session, new) }
        store?.syncDispatch(UpdateSecurityInfoAction(id, new.toSecurityInfoState()))
    }

    /**
     * Configuration data in case this session is used for a Custom Tab.
     */
    var customTabConfig: CustomTabConfig? by Delegates.observable<CustomTabConfig?>(null) { _, old, new ->
        notifyObservers { onCustomTabConfigChanged(this@Session, new) }

        // The custom tab config is set to null when we're migrating custom
        // tabs to regular tabs, so we have to dispatch the corresponding
        // browser actions to keep the store in sync.
        if (old != new && new == null) {
            store?.syncDispatch(RemoveCustomTabAction(id))
            store?.syncDispatch(AddTabAction(toTabSessionState()))
            engineSessionHolder.engineSession?.let { engineSession ->
                store?.syncDispatch(EngineAction.LinkEngineSessionAction(id, engineSession))
            }
        }
    }

    /**
     * The Web App Manifest for the currently visited page (or null).
     */
    var webAppManifest: WebAppManifest? by Delegates.observable<WebAppManifest?>(null) { _, old, new ->
        notifyObservers { onWebAppManifestChanged(this@Session, new) }

        if (old != new) {
            val action = if (new != null) {
                UpdateWebAppManifestAction(id, new)
            } else {
                RemoveWebAppManifestAction(id)
            }
            store?.syncDispatch(action)
        }
    }

    /**
     * Tracker blocking state, true if blocking trackers is enabled, otherwise false.
     */
    var trackerBlockingEnabled: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onTrackerBlockingEnabledChanged(this@Session, new) }

        store?.syncDispatch(TrackingProtectionAction.ToggleAction(id, new))
    }

    /**
     * List of [Tracker]s that have been blocked in this session.
     */
    var trackersBlocked: List<Tracker> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) {
            if (new.isNotEmpty()) {
                onTrackerBlocked(this@Session, new.last(), new)
            }
        }

        if (new.isEmpty()) {
            // From `EngineObserver` we can assume that this means the trackers have been cleared.
            // The `ClearTrackersAction` will also clear the loaded trackers list. That is always
            // the case when this list is cleared from `EngineObserver`. For the sake of migrating
            // to browser-state we assume that no other code changes the tracking properties.
            store?.syncDispatch(TrackingProtectionAction.ClearTrackersAction(id))
        } else {
            // `EngineObserver` always adds new trackers to the end of the list. So we just dispatch
            // an action for the last item in the list.
            store?.syncDispatch(TrackingProtectionAction.TrackerBlockedAction(id, new.last()))
        }
    }

    /**
     * List of [Tracker]s that could be blocked but have been loaded in this session.
     */
    var trackersLoaded: List<Tracker> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) {
            if (new.isNotEmpty()) {
                onTrackerLoaded(this@Session, new.last(), new)
            }
        }

        if (new.isNotEmpty()) {
            // The empty case is already handled by the `trackersBlocked` property since both
            // properties are always cleared together by `EngineObserver`.
            // `EngineObserver` always adds new trackers to the end of the list. So we just dispatch
            // an action for the last item in the list.
            store?.syncDispatch(TrackingProtectionAction.TrackerLoadedAction(id, new.last()))
        }
    }

    /**
     * List of results of that latest "find in page" operation.
     */
    var findResults: List<FindResult> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) {
            if (new.isNotEmpty()) {
                onFindResult(this@Session, new.last())
            }
        }

        if (new.isNotEmpty()) {
            store?.syncDispatch(AddFindResultAction(id, new.last().toFindResultState()))
        } else {
            store?.syncDispatch(ClearFindResultsAction(id))
        }
    }

    /**
     * The target of the latest long click operation.
     */
    var hitResult: Consumable<HitResult> by Delegates.vetoable(Consumable.empty()) { _, _, result ->
        store?.let {
            val hitResult = result.peek()
            if (hitResult == null) {
                it.syncDispatch(ConsumeHitResultAction(id))
            } else {
                it.syncDispatch(UpdateHitResultAction(id, hitResult))
                result.onConsume { it.syncDispatch(ConsumeHitResultAction(id)) }
            }
        }

        val consumers = wrapConsumers<HitResult> { onLongPress(this@Session, it) }
        !result.consumeBy(consumers)
    }

    /**
     * The target of the latest thumbnail.
     */
    var thumbnail: Bitmap? by Delegates.observable<Bitmap?>(null) { _, _, new ->
        notifyObservers { onThumbnailChanged(this@Session, new) }

        val action = if (new != null) UpdateThumbnailAction(id, new) else RemoveThumbnailAction(id)
        store?.syncDispatch(action)
    }

    /**
     * Desktop Mode state, true if the desktop mode is requested, otherwise false.
     */
    var desktopMode: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onDesktopModeChanged(this@Session, new) }
    }

    /**
     * Exits fullscreen mode if it's in that state.
     */
    var fullScreenMode: Boolean by Delegates.observable(false) { _, old, new ->
        if (notifyObservers(old, new) { onFullScreenChanged(this@Session, new) }) {
            store?.syncDispatch(FullScreenChangedAction(id, new))
        }
    }

    /**
     * Display cutout mode state.
     */
    var layoutInDisplayCutoutMode: Int by Delegates.observable(0) { _, _, new ->
        notifyObservers { onMetaViewportFitChanged(this@Session, new) }
        store?.syncDispatch(ViewportFitChangedAction(id, new))
    }

    /**
     * An icon for the currently visible page.
     */
    val icon: Bitmap?
        // This is a workaround until all callers are migrated to use browser-state. Until then
        // we try to lookup the icon from an attached BrowserStore if possible.
        get() = store?.state?.findTabOrCustomTab(id)?.content?.icon

    /**
     * [Consumable] permission request from web content. A [PermissionRequest]
     * must be consumed i.e. either [PermissionRequest.grant] or
     * [PermissionRequest.reject] must be called. A content permission request
     * can also be cancelled, which will result in a new empty [Consumable].
     */
    var contentPermissionRequest: Consumable<PermissionRequest> by Delegates.vetoable(Consumable.empty()) {
        _, _, request ->
            val consumers = wrapConsumers<PermissionRequest> { onContentPermissionRequested(this@Session, it) }
            !request.consumeBy(consumers)
    }

    /**
     * [Consumable] permission request for the app. A [PermissionRequest]
     * must be consumed i.e. either [PermissionRequest.grant] or
     * [PermissionRequest.reject] must be called.
     */
    var appPermissionRequest: Consumable<PermissionRequest> by Delegates.vetoable(Consumable.empty()) {
        _, _, request ->
            val consumers = wrapConsumers<PermissionRequest> { onAppPermissionRequested(this@Session, it) }
            !request.consumeBy(consumers)
    }

    /**
     * Whether this [Session] has crashed.
     *
     * In conjunction with a `concept-engine` implementation that uses a multi-process architecture, single sessions
     * can crash without crashing the whole app.
     *
     * A crashed session may still be operational (since the underlying engine implementation has recovered its content
     * process), but further action may be needed to restore the last state before the session has crashed (if desired).
     */
    var crashed: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onCrashStateChanged(this@Session, new) }
    }

    /**
     * List of recording devices (e.g. camera or microphone) currently in use by web content.
     */
    var recordingDevices: List<RecordingDevice> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) { onRecordingDevicesChanged(this@Session, new) }
    }

    /**
     * Returns whether or not this session is used for a Custom Tab.
     */
    fun isCustomTabSession() = customTabConfig != null

    /**
     * Helper method to notify observers only if a value changed.
     *
     * @param old the previous value of a session property
     * @param new the current (new) value of a session property
     *
     * @return true if the observers where notified (the values changed), otherwise false.
     */
    private fun notifyObservers(old: Any?, new: Any?, block: Observer.() -> Unit): Boolean {
        return if (old != new) {
            notifyObservers(block)
            true
        } else {
            false
        }
    }

    /**
     * Returns true if this [Session] has a parent [Session].
     *
     * A [Session] can have a parent [Session] if one was provided when calling [SessionManager.add]. The parent
     * [Session] is usually the [Session] the new [Session] was opened from - like opening a new tab from a link
     * context menu ("Open in new tab").
     */
    val hasParentSession: Boolean
        get() = parentId != null

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

    override fun toString(): String {
        return "Session($id, $url)"
    }
}
