/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.menu

import android.view.View
import android.widget.PopupWindow

/**
 * An interface to be implemented by a base menu that would contain [MenuItem]s.
 */
interface Menu {

    /**
     * Display the menu.
     *
     * @param anchor the view on which to pin the popup window.
     * @param orientation the preferred orientation to show the popup window.
     * @param endOfMenuAlwaysVisible when is set to true makes sure the bottom of the menu is always visible otherwise,
     *  the top of the menu is always visible.
     */
    fun show(
        anchor: View,
        orientation: Orientation = Orientation.DOWN,
        endOfMenuAlwaysVisible: Boolean = false,
        onDismiss: () -> Unit = {}
    ): PopupWindow

    /**
     * Close the menu.
     */
    fun dismiss()

    /**
     * Declare that the menu items should be updated if needed.
     */
    fun invalidate()

    /**
     * The vertical ordering of the [MenuItem]s in the [Menu] based on where the menu would be on the screen.
     *
     * For example, with [Orientation.DOWN] a list of items (A, B, C) would place the first item A at the bottom of the
     * menu and the last item C would be the highest.
     */
    enum class Orientation {
        UP,
        DOWN
    }
}
