/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket.helpers

import mozilla.components.concept.fetch.Response
import mozilla.components.support.test.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

/**
 * A collection of helper functions to generate mock [Response]s.
 */
object MockResponses {

    fun getError(): Response = getMockResponse(404)

    fun getSuccess(): Response = getMockResponse(200).also {
        // A successful response must contain a body.
        val body = mock(Response.Body::class.java).also { body ->
            `when`(body.string()).thenReturn("{}")
        }
        `when`(it.body).thenReturn(body)
    }

    fun getMockResponse(status: Int): Response = mock<Response>().also {
        `when`(it.status).thenReturn(status)
    }
}
