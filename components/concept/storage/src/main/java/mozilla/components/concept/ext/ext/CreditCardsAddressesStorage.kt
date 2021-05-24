/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.ext.ext

import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.CreditCardsAddressesStorage

/**
 * Decrypt a [CreditCardNumber.Encrypted] into its [CreditCardNumber.Plaintext] equivalent.
 *
 * @param encryptedCardNumber An encrypted credit card number to be decrypted.
 * @return A plaintext, non-encrypted credit card number, 'null' if decryption fails.
 */
private fun CreditCardsAddressesStorage.decrypt(
    encryptedCardNumber: CreditCardNumber.Encrypted
): CreditCardNumber.Plaintext? {
    val crypto = this.getCreditCardCrypto()
    return crypto.decrypt(crypto.key(), encryptedCardNumber)
}

/**
 * Validates the credit card number against existing cards list
 * @return true if the credit card number is a duplicate, false otherwise.
 */
suspend fun CreditCardsAddressesStorage.isDuplicate(
    creditCardNumber: String,
    guid: String?
): Boolean {
    val otherCreditCardNumberList: List<String> = this.getAllCreditCards()
        .filterNot {
            it.guid == guid
        }.map {
            this.decrypt(it.encryptedCardNumber)?.number.toString()
        }

    return (creditCardNumber in otherCreditCardNumberList)
}
