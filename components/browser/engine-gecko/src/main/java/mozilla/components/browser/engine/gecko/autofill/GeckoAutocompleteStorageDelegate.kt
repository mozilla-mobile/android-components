/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.autofill

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.engine.gecko.ext.toLogin
import mozilla.components.browser.engine.gecko.ext.toLoginEntry
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardsAddressesStorageDelegate
import mozilla.components.concept.storage.Login
import mozilla.components.concept.storage.LoginStorageDelegate
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.GeckoResult

/**
 * Gecko credit card and login storage delegate that handles runtime storage requests. This allows
 * the Gecko runtime to call the underlying storage to handle requests for fetching, saving and
 * updating of autocomplete items in the storage.
 *
 * @param creditCardsAddressesStorageDelegate An instance of [CreditCardsAddressesStorageDelegate].
 * Provides methods for retrieving [CreditCard]s from the underlying storage.
 * @param loginStorageDelegate An instance of [LoginStorageDelegate].
 * Provides read/write methods for the [Login] storage.
 */
class GeckoAutocompleteStorageDelegate(
    private val creditCardsAddressesStorageDelegate: CreditCardsAddressesStorageDelegate,
    private val loginStorageDelegate: LoginStorageDelegate
) : Autocomplete.StorageDelegate {

    override fun onCreditCardFetch(): GeckoResult<Array<Autocomplete.CreditCard>>? {
        val result = GeckoResult<Array<Autocomplete.CreditCard>>()

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(IO) {
            val creditCards = creditCardsAddressesStorageDelegate.onCreditCardsFetch().await()
                .mapNotNull {
                    val plaintextCardNumber =
                        creditCardsAddressesStorageDelegate.decrypt(it.encryptedCardNumber)?.number

                    if (plaintextCardNumber == null) {
                        null
                    } else {
                        Autocomplete.CreditCard.Builder()
                            .guid(it.guid)
                            .name(it.billingName)
                            .number(plaintextCardNumber)
                            .expirationMonth(it.expiryMonth.toString())
                            .expirationYear(it.expiryYear.toString())
                            .build()
                    }
                }
                .toTypedArray()
            result.complete(creditCards)
        }

        return result
    }

    override fun onLoginSave(login: Autocomplete.LoginEntry) {
        loginStorageDelegate.onLoginSave(login.toLogin())
    }

    override fun onLoginFetch(domain: String): GeckoResult<Array<Autocomplete.LoginEntry>>? {
        val result = GeckoResult<Array<Autocomplete.LoginEntry>>()

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(IO) {
            val storedLogins = loginStorageDelegate.onLoginFetch(domain)

            val logins = storedLogins.await()
                .map { it.toLoginEntry() }
                .toTypedArray()

            result.complete(logins)
        }

        return result
    }
}
