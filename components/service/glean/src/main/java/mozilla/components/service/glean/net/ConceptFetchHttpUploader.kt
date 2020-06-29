/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.net

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Header
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.toMutableHeaders
import mozilla.components.support.base.log.logger.Logger
import mozilla.telemetry.glean.net.HeadersList
import mozilla.telemetry.glean.net.HttpResponse
import mozilla.telemetry.glean.net.RecoverableFailure
import mozilla.telemetry.glean.net.UploadResult
import java.io.IOException
import java.util.concurrent.TimeUnit
import mozilla.telemetry.glean.net.PingUploader as CorePingUploader

typealias PingUploader = CorePingUploader

/**
 * A simple ping Uploader, which implements a "send once" policy, never
 * storing or attempting to send the ping again. This uses Android Component's
 * `concept-fetch`.
 */
class ConceptFetchHttpUploader(
    internal val client: Lazy<Client>
) : PingUploader {
    private val logger = Logger("glean/ConceptFetchHttpUploader")

    companion object {
        // The timeout, in milliseconds, to use when connecting to the server.
        const val DEFAULT_CONNECTION_TIMEOUT = 10000L
        // The timeout, in milliseconds, to use when reading from the server.
        const val DEFAULT_READ_TIMEOUT = 30000L

        /**
         * Export a constructor that is usable from Java.
         *
         * This looses the lazyness of creating the `client`.
         */
        @JvmStatic
        fun fromClient(client: Client): ConceptFetchHttpUploader {
            return ConceptFetchHttpUploader(lazy { client })
        }
    }

    /**
     * Synchronously upload a ping to a server.
     *
     * @param url the URL path to upload the data to
     * @param data the serialized text data to send
     * @param headers a [HeadersList] containing String to String [Pair] with
     *        the first entry being the header name and the second its value.
     *
     * @return true if the ping was correctly dealt with (sent successfully
     *         or faced an unrecoverable error), false if there was a recoverable
     *         error callers can deal with.
     */
    override fun upload(url: String, data: ByteArray, headers: HeadersList): UploadResult {
        val request = buildRequest(url, data, headers)

        return try {
            performUpload(client.value, request)
        } catch (e: IOException) {
            logger.warn("IOException while uploading ping", e)
            RecoverableFailure
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun buildRequest(
        url: String,
        data: ByteArray,
        headers: HeadersList
    ): Request {
        val conceptHeaders = headers.map { (name, value) -> Header(name, value) }.toMutableHeaders()

        return Request(
            url = url,
            method = Request.Method.POST,
            connectTimeout = Pair(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS),
            readTimeout = Pair(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS),
            headers = conceptHeaders,
            // Make sure we are not sending cookies. Unfortunately, HttpURLConnection doesn't
            // offer a better API to do that, so we nuke all cookies going to our telemetry
            // endpoint.
            cookiePolicy = Request.CookiePolicy.OMIT,
            body = Request.Body(data.inputStream())
        )
    }

    @Throws(IOException::class)
    internal fun performUpload(client: Client, request: Request): UploadResult {
        logger.debug("Submitting ping to: ${request.url}")
        client.fetch(request).use { response ->
            return HttpResponse(response.status)
        }
    }
}
