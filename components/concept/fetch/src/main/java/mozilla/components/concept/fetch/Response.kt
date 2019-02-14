/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.fetch

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

/**
 * The [Response] data class represents a reponse to a [Request] send by a [Client].
 *
 * You can create a [Response] object using the constructor, but you are more likely to encounter a [Response] object
 * being returned as the result of calling [Client.fetch].
 *
 * A [Response] may hold references to other resources (e.g. streams). Therefore it's important to always close the
 * [Response] object or its [Body]. This can be done by either consuming the content of the [Body] with one of the
 * available methods or by using Kotlin's extension methods for using [Closeable] implementations (like `use()`):
 *
 * ```Kotlin
 * val response = ...
 * response.use {
 *    // Use response. Resources will get released automatically at the end of the block.
 * }
 * ```
 */
data class Response(
    val url: String,
    val status: Int,
    val headers: Headers,
    val body: Body
) : Closeable {
    /**
     * Closes this [Response] and its [Body] and releases any system resources associated with it.
     */
    override fun close() {
        body.close()
    }

    /**
     * A [Body] returned along with the [Request].
     *
     * **The response body can be consumed only once.**.
     *
     * @param stream the input stream from which the response body can be read.
     * @param contentType optional content-type as provided in the response
     * header. If specified, an attempt will be made to look up the charset
     * which will be used for decoding the body. If not specified, or if the
     * charset can't be found, UTF-8 will be used for decoding.
     */
    open class Body(
        private val stream: InputStream,
        contentType: String? = null
    ) : Closeable, AutoCloseable {

        @Suppress("TooGenericExceptionCaught")
        private val charset = contentType?.let {
            val charset = it.substringAfter("charset=")
            try {
                Charset.forName(charset)
            } catch (e: Exception) {
                Charsets.UTF_8
            }
        } ?: Charsets.UTF_8

        /**
         * Creates a usable stream from this body.
         *
         * Executes the given [block] function with the stream as parameter and then closes it down correctly
         * whether an exception is thrown or not.
         */
        fun <R> useStream(block: (InputStream) -> R): R = use {
            block(stream)
        }

        /**
         * Creates a buffered reader from this body.
         *
         * Executes the given [block] function with the buffered reader as parameter and then closes it down correctly
         * whether an exception is thrown or not.
         */
        fun <R> useBufferedReader(charset: Charset? = null, block: (BufferedReader) -> R): R = use {
            block(stream.bufferedReader(charset ?: this.charset))
        }

        /**
         * Reads this body completely as a String.
         *
         * Takes care of closing the body down correctly whether an exception is thrown or not.
         */
        fun string(): String = useBufferedReader { it.readText() }

        /**
         * Closes this [Body] and releases any system resources associated with it.
         */
        override fun close() {
            try {
                stream.close()
            } catch (e: IOException) {
                // Ignore
            }
        }

        companion object {
            /**
             * Creates an empty response body.
             */
            fun empty() = Body("".byteInputStream())
        }
    }
}

/**
 * Returns true if the response was successful (status in the range 200-299) or false otherwise.
 */
@Suppress("MagicNumber")
val Response.success: Boolean
    get() = status in 200..299

/**
 * Returns true if the response was a client error (status in the range 400-499) or false otherwise.
 */
@Suppress("MagicNumber")
val Response.clientError: Boolean
    get() = status in 400..499
