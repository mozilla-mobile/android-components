/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.autofill

import android.content.Context
import androidx.annotation.GuardedBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import mozilla.appservices.autofill.ErrorException.NoSuchRecord
import mozilla.components.concept.storage.Address
import mozilla.components.concept.storage.CreditCard
import mozilla.components.concept.storage.CreditCardNumber
import mozilla.components.concept.storage.CreditCardsAddressesStorage
import mozilla.components.concept.storage.KeyGenerationReason
import mozilla.components.concept.storage.KeyRecoveryHandler
import mozilla.components.concept.storage.NewCreditCardFields
import mozilla.components.concept.storage.UpdatableAddressFields
import mozilla.components.concept.storage.UpdatableCreditCardFields
import mozilla.components.concept.sync.SyncableStore
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import mozilla.components.support.base.log.logger.Logger
import java.io.Closeable
import mozilla.appservices.autofill.Store as RustAutofillStorage

const val AUTOFILL_DB_NAME = "autofill.sqlite"

/**
 * An implementation of [CreditCardsAddressesStorage] backed by the application-services' `autofill`
 * library.
 *
 * @param context A [Context] used for disk access.
 * @param securePrefs A [SecureAbove22Preferences] wrapped in [Lazy] to avoid eager instantiation.
 * Used for storing encryption key material.
 */
