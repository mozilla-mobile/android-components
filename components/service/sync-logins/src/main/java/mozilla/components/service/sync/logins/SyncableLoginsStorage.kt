/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.logins

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import mozilla.appservices.sync15.SyncTelemetryPing
import mozilla.components.concept.storage.*
import mozilla.components.concept.sync.SyncableStore
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.utils.logElapsedTime
import org.json.JSONObject
import java.io.Closeable

const val DB_NAME = "logins.sqlite"

/**
 * The telemetry ping from a successful sync
 */
typealias SyncTelemetryPing = mozilla.appservices.sync15.SyncTelemetryPing

/**
 * The base class of all errors emitted by logins storage.
 *
 * Concrete instances of this class are thrown for operations which are
 * not expected to be handled in a meaningful way by the application.
 *
 * For example, caught Rust panics, SQL errors, failure to generate secure
 * random numbers, etc. are all examples of things which will result in a
 * concrete `LoginsStorageException`.
 */
typealias LoginsStorageException = mozilla.appservices.logins.LoginsStorageException

/**
 * This indicates that the authentication information (e.g. the [SyncUnlockInfo])
 * provided to [AsyncLoginsStorage.sync] is invalid. This often indicates that it's
 * stale and should be refreshed with FxA (however, care should be taken not to
 * get into a loop refreshing this information).
 */
typealias SyncAuthInvalidException = mozilla.appservices.logins.LoginsStorageException.SyncAuthInvalid

/**
 * This is thrown if `lock()`/`unlock()` pairs don't match up.
 */
typealias MismatchedLockException = mozilla.appservices.logins.LoginsStorageException.MismatchedLock

/**
 * This is thrown if `update()` is performed with a record whose GUID
 * does not exist.
 */
typealias NoSuchRecordException = mozilla.appservices.logins.LoginsStorageException.NoSuchRecord

/**
 * This is thrown on attempts to insert or update a record so that it
 * is no longer valid, where "invalid" is defined as such:
 *
 * - A record with a blank `password` is invalid.
 * - A record with a blank `hostname` is invalid.
 * - A record that doesn't have a `formSubmitURL` nor a `httpRealm` is invalid.
 * - A record that has both a `formSubmitURL` and a `httpRealm` is invalid.
 */
typealias InvalidRecordException = mozilla.appservices.logins.LoginsStorageException.InvalidRecord

/**
 * Error encrypting/decrypting logins data
 */
typealias CryptoException = mozilla.appservices.logins.LoginsStorageException.CryptoException

/**
 * This error is emitted when migrating from an sqlcipher DB in two cases:
 *
 * 1. An incorrect key is used to to open the login database
 * 2. The file at the path specified is not a sqlite database.
 *
 * SQLCipher does not give any way to distinguish between these two cases.
 */
typealias InvalidKeyException = mozilla.appservices.logins.LoginsStorageException.InvalidKey

/**
 * This error is emitted if a request to a sync server failed.
 */
typealias RequestFailedException = mozilla.appservices.logins.LoginsStorageException.RequestFailed

/**
 * Implements [LoginsStorage] and [SyncableStore] using the application-services logins library.
 *
 * Synchronization support is provided both directly (via [sync]) when only syncing this storage layer,
 * or via the SyncManager when syncing multiple stores [registerWithSyncManager].
 */
