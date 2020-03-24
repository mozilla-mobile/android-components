/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package mozilla.components.support.ktx.kotlin

import mozilla.components.support.utils.URLStringUtils
import java.net.MalformedURLException
import java.net.URL
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * A collection of regular expressions used in the `is*` methods below.
 */
private val re = object {
    val phoneish = "^\\s*tel:\\S?\\d+\\S*\\s*$".toRegex(IGNORE_CASE)
    val emailish = "^\\s*mailto:\\w+\\S*\\s*$".toRegex(IGNORE_CASE)
    val geoish = "^\\s*geo:\\S*\\d+\\S*\\s*$".toRegex(IGNORE_CASE)
}

/**
 * Checks if this String is a URL.
 */
fun String.isUrl() = URLStringUtils.isURLLike(this)

/**
 * Checks if this string is a URL.
 *
 * This method performs a strict check to determine whether the string is a URL. It takes longer
 * to execute than isUrl() but checks whether, e.g., the TLD is ICANN-recognized. Consider
 * using isUrl() unless these guarantees are required.
 */
fun String.isUrlStrict() = URLStringUtils.isURLLikeStrict(this)

fun String.toNormalizedUrl() = URLStringUtils.toNormalizedURL(this)

fun String.isPhone() = re.phoneish.matches(this)

fun String.isEmail() = re.emailish.matches(this)

fun String.isGeoLocation() = re.geoish.matches(this)

/**
 * Converts a [String] to a [Date] object.
 * @param format date format used for formatting the this given [String] object.
 * @param locale the locale to use when converting the String, defaults to [Locale.ROOT].
 * @return a [Date] object with the values in the provided in this string, if empty string was provided, a current date
 * will be returned.
 */
fun String.toDate(format: String, locale: Locale = Locale.ROOT): Date {
    val formatter = SimpleDateFormat(format, locale)
    return if (isNotEmpty()) {
        formatter.parse(this) ?: Date()
    } else {
        Date()
    }
}

/**
 * Calculates a SHA1 hash for this string.
 */
@Suppress("MagicNumber")
fun String.sha1(): String {
    val characters = "0123456789abcdef"
    val digest = MessageDigest.getInstance("SHA-1").digest(toByteArray())
    return digest.joinToString(separator = "", transform = { byte ->
        String(charArrayOf(characters[byte.toInt() shr 4 and 0x0f], characters[byte.toInt() and 0x0f]))
    })
}

/**
 * Tries to convert a [String] to a [Date] using a list of [possibleFormats].
 * @param possibleFormats one ore more possible format.
 * @return a [Date] object with the values in the provided in this string,
 * if the conversion is not possible null will be returned.
 */
fun String.toDate(
    vararg possibleFormats: String = arrayOf(
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd",
            "yyyy-'W'ww",
            "yyyy-MM",
            "HH:mm"
    )
): Date? {
    possibleFormats.forEach {
        try {
            return this.toDate(it)
        } catch (pe: ParseException) {
            // move to next possible format
        }
    }
    return null
}

/**
 * Tries to parse and get host part if this [String] is valid URL.
 * Otherwise returns the string.
 */
fun String.tryGetHostFromUrl(): String = try {
    URL(this).host
} catch (e: MalformedURLException) {
    this
}

/**
 * Compares 2 URLs and returns true if they have the same origin,
 * which means: same protocol, same host, same port.
 */
fun String.isSameOriginAs(other: String): Boolean {
    fun canonicalizeOrigin(urlStr: String): String {
        val url = URL(urlStr)
        val port = if (url.port == -1) url.defaultPort else url.port
        val canonicalized = URL(url.protocol, url.host, port, "")
        return canonicalized.toString()
    }
    return canonicalizeOrigin(this) == canonicalizeOrigin(other)
}
