/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import mozilla.components.concept.storage.CreditCardsAddressesCrypto

/**
 * An implementation of [AutofillCreditCardsAddressesCrypto] back by the application-services' `autofill`
 * library.
 */
class AutofillCreditCardsAddressesCrypto() : CreditCardsAddressesCrypto {
    override fun encryptString(key: String, cleartext: String): String {
        return mozilla.appservices.autofill.encryptString(key, cleartext)
    }

    override fun decryptString(key: String, ciphertext: String): String  {
        return mozilla.appservices.autofill.decryptString(key, ciphertext)
    }

    override fun createEncryptionKey(): String  {
        return mozilla.appservices.autofill.createKey()
    }
}
