/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.ping

import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import mozilla.components.service.glean.GleanMetrics.GleanBaseline
import mozilla.components.support.base.log.logger.Logger
import java.util.Locale

/**
 * BaselinePing facilitates setup and initialization of baseline pings.
 */
internal class BaselinePing(applicationContext: Context) {
    private val logger = Logger("glean/BaselinePing")

    companion object {
        const val STORE_NAME = "baseline"
    }

    init {
        // Set the OS type
        GleanBaseline.os.set("Android")

        // Set the OS version
        // https://developer.android.com/reference/android/os/Build.VERSION
        GleanBaseline.osVersion.set(Build.VERSION.SDK_INT.toString())

        // Set the device strings
        // https://developer.android.com/reference/android/os/Build
        GleanBaseline.deviceManufacturer.set(Build.MANUFACTURER)
        GleanBaseline.deviceModel.set(Build.MODEL)

        // Set the CPU architecture
        GleanBaseline.architecture.set(Build.SUPPORTED_ABIS[0])

        // Set the enabled accessibility services
        getEnabledAccessibilityServices(
            applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        ) ?.let {
            GleanBaseline.a11yServices.set(it)
        }

        GleanBaseline.locale.set(getLanguageTag())
    }

    /**
     * Records a list of the currently enabled accessibility services.
     *
     * https://developer.android.com/reference/android/view/accessibility/AccessibilityManager.html
     * @param accessibilityManager The system's [AccessibilityManager] as
     * returned from applicationContext.getSystemService
     * @returns services A list of ids of the enabled accessibility services. If
     *     the accessibility manager is disabled, returns null.
     */
    internal fun getEnabledAccessibilityServices(
        accessibilityManager: AccessibilityManager
    ): List<String>? {
        if (!accessibilityManager.isEnabled) {
            logger.info("AccessibilityManager is disabled")
            return null
        }
        return accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).mapNotNull {
            // Note that any reference in java code can be null, so we'd better
            // check for null values here as well.
            it.id
        }
    }

    /**
     * Gets a gecko-compatible locale string (e.g. "es-ES" instead of Java [Locale]
     * "es_ES") for the default locale.
     *
     * This method approximates the API21 method [Locale.toLanguageTag].
     *
     * @return a locale string that supports custom injected languages.
     */
    internal fun getLanguageTag(): String {
        // Thanks to toLanguageTag() being introduced in API21, we could have
        // simple returned `locale.toLanguageTag();` from this function. However
        // what kind of languages the Android build supports is up to the manufacturer
        // and our apps usually support translations for more rare languages, through
        // our custom locale injector. For this reason, we can't use `toLanguageTag`
        // and must try to replicate its logic ourselves.
        val locale = Locale.getDefault()
        val language = getLanguage(locale)
        val country = locale.country // Can be an empty string.
        return if (country.isEmpty()) language else "$language-$country"
    }

    /**
     * Sometimes we want just the language for a locale, not the entire language
     * tag. But Java's .getLanguage method is wrong. A reference to the deprecated
     * ISO language codes and their mapping can be found in [Locale.toLanguageTag] docs.
     *
     * @param locale a [Locale] object to be stringified.
     * @return a language string, such as "he" for the Hebrew locales.
     */
    internal fun getLanguage(locale: Locale): String {
        // Can, but should never be, an empty string.
        val language = locale.language

        // Modernize certain language codes.
        return when (language) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            else -> language
        }
    }
}
