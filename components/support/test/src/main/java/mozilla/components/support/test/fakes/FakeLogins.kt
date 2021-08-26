/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.test.fakes

import mozilla.components.concept.storage.Login
import mozilla.components.concept.storage.EncryptedLogin
import mozilla.components.concept.storage.LoginEntry

// Factory functions to create a fake login data

fun fakeEncryptedLogin(
    guid: String = "test-guid",
    origin: String = "https://example.com",
    formActionOrigin: String? = "https://example.com",
    httpRealm: String? = null,
    usernameField: String = "field-username",
    passwordField: String = "field-password",
    timesUsed: Long = 0,
    timeCreated: Long = 0,
    timeLastUsed: Long = 0,
    timePasswordChanged: Long = 0,
    secureFields: String = "fake-encrypted-data",
) = EncryptedLogin (
    guid = guid,
    origin = origin,
    formActionOrigin = formActionOrigin,
    httpRealm = httpRealm,
    usernameField = usernameField,
    passwordField = passwordField,
    timesUsed = timesUsed,
    timeCreated = timeCreated,
    timeLastUsed = timeLastUsed,
    timePasswordChanged = timePasswordChanged,
    secureFields = secureFields,
)

fun fakeLogin(
    guid: String = "test-guid",
    origin: String = "https://example.com",
    formActionOrigin: String? = "https://example.com",
    httpRealm: String? = null,
    usernameField: String = "field-username",
    passwordField: String = "field-password",
    timesUsed: Long = 0,
    timeCreated: Long = 0,
    timeLastUsed: Long = 0,
    timePasswordChanged: Long = 0,
    username: String = "test-user",
    password: String = "test-pass",
) = Login (
    guid = guid,
    origin = origin,
    formActionOrigin = formActionOrigin,
    httpRealm = httpRealm,
    usernameField = usernameField,
    passwordField = passwordField,
    timesUsed = timesUsed,
    timeCreated = timeCreated,
    timeLastUsed = timeLastUsed,
    timePasswordChanged = timePasswordChanged,
    username = username,
    password = password,
)

fun fakeLoginEntry(
    origin: String = "https://example.com",
    formActionOrigin: String? = "https://example.com",
    httpRealm: String? = null,
    usernameField: String = "field-username",
    passwordField: String = "field-password",
    username: String = "test-user",
    password: String = "test-pass",
) = LoginEntry(
    origin = origin,
    formActionOrigin = formActionOrigin,
    httpRealm = httpRealm,
    usernameField = usernameField,
    passwordField = passwordField,
    username = username,
    password = password,
)
