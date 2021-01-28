/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.ktor.fetch

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientTests {
    @Test
    fun simpleGet() {
        val server = MockWebServer()
        server.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("Hello World!"))

        HttpClient(Gecko).use { client ->
            val content = runBlocking { client.get<String>(server.url("/").toString()) }
            assertEquals("Hello World!", content)
        }
    }
}
