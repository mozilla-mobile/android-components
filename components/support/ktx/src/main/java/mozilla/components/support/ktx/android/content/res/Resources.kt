/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.content.res

import android.content.res.Resources
import mozilla.components.support.ktx.android.util.pxToDp

/**
 * Converts a value in density independent pixels (pxToDp) to the actual pixel values for the display.
 */
fun Resources.pxToDp(pixels: Int) = displayMetrics.pxToDp(pixels)
