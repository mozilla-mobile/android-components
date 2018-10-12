/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.loader

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.Log
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.icons.IconResponse
import mozilla.components.browser.icons.decoders.FaviconDecoder
import mozilla.components.browser.icons.decoders.LoadFaviconResult
import mozilla.components.browser.icons.storage.FailureCache
import mozilla.components.browser.icons.util.ProxySelector
import mozilla.components.support.ktx.kotlin.*

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.util.HashSet

/**
 * This loader implementation downloads icons from http(s) URLs.
 */
class IconDownloader : IconLoader {

    override fun load(request: IconRequest): IconResponse? {
        if (request.shouldSkipNetwork()) {
            return null
        }

        val iconUrl = request.bestIcon.url

        if (!iconUrl.isHttpOrHttps()) {
            return null
        }

        try {
            val result = downloadAndDecodeImage(request.context, iconUrl) ?: return null

            val bitmap = result.getBestBitmap(request.targetSize) ?: return null

            return IconResponse.createFromNetwork(bitmap, iconUrl)
        } catch (e: Exception) {
            Log.e(LOGTAG, "Error reading favicon", e)
        } catch (e: OutOfMemoryError) {
            Log.e(LOGTAG, "Insufficient memory to process favicon")
        }

        return null
    }

    /**
     * Download the Favicon from the given URL and pass it to the decoder function.
     *
     * @param targetFaviconURL URL of the favicon to download.
     * @return A LoadFaviconResult containing the bitmap(s) extracted from the downloaded file, or
     * null if no or corrupt data was received.
     */
    fun downloadAndDecodeImage(context: Context, targetFaviconURL: String): LoadFaviconResult? {
        // Try the URL we were given.
        val connection = tryDownload(targetFaviconURL) ?: return null

        // Decode the image from the fetched response.
        return connection.inputStream.use {
            decodeImageFromResponse(context, it, connection.getHeaderFieldInt("Content-Length", -1))
        }.also {
            connection.disconnect()
        }

//        // Decode the image from the fetched response.
//        try {
//            stream = connection.inputStream
//            return decodeImageFromResponse(context, stream, connection.getHeaderFieldInt("Content-Length", -1))
//        } catch (e: IOException) {
//            Log.d(LOGTAG, "IOException while reading and decoding ixon", e)
//            return null
//        } finally {
//            // Close the stream and free related resources.
//            // TODO: Use try-with-resources in Kotlin to close this stream.
//            //IOUtils.safeStreamClose(stream);
//            connection.disconnect()
//        }
    }

    /**
     * Helper method for trying the download request to grab a Favicon.
     *
     * @param faviconURI URL of Favicon to try and download
     * @return The HttpResponse containing the downloaded Favicon if successful, null otherwise.
     */
    private fun tryDownload(faviconURI: String): HttpURLConnection? {
        val visitedLinkSet = HashSet<String>()
        visitedLinkSet.add(faviconURI)
        return tryDownloadRecurse(faviconURI, visitedLinkSet)
    }

    /**
     * Try to download from the favicon URL and recursively follow redirects.
     */
    private fun tryDownloadRecurse(faviconURI: String, visited: HashSet<String>): HttpURLConnection? {
        if (visited.size == MAX_REDIRECTS_TO_FOLLOW) {
            return null
        }

        var connection: HttpURLConnection? = null

        try {
            connection = connectTo(faviconURI)

            // Was the response a failure?
            val status = connection.responseCode

            // Handle HTTP status codes requesting a redirect.
            if (status in 300..399) {
                val newURI = connection.getHeaderField("Location")

                // Handle mad web servers.
                try {
                    if (newURI == null || newURI == faviconURI) {
                        return null
                    }

                    if (visited.contains(newURI)) {
                        // Already been redirected here - abort.
                        return null
                    }

                    visited.add(newURI)
                } finally {
                    connection.disconnect()
                }

                return tryDownloadRecurse(newURI, visited)
            }

            if (status >= 400) {
                // Client or Server error. Let's not retry loading from this URL again for some time.
                FailureCache.get().rememberFailure(faviconURI)

                connection.disconnect()
                return null
            }
        } catch (e: IOException) {
            connection?.disconnect()
            return null
        } catch (e: URISyntaxException) {
            connection?.disconnect()
            return null
        }

        return connection
    }

    @Throws(URISyntaxException::class, IOException::class)
    fun connectTo(uri: String): HttpURLConnection {
        val connection = ProxySelector.openConnectionWithProxy(
            URI(uri)
        ) as HttpURLConnection

        // TODO: Find a way to pass the User-Agent to the IconDownloader
        //connection.setRequestProperty("User-Agent", GeckoApplication.getDefaultUAString())
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 5.0; Mobile; rv: 63.0) Gecko/63.0 Firefox/63.0")

        // We implemented or own way of following redirects back when this code was using HttpClient.
        // Nowadays we should let HttpUrlConnection do the work - assuming that it doesn't follow
        // redirects in loops forever.
        connection.instanceFollowRedirects = false

        return connection
    }

    /**
     * Copies the favicon stream to a buffer and decodes downloaded content into bitmaps using the
     * FaviconDecoder.
     *
     * @param stream to decode
     * @param contentLength as reported by the server (or -1)
     * @return A LoadFaviconResult containing the bitmap(s) extracted from the downloaded file, or
     * null if no or corrupt data were received.
     * @throws IOException If attempts to fully read the stream result in such an exception, such as
     * in the event of a transient connection failure.
     */
    @Throws(IOException::class)
    private fun decodeImageFromResponse(
        context: Context,
        stream: InputStream?,
        contentLength: Int
    ): LoadFaviconResult? {
        // This may not be provided, but if it is, it's useful.
        val bufferSize = if (contentLength > 0) {
            // The size was reported and sane, so let's use that.
            // Integer overflow should not be a problem for Favicon sizes...
            contentLength + 1
        } else {
            // No declared size, so guess and reallocate later if it turns out to be too small.
            DEFAULT_FAVICON_BUFFER_SIZE_BYTES
        }

        stream?.use {
            // Read the InputStream into a byte[].
            val data = it.readBytes(bufferSize)

            // Having downloaded the image, decode it.
            return FaviconDecoder.decodeFavicon(context, data, 0, data.size)
        }

        return null
    }

    companion object {
        private const val LOGTAG = "Gecko/Downloader"

        /**
         * The maximum number of http redirects (3xx) until we give up.
         */
        private const val MAX_REDIRECTS_TO_FOLLOW = 5

        /**
         * The default size of the buffer to use for downloading Favicons in the event no size is given
         * by the server.  */
        private const val DEFAULT_FAVICON_BUFFER_SIZE_BYTES = 25000
    }
}
