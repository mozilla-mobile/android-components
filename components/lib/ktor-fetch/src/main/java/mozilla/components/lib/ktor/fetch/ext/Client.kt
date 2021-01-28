/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.ktor.fetch.ext

import io.ktor.client.engine.HttpClientEngine
import mozilla.components.concept.fetch.Client
import mozilla.components.lib.ktor.fetch.FetchEngineConfig
import mozilla.components.lib.ktor.fetch.FetchEngine

/**
 * Creates a [HttpClientEngine] (Ktor) from this [Client] (`concept-fetch`).
 */
fun Client.toKtorEngine(
    block: FetchEngineConfig.() -> Unit = {}
) = FetchEngine(
    client = this,
    config = FetchEngineConfig().apply(block)
)
