/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.config

@Suppress("UNUSED_PARAMETER")
class Configuration(
    serverEndpoint: String = "",
    userAgent: String = "",
    connectionTimeout: Int = 0,
    readTimeout: Int = 0,
    maxEvents: Int = 0,
    logPings: Boolean = false
)
