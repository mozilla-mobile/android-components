/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardEntry
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.NewCreditCardFields
import mozilla.components.concept.storage.UpdatableCreditCardFields
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import mozilla.components.support.ktx.kotlin.last4Digits
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import mozilla.components.support.test.rule.runTestOnMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GeckoCreditCardsAddressesStorageDelegateTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    private val scope = coroutinesTestRule.scope

    private lateinit var storage: AutofillCreditCardsAddressesStorage
    private lateinit var securePrefs: SecureAbove22Preferences
    private lateinit var delegate: GeckoCreditCardsAddressesStorageDelegate

    init {
        testContext.getDatabasePath(AUTOFILL_DB_NAME)!!.parentFile!!.mkdirs()
    }

    @Before
    fun before() {
        // forceInsecure is set in the tests because a keystore wouldn't be configured in the test environment.
        securePrefs = SecureAbove22Preferences(testContext, "autofill", forceInsecure = true)
        storage = AutofillCreditCardsAddressesStorage(testContext, lazy { securePrefs })
        delegate = GeckoCreditCardsAddressesStorageDelegate(lazy { storage }, scope)
    }

    @Test
    fun `GIVEN a newly added credit card WHEN decrypt is called THEN it returns the plain credit card number`() =
        runTestOnMain {
            val plaintextNumber = CreditCardNumber.Plaintext("4111111111111111")
            val creditCardFields = NewCreditCardFields(
                billingName = "Jon Doe",
                plaintextCardNumber = plaintextNumber,
                cardNumberLast4 = "1111",
                expiryMonth = 12,
                expiryYear = 2028,
                cardType = "amex"
            )
            val creditCard = storage.addCreditCard(creditCardFields)
            val key = delegate.getOrGenerateKey()

            assertEquals(
                plaintextNumber,
                delegate.decrypt(key, creditCard.encryptedCardNumber)
            )
        }

    @Test
    fun `WHEN onAddressFetch is called THEN the storage is called to gett all addresses`() {
        scope.launch {
            delegate.onAddressesFetch()
            verify(storage, times(1)).getAllAddresses()
        }
    }

    @Test
    fun `GIVEN autofill enabled WHEN onCreditCardsFetch is called THEN it returns all stored cards`() {
        scope.launch {
            val storage: AutofillCreditCardsAddressesStorage = mock()
            val storedCards = listOf<CreditCard>(mock())
            doReturn(storedCards).`when`(storage).getAllCreditCards()
            delegate = GeckoCreditCardsAddressesStorageDelegate(lazy { storage }, scope) { true }

            val result = delegate.onCreditCardsFetch()

            verify(storage, times(1)).getAllCreditCards()
            assertEquals(storedCards, result)
        }
    }

    @Test
    fun `GIVEN autofill disabled WHEN onCreditCardsFetch is called THEN it returns an empty list of cards`() {
        scope.launch {
            val storage: AutofillCreditCardsAddressesStorage = mock()
            val storedCards = listOf<CreditCard>(mock())
            doReturn(storedCards).`when`(storage).getAllCreditCards()
            delegate = GeckoCreditCardsAddressesStorageDelegate(lazy { storage }, scope) { false }

            val result = delegate.onCreditCardsFetch()

            verify(storage, times(1)).getAllCreditCards()
            assertEquals(emptyList<CreditCard>(), result)
        }
    }

    @Test
    fun `GIVEN a new credit card WHEN onCreditCardSave is called THEN it adds a new credit card in storage`() {
        scope.launch {
            val billingName = "Jon Doe"
            val cardNumber = "4111111111111111"
            val expiryMonth = 12L
            val expiryYear = 2028L
            val cardType = "amex"

            delegate.onCreditCardSave(
                CreditCardEntry(
                    name = billingName,
                    number = cardNumber,
                    expiryMonth = expiryMonth.toString(),
                    expiryYear = expiryYear.toString(),
                    cardType = cardType
                )
            )

            verify(storage, times(1)).addCreditCard(
                creditCardFields = NewCreditCardFields(
                    billingName = billingName,
                    plaintextCardNumber = CreditCardNumber.Plaintext(cardNumber),
                    cardNumberLast4 = cardNumber.last4Digits(),
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cardType = cardType
                )
            )
        }
    }

    @Test
    fun `GIVEN an existing credit card WHEN onCreditCardSave is called THEN it updates the existing credit card in storage`() {
        scope.launch {
            val billingName = "Jon Doe"
            val cardNumber = "4111111111111111"
            val expiryMonth = 12L
            val expiryYear = 2028L
            val cardType = "amex"

            val creditCard = storage.addCreditCard(
                NewCreditCardFields(
                    billingName = "Jon Doe",
                    plaintextCardNumber = CreditCardNumber.Plaintext(cardNumber),
                    cardNumberLast4 = "1111",
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cardType = cardType
                )
            )

            delegate.onCreditCardSave(
                CreditCardEntry(
                    guid = creditCard.guid,
                    name = billingName,
                    number = "4111111111111112",
                    expiryMonth = expiryMonth.toString(),
                    expiryYear = expiryYear.toString(),
                    cardType = cardType
                )
            )

            verify(storage, times(1)).updateCreditCard(
                guid = creditCard.guid,
                creditCardFields = UpdatableCreditCardFields(
                    billingName = billingName,
                    cardNumber = CreditCardNumber.Plaintext("4111111111111112"),
                    cardNumberLast4 = "4111111111111112".last4Digits(),
                    expiryMonth = expiryMonth,
                    expiryYear = expiryYear,
                    cardType = cardType
                )
            )
        }
    }
}
