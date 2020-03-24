/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.logins

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import mozilla.appservices.logins.InvalidLoginReason
import mozilla.appservices.logins.InvalidRecordException
import mozilla.components.concept.storage.Login
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

    @Suppress("ComplexMethod") // This method is not actually complex
    override fun validateCanPersist(login: Login): Deferred<Result> {
        return scope.async {
            try {
                // We're setting guid=null here in order to trigger de-duping logic properly.
                // Internally, the library ensures to not dupe records against themselves
                // (via a `guid <> :guid` check), which means we need to "pretend" this is a new record,
                // in order for `ensureValid` to actually throw `DUPLICATE_LOGIN`.
                storage.value.ensureValid(login.copy(guid = null))
                Result.CanBeCreated
            } catch (e: InvalidRecordException) {
                when (e.reason) {
                    InvalidLoginReason.DUPLICATE_LOGIN -> Result.CanBeUpdated
                    InvalidLoginReason.EMPTY_PASSWORD -> Result.Error.EmptyPassword
                    InvalidLoginReason.EMPTY_ORIGIN -> Result.Error.GeckoError(e)
                    InvalidLoginReason.BOTH_TARGETS -> Result.Error.GeckoError(e)
                    InvalidLoginReason.NO_TARGET -> Result.Error.GeckoError(e)
                    // TODO in what ways can the login fields be illegal? represent these in the UI
                    InvalidLoginReason.ILLEGAL_FIELD_VALUE -> Result.Error.GeckoError(e)
                }
            }
        }
    }
}
