/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.drawable.toDrawable
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.browser.menu.R
import mozilla.components.concept.menu.candidate.ContainerStyle
import mozilla.components.concept.menu.candidate.DrawableMenuIcon
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.concept.menu.candidate.TextMenuIcon
import mozilla.components.concept.menu.candidate.TextStyle
import mozilla.components.concept.engine.webextension.Action
import mozilla.components.support.base.log.Log

/**
 * A browser menu item displaying a web extension action.
 *
 * @param action the [Action] to display.
 * @param listener a callback to be invoked when this menu item is clicked.
 */
class WebExtensionBrowserMenuItem(
    internal var action: Action,
    internal val listener: () -> Unit
) : BrowserMenuItem {
    override var visible: () -> Boolean = { true }

    override fun getLayoutResource() = R.layout.mozac_browser_menu_web_extension

    @Suppress("TooGenericExceptionCaught")
    override fun bind(menu: BrowserMenu, view: View) {
        val imageView = view.findViewById<ImageView>(R.id.action_image)
        val labelView = view.findViewById<TextView>(R.id.action_label)
        val badgeView = view.findViewById<TextView>(R.id.badge_text)
        val container = view.findViewById<View>(R.id.container)

        container.isEnabled = action.enabled ?: true

        action.title?.let {
            imageView.contentDescription = it
            labelView.text = it
        }
        badgeView.setBadgeText(action.badgeText)
        action.badgeTextColor?.let { badgeView.setTextColor(it) }
        action.badgeBackgroundColor?.let { badgeView.setBackgroundColor(it) }

        MainScope().launch {
            loadIcon(view.context, imageView.measuredHeight)?.let {
                imageView.setImageDrawable(it)
            }
        }

        container.setOnClickListener {
            listener.invoke()
            menu.dismiss()
        }
    }

    override fun invalidate(view: View) {
        val labelView = view.findViewById<TextView>(R.id.action_label)
        val badgeView = view.findViewById<TextView>(R.id.badge_text)

        action.title?.let {
            labelView.text = it
        }
        badgeView.setBadgeText(action.badgeText)

        labelView.invalidate()
        badgeView.invalidate()
    }

    override fun asCandidate(context: Context) = TextMenuCandidate(
        action.title.orEmpty(),
        start = runBlocking {
            val height = context.resources
                .getDimensionPixelSize(R.dimen.mozac_browser_menu_item_web_extension_icon_height)
            loadIcon(context, height)?.let { DrawableMenuIcon(it) }
        },
        end = TextMenuIcon(
            action.badgeText.orEmpty(),
            textStyle = TextStyle(
                color = action.badgeTextColor
            )
        ),
        containerStyle = ContainerStyle(
            isVisible = visible(),
            isEnabled = action.enabled ?: false
        ),
        onClick = listener
    )

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadIcon(context: Context, height: Int): Drawable? {
        return try {
            action.loadIcon?.invoke(height)?.toDrawable(context.resources)
        } catch (throwable: Throwable) {
            Log.log(
                Log.Priority.ERROR,
                "mozac-webextensions",
                throwable,
                "Failed to load browser action icon, falling back to default."
            )

            getDrawable(context, R.drawable.mozac_ic_web_extension_default_icon)
        }
    }
}

/**
 * Sets the badgeText and the visibility of the TextView based on empty/nullability of the badgeText.
 */
fun TextView.setBadgeText(badgeText: String?) {
    if (badgeText.isNullOrEmpty()) {
        visibility = View.INVISIBLE
    } else {
        visibility = View.VISIBLE
        text = badgeText
    }
}
