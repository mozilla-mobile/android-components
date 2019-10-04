/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.awesomebar.layout

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.awesomebar.BrowserAwesomeBar
import mozilla.components.browser.awesomebar.R
import mozilla.components.browser.awesomebar.widget.FlowLayout
import mozilla.components.concept.awesomebar.AwesomeBar

internal sealed class DefaultSuggestionViewHolder {
    /**
     * Default view holder for suggestions.
     */
    internal class Default(
        private val awesomeBar: BrowserAwesomeBar,
        view: View
    ) : SuggestionViewHolder(view) {
        private val titleView = view.findViewById<TextView>(R.id.mozac_browser_awesomebar_title).apply {
            setTextColor(awesomeBar.styling.titleTextColor)
        }
        private val descriptionView = view.findViewById<TextView>(R.id.mozac_browser_awesomebar_description).apply {
            setTextColor(awesomeBar.styling.descriptionTextColor)
        }
        private val iconLoader = IconLoader(view.findViewById(R.id.mozac_browser_awesomebar_icon))

        override fun bind(suggestion: AwesomeBar.Suggestion, selectionListener: () -> Unit) {
            val title = if (suggestion.title.isNullOrEmpty()) suggestion.description else suggestion.title

            iconLoader.load(suggestion)

            titleView.text = title

            if (suggestion.description.isNullOrEmpty()) {
                descriptionView.visibility = View.GONE
            } else {
                descriptionView.visibility = View.VISIBLE
                descriptionView.text = suggestion.description
            }

            view.setOnClickListener {
                suggestion.onSuggestionClicked?.invoke()
                selectionListener.invoke()
            }
        }

        companion object {
            val LAYOUT_ID = R.layout.mozac_browser_awesomebar_item_generic
        }
    }

    /**
     * View holder for suggestions that contain chips.
     */
    internal class Chips(
        private val awesomeBar: BrowserAwesomeBar,
        view: View
    ) : SuggestionViewHolder(view) {
        private val chipsView = view.findViewById<FlowLayout>(R.id.mozac_browser_awesomebar_chips).apply {
            spacing = awesomeBar.styling.chipSpacing
        }
        private val inflater = LayoutInflater.from(view.context)
        private val iconLoader = IconLoader(view.findViewById(R.id.mozac_browser_awesomebar_icon))

        override fun bind(suggestion: AwesomeBar.Suggestion, selectionListener: () -> Unit) {
            chipsView.removeAllViews()

            iconLoader.load(suggestion)

            suggestion
                .chips
                .forEach { chip ->
                    val view = inflater.inflate(
                        R.layout.mozac_browser_awesomebar_chip,
                        view as ViewGroup,
                        false
                    ) as TextView

                    view.setTextColor(awesomeBar.styling.chipTextColor)
                    view.setBackgroundColor(awesomeBar.styling.chipBackgroundColor)
                    view.text = chip.title
                    view.setOnClickListener {
                        suggestion.onChipClicked?.invoke(chip)
                        selectionListener.invoke()
                    }

                    chipsView.addView(view)
                }
        }

        companion object {
            val LAYOUT_ID = R.layout.mozac_browser_awesomebar_item_chips
        }
    }
}

/**
 * Helper class for loading icons asynchronously.
 */
internal class IconLoader(
    private val iconView: ImageView
) {
    @VisibleForTesting
    internal var iconJob: Job? = null

    fun load(suggestion: AwesomeBar.Suggestion) {
        val icon = suggestion.icon ?: return

        /*
        Originally we were nulling out the icon and setting it every time.
        This means that there is a "flicker" since even if the image is the same it must be redrawn.
        In order to get rid of this flicker, we must make it so a suggestion cannot be served until its icon is ready
        hence why we're doing a "runBlocking" here instead of launching a scope.
        This is what desktop does and it seems like a reasonable solution.
        */

        runBlocking {
            val bitmap = runBlocking {
                icon.invoke(iconView.measuredWidth, iconView.measuredHeight)
            }

            if (iconView.drawable == null) {
                iconView.setImageBitmap(bitmap)
                return@runBlocking
            }

            if (bitmap != null) {
                val currentDrawable = (iconView.drawable as BitmapDrawable)
                val newDrawable = BitmapDrawable(iconView.resources, bitmap)

                if (currentDrawable.bitmap == null) {
                    iconView.setImageBitmap(bitmap)
                    return@runBlocking
                }

                if (currentDrawable.constantState?.equals(newDrawable) == true || (currentDrawable.bitmap != null &&
                        (currentDrawable.bitmap).sameAs(newDrawable.bitmap))) {
                    // The bitmaps are the same, no need to redraw!
                } else {
                    // The bitmaps are different, so do a redraw
                    iconView.setImageBitmap(bitmap)
                }
            }
        }
    }
}
