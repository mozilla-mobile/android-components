/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.view

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.annotation.MainThread
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import mozilla.components.support.base.android.Padding
import mozilla.components.support.ktx.android.util.dpToPx
import java.lang.ref.WeakReference

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
 * Returns true if this view's visibility is set to View.VISIBLE.
 */
@Deprecated("Use Android KTX instead", ReplaceWith("isVisible", "androidx.core.view.isVisible"))
fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

/**
 * Returns true if this view's visibility is set to View.GONE.
 */
@Deprecated("Use Android KTX instead", ReplaceWith("isGone", "androidx.core.view.isGone"))
fun View.isGone(): Boolean {
    return visibility == View.GONE
}

/**
 * Returns true if this view's visibility is set to View.INVISIBLE.
 */
@Deprecated("Use Android KTX instead", ReplaceWith("isInvisible", "androidx.core.view.isInvisible"))
fun View.isInvisible(): Boolean {
    return visibility == View.INVISIBLE
}

/**
 * Tries to focus this view and show the soft input window for it.
 *
 *  @param flags Provides additional operating flags to be used with InputMethodManager.showSoftInput().
 *  Currently may be 0, SHOW_IMPLICIT or SHOW_FORCED.
 */
fun View.showKeyboard(flags: Int = InputMethodManager.SHOW_IMPLICIT) {
    ShowKeyboard(this, flags).post()
}

/**
 * Hides the soft input window.
 */
fun View.hideKeyboard() {
    val imm = (context.getSystemService(Context.INPUT_METHOD_SERVICE) ?: return)
        as InputMethodManager

    imm.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Fills the given [Rect] with data about location view in the window.
 *
 * @see View.getLocationInWindow
 */
fun View.getRectWithViewLocation(): Rect {
    val locationInWindow = IntArray(2).apply { getLocationInWindow(this) }
    return Rect(locationInWindow[0],
            locationInWindow[1],
            locationInWindow[0] + width,
            locationInWindow[1] + height)
}

/**
 * Set a padding using [Padding] object.
 */
fun View.setPadding(padding: Padding) {
    with(resources) {
        setPadding(
            padding.left.dpToPx(displayMetrics),
            padding.top.dpToPx(displayMetrics),
            padding.right.dpToPx(displayMetrics),
            padding.bottom.dpToPx(displayMetrics)
        )
    }
}

/**
 * Creates a [CoroutineScope] that is active as long as this [View] is attached. Once this [View]
 * gets detached this [CoroutineScope] gets cancelled automatically.
 *
 * By default coroutines dispatched on the created [CoroutineScope] run on the main dispatcher.
 *
 * Note: This scope gets only cancelled if the [View] gets detached. In cases where the [View] never
 * gets attached this may create a scope that never gets cancelled!
 */
@MainThread
fun View.toScope(): CoroutineScope {
    val scope = CoroutineScope(Dispatchers.Main)

    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit

        override fun onViewDetachedFromWindow(view: View) {
            scope.cancel()
            view.removeOnAttachStateChangeListener(this)
        }
    })

    return scope
}

/**
 * Registers a one-time callback to be invoked when the global layout state
 * or the visibility of views within the view tree changes.
 */
inline fun View.onNextGlobalLayout(crossinline callback: () -> Unit) {
    var listener: ViewTreeObserver.OnGlobalLayoutListener? = null
    listener = ViewTreeObserver.OnGlobalLayoutListener {
        viewTreeObserver.removeOnGlobalLayoutListener(listener)
        callback()
    }
    viewTreeObserver.addOnGlobalLayoutListener(listener)
}

private class ShowKeyboard(
    view: View,
    private val flags: Int = InputMethodManager.SHOW_IMPLICIT
) : Runnable {
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

            view.context?.getSystemService<InputMethodManager>()?.let { imm ->
                if (!imm.isActive(view)) {
                    // This view is not the currently active view for the input method yet.
                    post()
                    return
                }

                if (!imm.showSoftInput(view, flags)) {
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
