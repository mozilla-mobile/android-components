/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.Settings

/**
 * Contains use cases related to engine [Settings].
 *
 * @param engine reference to the application's browser [Engine].
 * @param sessionManager the application's [SessionManager].*
 */
class SettingsUseCases(
    engine: Engine,
    sessionManager: SessionManager
) {

    /**
     * Use case to update a setting and then change all
     * active browsing sessions to use the new setting.
     * @property engine reference to the application's browser [Engine].
     * @property sessionManager the application's [SessionManager]. Used to query the active sessions.
     */
    abstract class UpdateSettingUseCase<T> internal constructor(
        private val engine: Engine,
        private val sessionManager: SessionManager
    ) {

        /**
         * Updates the engine setting and all active sessions.
         *
         * @param value The new setting value
         */
        operator fun invoke(value: T) {
            update(engine.settings, value)
            with(sessionManager) {
                sessions.forEach {
                    getEngineSession(it)?.let { session -> forEachSession(session, value) }
                }
            }
            engine.clearSpeculativeSession()
        }

        /**
         * Called to update a [Settings] object using the value from the invoke call.
         */
        protected abstract fun update(settings: Settings, value: T)

        /**
         * Called to update an active session. Defaults to updating the session's [Settings] object.
         */
        protected open fun forEachSession(session: EngineSession, value: T) {
            update(session.settings, value)
        }
    }

    /**
     * Updates the tracking protection policy to the given policy value when invoked.
     * All active sessions are automatically updated with the new policy.
     */
    class UpdateTrackingProtectionUseCase internal constructor(
        engine: Engine,
        sessionManager: SessionManager
    ) : UpdateSettingUseCase<TrackingProtectionPolicy>(engine, sessionManager) {

        override fun update(settings: Settings, value: TrackingProtectionPolicy) {
            settings.trackingProtectionPolicy = value
        }

        override fun forEachSession(session: EngineSession, value: TrackingProtectionPolicy) {
            session.enableTrackingProtection(value)
        }
    }

    val updateTrackingProtection: UpdateTrackingProtectionUseCase by lazy {
        UpdateTrackingProtectionUseCase(engine, sessionManager)
    }
}
