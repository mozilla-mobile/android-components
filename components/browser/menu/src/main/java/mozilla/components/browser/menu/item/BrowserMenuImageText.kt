/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.browser.menu.R
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds

internal const val NO_ID = -1

internal fun ImageView.setTintResource(@ColorRes tintColorResource: Int) {
    if (tintColorResource != NO_ID) {
        imageTintList = ContextCompat.getColorStateList(context, tintColorResource)
    }
}

internal fun TextView.tintedDrawable(@DrawableRes imageResource: Int, @ColorRes tintColorResource: Int): Drawable {
    val draw = context.getDrawable(imageResource)!!

    if (tintColorResource != NO_ID) {
        draw.setTint(ContextCompat.getColor(context, tintColorResource))
    }

    return draw
}

internal fun TextView.setColorResource(@ColorRes textColorResource: Int) {
    if (textColorResource != NO_ID) {
        setTextColor(ContextCompat.getColor(context, textColorResource))
    }
}

/**
 * A menu item for displaying text with an image icon.
 *
 * @param label The visible label of this menu item.
 * @param imageResource ID of a drawable resource to be shown as icon.
 * @param iconTintColorResource Optional ID of color resource to tint the icon.
 * @param textColorResource Optional ID of color resource to tint the text.
 * @param listener Callback to be invoked when this menu item is clicked.
 */
open class BrowserMenuImageText(
    private val label: String,
    @DrawableRes
    private val imageResource: Int,
    @ColorRes
    private val iconTintColorResource: Int = NO_ID,
    @ColorRes
    private val textColorResource: Int = NO_ID,
    private val listener: () -> Unit = {}
) : BrowserMenuItem {

    override var visible: () -> Boolean = { true }

    override fun getLayoutResource() = R.layout.mozac_browser_menu_item_image_text

    override fun bind(menu: BrowserMenu, view: View) {
        bindText(view)

        view.setOnClickListener {
            listener.invoke()
            menu.dismiss()
        }
    }

    private fun bindText(view: View) {
        val textView = view.findViewById<TextView>(R.id.imageText)
        with(textView) {
            text = label
            setColorResource(textColorResource)
            putCompoundDrawablesRelativeWithIntrinsicBounds(
                    start = tintedDrawable(imageResource, iconTintColorResource)
            )
        }
    }
}
