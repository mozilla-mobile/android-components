/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.test

import mozilla.components.concept.storage.Login

fun createLogin(
    guid: String = "id",
    password: String = "password",
    username: String = "username",
    origin: String = "https://www.origin.com",
    httpRealm: String = "httpRealm",
    formActionOrigin: String = "https://www.origin.com",
    usernameField: String = "usernameField",
    passwordField: String = "passwordField"
) = Login(
    guid = guid,
    origin = origin,
    password = password,
    username = username,
    httpRealm = httpRealm,
    formActionOrigin = formActionOrigin,
    usernameField = usernameField,
    passwordField = passwordField
)
