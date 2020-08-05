/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu2

import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.annotation.Px
import androidx.coordinatorlayout.widget.CoordinatorLayout
import mozilla.components.browser.menu2.ext.displayPopup
import mozilla.components.browser.menu2.view.MenuView
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.Orientation
import mozilla.components.concept.menu.Side
import mozilla.components.concept.menu.candidate.MenuCandidate
import mozilla.components.concept.menu.candidate.NestedMenuCandidate
import mozilla.components.concept.menu.ext.findNestedMenuCandidate
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

/**
 * Controls a popup menu composed of MenuCandidate objects.
 * @param visibleSide Sets the menu to open with either the start or end visible.
 */
class BrowserMenuController(
    private val visibleSide: Side = Side.START
) : MenuController, Observable<MenuController.Observer> by ObserverRegistry() {

    private var currentPopupInfo: PopupMenuInfo? = null
    private var menuCandidates: List<MenuCandidate> = emptyList()

    private val menuDismissListener = PopupWindow.OnDismissListener {
        currentPopupInfo = null
        notifyObservers { onDismiss() }
    }

    override fun show(anchor: View, orientation: Orientation?): PopupWindow =
        show(anchor, orientation, defaultWidth(anchor.resources))

    /**
     * @param anchor The view on which to pin the popup window.
     * @param orientation The preferred orientation to show the popup window.
     * @param width The width of the popup menu. The height is always set to wrap content.
     */
    fun show(
        anchor: View,
        orientation: Orientation? = null,
        @Px width: Int = defaultWidth(anchor.resources)
    ): PopupWindow {
        val desiredOrientation = orientation ?: determineMenuOrientation(anchor.parent as? View?)
        val view = MenuView(anchor.context).apply {
            // Show nested list if present, or the standard menu candidates list.
            submitList(menuCandidates)
            setVisibleSide(visibleSide)
        }

        return MenuPopupWindow(view, width).apply {
            view.onDismiss = ::dismiss
            view.onReopenMenu = ::reopenMenu
            setOnDismissListener(menuDismissListener)
            displayPopup(view, anchor, desiredOrientation)
        }.also {
            currentPopupInfo = PopupMenuInfo(
                window = it,
                anchor = anchor,
                orientation = desiredOrientation,
                nested = null
            )
        }
    }

    /**
     * Re-opens the menu and displays the given nested list.
     * No-op if the menu is not yet open.
     */
    private fun reopenMenu(nested: NestedMenuCandidate?) {
        val info = currentPopupInfo ?: return
        info.window.run {
            // Dismiss silently
            setOnDismissListener(null)
            dismiss()
            setOnDismissListener(menuDismissListener)

            // Quickly remove the current list
            view.submitList(null)
            // Display the new nested list
            view.submitList(nested?.subMenuItems ?: menuCandidates)

            // Reopen the menu
            displayPopup(view, info.anchor, info.orientation)
        }
        currentPopupInfo = info.copy(nested = nested)
    }

    /**
     * Dismiss the menu popup if the menu is visible.
     */
    override fun dismiss() {
        currentPopupInfo?.window?.dismiss()
    }

    /**
     * Changes the contents of the menu.
     */
    override fun submitList(list: List<MenuCandidate>) {
        menuCandidates = list
        val info = currentPopupInfo

        // If menu is already open, update the displayed items
        if (info != null) {
            // If a nested menu is open, it should be displayed
            val displayedItems = if (info.nested != null) {
                list.findNestedMenuCandidate(info.nested.id)?.subMenuItems
            } else {
                list
            }

            // If the new menu is null, close & reopen the popup on the main list
            if (displayedItems == null) {
                // close & reopen popup
                reopenMenu(nested = null)
            } else {
                info.window.view.submitList(displayedItems)
            }
        }

        notifyObservers { onMenuListSubmit(list) }
    }

    @Px
    private fun defaultWidth(resources: Resources) =
        resources.getDimensionPixelSize(R.dimen.mozac_browser_menu2_width)

    private class MenuPopupWindow(
        val view: MenuView,
        @Px width: Int
    ) : PopupWindow(view, width, WRAP_CONTENT, true)

    private data class PopupMenuInfo(
        val window: MenuPopupWindow,
        val anchor: View,
        val orientation: Orientation,
        val nested: NestedMenuCandidate? = null
    )
}

/**
 * Determines the orientation to be used for a menu
 * based on the positioning of the [parent] in the layout.
 */
fun determineMenuOrientation(parent: View?): Orientation {
    val params = parent?.layoutParams as? CoordinatorLayout.LayoutParams
        ?: return Orientation.DOWN

    return Orientation.fromGravity(params.gravity)
}
