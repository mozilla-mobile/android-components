/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.trackingprotection

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine

class TrackingProtectionExceptionListUseCases(
    engine: Engine,
    sessionManager: SessionManager
) {

    class AddUseCase(
        private val engine: Engine,
        private val sessionManager: SessionManager
    ) {
        operator fun invoke(session: Session) {
          val engineSession =  requireNotNull(sessionManager.getEngineSession(session))
            engine.trackingProtectionExceptionStore.add(engineSession)
        }
    }

    class RemoveUseCase(
        private val engine: Engine,
        private val sessionManager: SessionManager
    ) {
        operator fun invoke(session: Session) {
            val engineSession =  requireNotNull(sessionManager.getEngineSession(session))
            engine.trackingProtectionExceptionStore.remove(engineSession)
        }
    }

    class GetAllUseCase(
        private val engine: Engine,
        private val sessionManager: SessionManager
    ) {
        operator fun invoke(onFinish: (List<String>) -> Unit) {
            engine.trackingProtectionExceptionStore.getAll(onFinish)
        }
    }

    class RemoveAllUseCase(
        private val engine: Engine) {
        operator fun invoke() {
            engine.trackingProtectionExceptionStore.removeAll()
        }
    }
}
