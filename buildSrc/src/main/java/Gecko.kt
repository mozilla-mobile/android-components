/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

internal object GeckoVersions {
    /**
     * GeckoView Nightly Version.
     */
    const val nightly_version = "72.0.20191202091209"

    /**
     * GeckoView Beta Version.
     */
    const val beta_version = "71.0.20200108003105"

    /**
     * GeckoView Release Version.
     */
    const val release_version = "70.0.20191022130254"
}

@Suppress("MaxLineLength")
object Gecko {
    const val geckoview_nightly = "org.mozilla.geckoview:geckoview-nightly:${GeckoVersions.nightly_version}"

    // On this release branch (releases/24.0.0) we are updating GeckoView from 71.0.20191125115111 to
    // 71.0.20200108003105 for a Fenix dot release. Since we are already past the 71 Beta cycle we
    // will have to switch to a release version here (geckoview-beta -> geckoview) if we want this
    // component (browser-engine-gecko-beta) to continue using GeckoView 71. The other option,
    // changing the release version here from 70 to 71, uplifting all required changes for that
    // and switching Fenix in the release branch to use the release component (browser-engine-gecko)
    // seems to be a way to big and risky change in comparison.
    const val geckoview_beta = "org.mozilla.geckoview:geckoview:${GeckoVersions.beta_version}"
    const val geckoview_release = "org.mozilla.geckoview:geckoview:${GeckoVersions.release_version}"
}
