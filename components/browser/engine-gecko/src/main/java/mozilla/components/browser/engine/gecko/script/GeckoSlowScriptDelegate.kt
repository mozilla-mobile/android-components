/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.script

import mozilla.components.browser.engine.gecko.GeckoEngineSession
import mozilla.components.concept.engine.script.SlowScriptRequest
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.SlowScriptResponse
import org.mozilla.geckoview.SlowScriptResponse.CONTINUE
import org.mozilla.geckoview.SlowScriptResponse.STOP

/**
 * Gecko-based implementation of [SlowScriptRequest].
 *
 * @property geckoEngineSession The engine session that initiated the callback.
 * @property geckoResponse The async response containing [SlowScriptResponse] to be returned
 * to Gecko.
 */
class GeckoSlowScriptDelegate(
    private val geckoEngineSession: GeckoEngineSession,
    private val geckoResponse: GeckoResult<SlowScriptResponse>,
) : SlowScriptRequest {

    /**
     * Allow the slow script to continue
     */
    override fun continueSlowScript() {
        geckoResponse.complete(CONTINUE)
        geckoEngineSession.notifyObservers { onSlowLoadingScriptConsumed() }
    }

    /**
     * Stop the slow script from running
     */
    override fun stopSlowScript() {
        geckoResponse.complete(STOP)
        geckoEngineSession.notifyObservers { onSlowLoadingScriptConsumed() }
    }
}
