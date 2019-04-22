/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.experiments

import android.content.Context
import android.os.Build
import java.util.Locale
import java.util.MissingResourceException
import kotlin.random.Random

/**
 * Class used to provide
 * custom filter values
 */
@Suppress("TooManyFunctions")
internal open class ValuesProvider {
    /**
     * Provides the user's language
     *
     * @return user's language as a three-letter abbreviation
     */
    open fun getLanguage(context: Context): String {
        return try {
            Locale.getDefault().isO3Language
        } catch (e: MissingResourceException) {
            Locale.getDefault().language
        }
    }

    /**
     * Provides the app name (package name)
     *
     * @return app name (package name)
     */
    open fun getAppId(context: Context): String {
        return context.packageName
    }

    /**
     * Provides the user's region
     *
     * @return user's region as a three-letter abbreviation
     */
    open fun getRegion(context: Context): String? {
        return null
    }

    /**
     * Provides the app version
     *
     * @return app version name
     */
    open fun getVersion(context: Context): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    /**
     * Provides the device manufacturer
     *
     * @return device manufacturer
     */
    open fun getManufacturer(context: Context): String {
        return Build.MANUFACTURER
    }

    /**
     * Provides the device model
     *
     * @return device model
     */
    open fun getDevice(context: Context): String {
        return Build.DEVICE
    }

    /**
     * Provides the user's country
     *
     * @return user's country, as a three-letter abbreviation
     */
    open fun getCountry(context: Context): String {
        return try {
            Locale.getDefault().isO3Country
        } catch (e: MissingResourceException) {
            Locale.getDefault().country
        }
    }

    /**
     * Provides the app's release channel (alpha, beta, ...)
     *
     * @return release channel of the app
     */
    open fun getReleaseChannel(context: Context): String? {
        return null
    }

    /**
     * Provides the client ID (UUID) used for bucketing the users.
     */
    open fun getClientId(context: Context): String {
        return DeviceUuidFactory(context).uuid
    }

    /**
     * Provides a random value for choosing an experiment branch within the given boundaries.
     *
     * @return a random branch value
     */
    open fun getRandomBranchValue(lower: Int, upper: Int): Int {
        return Random.nextInt(lower, upper)
    }

    /**
     * Provides a debug tag for the user if set.
     *
     * @return the users debug tag or null.
     */
    open fun getDebugTag(): String? {
        return null
    }
}
