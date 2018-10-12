/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.kotlin

import android.net.Uri
import android.text.TextUtils

/**
 * Normalizes a URL String.
 */
fun String.toNormalizedUrl(): String {
    val trimmedInput = this.trim()
    var uri = Uri.parse(trimmedInput)
    if (TextUtils.isEmpty(uri.scheme)) {
        uri = Uri.parse("http://$trimmedInput")
    }
    return uri.toString()
}

/**
 * Checks if this String is a URL.
 */
fun String.isUrl(): Boolean {
    val trimmedUrl = this.trim()
    if (trimmedUrl.contains(" ")) {
        return false
    }

    return trimmedUrl.contains(".") || trimmedUrl.contains(":")
}

fun String.isPhone(): Boolean = contains("tel:", true)

fun String.isEmail(): Boolean = contains("mailto:", true)

fun String.isGeoLocation(): Boolean = contains("geo:", true)

fun String.stripCommonSubdomains(): String? {
    // In contrast to desktop, we also strip mobile subdomains,
    // since its unlikely users are intentionally typing them
    var start = 0

    if (startsWith("www.")) {
        start = 4
    } else if (startsWith("mobile.")) {
        start = 7
    } else if (startsWith("m.")) {
        start = 2
    }

    return substring(start)
}

fun String.isHttpOrHttps(): Boolean =
    if (TextUtils.isEmpty(this)) {
        false
    } else {
        startsWith("http://") || startsWith("https://")
    }

fun String.isAboutPage(): Boolean =
    startsWith("about:")
