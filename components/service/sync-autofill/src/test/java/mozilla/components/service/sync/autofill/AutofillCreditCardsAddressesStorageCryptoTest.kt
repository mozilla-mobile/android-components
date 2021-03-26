/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.appservices.autofill.ErrorException as AutofillException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AutofillCryptoTest {
    @Test
    fun `testEncryptDecrypt`() {
        val crypto = AutofillCreditCardsAddressesCrypto()
        val key = crypto.createEncryptionKey()
        val ciphertext = crypto.encryptString(key, "hello")
        assertTrue(ciphertext != "hello") // would be embarrasing :)
        assertEquals(crypto.decryptString(key, ciphertext), "hello")
    }

    @Test(expected=AutofillException.CryptoError::class)
    fun `testBadKey`() {
        val crypto = AutofillCreditCardsAddressesCrypto()
        val key1 = crypto.createEncryptionKey()
        val ciphertext = crypto.encryptString(key1, "hello")
        val key2 = crypto.createEncryptionKey()
        crypto.decryptString(key2, ciphertext) // should fail with CryptoError
    }
}
