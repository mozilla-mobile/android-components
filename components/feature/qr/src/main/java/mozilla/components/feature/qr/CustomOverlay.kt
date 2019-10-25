/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * A custom overlay [View] that sets the opacity of the background to dim it.
 * This gives a focus like look to the qr reticle.
 * @property windowFrame a reference to a [Bitmap], used to create the window frame.
 * @property activity a reference to an [Activity], used to refer the child views of
 * calling parent view.
 * @property ALPHA defines the opacity of the background.
 * @property RADIUS defines the radius of the rectangle corners.
 */

class CustomOverlay @JvmOverloads constructor (
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var windowFrame: Bitmap? = null
    private val activity = context as Activity

    companion object {
        const val RADIUS = 30.0f
        const val ALPHA = 170
    }

    /**
     * This is called before the canvas is drawn. Initializes the windowFrame only once
     * @param canvas The canvas to draw upon.
     */
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        windowFrame ?: createWindowFrame()

        canvas.drawBitmap(windowFrame!!, 0f, 0f, null)
    }

    override fun isEnabled() = false

    override fun isClickable() = false

    /**
     * This function creates the windows frame around our R.id.qr_reticle ImageView
     * dynamically, so this will scale for all display sizes. Sets an alpha of 170%
     * to give a dimmed look for the background
     */
    private fun createWindowFrame() {
        val dm = DisplayMetrics()
        val imageView = activity.findViewById(R.id.qr_reticle) as ImageView
        val loc = IntArray(2)
        imageView.getLocationInWindow(loc)
        activity.windowManager.defaultDisplay.getMetrics(dm)
        val topOffset = dm.heightPixels - height
        val bottom = imageView.bottom
        val right = imageView.right

        windowFrame = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val osCanvas = Canvas(windowFrame!!)

        val outerRectangle = Rect(0, 0, width, height)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(ALPHA, 0, 0, 0)
        osCanvas.drawRect(outerRectangle, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
        val innerRectangle = RectF(
                loc[0].toFloat(),
                (loc[1] - topOffset).toFloat(),
                (width - (width - right)).toFloat(),
                (height - (height - bottom)).toFloat()
        )
        osCanvas.drawRoundRect(innerRectangle, RADIUS, RADIUS, paint)
    }

    override fun isInEditMode(): Boolean { return true }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        windowFrame = null
    }
}
