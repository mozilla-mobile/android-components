/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.readerview.view

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.AppCompatRadioButton
import android.view.View
import androidx.test.core.app.ApplicationProvider
import mozilla.components.feature.readerview.R
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.support.test.mock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderViewControlsBarTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `flags are set on UI init`() {
        val bar = spy(ReaderViewControlsBar(context))

        assertTrue(bar.isFocusableInTouchMode)
        assertTrue(bar.isClickable)
    }

    @Test
    fun `font options are set`() {
        val bar = ReaderViewControlsBar(context)
        val serifButton = bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_font_serif)
        val sansSerifButton = bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_font_sans_serif)

        assertFalse(serifButton.isChecked)

        bar.setFont(ReaderViewFeature.Config.FontType.SERIF)

        assertTrue(serifButton.isChecked)

        assertFalse(sansSerifButton.isChecked)

        bar.setFont(ReaderViewFeature.Config.FontType.SANS_SERIF)

        assertTrue(sansSerifButton.isChecked)
    }

    @Test
    fun `font size buttons are enabled or disabled`() {
        val bar = ReaderViewControlsBar(context)

        val sizeDecreaseButton = bar.findViewById<AppCompatButton>(R.id.mozac_feature_readerview_font_size_decrease)
        val sizeIncreaseButton = bar.findViewById<AppCompatButton>(R.id.mozac_feature_readerview_font_size_increase)

        bar.setFontSize(5)

        assertTrue(sizeDecreaseButton.isEnabled)
        assertTrue(sizeIncreaseButton.isEnabled)

        bar.setFontSize(1)

        assertFalse(sizeDecreaseButton.isEnabled)
        assertTrue(sizeIncreaseButton.isEnabled)

        bar.setFontSize(0)

        assertFalse(sizeDecreaseButton.isEnabled)
        assertTrue(sizeIncreaseButton.isEnabled)

        bar.setFontSize(9)

        assertTrue(sizeDecreaseButton.isEnabled)
        assertFalse(sizeIncreaseButton.isEnabled)

        bar.setFontSize(10)

        assertTrue(sizeDecreaseButton.isEnabled)
        assertFalse(sizeIncreaseButton.isEnabled)
    }

    @Test
    fun `color scheme is set`() {
        val bar = ReaderViewControlsBar(context)
        val colorOptionDark = bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_color_dark)
        val colorOptionSepia = bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_color_sepia)
        val colorOptionLight = bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_color_light)

        bar.setColorScheme(ReaderViewFeature.Config.ColorScheme.DARK)

        assertTrue(colorOptionDark.isChecked)
        assertFalse(colorOptionSepia.isChecked)
        assertFalse(colorOptionLight.isChecked)

        bar.setColorScheme(ReaderViewFeature.Config.ColorScheme.SEPIA)

        assertFalse(colorOptionDark.isChecked)
        assertTrue(colorOptionSepia.isChecked)
        assertFalse(colorOptionLight.isChecked)

        bar.setColorScheme(ReaderViewFeature.Config.ColorScheme.LIGHT)

        assertFalse(colorOptionDark.isChecked)
        assertFalse(colorOptionSepia.isChecked)
        assertTrue(colorOptionLight.isChecked)
    }

    @Test
    fun `showControls updates visibility and requests focus`() {
        val bar = spy(ReaderViewControlsBar(context))

        bar.showControls()

        verify(bar).visibility = View.VISIBLE
        verify(bar).requestFocus()
    }

    @Test
    fun `hideControls updates visibility`() {
        val bar = spy(ReaderViewControlsBar(context))

        bar.hideControls()

        verify(bar).visibility = View.GONE
    }

    @Test
    fun `listener is invoked when clicking a font option`() {
        val bar = ReaderViewControlsBar(context)
        val listener: ReaderViewControlsView.Listener = mock()

        assertNull(bar.listener)

        bar.listener = listener

        bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_font_sans_serif).performClick()

        verify(listener).onFontChanged(ReaderViewFeature.Config.FontType.SANS_SERIF)
    }

    @Test
    fun `listener is invoked when clicking a font size option`() {
        val bar = ReaderViewControlsBar(context)
        val listener: ReaderViewControlsView.Listener = mock()

        assertNull(bar.listener)

        bar.listener = listener

        bar.findViewById<AppCompatButton>(R.id.mozac_feature_readerview_font_size_increase).performClick()

        verify(listener).onFontSizeIncreased()
    }

    @Test
    fun `listener is invoked when clicking a color scheme`() {
        val bar = ReaderViewControlsBar(context)
        val listener: ReaderViewControlsView.Listener = mock()

        assertNull(bar.listener)

        bar.listener = listener

        bar.findViewById<AppCompatRadioButton>(R.id.mozac_feature_readerview_color_sepia).performClick()

        verify(listener).onColorSchemeChanged(ReaderViewFeature.Config.ColorScheme.SEPIA)
    }
}
