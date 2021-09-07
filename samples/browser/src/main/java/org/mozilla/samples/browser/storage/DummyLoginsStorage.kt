/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.storage

import mozilla.components.concept.storage.*
import org.json.JSONObject
import java.util.UUID

// "Encryption" for us is just serializing to JSON
internal fun encryptLoginData(username: String, password: String): String {
    var obj = JSONObject()
    obj.put("username", username)
    obj.put("password", password)
    return obj.toString()
}

internal fun decryptLoginData(secureFields: String): Pair<String, String> {
    val obj = JSONObject(secureFields)
    return Pair(obj.getString("username"), obj.getString("password"))
}

// Define some functions to convert between the login types

fun LoginEntry.toNewEncryptedLogin() = EncryptedLogin(
    guid = UUID.randomUUID().toString(),
    origin = origin,
    formActionOrigin = formActionOrigin,
    httpRealm = httpRealm,
    usernameField = usernameField,
    passwordField = passwordField,
    timesUsed = 0,
    timeLastUsed = 0,
    timeCreated = 0,
    timePasswordChanged = 0,
    secureFields =  encryptLoginData(username, password),
)

fun EncryptedLogin.toLogin(): Login {
    val (username, password) = decryptLoginData(secureFields)
    return Login(
        guid = guid,
        origin = origin,
        formActionOrigin = formActionOrigin,
        httpRealm = httpRealm,
        usernameField = usernameField,
        passwordField = passwordField,
        username = username,
        password = password,
        timesUsed = timesUsed,
        timeLastUsed = timeLastUsed,
        timeCreated = timeCreated,
        timePasswordChanged = timePasswordChanged,
    )
}

fun EncryptedLogin.copyWithEntry(entry: LoginEntry) = EncryptedLogin(
    guid = guid,
    origin = entry.origin,
    formActionOrigin = entry.formActionOrigin,
    httpRealm = entry.httpRealm,
    usernameField = entry.usernameField,
    passwordField = entry.passwordField,
    timesUsed = timesUsed,
    timeLastUsed = timeLastUsed,
    timeCreated = timeCreated,
    timePasswordChanged = timePasswordChanged,
    secureFields = encryptLoginData(entry.username, entry.password),
)

/**
 * A dummy [LoginsStorage] that returns a fixed set of fake logins. Currently sample browser does
 * not use an actual login storage backend.
 */
class DummyLoginsStorage : LoginsStorage {
    // A list of fake logins for testing purposes.
    private val logins = mutableListOf(
        EncryptedLogin(
            guid = "9282b6fd-97ba-4636-beca-975d9f4fd150",
            origin = "http://twitter.com/",
            formActionOrigin = "http://twitter.com/",
            httpRealm = null,
            usernameField = "",
            passwordField = "",
            timesUsed = 0,
            timeLastUsed = 0,
            timeCreated = 0,
            timePasswordChanged = 0,
            secureFields =  encryptLoginData("NotAnAccount", "NotReallyAPassword"),
        ),
        EncryptedLogin(
            guid = "8034a37f-5f9e-4136-95b2-e0293116b322",
            origin = "http://twitter.com/",
            formActionOrigin = "http://twitter.com/",
            httpRealm = null,
            usernameField = "",
            passwordField = "",
            timesUsed = 0,
            timeLastUsed = 0,
            timeCreated = 0,
            timePasswordChanged = 0,
            secureFields =  encryptLoginData("", "NotReallyAPassword"),
        )
    )

    override suspend fun touch(guid: String) = Unit

    override suspend fun delete(guid: String): Boolean = logins.removeAll { login -> login.guid == guid }

    override suspend fun get(guid: String): EncryptedLogin? = logins.first { login -> login.guid == guid }

    override suspend fun list(): List<EncryptedLogin> = logins

    override suspend fun wipe() = logins.clear()

    override suspend fun wipeLocal() = logins.clear()

    override suspend fun add(entry: LoginEntry): EncryptedLogin {
        return entry.toNewEncryptedLogin().also {
            logins.add(it)
        }
    }

    override suspend fun update(guid: String, entry: LoginEntry): EncryptedLogin {
        val idx = logins.indexOfFirst { it.guid == guid }
        logins[idx] = logins[idx].copyWithEntry(entry)
        return logins[idx]
    }

    // Simplified version of the findLoginToUpdate logic from application-services
    override fun findLoginToUpdate(entry: LoginEntry, logins: List<Login>): Login? {
        return logins.firstOrNull { it.username == entry.username }
    }

    // Simplified version of the addOrUpdate logic from application-services
    override suspend fun addOrUpdate(entry: LoginEntry): EncryptedLogin {
        val existing = logins.firstOrNull {
            it.origin == entry.origin && decryptLoginData(it.secureFields).first == entry.username
        }
        if (existing == null) {
            return add(entry)
        } else {
            return update(existing.guid, entry)
        }
    }

    override suspend fun getByBaseDomain(origin: String): List<EncryptedLogin> {
        return logins.filter { it.origin == origin }
    }

    override suspend fun decryptLogins(logins: List<EncryptedLogin>): List<Login> {
        return logins.map { it.toLogin() }
    }

    override suspend fun importLoginsAsync(logins: List<Login>) = JSONObject()

    override fun close() = Unit
}
