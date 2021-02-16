/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.content.Context
import android.content.res.Resources.getSystem
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.PopupWindow
import androidx.annotation.VisibleForTesting
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.menu.BrowserMenu.Orientation.DOWN
import mozilla.components.browser.menu.BrowserMenu.Orientation.UP
import mozilla.components.concept.menu.MenuStyle
import mozilla.components.support.ktx.android.view.isRTL
import mozilla.components.support.ktx.android.view.onNextGlobalLayout

/**
 * A popup menu composed of BrowserMenuItem objects.
 */
open class BrowserMenu internal constructor(
    internal val adapter: BrowserMenuAdapter
) : View.OnAttachStateChangeListener {
    protected var currentPopup: PopupWindow? = null
    private var menuList: RecyclerView? = null
    internal var currAnchor: View? = null
    internal var isShown = false

    private var isWindowExpanded = false

    /**
     * @param anchor the view on which to pin the popup window.
     * @param orientation the preferred orientation to show the popup window.
     * @param style Custom styling for this menu.
     * @param endOfMenuAlwaysVisible when is set to true makes sure the bottom of the menu is always visible otherwise,
     *  the top of the menu is always visible.
     */
    @Suppress("InflateParams", "ComplexMethod")
    open fun show(
        anchor: View,
        orientation: Orientation = DOWN,
        style: MenuStyle? = null,
        endOfMenuAlwaysVisible: Boolean = false,
        onDismiss: () -> Unit = {}
    ): PopupWindow {
        val view = LayoutInflater.from(anchor.context).inflate(R.layout.mozac_browser_menu, null)

        adapter.menu = this

        menuList = view.findViewById<RecyclerView>(R.id.mozac_browser_menu_recyclerView).apply {
            layoutManager = LinearLayoutManager(anchor.context, RecyclerView.VERTICAL, false).also {
                setEndOfMenuAlwaysVisibleCompact(endOfMenuAlwaysVisible, it)
            }
            adapter = this@BrowserMenu.adapter

            // Always show the top most items
            scrollToPosition(0)

            // Simple way to know where the menu is placed on the screen. May need a better approach.
            val topBottomDistance = getMaxAvailableHeightToTopAndBottom(anchor)

            // Popup is shown at the top of the screen
            if (topBottomDistance.first > topBottomDistance.second) {
                // The menu is "expanded" by default. "Collapse" it.
                val placeholderCollapsedSize = (470 * getSystem().displayMetrics.density).toInt()
                val measureSpec = View.MeasureSpec.makeMeasureSpec(placeholderCollapsedSize, AT_MOST)
                measure(WRAP_CONTENT, measureSpec)
                layoutParams.height = measuredHeight
                requestLayout()

                // A scroll up should expand it.
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)

                        if (dy > 0) {

                            // "PopupWindow.update()" would increase the height of the window from below the screen bounds
                            // and then translate it up. This has a negative visual impact.
                            // Try to do the same "manually" while ensuring the window always respects the anchor.

                            isWindowExpanded = true
                            removeOnScrollListener(this)

                            // Expand the menu list
                            measure(WRAP_CONTENT, WRAP_CONTENT)
                            layoutParams.height = measuredHeight
                            requestLayout()

                            val popupView = currentPopup!!.contentView
                            val decorView = (((popupView as ViewGroup).parent as ViewGroup).parent) as ViewGroup
                            val windowManager = decorView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                            // Increase the actual Popup "Window" that contains the menu items list.
                            val currentParams = decorView.layoutParams as WindowManager.LayoutParams
                            val newParams = WindowManager.LayoutParams()
                            // The new y delta is now below the screen bounds. Translate the entire window up.
                            newParams.y = -580
                            newParams.height = 1840

                            // Reuse only the absolutely needed existing params properties.
                            // There is at least one that would throw this overboard and break the entire layout.
                            newParams.width = currentParams.width
                            newParams.title = currentParams.title
                            newParams.token = currentParams.token
                            newParams.type = currentParams.type
                            newParams.gravity = Gravity.END or Gravity.BOTTOM
                            // Original animation set because we want the same dismiss behavior.
                            newParams.windowAnimations = R.style.Mozac_Browser_Menu_Animation_OverflowMenuBottom

                            windowManager.updateViewLayout(decorView, newParams)
                        }
                    }
                })
            }
        }

        view.findViewById<CardView>(R.id.mozac_browser_menu_menuView).apply {
            style?.backgroundColor?.let { setCardBackgroundColor(it) }
        }

        menuList?.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View?, info: AccessibilityNodeInfo?) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info?.collectionInfo = AccessibilityNodeInfo.CollectionInfo.obtain(
                    adapter.interactiveCount, 0, false
                )
            }
        }

        return PopupWindow(
            view,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isFocusable = true
            elevation = view.resources.getDimension(R.dimen.mozac_browser_menu_elevation)

            setOnDismissListener {
                adapter.menu = null
                currentPopup = null
                isShown = false
                onDismiss()
            }

            displayPopup(view, anchor, orientation).also {
                anchor.addOnAttachStateChangeListener(this@BrowserMenu)
                currAnchor = anchor
            }
        }.also {
            currentPopup = it
            isShown = true
        }
    }

    private fun RecyclerView.setEndOfMenuAlwaysVisibleCompact(
        endOfMenuAlwaysVisible: Boolean,
        layoutManager: LinearLayoutManager
    ) {
        // In devices with Android 6 and below stackFromEnd is not working properly,
        // as a result, we have to provided a backwards support.
        // See: https://github.com/mozilla-mobile/android-components/issues/3211
        if (endOfMenuAlwaysVisible && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            scrollOnceToTheBottom(this)
        } else {
            layoutManager.stackFromEnd = endOfMenuAlwaysVisible
        }
    }

    @VisibleForTesting
    internal fun scrollOnceToTheBottom(recyclerView: RecyclerView) {
        recyclerView.onNextGlobalLayout {
            recyclerView.adapter?.let { recyclerView.scrollToPosition(it.itemCount - 1) }
        }
    }

    fun dismiss() {
        currentPopup?.dismiss()
    }

    fun invalidate() {
        menuList?.let { adapter.invalidate(it) }
    }

    companion object {
        /**
         * Determines the orientation to be used for a menu based on the positioning of the [parent] in the layout.
         */
        fun determineMenuOrientation(parent: View?): Orientation {
            if (parent == null) {
                return DOWN
            }

            val params = parent.layoutParams
            return if (params is CoordinatorLayout.LayoutParams) {
                if ((params.gravity and Gravity.BOTTOM) == Gravity.BOTTOM) {
                    UP
                } else {
                    DOWN
                }
            } else {
                DOWN
            }
        }
    }

    enum class Orientation(val concept: mozilla.components.concept.menu.Orientation) {
        UP(mozilla.components.concept.menu.Orientation.UP),
        DOWN(mozilla.components.concept.menu.Orientation.DOWN)
    }

    override fun onViewDetachedFromWindow(v: View?) {
        currentPopup?.dismiss()
        currAnchor?.removeOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(v: View?) {
        // no-op
    }
}

