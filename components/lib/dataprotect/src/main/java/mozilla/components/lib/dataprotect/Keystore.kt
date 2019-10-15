/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.dataprotect

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.FileNotFoundException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.UnrecoverableKeyException
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal

private const val PROVIDER_ANDROID = "AndroidKeyStore"
private const val ENCRYPTED_VERSION = 0x02
internal const val PROVIDER_BOUNCY_CASTLE = "BC"

internal const val BLOCK_MODE_ECB = "ECB"
internal const val BLOCK_MODE_GCM = "GCM"
internal const val KEY_SIZE_AES = 256
internal const val KEY_SIZE_RSA = 4096
internal const val KEY_ALGORITHM_AES = "AES"
internal const val KEY_ALGORITHM_RSA = "RSA"

internal const val CIPHER_TAG_LEN = 128
internal const val ENCRYPTION_PADDING_NONE = "NoPadding"
internal const val ENCRYPTION_PADDING_RSA_PKCS1 = "PKCS1Padding"
internal const val CIPHER_SPEC_SYMMETRIC =
    "$KEY_ALGORITHM_AES/$BLOCK_MODE_GCM/$ENCRYPTION_PADDING_NONE"
internal const val CIPHER_SPEC_ASYMMETRIC =
    "$KEY_ALGORITHM_RSA/$BLOCK_MODE_ECB/$ENCRYPTION_PADDING_RSA_PKCS1"
internal const val CIPHER_NONCE_LEN = 12

// Alias of asymmetric key used for encrypting symmetric keys before writing them to file on Android before M
internal const val ALIAS_WRAP_KEY = "compat_wrap_key"
// File name prefix for files that are used for storage of encrypted secrets key on Android before M
internal const val SECRET_KEY_FILE_NAME_PREFIX = "kf_"
// Common Name for self-signed certificate that is used for KeyPair generation on Android before M
internal const val CN = "self-signed"

/**
 * KeyProvider interface
 */
interface KeyProvider {
    /**
     * Get [KeyPair] from KeyStore or generate new one if it doesn't exists
     */
    fun getOrCreateKeyPair(alias: String): KeyPair

    /**
     * Get [SecretKey] from KeyStore or generate new one if it doesn't exists
     */
    fun getOrCreateSecretKey(alias: String): SecretKey
}

/**
 * Wraps the critical functions around a Java KeyStore to better facilitate testing
 * and instrumenting.
 * <23 Implementation modified from https://gist.github.com/alapshin/c82a87d30c4a0cc3015381c454e0ebf2
 */
@Suppress("TooManyFunctions")
open class KeyStoreWrapper(private val context: Context) : KeyProvider {
    private var keystore: KeyStore? = null

    /**
     * Retrieves the underlying KeyStore, loading it if necessary.
     */
    fun getKeyStore(): KeyStore {
        var ks = keystore
        if (ks == null) {
            ks = loadKeyStore()
            keystore = ks
        }

        return ks
    }

    /**
     * Get [KeyPair] from KeyStore or generate new one if it doesn't exists
     */
    override fun getOrCreateKeyPair(alias: String): KeyPair {
        val publicKey = getKeyStore().getCertificate(alias)?.publicKey
        val privateKey = getKeyStore().getKey(alias, null) as PrivateKey?

        return if (publicKey == null || privateKey == null) {
            generateKeyPair(alias)
        } else {
            return KeyPair(publicKey, privateKey)
        }
    }

    /**
     * Generate new [KeyPair] and store it in KeyStore
     */
    @Suppress("Deprecation")
    private fun generateKeyPair(alias: String): KeyPair {
        val spec = if (Build.VERSION.SDK_INT < M) {
            android.security.KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSerialNumber(BigInteger.ONE)
                .setSubject(X500Principal("CN=$CN"))
                .setStartDate(START_DATE)
                .setEndDate(END_DATE)
                .setKeySize(KEY_SIZE_RSA)
                .build()
        } else {
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(KEY_SIZE_RSA)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build()
        }

        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA, PROVIDER_ANDROID).apply {
            initialize(spec)
        }

