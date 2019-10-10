/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.p2p.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.mozac_feature_p2p_view.view.*
import mozilla.components.feature.p2p.R
import mozilla.components.lib.nearby.NearbyConnection.ConnectionState


private const val DEFAULT_VALUE = 0

/**
 * A customizable "Find in page" bar implementing [P2PView].
 */
@Suppress("TooManyFunctions")
class P2PBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), P2PView {
    // Initialized in P2PInteractor
    override var listener: P2PView.Listener? = null

    init {
        inflate(getContext(), R.layout.mozac_feature_p2p_view, this)

        p2pAdvertiseBtn.setOnClickListener {
            listener!!.onAdvertise()
            p2pAdvertiseBtn.isEnabled = false
            p2pDiscoverBtn.isEnabled = false
        }
        p2pDiscoverBtn.setOnClickListener {
            listener!!.onDiscover()
            p2pAdvertiseBtn.isEnabled = false
            p2pDiscoverBtn.isEnabled = false
        }
    }

    override fun updateStatus(status: String) {
        p2pStatusText.text = status
    }

    override fun focus() {
    }

    override fun enable() {
        p2pAdvertiseBtn.isEnabled = true
        p2pDiscoverBtn.isEnabled = true
    }

    override fun clear() {
        enable()
        p2pStatusText.text = ""
    }
}

internal data class P2PBarStyling(
    val queryTextColor: Int,
    val queryHintTextColor: Int,
    val queryTextSize: Int,
    val resultCountTextColor: Int,
    val resultNoMatchesTextColor: Int,
    val resultCountTextSize: Int,
    val buttonsTint: ColorStateList?
)

private fun TextView.setTextSizeIfNotDefaultValue(newValue: Int) {
    if (newValue != DEFAULT_VALUE) {
        setTextSize(COMPLEX_UNIT_PX, newValue.toFloat())
    }
}

private fun TextView.setTextColorIfNotDefaultValue(newValue: Int) {
    if (newValue != DEFAULT_VALUE) {
        setTextColor(newValue)
    }
}

private fun TextView.setHintTextColorIfNotDefaultValue(newValue: Int) {
    if (newValue != DEFAULT_VALUE) {
        setHintTextColor(newValue)
    }
}

private fun AppCompatImageButton.setIconTintIfNotDefaultValue(newValue: ColorStateList?) {
    val safeValue = newValue ?: return
    imageTintList = safeValue
}
