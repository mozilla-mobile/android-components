/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.R

/**
 * A browser menu item with image and label and a custom checkbox.
 *
 * @param imageResource ID of a drawable resource to be shown as icon.
 * @param iconTintColorResource Optional ID of color resource to tint the icon.
 * @param label The visible label of this menu item.
 * @param textColorResource Optional ID of color resource to tint the text.
 * @param labelListener Callback to be invoked when this menu item is clicked.
 * @param primaryStateIconResource ID of a drawable resource for checkbox drawable in primary state.
 * @param secondaryStateIconResource ID of a drawable resource for checkbox drawable in secondary state.
 * @param iconTintColorResource Optional ID of color resource to tint the checkbox drawable.
 * @param primaryLabel The visible label of the checkbox in primary state.
 * @param secondaryLabel The visible label of this menu item in secondary state.
 * @param isInPrimaryState Lambda to return true/false to indicate checkbox primary or secondary state.
 * @param onCheckedChangedListener Callback to be invoked when checkbox is clicked.
 */
@Suppress("LongParameterList")
class BrowserMenuImageTextCheckboxButton(
    @DrawableRes imageResource: Int,
    private val label: String,
    @ColorRes internal val iconTintColorResource: Int = NO_ID,
    @ColorRes internal val textColorResource: Int = NO_ID,
    @VisibleForTesting internal val labelListener: () -> Unit,
    @DrawableRes val primaryStateIconResource: Int,
    @DrawableRes val secondaryStateIconResource: Int,
    @ColorRes internal val tintColorResource: Int = NO_ID,
    private val primaryLabel: String,
    private val secondaryLabel: String,
    val isInPrimaryState: () -> Boolean = { true },
    private val onCheckedChangedListener: (Boolean) -> Unit
) : BrowserMenuImageText(label, imageResource, iconTintColorResource, textColorResource, labelListener) {
    override var visible: () -> Boolean = { true }
    override fun getLayoutResource(): Int = R.layout.mozac_browser_menu_item_image_text_checkbox_button

    override fun bind(menu: BrowserMenu, view: View) {
        super.bind(menu, view)

        view.findViewById<View>(R.id.accessibilityRegion).apply {
            setOnClickListener { labelListener.invoke() }
            contentDescription = label
        }

        bindCheckbox(menu, view.findViewById(R.id.checkbox) as AppCompatCheckBox)
    }

    private fun bindCheckbox(menu: BrowserMenu, button: AppCompatCheckBox) {

        val buttonText = if (isInPrimaryState()) primaryLabel else secondaryLabel
        val tintColor = ContextCompat.getColor(button.context, tintColorResource)
        val buttonDrawableIcon = if (isInPrimaryState()) {
            ContextCompat.getDrawable(button.context, primaryStateIconResource)
        } else {
            ContextCompat.getDrawable(button.context, secondaryStateIconResource)
        }
        buttonDrawableIcon?.setTint(tintColor)

        button.apply {
            text = buttonText
            setTextColor(tintColor)
            buttonDrawable = buttonDrawableIcon

            setOnCheckedChangeListener { _, isChecked ->
                onCheckedChangedListener(isChecked)
                menu.dismiss()
            }
        }
    }
}
