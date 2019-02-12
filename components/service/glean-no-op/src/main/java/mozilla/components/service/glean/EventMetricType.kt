/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

@Suppress("UNUSED_PARAMETER")
class EventMetricType(
    disabled: Boolean,
    category: String,
    lifetime: Lifetime,
    name: String,
    sendInPings: List<String>,
    objects: List<String>,
    allowedExtraKeys: List<String>? = null
) {
    @Suppress("UNUSED_PARAMETER")
    fun record(objectId: String, value: String? = null, extra: Map<String, String>? = null) {
    }
}
