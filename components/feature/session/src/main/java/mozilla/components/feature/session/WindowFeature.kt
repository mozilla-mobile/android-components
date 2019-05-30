/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session

import mozilla.components.browser.session.SelectionAwareSessionObserver
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.support.base.feature.LifecycleAwareFeature

/**
 * Feature implementation for handling window requests.
 *
 * @param engine [Engine] used to create new [Session] instances.
 * @param sessionManager Session Manager informed of window changes.
 * @param sessionId ID of specific session to observe.
 */
class WindowFeature(
    private val engine: Engine,
    private val sessionManager: SessionManager,
    sessionId: String? = null
) : SelectionAwareSessionObserver(sessionManager, sessionId), LifecycleAwareFeature {

    override fun onOpenWindowRequested(session: Session, windowRequest: WindowRequest): Boolean {
        val newSession = Session(windowRequest.url, session.private)
        val newEngineSession = engine.createSession(session.private)
        windowRequest.prepare(newEngineSession)

        sessionManager.add(newSession, true, newEngineSession, parent = session)
        windowRequest.start()
        return true
    }

    override fun onCloseWindowRequested(session: Session, windowRequest: WindowRequest): Boolean {
        sessionManager.remove(session)
        return true
    }
}