class AutofillCreditCardsAddressesStorage(
    context: Context,
    securePrefs: Lazy<SecureAbove22Preferences>
) : CreditCardsAddressesStorage, KeyRecoveryHandler, SyncableStore, AutoCloseable {
    private val logger = Logger("AutofillCCAddressesStorage")

    private val coroutineContext by lazy { Dispatchers.IO }

    val crypto by lazy { AutofillCrypto(context, securePrefs.value, this) }

    private val conn by lazy {
        AutofillStorageConnection.init(dbPath = context.getDatabasePath(AUTOFILL_DB_NAME).absolutePath)
        AutofillStorageConnection
    }

    override fun recoverFromBadKey(reason: KeyGenerationReason.RecoveryNeeded) {
        when (reason) {
            // At this point, we need A-S API to recover: https://github.com/mozilla/application-services/issues/4015
            is KeyGenerationReason.RecoveryNeeded.Lost -> logger.warn("CC key lost, new one generated")
            is KeyGenerationReason.RecoveryNeeded.Corrupt -> logger.warn("CC key was corrupted, new one generated")
            is KeyGenerationReason.RecoveryNeeded.AbnormalState -> logger.warn(
                "CC key lost due to storage malfunction, new one generated"
            )
        }
    }

    override suspend fun addCreditCard(
        creditCardFields: NewCreditCardFields
    ): CreditCard = withContext(coroutineContext) {
        val key = crypto.key()

        // Assume our key is good, and that this operation shouldn't fail.
        val encryptedCardNumber = crypto.encrypt(key, creditCardFields.plaintextCardNumber)!!
        val updatableCreditCardFields = UpdatableCreditCardFields(
            billingName = creditCardFields.billingName,
            cardNumber = encryptedCardNumber,
            cardNumberLast4 = creditCardFields.cardNumberLast4,
            expiryMonth = creditCardFields.expiryMonth,
            expiryYear = creditCardFields.expiryYear,
            cardType = creditCardFields.cardType
        )

        conn.getStorage().addCreditCard(updatableCreditCardFields.into()).into()
    }

    override suspend fun updateCreditCard(
        guid: String,
        creditCardFields: UpdatableCreditCardFields
    ) = withContext(coroutineContext) {
        val updatableCreditCardFields = when (creditCardFields.cardNumber) {
            // If credit card number changed, we need to encrypt it.
            is CreditCardNumber.Plaintext -> {
                val key = crypto.key()
                // Assume our key is good, and that this operation shouldn't fail.
                val encryptedCardNumber = crypto.encrypt(
                    key, creditCardFields.cardNumber as CreditCardNumber.Plaintext
                )!!
                UpdatableCreditCardFields(
                    billingName = creditCardFields.billingName,
                    cardNumber = encryptedCardNumber,
                    cardNumberLast4 = creditCardFields.cardNumberLast4,
                    expiryMonth = creditCardFields.expiryMonth,
                    expiryYear = creditCardFields.expiryYear,
                    cardType = creditCardFields.cardType
                )
            }
            // If card number didn't change, we're just round-tripping an existing encrypted version.
            is CreditCardNumber.Encrypted -> {
                UpdatableCreditCardFields(
                    billingName = creditCardFields.billingName,
                    cardNumber = creditCardFields.cardNumber,
                    cardNumberLast4 = creditCardFields.cardNumberLast4,
                    expiryMonth = creditCardFields.expiryMonth,
                    expiryYear = creditCardFields.expiryYear,
                    cardType = creditCardFields.cardType
                )
            }
        }
        conn.getStorage().updateCreditCard(guid, updatableCreditCardFields.into())
    }

    override suspend fun getCreditCard(guid: String): CreditCard? = withContext(coroutineContext) {
        try {
            conn.getStorage().getCreditCard(guid).into()
        } catch (e: NoSuchRecord) {
            null
        }
    }

    override suspend fun getAllCreditCards(): List<CreditCard> = withContext(coroutineContext) {
        conn.getStorage().getAllCreditCards().map { it.into() }
    }

    override suspend fun deleteCreditCard(guid: String): Boolean = withContext(coroutineContext) {
        conn.getStorage().deleteCreditCard(guid)
    }

    override suspend fun touchCreditCard(guid: String) = withContext(coroutineContext) {
        conn.getStorage().touchCreditCard(guid)
    }

    override suspend fun addAddress(addressFields: UpdatableAddressFields): Address =
        withContext(coroutineContext) {
            conn.getStorage().addAddress(addressFields.into()).into()
        }

    override suspend fun getAddress(guid: String): Address? = withContext(coroutineContext) {
        try {
            conn.getStorage().getAddress(guid).into()
        } catch (e: NoSuchRecord) {
            null
        }
    }

    override suspend fun getAllAddresses(): List<Address> = withContext(coroutineContext) {
        conn.getStorage().getAllAddresses().map { it.into() }
    }

    override suspend fun updateAddress(guid: String, address: UpdatableAddressFields) =
        withContext(coroutineContext) {
            conn.getStorage().updateAddress(guid, address.into())
        }

    override suspend fun deleteAddress(guid: String): Boolean = withContext(coroutineContext) {
        conn.getStorage().deleteAddress(guid)
    }

    override suspend fun touchAddress(guid: String) = withContext(coroutineContext) {
        conn.getStorage().touchAddress(guid)
    }

    override fun getCreditCardCrypto(): AutofillCrypto {
        return crypto
    }

    override fun registerWithSyncManager() {
        conn.getStorage().registerWithSyncManager()
    }

    override fun getHandle(): Long {
        throw NotImplementedError("Use registerWithSyncManager instead")
    }

    override fun close() {
        coroutineContext.cancel()
        conn.close()
    }
}

/**
 * A singleton wrapping a [RustAutofillStorage] connection.
 */
internal object AutofillStorageConnection : Closeable {
    @GuardedBy("this")
    private var storage: RustAutofillStorage? = null

    internal fun init(dbPath: String = AUTOFILL_DB_NAME) = synchronized(this) {
        if (storage == null) {
            storage = RustAutofillStorage(dbPath)
        }
    }

    internal fun getStorage(): RustAutofillStorage = synchronized(this) {
        check(storage != null) { "must call init first" }
        return storage!!
    }

    override fun close() = synchronized(this) {
        check(storage != null) { "must call init first" }
        storage!!.destroy()
        storage = null
    }
}
