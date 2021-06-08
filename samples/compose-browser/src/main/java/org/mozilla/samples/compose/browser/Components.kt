/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.compose.browser

import android.content.Context
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.session.SessionUseCases

/**
 * Global components of the sample browser.
 */
class Components(
    context: Context
) {
    val engine: Engine by lazy { GeckoEngine(context) }

    val store: BrowserStore by lazy {
        BrowserStore(
            middleware = EngineMiddleware.create(engine)
        )
    }

    val sessionUseCases by lazy { SessionUseCases(store) }
}
