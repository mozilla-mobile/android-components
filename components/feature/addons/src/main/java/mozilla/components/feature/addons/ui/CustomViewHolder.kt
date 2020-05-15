/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.addons.ui

import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

/**
 * A base view holder.
 */
sealed class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    /**
     * A view holder for displaying section items.
     */
    class SectionViewHolder(
        view: View,
        val titleView: TextView
    ) : CustomViewHolder(view)

    /**
     * A view holder for displaying Not yet supported section items.
     */
    class UnsupportedSectionViewHolder(
        view: View,
        val titleView: TextView,
        val descriptionView: TextView
    ) : CustomViewHolder(view)

    /**
     * A view holder for displaying add-on items.
     */
    class AddonViewHolder(
        view: View,
        val iconContainer: CardView,
        val iconView: ImageView,
        val titleView: TextView,
        val summaryView: TextView,
        val ratingView: RatingBar,
        val ratingAccessibleView: TextView,
        val userCountView: TextView,
        val addButton: ImageView,
        val allowedInPrivateBrowsingLabel: ImageView
    ) : CustomViewHolder(view)
}
