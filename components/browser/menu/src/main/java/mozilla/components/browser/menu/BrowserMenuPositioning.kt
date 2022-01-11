/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("MatchingDeclarationName")

package mozilla.components.browser.menu

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px

/**
 * All data needed for menu positioning.
 */
internal data class MenuPositioningData(
    /**
     * The orientation asked by users of this class when initializing it.
     */
    val askedOrientation: BrowserMenu.Orientation = BrowserMenu.Orientation.DOWN,

    /**
     * Whether the menu fits in the space between [display top, anchor] in a top - down layout.
     */
    val fitsUp: Boolean = false,

    /**
     * Whether the menu fits in the space between [anchor, display top] in a top - down layout.
     */
    val fitsDown: Boolean = false,

    /**
     * Distance between [display top, anchor top margin]. Used for better positioning the menu.
     */
    @Px val availableHeightToTop: Int = 0,

    /**
     * Distance between [display bottom, anchor bottom margin]. Used for better positioning the menu.
     */
    @Px val availableHeightToBottom: Int = 0,

    /**
     * [View#measuredHeight] of the menu. May be bigger than the available screen height.
     */
    @Px val containerViewHeight: Int = 0
)

/**
 * Measure, calculate, obtain all data needed to know how the menu shown in a PopupWindow should be positioned.
 *
 * This method assumes [currentData] already contains the [MenuPositioningData.askedOrientation].
 *
 * @param containerView the menu layout that will be wrapped in the PopupWindow.
 * @param anchor view the PopupWindow will be aligned to.
 * @param currentData current known data for how the menu should be positioned.
 *
 * @return new [MenuPositioningData] containing the current constraints of the PopupWindow.
 */
internal fun inferMenuPositioningData(
    containerView: ViewGroup,
    anchor: View,
    askedOrientation: BrowserMenu.Orientation
): Pair<MenuPositioningData, BrowserMenuPlacement> {
    // Measure the menu allowing it to expand entirely.
    val spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    containerView.measure(spec, spec)

    val (availableHeightToTop, availableHeightToBottom) = getMaxAvailableHeightToTopAndBottom(anchor)
    val containerHeight = containerView.measuredHeight

    val fitsUp = availableHeightToTop >= containerHeight
    val fitsDown = availableHeightToBottom >= containerHeight

    val positioningData = MenuPositioningData(
        askedOrientation = askedOrientation,
        fitsUp = fitsUp,
        fitsDown = fitsDown,
        availableHeightToTop = availableHeightToTop,
        availableHeightToBottom = availableHeightToBottom,
        containerViewHeight = containerHeight
    )

    return positioningData to inferMenuPlacement(
        anchor,
        positioningData
    )
}

/**
 * Infer where and how the PopupWindow should be shown based on the data available in [positioningData].
 * Should be called only once per menu to be shown.
 *
 * @param anchor view the PopupWindow will be aligned to.
 * @param positioningData known data for how the menu should be positioned.
 *
 * @return inferred [BrowserMenuPlacement].
 */
internal fun inferMenuPlacement(anchor: View, positioningData: MenuPositioningData): BrowserMenuPlacement {
    // Try to use the preferred orientation, if doesn't fit fallback to the best fit.

    return if (positioningData.askedOrientation == BrowserMenu.Orientation.DOWN && positioningData.fitsDown) {
            BrowserMenuPlacement.AnchoredToTop.Dropdown(anchor)
        } else if (positioningData.askedOrientation == BrowserMenu.Orientation.UP && positioningData.fitsUp) {
            BrowserMenuPlacement.AnchoredToBottom.Dropdown(anchor)
        } else {
            if (!positioningData.fitsUp && !positioningData.fitsDown) {
                if (positioningData.availableHeightToTop < positioningData.availableHeightToBottom) {
                    BrowserMenuPlacement.AnchoredToTop.ManualAnchoring(anchor)
                } else {
                    BrowserMenuPlacement.AnchoredToBottom.ManualAnchoring(anchor)
                }
            } else {
                if (positioningData.fitsDown) {
                    BrowserMenuPlacement.AnchoredToTop.Dropdown(anchor)
                } else {
                    BrowserMenuPlacement.AnchoredToBottom.Dropdown(anchor)
                }
            }
        }
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
