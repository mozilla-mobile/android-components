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

private const val DEFAULT_VALUE = 0

/**
 * A toolbar for peer-to-peer communication between browsers.
 */
class P2PBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), P2PView {
    override var listener: P2PView.Listener? = null

    init {
        inflate(getContext(), R.layout.mozac_feature_p2p_view, this)

        p2pAdvertiseBtn.setOnClickListener {
            require(listener != null)
            listener?.onAdvertise()
            p2pAdvertiseBtn.isEnabled = false
            p2pDiscoverBtn.isEnabled = false
        }
        p2pDiscoverBtn.setOnClickListener {
            require(listener != null)
            listener?.onDiscover()
            p2pAdvertiseBtn.isEnabled = false
            p2pDiscoverBtn.isEnabled = false
        }
        p2pSendBtn.setOnClickListener {
            require(listener != null)
            listener?.onSendURL()
            p2pSendBtn.isEnabled = false
        }
    }

    override fun updateStatus(status: String) {
        p2pStatusText.text = status
    }

    override fun authenticate(neighborId: String, neighborName: String, token: String) {
        require(listener != null)
        AlertDialog.Builder(context)
            .setTitle("Accept connection to $neighborName")
            .setMessage("Confirm the code matches on both devices: $token")
            .setPositiveButton(android.R.string.yes) { _, _ -> listener?.onAccept(token) }
            .setNegativeButton(android.R.string.no) { _, _ -> listener?.onReject(token) }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    override fun reset() {
        require(listener != null) // We could enforce this by adding a listener argument
        p2pAdvertiseBtn.isEnabled = true
        p2pDiscoverBtn.isEnabled = true
    }

    override fun readyToSend() {
        require(listener != null)
        p2pSendBtn.isEnabled = true
    }

    override fun displayMessage(neighborId: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle("Accept URL from $neighborId")
            .setMessage("Visit $message")
            .setPositiveButton(android.R.string.yes) { _, _ ->  }
            .setNegativeButton(android.R.string.no) { _, _ ->  }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    override fun clear() {
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
