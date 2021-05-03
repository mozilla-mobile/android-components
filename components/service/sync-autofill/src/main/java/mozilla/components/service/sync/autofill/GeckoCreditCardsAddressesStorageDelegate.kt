/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.Address
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.CreditCardsAddressesStorage
import mozilla.components.concept.storage.CreditCardsAddressesStorageDelegate

/**
 * [CreditCardsAddressesStorageDelegate] implementation.
 */
class GeckoCreditCardsAddressesStorageDelegate(
    private val storage: Lazy<CreditCardsAddressesStorage>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CreditCardsAddressesStorageDelegate {

    override fun decrypt(encryptedCardNumber: CreditCardNumber.Encrypted): CreditCardNumber.Plaintext? {
        val crypto = storage.value.getCreditCardCrypto()
        val key = crypto.key()
        return crypto.decrypt(key, encryptedCardNumber)
    }

    override suspend fun onAddressesFetch(): List<Address> = withContext(scope.coroutineContext) {
        storage.value.getAllAddresses()
    }

    override fun onAddressSave(address: Address) {
        TODO("Not yet implemented")
    }

    override suspend fun onCreditCardsFetch(): List<CreditCard> =
        withContext(scope.coroutineContext) {
            storage.value.getAllCreditCards()
        }

    override fun onCreditCardSave(creditCard: CreditCard) {
        TODO("Not yet implemented")
    }
}
