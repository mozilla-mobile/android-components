/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

@Suppress("UNUSED_PARAMETER")
class LabeledMetricType<T>(
    disabled: Boolean,
    category: String,
    lifetime: Lifetime,
    name: String,
    sendInPings: List<String>,
    subMetric: T,
    labels: Set<String>? = null
) {
    val subMetric = subMetric

    @Suppress("UNUSED_PARAMETER")
    operator fun get(label: String): T {
        return subMetric
    }
}
