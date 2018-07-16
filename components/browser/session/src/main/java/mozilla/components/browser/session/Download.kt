/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session

import android.os.Environment

data class Download(
    val url: String,
    val contentDisposition: String,
    val mimeType: String,
    val contentLength: Long,
    val userAgent: String, // TODO: needed??
    val destinationDirectory: String = Environment.DIRECTORY_DOWNLOADS
)
