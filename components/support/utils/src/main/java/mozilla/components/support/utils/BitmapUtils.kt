/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.support.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.v7.graphics.Palette
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.os.Build

object BitmapUtils {

    @JvmStatic
    fun decodeByteArray(
        bytes: ByteArray,
        offset: Int = 0,
        length: Int = bytes.size,
        options: BitmapFactory.Options? = null
    ): Bitmap? {
        if (bytes.isEmpty()) {
            throw IllegalArgumentException(
                "bytes.length " + bytes.size
                    + " must be a positive number"
            )
        }

        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeByteArray(bytes, offset, length, options)
        } catch (e: OutOfMemoryError) {
            Logger.error(
                ("decodeByteArray(bytes.length=" + bytes.size
                    + ", options= " + options + ") OOM!"), e
            )
            return null
        }

        if (bitmap == null) {
            Logger.warn("decodeByteArray() returning null because BitmapFactory returned null")
            return null
        }

        if (bitmap.width <= 0 || bitmap.height <= 0) {
            Logger.warn(
                ("decodeByteArray() returning null because BitmapFactory returned "
                    + "a bitmap with dimensions " + bitmap.width
                    + "x" + bitmap.height)
            )
            return null
        }

        return bitmap
    }

    @ColorInt
    @JvmStatic
    fun getDominantColor(source: Bitmap, @ColorInt defaultColor: Int): Int {
        return if (Build.isX86System()) {
            // (Bug 1318667) We are running into crashes when using the palette library with
            // specific icons on x86 devices. They take down the whole VM and are not recoverable.
            // Unfortunately our release icon is triggering this crash. Until we can switch to a
            // newer version of the support library where this does not happen, we are using our
            // own slower implementation.
            getDominantColorCustomImplementation(source, true, defaultColor)
        } else {
            try {
                val palette = Palette.from(source).generate()
                palette.getVibrantColor(defaultColor)
            } catch (e: ArrayIndexOutOfBoundsException) {
                // We saw the palette library fail with an ArrayIndexOutOfBoundsException intermittently
                // in automation. In this case lets just swallow the exception and move on without a
                // color. This is a valid condition and callers should handle this gracefully (Bug 1318560).
                Logger.warn("Palette generation failed with ArrayIndexOutOfBoundsException", e)

                defaultColor
            }
        }
    }

    @ColorInt
    @JvmStatic
    fun getDominantColorCustomImplementation(
        source: Bitmap?,
        applyThreshold: Boolean = true,
        @ColorInt defaultColor: Int = Color.WHITE
    ): Int {
        if (source == null) {
            return defaultColor
        }

        // Keep track of how many times a hue in a given bin appears in the image.
        // Hue values range [0 .. 360), so dividing by 10, we get 36 bins.
        val colorBins = IntArray(36)

        // The bin with the most colors. Initialize to -1 to prevent accidentally
        // thinking the first bin holds the dominant color.
        var maxBin = -1

        // Keep track of sum hue/saturation/value per hue bin, which we'll use to
        // compute an average to for the dominant color.
        val sumHue = FloatArray(36)
        val sumSat = FloatArray(36)
        val sumVal = FloatArray(36)
        val hsv = FloatArray(3)

        val height = source.height
        val width = source.width
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        for (row in 0 until height) {
            for (col in 0 until width) {
                val c = pixels[col + row * width]
                // Ignore pixels with a certain transparency.
                if (Color.alpha(c) < 128)
                    continue

                Color.colorToHSV(c, hsv)

                // If a threshold is applied, ignore arbitrarily chosen values for "white" and "black".
                if (applyThreshold && (hsv[1] <= 0.35f || hsv[2] <= 0.35f))
                    continue

                // We compute the dominant color by putting colors in bins based on their hue.
                val bin = Math.floor((hsv[0] / 10.0f).toDouble()).toInt()

                // Update the sum hue/saturation/value for this bin.
                sumHue[bin] = sumHue[bin] + hsv[0]
                sumSat[bin] = sumSat[bin] + hsv[1]
                sumVal[bin] = sumVal[bin] + hsv[2]

                // Increment the number of colors in this bin.
                colorBins[bin]++

                // Keep track of the bin that holds the most colors.
                if (maxBin < 0 || colorBins[bin] > colorBins[maxBin])
                    maxBin = bin
            }
        }

        // maxBin may never get updated if the image holds only transparent and/or black/white pixels.
        if (maxBin < 0) {
            return defaultColor
        }

        // Return a color with the average hue/saturation/value of the bin with the most colors.
        hsv[0] = sumHue[maxBin] / colorBins[maxBin]
        hsv[1] = sumSat[maxBin] / colorBins[maxBin]
        hsv[2] = sumVal[maxBin] / colorBins[maxBin]
        return Color.HSVToColor(hsv)
    }
}