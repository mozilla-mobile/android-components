/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.browser.menu.R
import mozilla.components.concept.menu.candidate.CompoundToolbarMenuCandidate
import mozilla.components.concept.menu.candidate.DrawableButtonMenuIcon
import mozilla.components.concept.menu.candidate.MenuCandidate
import mozilla.components.concept.menu.candidate.TextMenuIcon
import mozilla.components.support.ktx.android.content.res.resolveAttribute

/**
 * A menu item used to display a toolbar with 2 separate functions.
 *
 */
@Suppress("LongParameterList")
class BrowserMenuItemCompoundToolbar(
    private val startLabelText: String,
    private val primaryLabelText: String,
    private val secondaryLabelText: String,
    private val primaryContentDescription: String,
    private val secondaryContentDescription: String,
    @DrawableRes
    private val primaryImageResource: Int,
    @DrawableRes
    private val secondaryImageResource: Int,
    @ColorRes
    private val tintColorResource: Int = NO_ID,
    private val isInPrimaryState: () -> Boolean = { true },
    private val startLabelListener: () -> Unit,
    private val endLabelListener: () -> Unit
) : BrowserMenuItem {

    override var visible: () -> Boolean = { true }

    override val interactiveCount: () -> Int = { 2 }

    override fun getLayoutResource() = R.layout.mozac_browser_menu_item_compound_toolbar

    override fun bind(menu: BrowserMenu, view: View) {
        val layout = view as ConstraintLayout
        val selectableBackground =
            layout.context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless)

        val startLabel = view.findViewById<TextView>(R.id.startLabel)
        val endLabel = view.findViewById<TextView>(R.id.endLabel)
        val endLabelIcon = view.findViewById<ImageView>(R.id.endLabelIcon)

        startLabel.apply {
            text = startLabelText
            setOnClickListener { startLabelListener() }
        }

        endLabelIcon.apply {
            setImageDrawable(
                if (isInPrimaryState()) {
                    ContextCompat.getDrawable(view.context, primaryImageResource)
                } else {
                    ContextCompat.getDrawable(view.context, secondaryImageResource)
                }
            )
            setColorFilter(ContextCompat.getColor(context, tintColorResource))
        }

        endLabel.apply {
            if (isInPrimaryState()) {
                text = primaryLabelText
                contentDescription = primaryContentDescription
            } else {
                text = secondaryLabelText
                contentDescription = secondaryContentDescription
            }

            setTextColor(ContextCompat.getColor(context, tintColorResource))
            setBackgroundResource(selectableBackground)
            setOnClickListener {
                endLabelListener()
                menu.dismiss()
            }
        }
    }

    override fun invalidate(view: View) {
        val startLabel = view.findViewById<TextView>(R.id.startLabel)
        val endLabel = view.findViewById<TextView>(R.id.endLabel)
        startLabel.invalidate()
        endLabel.invalidate()
    }

    override fun asCandidate(context: Context): MenuCandidate = CompoundToolbarMenuCandidate(
        startIcon = TextMenuIcon(
            startLabelText,
            ContextCompat.getColor(context, tintColorResource),
            onClick = startLabelListener
        ),
        endText = if (isInPrimaryState()) primaryLabelText else secondaryLabelText,
        contentDescription = if (isInPrimaryState()) primaryContentDescription else secondaryContentDescription,
        endIcon = if (isInPrimaryState()) {
            DrawableButtonMenuIcon(
                ContextCompat.getDrawable(context, primaryImageResource),
                ContextCompat.getColor(context, tintColorResource),
                endLabelListener
            )
        } else {
            DrawableButtonMenuIcon(
                ContextCompat.getDrawable(context, primaryImageResource),
                ContextCompat.getColor(context, tintColorResource),
                endLabelListener
            )
        }
    )
}
