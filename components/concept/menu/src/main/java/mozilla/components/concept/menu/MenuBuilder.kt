/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.menu

import android.content.Context

/**
 * Interface to be implemented by components that provide browser toolbar functionality that also for want to add
 * browser menus.
 */
interface MenuBuilder {

    /**
     * List of BrowserMenuItem objects to compose the menu from.
     */
    val items: MutableList<MenuItem>

    /**
     * Map of extra values that are added to emitted facts
     */
    val extras: Map<String, Any>

    /**
     * Ensures the bottom of the menu is always visible when true, otherwise the top of the menu is always visible.
     */
    val endOfMenuAlwaysVisible: Boolean

    /**
     * Combine all of the options that have been set and return a new [Menu] object.
     */
    fun build(context: Context): Menu

    /**
     * Inserts all of the menus of the specified collection [menus] into this list at the specified [index].
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    fun addAllMenus(index: Int, menus: Collection<MenuItem>): Boolean

    /**
     * Associates the specified [value] with the specified [key] in the facts emitted.
     */
    fun addFactExtra(key: String, value: Any)
}
