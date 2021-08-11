/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.ext

import mozilla.components.concept.engine.prompt.CreditCard
import mozilla.components.support.utils.creditCardIIN
import org.mozilla.geckoview.Autocomplete

/**
 * Converts a GeckoView [Autocomplete.CreditCard] to an Android Components [CreditCard].
 */
fun Autocomplete.CreditCard.toCreditCard() = CreditCard(
    guid = guid,
    name = name,
    number = number,
    expiryMonth = expirationMonth,
    expiryYear = expirationYear,
    cardType = number.creditCardIIN()?.creditCardIssuerNetwork?.name ?: ""
)

/**
 * Converts an Android Components [CreditCard] to a GeckoView [Autocomplete.CreditCard].
 */
fun CreditCard.toAutocompleteCreditCard() = Autocomplete.CreditCard.Builder()
    .guid(guid)
    .name(name)
    .number(number)
    .expirationMonth(expiryMonth)
    .expirationYear(expiryYear)
    .build()
