/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardValidationDelegate.Result
import mozilla.components.concept.storage.UpdatableCreditCardFields
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DefaultCreditCardValidationDelegateTest {

    private lateinit var storage: AutofillCreditCardsAddressesStorage
    private lateinit var securePrefs: SecureAbove22Preferences
    private lateinit var validationDelegate: DefaultCreditCardValidationDelegate

    @Before
    fun before() = runBlocking {
        // forceInsecure is set in the tests because a keystore wouldn't be configured in the test environment.
        securePrefs = SecureAbove22Preferences(testContext, "autofill", forceInsecure = true)
        storage = AutofillCreditCardsAddressesStorage(testContext, lazy { securePrefs })
        validationDelegate = DefaultCreditCardValidationDelegate(storage = lazy { storage })
    }

    @After
    fun cleanup() = runBlocking {
        storage.wipeLocalCreditCards()
    }

    @Test
    fun `WHEN no credit cards exist in the storage, THEN add the new credit card to storage`() =
        runBlocking {
            val newCreditCard = createCreditCard(guid = "1")
            val result = validationDelegate.validate(newCreditCard)

            assertEquals(Result.CanBeCreated, result)
        }

    @Test
    fun `WHEN existing credit card matches by guid and card number, THEN update the credit card in storage`() =
        runBlocking {
            val creditCardFields = UpdatableCreditCardFields(
                billingName = "Pineapple Orange",
                cardNumber = "4111111111111111",
                expiryMonth = 2,
                expiryYear = 2028,
                cardType = "visa"
            )
            val creditCard = storage.addCreditCard(creditCardFields)
            val result = validationDelegate.validate(creditCard)

            assertEquals(Result.CanBeUpdated(creditCard), result)
        }

    @Test
    fun `WHEN existing credit card matches by guid only, THEN update the credit card in storage`() =
        runBlocking {
            val creditCardFields = UpdatableCreditCardFields(
                billingName = "Pineapple Orange",
                cardNumber = "4111111111111113",
                expiryMonth = 2,
                expiryYear = 2028,
                cardType = "visa"
            )
            val creditCard = storage.addCreditCard(creditCardFields)
            val newCreditCard = createCreditCard(guid = creditCard.guid)
            val result = validationDelegate.validate(newCreditCard)

            assertEquals(Result.CanBeUpdated(creditCard), result)
        }

    @Test
    fun `WHEN existing credit card matches by card number only, THEN update the credit card in storage`() =
        runBlocking {
            val creditCardFields = UpdatableCreditCardFields(
                billingName = "Pineapple Orange",
                cardNumber = "4111111111111114",
                expiryMonth = 2,
                expiryYear = 2028,
                cardType = "visa"
            )
            val creditCard = storage.addCreditCard(creditCardFields)
            val newCreditCard = createCreditCard(
                guid = creditCard.guid + 1,
                billingName = creditCard.billingName,
                cardNumber = creditCard.cardNumber,
                expiryMonth = creditCard.expiryMonth,
                expiryYear = creditCard.expiryYear,
                cardType = creditCard.cardType
            )
            val result = validationDelegate.validate(newCreditCard)

            assertEquals(Result.CanBeUpdated(creditCard), result)
        }

    @Test
    fun `WHEN existing credit card does not match by guid and card number, THEN add the new credit card to storage`() =
        runBlocking {
            val newCreditCard = createCreditCard(guid = "2")
            val creditCardFields = UpdatableCreditCardFields(
                billingName = "Pineapple Orange",
                cardNumber = "4111111111111116",
                expiryMonth = 2,
                expiryYear = 2028,
                cardType = "visa"
            )
            storage.addCreditCard(creditCardFields)

            val result = validationDelegate.validate(newCreditCard)

            assertEquals(Result.CanBeCreated, result)
        }
}

fun createCreditCard(
    guid: String = "id",
    billingName: String = "Banana Apple",
    cardNumber: String = "4111111111111110",
    expiryMonth: Long = 1,
    expiryYear: Long = 2030,
    cardType: String = "amex"
) = CreditCard(
    guid = guid,
    billingName = billingName,
    cardNumber = cardNumber,
    expiryMonth = expiryMonth,
    expiryYear = expiryYear,
    cardType = cardType
)
