/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu2.adapter

import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.TooltipCompat
import mozilla.components.browser.menu2.R
import mozilla.components.browser.menu2.ext.applyIcon
import mozilla.components.browser.menu2.ext.applyStyle
import mozilla.components.concept.menu.candidate.SmallMenuCandidate

internal class SmallMenuCandidateViewHolder(
    itemView: View,
    private val dismiss: () -> Unit
) : LastItemViewHolder<SmallMenuCandidate>(itemView), View.OnClickListener {

    private val iconView = itemView as AppCompatImageButton
    private var onClickListener: (() -> Unit)? = null

    init {
        iconView.setOnClickListener(this)
    }

    override fun bind(newCandidate: SmallMenuCandidate, oldCandidate: SmallMenuCandidate?) {
        if (newCandidate.contentDescription != oldCandidate?.contentDescription) {
            iconView.contentDescription = newCandidate.contentDescription
            TooltipCompat.setTooltipText(iconView, newCandidate.contentDescription)
        }
        onClickListener = newCandidate.onClick
        iconView.applyIcon(newCandidate.icon, oldCandidate?.icon)
        iconView.applyStyle(newCandidate.containerStyle, oldCandidate?.containerStyle)
    }

    override fun onClick(v: View?) {
        onClickListener?.invoke()
        dismiss()
    }

    companion object {
        @LayoutRes
        val layoutResource = R.layout.mozac_browser_menu2_candidate_row_small_icon
    }
}
