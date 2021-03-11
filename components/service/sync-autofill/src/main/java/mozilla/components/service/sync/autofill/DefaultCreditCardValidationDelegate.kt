/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.CreditCardValidationDelegate
import mozilla.components.concept.storage.CreditCardValidationDelegate.Result
import mozilla.components.concept.storage.CreditCardsAddressesStorage

/**
 * A delegate that will check against the [CreditCardsAddressesStorage] to determine if a given
 * [CreditCard] can be persisted and returns information about why it can or cannot.
 *
 * @param storage An instance of [CreditCardsAddressesStorage].
 */
class DefaultCreditCardValidationDelegate(
    private val storage: Lazy<CreditCardsAddressesStorage>
) : CreditCardValidationDelegate {

    private val coroutineContext by lazy { Dispatchers.IO }

    override suspend fun validate(creditCard: CreditCard): Result =
        withContext(coroutineContext) {
            val creditCards = storage.value.getAllCreditCards()

            val foundCreditCard = if (creditCards.isEmpty()) {
                // No credit cards exist in the storage, create a new credit card to the storage.
                null
            } else {
                // Found a matching guid and credit card number -> update
                creditCards.find {
                    it.guid == creditCard.guid &&
                        decrypt(it.encryptedCardNumber) == decrypt(creditCard.encryptedCardNumber)
                }
                // Found a matching guid and blank credit card number -> update
                    ?: creditCards.find { it.guid == creditCard.guid && it.encryptedCardNumber.number.isEmpty() }
                    // Found a matching credit card number -> update
                    ?: creditCards.find { decrypt(it.encryptedCardNumber) == decrypt(creditCard.encryptedCardNumber) }
                    // Found a non-matching guid and blank credit card number -> update
                    ?: creditCards.find { it.encryptedCardNumber.number.isEmpty() }
                // Else create a new credit card
            }

            if (foundCreditCard == null) Result.CanBeCreated else Result.CanBeUpdated(
                foundCreditCard
            )
        }

    /**
     * Helper function to decrypt a [CreditCardNumber.Encrypted] into its plaintext equivalent or
     * `null` if it fails to decrypt.
     *
     * @param encryptedCardNumber An encrypted credit card number to be decrypted.
     * @return A plaintext, non-encrypted credit card number.
     */
    private fun decrypt(encryptedCardNumber: CreditCardNumber.Encrypted): CreditCardNumber.Plaintext? {
        val crypto = storage.value.getCreditCardCrypto()
        val key = crypto.key()
        return crypto.decrypt(key, encryptedCardNumber)
    }

    companion object {
        fun CreditCard.mergeWithCreditCard(creditCard: CreditCard): CreditCard {
            infix fun String?.orUseExisting(other: String?) =
                if (this?.isNotEmpty() == true) this else other

            infix fun String?.orUseExisting(other: String) =
                if (this?.isNotEmpty() == true) this else other

            val billingName = creditCard.billingName orUseExisting billingName
            val encryptedCardNumber = if (creditCard.encryptedCardNumber.number.isNotEmpty())
                creditCard.encryptedCardNumber else encryptedCardNumber


            return copy(
                billingName = billingName,
                encryptedCardNumber = encryptedCardNumber,
                cardNumberLast4 = cardNumberLast4,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                cardType = cardType
            )
        }
    }
}
