/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.logins

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import mozilla.components.concept.storage.Login
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.concept.storage.LoginValidationDelegate
import mozilla.components.concept.storage.LoginValidationDelegate.Result
import mozilla.components.concept.storage.LoginsStorage

/**
 * A delegate that will check against [storage] to see if a given Login can be persisted, and return
 * information about why it can or cannot.
 */
class DefaultLoginValidationDelegate(
    private val storage: Lazy<LoginsStorage>,
    private val scope: CoroutineScope = CoroutineScope(IO)
) : LoginValidationDelegate {

    /**
     * Compares a [Login] to a passed in list of potential dupes [Login] or queries underlying
     * storage for potential dupes list of [Login] to determine if it should be updated or created.
     */
    override fun shouldUpdateOrCreateAsync(entry: LoginEntry): Deferred<Result> {
        return scope.async {
            val foundLogin = storage.value.findLoginToUpdate(entry)
            if (foundLogin == null) Result.CanBeCreated else Result.CanBeUpdated(foundLogin)
        }
    }
}
