/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import mozilla.components.support.ktx.android.content.systemService
import java.lang.ref.WeakReference

/**
 * The resolved layout direction for this view.
 *
 * @return {@link #LAYOUT_DIRECTION_RTL} if the layout direction is RTL or returns
 * {@link #LAYOUT_DIRECTION_LTR} if the layout direction is not RTL.
 */
val View.layoutDirection: Int
    get() =
        ViewCompat.getLayoutDirection(this)

/**
 * Is the horizontal layout direction of this view from Right to Left?
 */
val View.isRTL: Boolean
    get() = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL

/**
 * Is the horizontal layout direction of this view from Left to Right?
 */
val View.isLTR: Boolean
    get() = layoutDirection == ViewCompat.LAYOUT_DIRECTION_LTR

/**
 * Converts a value in density independent pixels (dp) to the actual pixel values for the display.
 */
fun View.dp(pixels: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, pixels.toFloat(), resources.displayMetrics).toInt()

/**
 * Returns true if this view's visibility is set to View.VISIBLE.
 */
fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

/**
 * Returns true if this view's visibility is set to View.GONE.
 */
fun View.isGone(): Boolean {
    return visibility == View.GONE
}

/**
 * Returns true if this view's visibility is set to View.INVISIBLE.
 */
fun View.isInvisible(): Boolean {
    return visibility == View.INVISIBLE
}

/**
 * Tries to focus this view and show the soft input window for it.
 */
fun View.showKeyboard() {
    ShowKeyboard(this).post()
}

/**
 * Hides the soft input window.
 */
fun View.hideKeyboard() {
    val imm = (context.getSystemService(Context.INPUT_METHOD_SERVICE) ?: return)
            as InputMethodManager

    imm.hideSoftInputFromWindow(windowToken, 0)
}

private class ShowKeyboard(view: View) : Runnable {
    private val weakReference: WeakReference<View> = WeakReference(view)
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var tries: Int = TRIES

    override fun run() {
        weakReference.get()?.let { view ->
            if (!view.isFocusable || !view.isFocusableInTouchMode) {
                // The view is not focusable - we can't show the keyboard for it.
                return
            }

            if (!view.requestFocus()) {
                // Focus this view first.
                post()
                return
            }

            view.context?.systemService<InputMethodManager>(Context.INPUT_METHOD_SERVICE)?.let { imm ->
                if (!imm.isActive(view)) {
                    // This view is not the currently active view for the input method yet.
                    post()
                    return
                }

                if (!imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)) {
                    // Showing they keyboard failed. Try again later.
                    post()
                }
            }
        }
    }

    fun post() {
        tries--

        if (tries > 0) {
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    companion object {
        private const val INTERVAL_MS = 100L
        private const val TRIES = 10
    }
}