class SyncableLoginsStorage(
    private val context: Context,
    private val key: String
) : LoginsStorage, SyncableStore, AutoCloseable {
    private val logger = Logger("LoginStorage")
    private val coroutineContext by lazy { Dispatchers.IO }
    private val store by lazy {
        mozilla.appservices.logins.LoginStore(context.getDatabasePath(DB_NAME).absolutePath)
    }

    /**
     * "Warms up" this storage layer by establishing the database connection.
     */
    suspend fun warmUp() = withContext(coroutineContext) {
        logElapsedTime(logger, "Warming up storage") { store }
        Unit
    }

    /**
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override suspend fun wipe() = withContext(coroutineContext) {
        store.wipe()
    }

    /**
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override suspend fun wipeLocal() = withContext(coroutineContext) {
        store.wipeLocal()
    }

    /**
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override suspend fun delete(guid: String): Boolean = withContext(coroutineContext) {
        store.delete(guid)
    }

    /**
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override suspend fun get(guid: String): EncryptedLogin? = withContext(coroutineContext) {
        store.get(guid)?.toAC()
    }

    /**
     * @throws [NoSuchRecordException] if the login does not exist.
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(NoSuchRecordException::class, LoginsStorageException::class)
    override suspend fun touch(guid: String) = withContext(coroutineContext) {
        store.touch(guid)
    }

    /**
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override suspend fun list(): List<EncryptedLogin> = withContext(coroutineContext) {
        store.list().map { it.toAC() }
    }

    /**
     * @throws [InvalidRecordException] if the record is invalid.
     * @throws [CryptoException] invalid encryption key
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(CryptoException::class, InvalidRecordException::class, LoginsStorageException::class)
    override suspend fun add(entry: LoginEntry): EncryptedLogin = withContext(coroutineContext) {
        store.add(entry.toAS(), key).toAC()
    }

    /**
     * @throws [NoSuchRecordException] if the login does not exist.
     * @throws [CryptoException] invalid encryption key
     * @throws [InvalidRecordException] if the update would create an invalid record.
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(CryptoException::class, NoSuchRecordException::class, InvalidRecordException::class, LoginsStorageException::class)
    override suspend fun update(guid: String, entry: LoginEntry): EncryptedLogin = withContext(coroutineContext) {
        store.update(guid, entry.toAS(), key).toAC()
    }

    /**
     * @throws [InvalidRecordException] if the update would create an invalid record.
     * @throws [CryptoException] invalid encryption key
     * @throws [LoginsStorageException] if the storage is locked, and on unexpected
     *              errors (IO failure, rust panics, etc)
     */
    @Throws(CryptoException::class, InvalidRecordException::class, LoginsStorageException::class)
    override suspend fun addOrUpdate(entry: LoginEntry): EncryptedLogin = withContext(coroutineContext) {
        store.addOrUpdate(entry.toAS(), key).toAC()
    }

    override fun registerWithSyncManager() {
        store.registerWithSyncManager()
    }

    override fun getHandle(): Long {
        throw NotImplementedError("Use registerWithSyncManager instead")
    }

    /**
     * @throws [CryptoException] invalid encryption key
     * @throws [LoginsStorageException] If DB isn't empty during an import; also, on unexpected errors
     * (IO failure, rust panics, etc).
     */
    @Throws(CryptoException::class, LoginsStorageException::class)
    override suspend fun importLoginsAsync(logins: List<Login>): JSONObject = withContext(coroutineContext) {
        JSONObject(store.importMultiple(logins.map { it.toAS() }, key))
    }

    /**
     * @throws [LoginsStorageException] On unexpected errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override suspend fun getByBaseDomain(origin: String): List<EncryptedLogin> = withContext(coroutineContext) {
        store.getByBaseDomain(origin).map { it.toAC() }
    }

    /**
     * @throws [CryptoException] invalid encryption key
     * @throws [LoginsStorageException] On unexpected errors (IO failure, rust panics, etc)
     */
    @Throws(LoginsStorageException::class)
    override fun findLoginToUpdate(entry: LoginEntry, logins: List<Login>): Login? {
        return mozilla.appservices.logins.findLoginToUpdate(entry.toAS(), logins.map { it.toAS() })?.toAC()
    }

    /**
     * @throws [CryptoException] invalid encryption key
     */
    override suspend fun decryptLogins(logins: List<EncryptedLogin>): List<Login> = withContext(coroutineContext) {
        logins.map {
            mozilla.appservices.logins.decryptLogin(it.toAS(), key).toAC()
        }
    }

    override fun close() {
        coroutineContext.cancel()
        store.close()
    }
}
