/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import mozilla.components.support.base.log.logger.Logger

import java.util.ArrayList

/**
 * External applications can pass values into Intents that can cause us to crash: in defense,
 * we wrap [Intent] and catch the exceptions they may force us to throw. See bug 1090385
 * for more.
 */
class SafeIntent(val unsafe: Intent) {
    val extras: Bundle?
        get() = safeAccess { unsafe.extras }

    val action: String?
        get() = unsafe.action

    val flags: Int
        get() = unsafe.flags

    val isLauncherIntent: Boolean
        get() {
            val intentCategories = unsafe.categories
            return intentCategories != null && intentCategories.contains(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN == unsafe.action
        }

    val dataString: String?
        get() = safeAccess { unsafe.dataString }

    val data: Uri?
        get() = safeAccess { unsafe.data }

    val categories: Set<String>?
        get() = safeAccess { unsafe.categories }

    fun hasExtra(name: String): Boolean = safeAccess(false) {
        unsafe.hasExtra(name)
    }!!

    fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean = safeAccess(defaultValue) {
        unsafe.getBooleanExtra(name, defaultValue)
    }!!

    fun getIntExtra(name: String, defaultValue: Int): Int = safeAccess(defaultValue) {
        unsafe.getIntExtra(name, defaultValue)
    }!!

    fun getStringExtra(name: String): String? = safeAccess {
        unsafe.getStringExtra(name)
    }

    fun getBundleExtra(name: String): SafeBundle? = safeAccess {
        val bundle = unsafe.getBundleExtra(name)
        if (bundle != null) {
            SafeBundle(bundle)
        } else {
            null
        }
    }

    fun getCharSequenceExtra(name: String): CharSequence? = safeAccess {
        unsafe.getCharSequenceExtra(name)
    }

    fun <T : Parcelable> getParcelableExtra(name: String): T? = safeAccess {
        unsafe.getParcelableExtra(name) as T
    }

    fun <T : Parcelable> getParcelableArrayListExtra(name: String): ArrayList<T>? {
        return safeAccess {
            val value: ArrayList<T> = unsafe.getParcelableArrayListExtra(name)
            value
        }
    }

    fun getStringArrayListExtra(name: String): ArrayList<String>? = safeAccess {
            getStringArrayListExtra(name)
    }

    private fun <T> safeAccess(default: T? = null, block: Intent.() -> T): T? {
        return try {
            block(unsafe)
        } catch (e: OutOfMemoryError) {
            Logger.warn("Could not read from intent: OOM. Malformed?")
            default
        } catch (e: RuntimeException) {
            Logger.warn("Could not read from intent.", e)
            default
        }
    }
}

/**
 * Returns a [SafeIntent] for the given [Intent].
 */
fun Intent.toSafeIntent(): SafeIntent = SafeIntent(this)
