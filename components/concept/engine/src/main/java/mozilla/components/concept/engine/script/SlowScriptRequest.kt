/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.script

/**
 * Interface for classes that want to handle the slowScript callback
 */
interface SlowScriptRequest {
    /**
     * Handle the continue response for the slow script
     */
    fun continueSlowScript()

    /**
     * Handle the stop response for the slow script
     */
    fun stopSlowScript()
}
