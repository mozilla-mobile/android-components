/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.utils

import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager

/**
 * This class is a combination of [Session.Observer] and [SessionManager.Observer]. It observers all [Session] instances
 * that get added to the [SessionManager] and automatically unsubscribes from [Session] instances that get removed.
 *
 * @property sessionManager the application's session manager
 * @property sessionObserver observer to register on all sessions
 * @property includeCustomTabs if true, registers the observer on custom tabs as well.
 */
class AllSessionsObserver(
    private val sessionManager: SessionManager,
    private val sessionObserver: Observer,
    private val includeCustomTabs: Boolean = true
) {
    private val observer = SessionManagerObserver(this)
    private val registeredSessions = mutableSetOf<Session>()

    fun start() {
        registerToAllSessions()
        sessionManager.register(observer)
    }

    fun stop() {
        sessionManager.unregister(observer)
        unregisterAllSessions()
    }

    internal fun registerToAllSessions() {
        val all = if (includeCustomTabs) sessionManager.all else sessionManager.sessions
        all.forEach(::registerSession)
    }

    internal fun registerSession(session: Session) {
        if (session !in registeredSessions) {
            session.register(sessionObserver)
            registeredSessions.add(session)
            sessionObserver.onRegisteredToSession(session)
        }
    }

    internal fun unregisterSession(session: Session) {
        registeredSessions.remove(session)
        session.unregister(sessionObserver)
        sessionObserver.onUnregisteredFromSession(session)
    }

    internal fun unregisterAllSessions() {
        registeredSessions.forEach(::unregisterSession)
    }

    interface Observer : Session.Observer {
        fun onRegisteredToSession(session: Session) = Unit
        fun onUnregisteredFromSession(session: Session) = Unit
    }

    private class SessionManagerObserver(
        val parent: AllSessionsObserver
    ) : SessionManager.Observer {
        override fun onSessionAdded(session: Session) = parent.registerSession(session)

        override fun onSessionRemoved(session: Session) = parent.unregisterSession(session)

        override fun onAllSessionsRemoved() = parent.unregisterAllSessions()

        override fun onSessionsRestored() = parent.registerToAllSessions()
    }
}
