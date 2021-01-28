/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.ktor.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.lib.ktor.fetch.ext.toKtorEngine

/**
 * Factory for creating an [HttpClientEngine] using GeckoView running in the test context.
 */
object Gecko : HttpClientEngineFactory<FetchEngineConfig> {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    override fun create(block: FetchEngineConfig.() -> Unit): HttpClientEngine {
        val client = runBlocking(Dispatchers.Main) { GeckoViewFetchClient(context) }
        return client.toKtorEngine()
    }
}