@VisibleForTesting
internal fun PopupWindow.displayPopup(
    containerView: View,
    anchor: View,
    preferredOrientation: BrowserMenu.Orientation
) {
    // Measure menu
    val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    containerView.measure(spec, spec)

    val (availableHeightToTop, availableHeightToBottom) = getMaxAvailableHeightToTopAndBottom(anchor)
    val containerHeight = containerView.measuredHeight

    val fitsUp = availableHeightToTop >= containerHeight
    val fitsDown = availableHeightToBottom >= containerHeight

    // Try to use the preferred orientation, if doesn't fit fallback to the best fit.
    when {
        preferredOrientation == DOWN && fitsDown -> {
            showPopupWithDownOrientation(anchor)
        }
        preferredOrientation == UP && fitsUp -> {
            showPopupWithUpOrientation(anchor, availableHeightToBottom, containerHeight)
        }
        else -> {
            showPopupWhereBestFits(
                anchor,
                fitsUp,
                fitsDown,
                availableHeightToTop,
                availableHeightToBottom,
                containerHeight
            )
        }
    }
}

@Suppress("LongParameterList")
private fun PopupWindow.showPopupWhereBestFits(
    anchor: View,
    fitsUp: Boolean,
    fitsDown: Boolean,
    availableHeightToTop: Int,
    availableHeightToBottom: Int,
    containerHeight: Int
) {
    // We don't have enough space to show the menu UP neither DOWN.
    // Let's just show the popup at the location of the anchor.
    if (!fitsUp && !fitsDown) {
        showAtAnchorLocation(anchor, availableHeightToTop < availableHeightToBottom)
    } else {
        if (fitsDown) {
            showPopupWithDownOrientation(anchor)
        } else {
            showPopupWithUpOrientation(anchor, availableHeightToBottom, containerHeight)
        }
    }
}

private fun PopupWindow.showPopupWithUpOrientation(anchor: View, availableHeightToBottom: Int, containerHeight: Int) {
    val xOffset = if (anchor.isRTL) -anchor.width else 0
    animationStyle = R.style.Mozac_Browser_Menu_Animation_OverflowMenuBottom

    // Positioning the menu above and overlapping the anchor.
    val yOffset = if (availableHeightToBottom < 0) {
        // The anchor is partially below of the bottom of the screen, let's make the menu completely visible.
        availableHeightToBottom - containerHeight
    } else {
        -containerHeight
    }
    showAsDropDown(anchor, xOffset, yOffset)
}

private fun PopupWindow.showPopupWithDownOrientation(anchor: View) {
    val xOffset = if (anchor.isRTL) -anchor.width else 0
    animationStyle = R.style.Mozac_Browser_Menu_Animation_OverflowMenuTop
    // Menu should overlay the anchor.
    showAsDropDown(anchor, xOffset, -anchor.height)
}

private fun PopupWindow.showAtAnchorLocation(anchor: View, isCloserToTop: Boolean) {
    val anchorPosition = IntArray(2)

    // Apply the best fit animation style based on positioning
    animationStyle = if (isCloserToTop) {
        R.style.Mozac_Browser_Menu_Animation_OverflowMenuTop
    } else {
        R.style.Mozac_Browser_Menu_Animation_OverflowMenuBottom
    }

    anchor.getLocationOnScreen(anchorPosition)
    val (x, y) = anchorPosition
    PopupWindowCompat.setOverlapAnchor(this, true)
    showAtLocation(anchor, Gravity.START or Gravity.TOP, x, y)
}

private fun getMaxAvailableHeightToTopAndBottom(anchor: View): Pair<Int, Int> {
    val anchorPosition = IntArray(2)
    val displayFrame = Rect()

    val appView = anchor.rootView
    appView.getWindowVisibleDisplayFrame(displayFrame)

    anchor.getLocationOnScreen(anchorPosition)

    val bottomEdge = displayFrame.bottom

    val distanceToBottom = bottomEdge - (anchorPosition[1] + anchor.height)
    val distanceToTop = anchorPosition[1] - displayFrame.top

    return distanceToTop to distanceToBottom
}
