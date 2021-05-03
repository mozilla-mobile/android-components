/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.autofill

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.engine.gecko.await
import mozilla.components.browser.engine.gecko.ext.toLogin
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.CreditCardsAddressesStorageDelegate
import mozilla.components.concept.storage.LoginStorageDelegate
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import mozilla.components.test.createLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mozilla.geckoview.Autocomplete

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GeckoAutocompleteStorageDelegateTest {

    private lateinit var creditCardsAddressesStorageDelegate: CreditCardsAddressesStorageDelegate
    private lateinit var loginStorageDelegate: LoginStorageDelegate
    private lateinit var geckoAutocompleteStorageDelegate: GeckoAutocompleteStorageDelegate

    @Before
    fun setup() {
        creditCardsAddressesStorageDelegate = mock()
        loginStorageDelegate = mock()
        geckoAutocompleteStorageDelegate = GeckoAutocompleteStorageDelegate(
            creditCardsAddressesStorageDelegate,
            loginStorageDelegate
        )
    }

    @Test
    fun onCreditCardFetch() = runBlockingTest {
        val cardNumber = "4111111111111110"
        val plaintextCardNumber = CreditCardNumber.Plaintext(cardNumber)
        val encryptedCardNumber = CreditCardNumber.Encrypted(cardNumber)
        val creditCard = CreditCard(
            guid = "id",
            billingName = "Banana Apple",
            encryptedCardNumber = encryptedCardNumber,
            cardNumberLast4 = "1110",
            expiryMonth = 5,
            expiryYear = 2030,
            cardType = "amex",
            timeCreated = 1L,
            timeLastUsed = 1L,
            timeLastModified = 1L,
            timesUsed = 1L
        )
        val creditCards = listOf(creditCard)

        whenever(creditCardsAddressesStorageDelegate.onCreditCardsFetch()).thenReturn(creditCards)
        whenever(creditCardsAddressesStorageDelegate.decrypt(any())).thenReturn(plaintextCardNumber)

        val result = geckoAutocompleteStorageDelegate.onCreditCardFetch()

        verify(creditCardsAddressesStorageDelegate).onCreditCardsFetch()
        verify(creditCardsAddressesStorageDelegate).decrypt(encryptedCardNumber)

        val autocompleteCreditCards = result?.await()

        assertNotNull(autocompleteCreditCards)
        assertEquals(creditCards.size, autocompleteCreditCards?.size)

        with(autocompleteCreditCards?.get(0)!!) {
            assertEquals(creditCard.guid, guid)
            assertEquals(creditCard.billingName, name)
            assertEquals(cardNumber, number)
            assertEquals(creditCard.expiryMonth.toString(), expirationMonth)
            assertEquals(creditCard.expiryYear.toString(), expirationYear)
        }
    }

    @Test
    fun onLoginSave() {
        val login = Autocomplete.LoginEntry.Builder()
            .guid("id")
            .origin("https://www.origin.com")
            .formActionOrigin("https://www.origin.com")
            .httpRealm("httpRealm")
            .username("usernameField")
            .password("passwordField")
            .build()

        geckoAutocompleteStorageDelegate.onLoginSave(login)

        verify(loginStorageDelegate).onLoginSave(login.toLogin())
    }

    @Test
    fun onLoginFetch() = runBlockingTest {
        val domain = "https://www.origin.com"
        val login = createLogin()
        val logins = listOf(createLogin())

        whenever(loginStorageDelegate.onLoginFetch(anyString())).thenReturn(logins)

        val result = geckoAutocompleteStorageDelegate.onLoginFetch(domain)

        verify(loginStorageDelegate).onLoginFetch(domain)

        val autocompleteLoginEntries = result?.await()

        assertNotNull(autocompleteLoginEntries)
        assertEquals(logins.size, autocompleteLoginEntries?.size)

        with(autocompleteLoginEntries?.get(0)!!) {
            assertEquals(login.guid, guid)
            assertEquals(login.origin, origin)
            assertEquals(login.formActionOrigin, formActionOrigin)
            assertEquals(login.httpRealm, httpRealm)
            assertEquals(login.username, username)
            assertEquals(login.password, password)
        }
    }
}
