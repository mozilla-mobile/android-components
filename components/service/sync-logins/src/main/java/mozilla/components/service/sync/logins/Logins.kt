/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.logins

import mozilla.components.concept.storage.*

/// Convert between application-services data classes and the ones in
/// concepts.storage.

fun mozilla.appservices.logins.Login.toAC() = Login(
    guid = record.id,
    origin = fields.origin,
    formActionOrigin = fields.formActionOrigin,
    httpRealm = fields.httpRealm,
    usernameField = fields.usernameField,
    passwordField = fields.passwordField,
    timesUsed = record.timesUsed,
    timeCreated = record.timeCreated,
    timeLastUsed = record.timeLastUsed,
    timePasswordChanged = record.timePasswordChanged,
    username = secFields.username,
    password = secFields.password,
)

fun mozilla.appservices.logins.EncryptedLogin.toAC() = EncryptedLogin(
    guid = record.id,
    origin = fields.origin,
    formActionOrigin = fields.formActionOrigin,
    httpRealm = fields.httpRealm,
    usernameField = fields.usernameField,
    passwordField = fields.passwordField,
    timesUsed = record.timesUsed,
    timeCreated = record.timeCreated,
    timeLastUsed = record.timeLastUsed,
    timePasswordChanged = record.timePasswordChanged,
    secureFields = secFields,
)

fun LoginEntry.toAS() = mozilla.appservices.logins.LoginEntry(
    fields = mozilla.appservices.logins.LoginFields(
        origin = origin,
        formActionOrigin = formActionOrigin,
        httpRealm = httpRealm,
        usernameField = usernameField,
        passwordField = passwordField,
    ),
    secFields = mozilla.appservices.logins.SecureLoginFields(
        username = username,
        password = password,
    ),
)

fun Login.toAS() = mozilla.appservices.logins.Login(
    record = mozilla.appservices.logins.RecordFields(
        id = guid,
        timesUsed = timesUsed,
        timeCreated = timeCreated,
        timeLastUsed = timeLastUsed,
        timePasswordChanged = timePasswordChanged,
    ),
    fields = mozilla.appservices.logins.LoginFields(
        origin = origin,
        formActionOrigin = formActionOrigin,
        httpRealm = httpRealm,
        usernameField = usernameField,
        passwordField = passwordField,
    ),
    secFields = mozilla.appservices.logins.SecureLoginFields(
        username = username,
        password = password,
    ),
)

fun EncryptedLogin.toAS() = mozilla.appservices.logins.EncryptedLogin(
    record = mozilla.appservices.logins.RecordFields(
        id = guid,
        timesUsed = timesUsed,
        timeCreated = timeCreated,
        timeLastUsed = timeLastUsed,
        timePasswordChanged = timePasswordChanged,
    ),
    fields = mozilla.appservices.logins.LoginFields(
        origin = origin,
        formActionOrigin = formActionOrigin,
        httpRealm = httpRealm,
        usernameField = usernameField,
        passwordField = passwordField,
    ),
    secFields = secureFields,
)
