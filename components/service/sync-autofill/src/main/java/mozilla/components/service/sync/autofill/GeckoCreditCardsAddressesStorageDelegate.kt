/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.Address
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.CreditCardValidationDelegate
import mozilla.components.concept.storage.CreditCardsAddressesStorage
import mozilla.components.concept.storage.CreditCardsAddressesStorageDelegate

/**
 * [CreditCardsAddressesStorageDelegate] implementation.
 *
 * @param storage The [CreditCardsAddressesStorage] used for looking up addresses and credit cards to autofill.
 * @param scope [CoroutineScope] for long running operations. Defaults to using the [Dispatchers.IO].
 * @param isCreditCardAutofillEnabled callback allowing to limit [storage] operations if autofill is disabled.
 */
class GeckoCreditCardsAddressesStorageDelegate(
    private val storage: Lazy<CreditCardsAddressesStorage>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val isCreditCardAutofillEnabled: () -> Boolean = { false }
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

    override suspend fun onCreditCardsFetch(): List<CreditCard> {
        if (isCreditCardAutofillEnabled().not()) {
            return listOf()
        }

        return withContext(scope.coroutineContext) {
            storage.value.getAllCreditCards()
        }
    }

    override fun onCreditCardSave(creditCard: CreditCard) {
        val validationDelegate = DefaultCreditCardValidationDelegate(storage)

        scope.launch {
            when (val result = validationDelegate.validate(creditCard)) {
                is CreditCardValidationDelegate.Result.CanBeCreated -> {
                    decrypt(creditCard.encryptedCardNumber)?.let { plaintextCardNumber ->
                        storage.value.addCreditCard(
                            creditCard.intoNewCreditCardFields(
                                plaintextCardNumber
                            )
                        )
                    }
                }
                is CreditCardValidationDelegate.Result.CanBeUpdated -> {
                    storage.value.updateCreditCard(
                        guid = result.foundCreditCard.guid,
                        creditCardFields = creditCard.intoUpdatableCreditCardFields()
                    )
                }
                is CreditCardValidationDelegate.Result.Error -> {
                    // Do nothing since an error occurred and the credit card cannot be saved.
                }
            }
        }
    }
}
