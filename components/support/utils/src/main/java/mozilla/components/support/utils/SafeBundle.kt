/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils

import android.os.Bundle
import android.os.Parcelable
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.ext.getParcelableCompat

/**
 * See SafeIntent for more background: applications can put garbage values into Bundles. This is primarily
 * experienced when there's garbage in the Intent's Bundle. However that Bundle can contain further bundles,
 * and we need to handle those defensively too.
 *
 * See bug 1090385 for more.
 */
class SafeBundle(val unsafe: Bundle) {

    fun getInt(name: String, defaultValue: Int = 0): Int =
        safeAccess(defaultValue) { getInt(name, defaultValue) }!!

    fun getString(name: String): String? =
        safeAccess { getString(name) }

    fun keySet(): Set<String>? =
        safeAccess { keySet() }

    /**
     * Returns the value associated with the given key or null.
     * @param name the key name.
     * @param clazz the desired class of the object .
     * null is returned when the Object is not of type clazz, no mapping exists for that key name
     * or a value of null is explicitly associated with the key.
     */
    fun <T : Parcelable> getParcelable(name: String, clazz: Class<T>): T? =
        safeAccess { getParcelableCompat(name, clazz) }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun <T> safeAccess(default: T? = null, block: Bundle.() -> T): T? {
        return try {
            block(unsafe)
        } catch (e: OutOfMemoryError) {
            Logger.warn("Couldn't get bundle items: OOM. Malformed?")
            default
        } catch (e: RuntimeException) {
            Logger.warn("Couldn't get bundle items.", e)
            default
        }
    }
}

/**
 * Returns a [SafeBundle] for the given [Bundle].
 */
fun Bundle.toSafeBundle() = SafeBundle(this)
