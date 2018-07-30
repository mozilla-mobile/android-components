/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine

import android.support.annotation.CallSuper
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

/**
 * Class representing a single engine session.
 *
 * In browsers usually a session corresponds to a tab.
 */
abstract class EngineSession(
    private val delegate: Observable<EngineSession.Observer> = ObserverRegistry()
) : Observable<EngineSession.Observer> by delegate {
    /**
     * Interface to be implemented by classes that want to observe this engine session.
     */
    interface Observer {
        fun onLocationChange(url: String) = Unit
        fun onTitleChange(title: String) = Unit
        fun onProgress(progress: Int) = Unit
        fun onLoadingStateChange(loading: Boolean) = Unit
        fun onNavigationStateChange(canGoBack: Boolean? = null, canGoForward: Boolean? = null) = Unit
        fun onSecurityChange(secure: Boolean, host: String? = null, issuer: String? = null) = Unit
        fun onTrackerBlockingEnabledChange(enabled: Boolean) = Unit
        fun onTrackerBlocked(url: String) = Unit

        @Suppress("LongParameterList")
        fun onExternalResource(
            url: String,
            fileName: String? = null,
            contentLength: Long? = null,
            contentType: String? = null,
            cookie: String? = null,
            userAgent: String? = null
        ) = Unit
    }

    /**
     * Represents a tracking protection policy which is a combination of
     * tracker categories that should be blocked.
     */
    class TrackingProtectionPolicy(val categories: Int) {
        companion object {
            internal const val NONE: Int = 0
            const val AD: Int = 1 shl 0
            const val ANALYTICS: Int = 1 shl 1
            const val SOCIAL: Int = 1 shl 2
            const val CONTENT: Int = 1 shl 3
            internal const val ALL: Int = (1 shl 4) - 1

            fun none(): TrackingProtectionPolicy = TrackingProtectionPolicy(NONE)
            fun all(): TrackingProtectionPolicy = TrackingProtectionPolicy(ALL)
            fun select(vararg categories: Int): TrackingProtectionPolicy =
                TrackingProtectionPolicy(categories.sum())
        }
    }

    /**
     * Loads the given URL.
     */
    abstract fun loadUrl(url: String)

    /**
     * Reloads the current URL.
     */
    abstract fun reload()

    /**
     * Navigates back in the history of this session.
     */
    abstract fun goBack()

    /**
     * Navigates forward in the history of this session.
     */
    abstract fun goForward()

    /**
     * Saves and returns the engine state. Engine implementations are not required
     * to persist the state anywhere else than in the returned map. Engines that
     * already provide a serialized state can use a single entry in this map to
     * provide this state. The only requirement is that the same map can be used
     * to restore the original state. See [restoreState] and the specific
     * engine implementation for details.
     */
    abstract fun saveState(): Map<String, Any>

    /**
     * Restores the engine state as provided by [saveState].
     *
     * @param state state retrieved from [saveState]
     */
    abstract fun restoreState(state: Map<String, Any>)

    /**
     * Enables tracking protection for this engine session.
     *
     * @param policy the tracking protection policy to use, defaults to blocking all trackers.
     */
    open fun enableTrackingProtection(policy: TrackingProtectionPolicy = TrackingProtectionPolicy.all()) = Unit

    /**
     * Disables tracking protection for this engine session.
     */
    open fun disableTrackingProtection() = Unit

    /**
     * Close the session. This may free underlying objects. Call this when you are finished using
     * this session.
     */
    @CallSuper
    fun close() = delegate.unregisterObservers()
}
