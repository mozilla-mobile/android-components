/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License. */

package mozilla.components.support.utils

import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.util.Locale
import java.util.regex.Pattern

object DownloadUtils {

    /**
     * Format as defined in RFC 2616 and RFC 5987
     * Only the attachment type is supported.
     */
    private val CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment\\s*;\\s*filename\\s*=\\s*" +
            "(\"((?:\\\\.|[^\"\\\\])*)\"|[^;]*)\\s*" +
            "(?:;\\s*filename\\*\\s*=\\s*(utf-8|iso-8859-1)'[^']*'(\\S*))?",
            Pattern.CASE_INSENSITIVE)

    /**
     * Definition as per RFC 5987, section 3.2.1. (value-chars)
     */
    private val ENCODED_SYMBOL_PATTERN = Pattern.compile("%[0-9a-f]{2}|[0-9a-z!#$&+-.^_`|~]", Pattern.CASE_INSENSITIVE)

    /**
     * Guess the name of the file that should be downloaded.
     *
     * This method is largely identical to [android.webkit.URLUtil.guessFileName]
     * which unfortunately does not implement RfC 5987.
     */
    @JvmStatic
    fun guessFileName(contentDisposition: String?, url: String?, mimeType: String?): String {
        var filename: String? = null
        var extension: String? = null

        // Extract file name from content disposition header field
        if (contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition)
            if (filename != null) {
                val index = filename.lastIndexOf('/') + 1
                if (index > 0) {
                    filename = filename.substring(index)
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (filename == null) {
            var decodedUrl: String? = Uri.decode(url)
            if (decodedUrl != null) {
                val queryIndex = decodedUrl.indexOf('?')
                // If there is a query string strip it, same as desktop browsers
                if (queryIndex > 0) {
                    decodedUrl = decodedUrl.substring(0, queryIndex)
                }
                if (!decodedUrl.endsWith("/")) {
                    val index = decodedUrl.lastIndexOf('/') + 1
                    if (index > 0) {
                        filename = decodedUrl.substring(index)
                    }
                }
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        if (filename == null) {
            filename = "downloadfile"
        }

        // Split filename between base and extension
        // Add an extension if filename does not have one
        val dotIndex = filename.indexOf('.')
        if (dotIndex < 0) {
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (extension != null) {
                    extension = "" + extension
                }
            }
            if (extension == null) {
                if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
                    if (mimeType.equals("text/html", ignoreCase = true)) {
                        extension = ".html"
                    } else {
                        extension = ".txt"
                    }
                } else {
                    extension = ".bin"
                }
            }
        } else {
            if (mimeType != null) {
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                val lastDotIndex = filename.lastIndexOf('.')
                val typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        filename.substring(lastDotIndex + 1))
                if (typeFromExt != null && !typeFromExt.equals(mimeType, ignoreCase = true)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    if (extension != null) {
                        extension = "" + extension
                    }
                }
            }
            if (extension == null) {
                extension = filename.substring(dotIndex)
            }
            filename = filename.substring(0, dotIndex)
        }

        return filename + extension!!
    }

    private fun parseContentDisposition(contentDisposition: String): String? {
        try {
            val m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition)

            if (m.find()) {
                // If escaped string is found, decode it using the given encoding.
                val encodedFileName = m.group(4)
                val encoding = m.group(3)

                if (encodedFileName != null) {
                    return decodeHeaderField(encodedFileName, encoding)
                }

                // Return quoted string if available and replace escaped characters.
                val quotedFileName = m.group(2)

                return if (quotedFileName != null) {
                    quotedFileName.replace("\\\\(.)".toRegex(), "$1")
                } else m.group(1)

                // Otherwise try to extract the unquoted file name
            }
        } catch (ex: IllegalStateException) {
            // This function is defined as returning null when it can't parse the header
        } catch (ex: UnsupportedEncodingException) {
        }

        return null
    }

    @Throws(UnsupportedEncodingException::class)
    private fun decodeHeaderField(field: String, encoding: String): String {
        val m = ENCODED_SYMBOL_PATTERN.matcher(field)
        val stream = ByteArrayOutputStream()

        while (m.find()) {
            val symbol = m.group()

            if (symbol.startsWith("%")) {
                stream.write(Integer.parseInt(symbol.substring(1), 16))
            } else {
                stream.write(symbol[0].toInt())
            }
        }

        return stream.toString(encoding)
    }
}
