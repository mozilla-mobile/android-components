/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.autofill

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.engine.gecko.ext.toAutocompleteLoginEntry
import mozilla.components.browser.engine.gecko.ext.toLoginEntry
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.concept.storage.LoginsStorage
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/**
 * This class exists only to convert incoming [LoginEntry] arguments into [Login]s, then forward
 * them to [loginsStorage]. This allows us to avoid duplicating [LoginsStorage] code
 * between different versions of GeckoView, by duplicating this wrapper instead.
 */
class GeckoLoginDelegateWrapper(private val loginsStorage: LoginsStorage) :
    Autocomplete.StorageDelegate {

    override fun onLoginSave(login: Autocomplete.LoginEntry) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(IO) {
            loginsStorage.addOrUpdate(login.toLoginEntry())
        }
    }

    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>>? {
        val result = GeckoResult<Array<Autocomplete.LoginEntry>>()

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(IO) {
            val storedLogins = loginsStorage.decryptLogins(
                loginsStorage.getByBaseDomain(domain)
            )

            val logins = storedLogins
                .map { it.toAutocompleteLoginEntry() }
                .toTypedArray()

            result.complete(logins)
        }

        return result
    }
}
