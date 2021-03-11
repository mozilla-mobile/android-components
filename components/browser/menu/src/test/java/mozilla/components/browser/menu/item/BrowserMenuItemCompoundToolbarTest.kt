/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu.item

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.R
import mozilla.components.concept.menu.candidate.CompoundToolbarMenuCandidate
import mozilla.components.concept.menu.candidate.DrawableButtonMenuIcon
import mozilla.components.concept.menu.candidate.TextMenuIcon
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class BrowserMenuItemCompoundToolbarTest {
    private lateinit var compoundToolbar: BrowserMenuItemCompoundToolbar
    private lateinit var context: Context
    private val startLabel = "Bookmarks"
    private val tintColorResource = 1
    private val primaryImageResource = 2
    private val secondaryImageResource = 3
    private val mockDrawable: Drawable = mock()

    private val listener = {}

    private val primaryLabelText = "Add"
    private val secondaryLabelText = "Edit"
    private val primaryContentDescription = "Add bookmark"
    private val secondaryContentDescription = "Edit bookmark"

    @Before
    fun setup() {
        context = mock()

        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 30)

        whenever(context.getDrawable(primaryImageResource)).thenReturn(mockDrawable)
        whenever(context.getDrawable(secondaryImageResource)).thenReturn(mockDrawable)
        whenever(context.getColor(tintColorResource)).thenReturn(Color.BLACK)

        compoundToolbar = BrowserMenuItemCompoundToolbar(
            startLabelText = startLabel,
            primaryLabelText = primaryLabelText,
            secondaryLabelText = secondaryLabelText,
            primaryContentDescription = primaryContentDescription,
            secondaryContentDescription = secondaryContentDescription,
            primaryImageResource = primaryImageResource,
            secondaryImageResource = secondaryImageResource,
            tintColorResource = tintColorResource,
            isInPrimaryState = { true },
            startLabelListener = listener,
            endLabelListener = listener
        )
    }

    @Test
    fun `browser compound toolbar uses correct layout`() {
        assertEquals(
            R.layout.mozac_browser_menu_item_compound_toolbar,
            compoundToolbar.getLayoutResource()
        )
    }

    @Test
    fun `browser compound toolbar is visible by default`() {
        Assert.assertTrue(compoundToolbar.visible())
    }

    @Test
    fun `browser compound toolbar can be converted to candidate`() {
        assertEquals(
            compoundToolbar.asCandidate(context),
            CompoundToolbarMenuCandidate(
                TextMenuIcon(
                    startLabel,
                    ContextCompat.getColor(context, tintColorResource),
                    onClick = listener
                ),
                primaryLabelText,
                primaryContentDescription,
                DrawableButtonMenuIcon(
                    mockDrawable,
                    ContextCompat.getColor(context, tintColorResource),
                    listener
                )
            )
        )
    }
}
