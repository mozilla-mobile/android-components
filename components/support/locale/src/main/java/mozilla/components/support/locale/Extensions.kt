/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.locale

import java.util.Locale

fun String.toLocale(): Locale {
    val index: Int = if (contains('-')) {
        indexOf('-')
    } else {
        indexOf('_')
    }
    return if (index != -1) {
        val langCode = substring(0, index)
        val countryCode = substring(index + 1)
        Locale(langCode, countryCode)
    } else {
        Locale(this)
    }
}

/**
 * Gets a gecko-compatible locale string (e.g. "es-ES" instead of Java [Locale]
 * "es_ES") for the default locale.
 * If the locale can't be determined on the system, the value is "und",
 * to indicate "undetermined".
 *
 * This method approximates the API21 method [Locale.toLanguageTag].
 *
 * @return a locale string that supports custom injected locale/languages.
 */
fun Locale.getLocaleTag(): String {
    // Thanks to toLanguageTag() being introduced in API21, we could have
    // simply returned `locale.toLanguageTag();` from this function. However
    // what kind of languages the Android build supports is up to the manufacturer
    // and our apps usually support translations for more rare languages, through
    // our custom locale injector. For this reason, we can't use `toLanguageTag`
    // and must try to replicate its logic ourselves.

    // `locale.language` can, but should never be, an empty string.
    // Modernize certain language codes.
    val language = when (this.language) {
        "iw" -> "he"
        "in" -> "id"
        "ji" -> "yi"
        else -> this.language
    }
    val country = this.country // Can be an empty string.

    return when {
        language.isEmpty() -> "und"
        country.isEmpty() -> language
        else -> "$language-$country"
    }
}
