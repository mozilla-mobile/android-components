/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.util

import android.util.DisplayMetrics
import android.util.TypedValue

/**
 * Converts a value in density independent pixels (pxToDp) to the actual pixel values for the display.
 */
fun DisplayMetrics.pxToDp(pixels: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, pixels.toFloat(), this).toInt()
