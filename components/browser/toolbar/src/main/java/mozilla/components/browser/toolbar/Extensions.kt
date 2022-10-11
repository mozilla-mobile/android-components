/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar

import java.text.NumberFormat
import java.util.Locale

// This is used for truncating URLs to prevent extreme cases from
// slowing down UI rendering e.g. in case of a bookmarklet or a data URI.
// https://github.com/mozilla-mobile/android-components/issues/5249
const val MAX_URI_LENGTH = 25000

fun getTrimmedUrl(url: String): String {
    return url.take(MAX_URI_LENGTH)
}