        return generator.generateKeyPair()
    }

    /**
     * Get [SecretKey] from KeyStore or generate new one if it doesn't exists
     */
    override fun getOrCreateSecretKey(alias: String): SecretKey {
        val secretKey = if (Build.VERSION.SDK_INT < M) {
            getSecretKeyV18(alias)
        } else {
            getSecretKeyV23(alias)
        }
        return secretKey ?: generateSecretKey(alias)
    }

    /**
     * Get [SecretKey] on Android before M
     *
     * On Android before M KeyStore doesn't support storage of symmetric keys.
     * This implementation reads encrypted key from file in internal storage, decrypts and returns it.
     */
    @Suppress("SwallowedException")
    private fun getSecretKeyV18(alias: String): SecretKey? {
        return try {
            val privateKey = getOrCreateKeyPair(ALIAS_WRAP_KEY).private
            val cipher = Cipher.getInstance(CIPHER_SPEC_ASYMMETRIC).apply {
                init(Cipher.UNWRAP_MODE, privateKey)
            }
            val wrappedSecretKey =
                context.openFileInput(SECRET_KEY_FILE_NAME_PREFIX + alias).use { input ->
                    input.readBytes()
                }
            cipher.unwrap(wrappedSecretKey, KEY_ALGORITHM_AES, Cipher.SECRET_KEY) as SecretKey
        } catch (e: FileNotFoundException) {
            null // Expected exception if key doesn't exist
        }
    }

    /**
     * Get [SecretKey] from KeyStore on Android M and later
     */
    private fun getSecretKeyV23(alias: String): SecretKey? {
        return getKeyStore().getKey(alias, null) as SecretKey?
    }

    /**
     * Generate new [SecretKey] and store it in KeyStore
     */
    private fun generateSecretKey(alias: String): SecretKey {
        return if (Build.VERSION.SDK_INT < M) {
            generateSecretKeyV18(alias)
        } else {
            generateSecretKeyV23(alias)
        }
    }

    /**
     * Generate new [SecretKey] on Android before M
     *
     * On Android before M KeyStore doesn't support generation and storage of symmetric keys.
     * This implementation generates secret key using Bouncy Castle, encrypts it with private key from [KeyPair] and
     * saves it to file in internal storage.
     */
    private fun generateSecretKeyV18(alias: String): SecretKey {
        val generator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, PROVIDER_BOUNCY_CASTLE).apply {
            init(KEY_SIZE_AES)
        }
        val secretKey = generator.generateKey()
        val publicKey = getOrCreateKeyPair(ALIAS_WRAP_KEY).public
        val cipher = Cipher.getInstance(CIPHER_SPEC_ASYMMETRIC).apply {
            init(Cipher.WRAP_MODE, publicKey)
        }
        val wrappedSecretKey = cipher.wrap(secretKey)
        context.openFileOutput(SECRET_KEY_FILE_NAME_PREFIX + alias, Context.MODE_PRIVATE)
            .use { output ->
                output.write(wrappedSecretKey)
            }

        return secretKey
    }

    /**
     * Generate new [SecretKey] and store it in KeyStore on Android M and later
     */
    @TargetApi(M)
    private fun generateSecretKeyV23(alias: String): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_AES)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        val generator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, PROVIDER_ANDROID).apply {
            init(spec)
        }

        return generator.generateKey()
    }

    /**
     * Creates a SecretKey for the given label.
     *
     * This method generates a SecretKey pre-bound to the `AndroidKeyStore` and configured
     * with the strongest "algorithm/blockmode/padding" (and key size) available.
     *
     * Subclasses override this method to properly associate the generated key with
     * the given label in the underlying KeyStore.
     *
     * @param label The label to associate with the created key
     * @return The newly-generated key for `label`
     * @throws NoSuchAlgorithmException If the cipher algorithm is not supported
     */
    open fun makeKeyFor(label: String): SecretKey {
        return generateSecretKey(label)
    }

    /**
     * Retrieves the SecretKey for the given label.
     *
     * This method queries for a SecretKey with the given label and no passphrase.
     *
     * Subclasses override this method if additional properties are needed
     * to retrieve the key.
     *
     * @param label The label to query
     * @return The key for the given label, or `null` if not present
     * @throws InvalidKeyException If there is a Key but it is not a SecretKey
     * @throws NoSuchAlgorithmException If the recovery algorithm is not supported
     * @throws UnrecoverableKeyException If the key could not be recovered for some reason
     */
    open fun getKeyFor(label: String): Key? {
        val secretKey = if (Build.VERSION.SDK_INT < M) {
            getSecretKeyV18(label)
        } else {
            getSecretKeyV23(label)
        }
        return secretKey
    }

    /**
     * Deletes a key with the given label.
     *
     * @param label The label of the associated key to delete
     * @throws KeyStoreException If there is no key for `label`
     */
    fun removeKeyFor(label: String) {
        getKeyStore().deleteEntry(label)
    }

    /**
     * Creates and initializes the KeyStore in use.
     *
     * This method loads a`"AndroidKeyStore"` type KeyStore.
     *
     * Subclasses override this to load a KeyStore appropriate to the testing environment.
     *
     * @return The KeyStore, already initialized
     * @throws KeyStoreException if the type of store is not supported
     */
    open fun loadKeyStore(): KeyStore {
        val ks = KeyStore.getInstance(PROVIDER_ANDROID)
        ks.load(null)
        return ks
    }

    companion object {
        val calendar = Calendar.getInstance()
        val START_DATE = calendar.apply { set(1970, 0, 1) }.time
        val END_DATE = calendar.apply { set(2048, 0, 1) }.time
    }
}

