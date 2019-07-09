/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.browser.menu.R

/**
 * A simple browser menu checkbox.
 *
 * @param label The visible label of this menu item.
 * @param initialState The initial value the checkbox should have.
 * @param listener Callback to be invoked when this menu item is checked.
 */
class BrowserMenuCheckbox(
    private val label: String,
    private val initialState: () -> Boolean = { false },
    private val listener: (Boolean) -> Unit
) : BrowserMenuItem {

    override fun getLayoutResource() = R.layout.mozac_browser_menu_item_checkbox

    override val visible: () -> Boolean = { true }

    override fun bind(menu: BrowserMenu, view: View) {
        (view as LinearLayout).apply {

            val textView = findViewById<TextView>(R.id.text)
            val checkbox = findViewById<AppCompatCheckBox>(R.id.checkbox)
            textView.text = label
            checkbox.isChecked = initialState()

            // We delay the dismissing of the menu so the user has feedback the checkbox is selected
            // This is to maintain parity with how this feature worked prior to its refactor
            setOnClickListener {
                checkbox.performClick()
                listener(checkbox.isChecked)
                handler.postDelayed({
                    menu.dismiss()
                }, DISMISS_DELAY)
            }
        }
    }

    private companion object {
        const val DISMISS_DELAY = 250L
    }
}
