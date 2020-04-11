/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

internal object GeckoVersions {
    /**
     * GeckoView Nightly Version.
     */
    const val nightly_version = "77.0.20200411093441"

    /**
     * GeckoView Beta Version.
     */
    const val beta_version = "76.0.20200409223136"

    /**
     * GeckoView Release Version.
     */
    const val release_version = "75.0.20200403170909"
}

@Suppress("MaxLineLength")
object Gecko {
    const val geckoview_nightly = "org.mozilla.geckoview:geckoview-nightly:${GeckoVersions.nightly_version}"
    const val geckoview_beta = "org.mozilla.geckoview:geckoview-beta:${GeckoVersions.beta_version}"
    const val geckoview_release = "org.mozilla.geckoview:geckoview:${GeckoVersions.release_version}"
}