/**
 * Manages data protection using a system-isolated cryptographic key.
 *
 * This class provides for both:
 * * management for a specific crypto graphic key (identified by a string label)
 * * protection (encryption/decryption) of data using the managed key
 *
 * The specific cryptographic properties are pre-chosen to be the following:
 * * Algorithm is "AES/GCM/NoPadding"
 * * Key size is 256 bits
 * * Tag size is 128 bits
 *
 * @property label The label the cryptographic key is identified as
 * @constructor Creates a new instance around a key identified by the given label
 *
 * Unless `manual` is `true`, the key is created if not already present in the
 * platform's key storage.
 */
open class Keystore(
    val context: Context,
    val label: String,
    manual: Boolean = false,
    internal val wrapper: KeyStoreWrapper = KeyStoreWrapper(context)
) {
    init {
        if (!manual and !available()) {
            generateKey()
        }
    }

    private fun getKey(): SecretKey? =
        wrapper.getKeyFor(label) as? SecretKey?

    /**
     * Determines if the managed key is available for use.  Consumers can use this to
     * determine if the key was somehow lost and should treat any previously-protected
     * data as invalid.
     *
     * @return `true` if the managed key exists and ready for use.
     */
    fun available(): Boolean = (getKey() != null)

    /**
     * Generates the managed key if it does not already exist.
     *
     * @return `true` if a new key was generated; `false` if the key already exists and can
     * be used.
     * @throws GeneralSecurityException If the key could not be created
     */
    @Throws(GeneralSecurityException::class)
    fun generateKey(): Boolean {
        val key = wrapper.getKeyFor(label)
        if (key != null) {
            when (key) {
                is SecretKey -> return false
                else -> throw InvalidKeyException("unsupported key type")
            }
        }

        wrapper.makeKeyFor(label)

        return true
    }

    /**
     *  Deletes the managed key.
     *
     *  **NOTE:** Once this method returns, any data protected with the (formerly) managed
     *  key cannot be decrypted and therefore is inaccessble.
     */
    fun deleteKey() {
        val key = wrapper.getKeyFor(label)
        if (key != null) {
            wrapper.removeKeyFor(label)
        }
    }

    /**
     * Encrypts data using the managed key.
     *
     * The output of this method includes the input factors (i.e., initialization vector),
     * ciphertext, and authentication tag as a single byte string; this output can be passed
     * directly to [decryptBytes].
     *
     * @param plain The "plaintext" data to encrypt
     * @return The encrypted data to be stored
     * @throws GeneralSecurityException If the data could not be encrypted
     */
    @Throws(GeneralSecurityException::class)
    open fun encryptBytes(plain: ByteArray): ByteArray {
        // 5116-style interface  = [ inputs || ciphertext || atag ]
        // - inputs = [ version = 0x02 || cipher.iv (always 12 bytes) ]
        // - cipher.doFinal() provides [ ciphertext || atag ]
        val cipher = createEncryptCipher()
        val cdata = cipher.doFinal(plain)
        val nonce = cipher.iv

        return byteArrayOf(ENCRYPTED_VERSION.toByte()) + nonce + cdata
    }

    /**
     * Decrypts data using the managed key.
     *
     * The input of this method is expected to include input factors (i.e., initialization
     * vector), ciphertext, and authentication tag as a single byte string; it is the direct
     * output from [encryptBytes].
     *
     * @param encrypted The encrypted data to decrypt
     * @return The decrypted "plaintext" data
     * @throws KeystoreException If the data could not be decrypted
     */
    @Throws(KeystoreException::class)
    open fun decryptBytes(encrypted: ByteArray): ByteArray {
        val version = encrypted[0].toInt()
        if (version != ENCRYPTED_VERSION) {
            throw KeystoreException("unsupported encrypted version: $version")
        }

        val iv = encrypted.sliceArray(1..CIPHER_NONCE_LEN)
        val cdata = encrypted.sliceArray((CIPHER_NONCE_LEN + 1) until encrypted.size)
        val cipher = createDecryptCipher(iv)
        return cipher.doFinal(cdata)
    }

    /**
     * Create a cipher initialized for encrypting data with the managed key.
     *
     * This "low-level" method is useful when a cryptographic context is needed to integrate with
     * other APIs, such as the `FingerprintManager`.
     *
     * **NOTE:** The caller is responsible for associating certain encryption factors, such as
     * the initialization vector and/or additional authentication data (AAD), with the resulting
     * ciphertext or decryption will fail.
     *
     * @return The [Cipher], initialized and ready to encrypt data with.
     * @throws GeneralSecurityException If the Cipher could not be created and initialized
     */
    @Throws(GeneralSecurityException::class)
    open fun createEncryptCipher(): Cipher {
        val key = getKey() ?: throw InvalidKeyException("unknown label: $label")
        val cipher = Cipher.getInstance(CIPHER_SPEC_SYMMETRIC)
        val iv = ByteArray(CIPHER_NONCE_LEN)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(CIPHER_TAG_LEN, iv))

        return cipher
    }

    /**
     * Create a cipher initialized for decrypting data with the managed key.
     *
     * This "low-level" method is useful when a cryptographic context is needed to integrate with
     * other APIs, such as the `FingerprintManager`.
     *
     * **NOTE:** The caller is responsible for associating certain encryption factors, such as
     * the initialization vector and/or additional authentication data (AAD), with the stored
     * ciphertext or decryption will fail.
     *
     * @param iv The initialization vector/nonce to decrypt with
     * @return The [Cipher], initialized and ready to decrypt data with.
     * @throws GeneralSecurityException If the cipher could not be created and initialized
     */
    @Throws(GeneralSecurityException::class)
    open fun createDecryptCipher(iv: ByteArray): Cipher {
        val key = getKey() ?: throw InvalidKeyException("unknown label: $label")
        val cipher = Cipher.getInstance(CIPHER_SPEC_SYMMETRIC)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(CIPHER_TAG_LEN, iv))

        return cipher
    }
}
