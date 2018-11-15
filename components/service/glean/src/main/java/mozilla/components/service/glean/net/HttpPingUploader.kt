/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.net

import android.support.annotation.VisibleForTesting
import mozilla.components.service.glean.BuildConfig
import mozilla.components.service.glean.config.Configuration
import mozilla.components.support.base.log.logger.Logger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * A simple ping Uploader, which implements a "send once" policy, never
 * storing or attempting to send the ping again.
 */
class HttpPingUploader(configuration: Configuration) : PingUploader {
    private val config = configuration
    private val logger = Logger("glean/HttpPingUploader")

    /**
     * Synchronously upload a ping to Mozilla servers.
     * Note that the `X-Client-Type`: `Glean` and `X-Client-Version`: <SDK version>
     * headers are added to the HTTP request in addition to the UserAgent. This allows
     * us to easily handle pings coming from glean on the legacy Mozilla pipeline.
     *
     * @param path the URL path to append to the server address
     * @param data the serialized text data to send
     *
     * @return true if the ping was correctly dealt with (sent successfully
     *         or faced an unrecoverable error), false if there was a recoverable
     *         error callers can deal with.
     */
    @Suppress("ReturnCount", "MagicNumber")
    override fun upload(path: String, data: String): Boolean {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(config.serverEndpoint, path)
            connection.requestMethod = "POST"
            connection.connectTimeout = config.connectionTimeout
            connection.readTimeout = config.readTimeout
            connection.doOutput = true

            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("User-Agent", config.userAgent)
            connection.setRequestProperty("Date", createDateHeaderValue())

            // Add headers for supporting the legacy pipeline.
            connection.setRequestProperty("X-Client-Type", "Glean")
            connection.setRequestProperty("X-Client-Version", BuildConfig.LIBRARY_VERSION)

            val responseCode = doUpload(connection, data)

            logger.debug("Ping upload: $responseCode")

            when (responseCode) {
                in HttpURLConnection.HTTP_OK..(HttpURLConnection.HTTP_OK + 99) -> {
                    // Known success errors (2xx):
                    // 200 - OK. Request accepted into the pipeline.

                    // We treat all success codes as successful upload even though we only expect 200.
                    logger.debug("Ping successfully sent ($responseCode)")
                    return true
                }
                in HttpURLConnection.HTTP_BAD_REQUEST..(HttpURLConnection.HTTP_BAD_REQUEST + 99) -> {
                    // Known client (4xx) errors:
                    // 404 - not found - POST/PUT to an unknown namespace
                    // 405 - wrong request type (anything other than POST/PUT)
                    // 411 - missing content-length header
                    // 413 - request body too large (Note that if we have badly-behaved clients that
                    //       retry on 4XX, we should send back 202 on body/path too long).
                    // 414 - request path too long (See above)

                    // Something our client did is not correct. It's unlikely that the client is going
                    // to recover from this by re-trying again, so we just log and error and report a
                    // successful upload to the service.
                    logger.error("Server returned client error code: $responseCode")
                    return true
                }
                else -> {
                    // Known other errors:
                    // 500 - internal error

                    // For all other errors we log a warning an try again at a later time.
                    logger.warn("Server returned response code: $responseCode")
                    return false
                }
            }
        } catch (e: MalformedURLException) {
            // There's nothing we can do to recover from this here. So let's just log an error and
            // notify the service that this job has been completed - even though we didn't upload
            // anything to the server.
            logger.error("Could not upload telemetry due to malformed URL", e)
            return true
        } catch (e: IOException) {
            logger.warn("IOException while uploading ping", e)
            return false
        } finally {
            connection?.disconnect()
        }
    }

    @Throws(IOException::class)
    fun doUpload(connection: HttpURLConnection, data: String): Int {
        connection.outputStream.bufferedWriter().use {
            it.write(data)
            it.flush()
        }

        return connection.responseCode
    }

    @VisibleForTesting @Throws(IOException::class)
    fun openConnection(endpoint: String, path: String): HttpURLConnection {
        return URL(endpoint + path).openConnection() as HttpURLConnection
    }
}
