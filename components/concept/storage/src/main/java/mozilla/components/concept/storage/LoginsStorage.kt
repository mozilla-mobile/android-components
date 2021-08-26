/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.storage

import kotlinx.coroutines.Deferred
import org.json.JSONObject

/**
 * A login stored in the database
 */
data class Login (
    /**
     * The unique identifier for this login entry.
     */
    val guid: String,
    /**
     * The username for this login entry.
     */
    val username: String,
    /**
     * The password for this login entry.
     */
    val password: String,
    /**
     * The origin this login entry applies to.
     */
    val origin: String,
    /**
     * The origin this login entry was submitted to.
     * This only applies to form-based login entries.
     * It's derived from the action attribute set on the form element.
     */
    val formActionOrigin: String?,
    /**
     * The HTTP realm this login entry was requested for.
     * This only applies to non-form-based login entries.
     * It's derived from the WWW-Authenticate header set in a HTTP 401
     * response, see RFC2617 for details.
     */
    val httpRealm: String?,
    /**
     * HTML field associated with the [username].
     */
    val usernameField: String,
    /**
     * HTML field associated with the [password].
     */
    val passwordField: String,
    /**
     * Number of times this password has been used.
     */
    val timesUsed: Long,
    /**
     * Time of creation in milliseconds from the unix epoch.
     */
    val timeCreated: Long,
    /**
     * Time of last use in milliseconds from the unix epoch.
     */
    val timeLastUsed: Long,
    /**
     * Time of last password change in milliseconds from the unix epoch.
     */
    val timePasswordChanged: Long,
) {
    fun toEntry() = LoginEntry(
        origin = origin,
        formActionOrigin = formActionOrigin,
        httpRealm = httpRealm,
        usernameField = usernameField,
        passwordField = passwordField,
        username = username,
        password = password,
    )
}

/**
 * A [Login], but with sensitive data encrypted
 */
data class EncryptedLogin (
    /**
     * These are the same as the [Login]
     */
    val guid: String,
    val origin: String,
    val formActionOrigin: String?,
    val httpRealm: String?,
    val usernameField: String,
    val passwordField: String,
    val timesUsed: Long,
    val timeCreated: Long,
    val timeLastUsed: Long,
    val timePasswordChanged: Long,
    /**
     * This encrypts the username/password data
     */
    val secureFields: String,
)

/**
 * Login autofill entry
 *
 * This contains the data needed to handle autofill but not the data related to
 * the DB record.  [LoginsStorage] methods that save data typically input
 * [LoginEntry] instances.  This allows the storage backend handle
 * dupe-checking issues like determining which login record should be updated
 * for a given [LoginEntry].
 *
 * [LoginEntry] also represents the login data that's editable in the API.
 *
 * [LoginEntry] is a data class rather than an interface because the consumer
 * code needs to construct these.  All fields have the same meaning as in
 * [Login].
 */
data class LoginEntry(
    val origin: String,
    val formActionOrigin: String? = null,
    val httpRealm: String? = null,
    val usernameField: String = "",
    val passwordField: String = "",
    val username: String,
    val password: String,
)

/**
 * An interface describing a storage layer for logins/passwords.
 */
interface LoginsStorage : AutoCloseable {
    /**
     * Deletes all login records. These deletions will be synced to the server on the next call to sync.
     */
    suspend fun wipe()

    /**
     * Clears out all local state, bringing us back to the state before the first write (or sync).
     */
    suspend fun wipeLocal()

    /**
     * Deletes the password with the given guid.
     *
     * @return True if the deletion did anything, false otherwise.
     */
    suspend fun delete(guid: String): Boolean

    /**
     * Fetches a password from the underlying storage layer by its GUID
     *
     * @param guid Unique identifier for the desired record.
     * @return [EncryptedLogin] record, or `null` if the record does not exist.
     */
    suspend fun get(guid: String): EncryptedLogin?

    /**
     * Marks that a login has been used
     *
     * @param guid Unique identifier for the desired record.
     */
    suspend fun touch(guid: String)

    /**
     * Fetches the full list of logins from the underlying storage layer.
     *
     * @return A list of stored [EncryptedLogin] records.
     */
    suspend fun list(): List<EncryptedLogin>

    /**
     * Calculate how we should save a login
     *
     * Given a [LoginEntry] to save, and a list of [Login] records stored in
     * the database, choose which [Login] should be updated (if any).
     *
     * One assumption is that the list of login records are for the same site
     * as the new [LoginEntry].  The intended use is to call
     * [getByBaseDomain()], decrypt all the logins, then pass in the decrypted
     * login list.
     *
     * @param entry [LoginEntry] being saved
     * @param logins list of [Login] records to search
     * @return [Login] that should be updated, or null if the login should be added
     */
    fun findLoginToUpdate(entry: LoginEntry, logins: List<Login>): Login?

    /**
     * Inserts the provided login into the database

     * This will return an error result if the provided record is invalid
     * (missing password, origin, or doesn't have exactly one of formSubmitURL
     * and httpRealm).
     *
     * @param login [LoginEntry] to add.
     * @return [EncryptedLogin] for the created record
     */
    suspend fun add(entry: LoginEntry): EncryptedLogin

    /**
     * Updates an existing login in the database
     *
     * This will throw if `guid` does not refer to a record that exists in the
     * database, or if the provided record is invalid (missing password,
     * origin, or doesn't have exactly one of formSubmitURL and httpRealm).
     *
     * @param guid Unique identifier for the record
     * @param login [LoginEntry] to add.
     * @return [EncryptedLogin] for the updated record
     */
    suspend fun update(guid: String, entry: LoginEntry): EncryptedLogin

    /**
     * Checks if a record exists for a [LoginEntry] and calls either add() or update()
     *
     * This will throw if the provided record is invalid (missing password,
     * origin, or doesn't have exactly one of formSubmitURL and httpRealm).
     *
     * @param login [LoginEntry] to add or update.
     * @return [EncryptedLogin] for the added/updated record
     */
    suspend fun addOrUpdate(entry: LoginEntry): EncryptedLogin

    /**
     * Bulk-import of a list of [Login].
     * Storage must be empty; implementations expected to throw otherwise.
     *
     * This method exists to support the Fennic -> Fenix migration.  It needs
     * to input [Login] instances in order to ensure the imported logins get
     * the same GUID.
     *
     * @param logins A list of [Login] records to be imported.
     * @return JSON object with detailed information about imported logins.
     */
    suspend fun importLoginsAsync(logins: List<Login>): JSONObject

    /**
     * Fetch the list of logins for some origin from the underlying storage layer.
     *
     * @param origin A host name used to look up logins
     * @return A list of [EncryptedLogin] objects, representing matching logins.
     */
    suspend fun getByBaseDomain(origin: String): List<EncryptedLogin>

    /**
     * Decrypt an [EncryptedLogin] list
     *
     * This is a suspending function to prepare for a future world where we
     * defer getting the encryption key until we need to decrypt.  In that
     * world, this method might be long-running while we wait for the user to
     * authenticate.
     *
     * @param logins [List<EncryptedLogin>] to decrypt
     * @return [Login]
     */
    suspend fun decryptLogins(logins: List<EncryptedLogin>): List<Login>
}
