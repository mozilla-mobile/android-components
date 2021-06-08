/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.app.Activity
import org.mozilla.samples.compose.browser.BrowserApplication
import org.mozilla.samples.compose.browser.Components

/**
 * Global components for this application.
 */
val Activity.components: Components
    get() = (application as BrowserApplication).components
