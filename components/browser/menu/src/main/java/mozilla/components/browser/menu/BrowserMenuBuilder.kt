/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.content.Context
import mozilla.components.concept.menu.MenuBuilder
import mozilla.components.concept.menu.MenuItem

/**
 * Helper class for building browser menus.
 *
 * @param items List of BrowserMenuItem objects to compose the menu from.
 * @param extras Map of extra values that are added to emitted facts
 * @param endOfMenuAlwaysVisible when true ensures the bottom of the menu is always visible otherwise,
 *  the top of the menu is always visible.
 */
open class BrowserMenuBuilder(
    override val items: MutableList<MenuItem>,
    override val extras: MutableMap<String, Any> = mutableMapOf(),
    override val endOfMenuAlwaysVisible: Boolean = false
) : MenuBuilder {

    /**
     * Builds and returns a browser menu with [items]. See [MenuBuilder.build].
     */
    override fun build(context: Context): BrowserMenu {
        val adapter = BrowserMenuAdapter(context, items)
        return BrowserMenu(adapter)
    }

    /**
     * See [MenuBuilder.addFactExtra].
     */
    override fun addAllMenus(index: Int, menus: Collection<MenuItem>): Boolean {
        val insertIndex = index.coerceIn(0, items.size)
        return items.addAll(insertIndex, menus)
    }

    /**
     * See [MenuBuilder.addFactExtra].
     */
    override fun addFactExtra(key: String, value: Any) {
        extras[key] = value
    }
}
