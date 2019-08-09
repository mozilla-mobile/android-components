/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.engine

import mozilla.components.browser.session.Session
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSessionState

/**
 * Used for linking a [Session] to an [EngineSession] or the [EngineSessionState] to create an [EngineSession] from it.
 * The attached [EngineObserver] is used to update the [Session] whenever the [EngineSession] emits events.
 *
 * To ensure atomicity and sane visibility of enclosed values, make sure to synchronize access to
 * instances of this class if they are to be used in a multi-threaded context.
 */
internal class EngineSessionHolder {
    var engineSession: EngineSession? = null
    var engineObserver: EngineObserver? = null
    var engineSessionState: EngineSessionState? = null
}
