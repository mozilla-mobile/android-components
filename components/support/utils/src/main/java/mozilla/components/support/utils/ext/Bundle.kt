/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils.ext

import android.os.Build
import android.os.Bundle
import java.io.Serializable

/**
 * Retrieve extended data from the bundle.
 */
fun <T> Bundle.getParcelableCompat(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(name) as? T?
    }
}

/**
 * Retrieve extended data from the bundle.
 */
fun <T : Serializable> Bundle.getSerializableCompat(name: String, clazz: Class<T>): Serializable? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        getSerializable(name)
    }
}
