/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.readerview.view

import android.content.Context
import android.support.annotation.IdRes
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.AppCompatButton
import android.util.AttributeSet
import android.view.View
import android.widget.RadioGroup
import mozilla.components.feature.readerview.R
import mozilla.components.feature.readerview.ReaderViewFeature.Config.ColorScheme
import mozilla.components.feature.readerview.ReaderViewFeature.Config.FontType

const val MAX_TEXT_SIZE = 9
const val MIN_TEXT_SIZE = 1

/**
 * A customizable ReaderView control bar implementing [ReaderViewControlsView].
 */
class ReaderViewControlsBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ReaderViewControlsView {

    override var listener: ReaderViewControlsView.Listener? = null

    private lateinit var fontIncrementButton: AppCompatButton
    private lateinit var fontDecrementButton: AppCompatButton
    private lateinit var fontGroup: RadioGroup
    private lateinit var colorSchemeGroup: RadioGroup

    init {
        View.inflate(getContext(), R.layout.mozac_feature_readerview_view, this)

        isFocusableInTouchMode = true
        isClickable = true

        bindViews()
    }

    /**
     * Sets the font type of the current and future ReaderView sessions.
     *
     * @param font The applicable font types available.
     */
    override fun setFont(font: FontType) {
        val selected = when (font) {
            FontType.SERIF -> R.id.mozac_feature_readerview_font_serif
            FontType.SANS_SERIF -> R.id.mozac_feature_readerview_font_sans_serif
        }
        fontGroup.check(selected)
    }

    /**
     * Sets the font size of the current and future ReaderView sessions.
     *
     * Note: The readerview.js implementation under the hood scales the entire page's contents and not just
     * the text size.
     *
     * @param size An integer value in the range [MIN_TEXT_SIZE] to [MAX_TEXT_SIZE].
     */
    override fun setFontSize(size: Int) {
        val (incrementState, decrementState) = when {
            size <= MIN_TEXT_SIZE -> {
                Pair(first = true, second = false)
            }
            size >= MAX_TEXT_SIZE -> {
                Pair(first = false, second = true)
            }
            else -> {
                Pair(first = true, second = true)
            }
        }
        fontIncrementButton.isEnabled = incrementState
        fontDecrementButton.isEnabled = decrementState
    }

    /**
     * Sets the color scheme of the current and future ReaderView sessions.
     *
     * @param scheme The applicable colour schemes available.
     */
    override fun setColorScheme(scheme: ColorScheme) {
        val selected = when (scheme) {
            ColorScheme.DARK -> R.id.mozac_feature_readerview_color_dark
            ColorScheme.SEPIA -> R.id.mozac_feature_readerview_color_sepia
            ColorScheme.LIGHT -> R.id.mozac_feature_readerview_color_light
        }

        colorSchemeGroup.check(selected)
    }

    /**
     * Requests visibility and focus of the UI controls.
     */
    override fun showControls() {
        visibility = View.VISIBLE
        requestFocus()
    }

    /**
     * Requests invisibility of the UI controls.
     */
    override fun hideControls() {
        visibility = View.GONE
    }

    private fun bindViews() {
        fontGroup = applyCheckedListener(R.id.mozac_feature_readerview_font_group) { checkedId ->
            val fontType = when (checkedId) {
                R.id.mozac_feature_readerview_font_sans_serif -> FontType.SANS_SERIF
                R.id.mozac_feature_readerview_font_serif -> FontType.SERIF
                else -> FontType.SERIF
            }
            listener?.onFontChanged(fontType)
        }
        colorSchemeGroup = applyCheckedListener(R.id.mozac_feature_readerview_color_scheme_group) { checkedId ->
            val colorSchemeChoice = when (checkedId) {
                R.id.mozac_feature_readerview_color_dark -> ColorScheme.DARK
                R.id.mozac_feature_readerview_color_sepia -> ColorScheme.SEPIA
                R.id.mozac_feature_readerview_color_light -> ColorScheme.LIGHT
                else -> ColorScheme.DARK
            }
            listener?.onColorSchemeChanged(colorSchemeChoice)
        }
        fontIncrementButton = applyClickListener(R.id.mozac_feature_readerview_font_size_increase) {
            listener?.onFontSizeIncreased()
        }
        fontDecrementButton = applyClickListener(R.id.mozac_feature_readerview_font_size_decrease) {
            listener?.onFontSizeDecreased()
        }
    }

    private inline fun applyClickListener(@IdRes id: Int, crossinline block: () -> Unit): AppCompatButton {
        return findViewById<AppCompatButton>(id).apply {
            setOnClickListener { block() }
        }
    }

    private inline fun applyCheckedListener(@IdRes id: Int, crossinline block: (Int) -> Unit): RadioGroup {
        return findViewById<RadioGroup>(id).apply {
            setOnCheckedChangeListener { _, checkedId -> block(checkedId) }
        }
    }
}
