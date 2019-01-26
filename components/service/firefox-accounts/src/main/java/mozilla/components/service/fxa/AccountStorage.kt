/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import android.content.Context
import android.content.SharedPreferences

const val FXA_STATE_PREFS_KEY = "fxaAppState"
const val FXA_STATE_KEY = "fxaState"

interface AccountStorage {
    @Throws(Exception::class)
    fun read(): FirefoxAccountShaped?
    fun write(account: FirefoxAccountShaped)
    fun clear()
}

class SharedPrefAccountStorage(val context: Context) : AccountStorage {
    /**
     * @throws FxaException if JSON failed to parse into a [FirefoxAccount].
     */
    override fun read(): FirefoxAccountShaped? {
        val savedJSON = accountPreferences().getString(FXA_STATE_KEY, null)
                ?: return null

        // May throw a generic FxaException if it fails to process saved JSON.
        return FirefoxAccount.fromJSONString(savedJSON)
    }

    override fun write(account: FirefoxAccountShaped) {
        accountPreferences()
            .edit()
            .putString(FXA_STATE_KEY, account.toJSONString())
            .apply()
    }

    override fun clear() {
        accountPreferences()
            .edit()
            .remove(FXA_STATE_KEY)
            .apply()
    }

    private fun accountPreferences(): SharedPreferences {
        return context.getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE)
    }
}
