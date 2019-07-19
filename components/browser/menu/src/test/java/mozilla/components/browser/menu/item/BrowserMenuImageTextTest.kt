/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.menu.item

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class BrowserMenuImageTextTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `browser menu ImageText should be inflated`() {
        var onClickWasPress = false
        val item = BrowserMenuImageText(
            "label",
            android.R.drawable.ic_menu_report_image,
            android.R.color.black
        ) {
            onClickWasPress = true
        }

        val view = inflate(item)

        view.performClick()
        assertTrue(onClickWasPress)
    }

    @Test
    fun `browser menu ImageText should have the right text, image, and iconTintColorResource`() {
        val item = BrowserMenuImageText(
            "label",
            android.R.drawable.ic_menu_report_image,
            android.R.color.black
        ) {
        }

        val view = inflate(item)

        val textView = view.findViewById<TextView>(R.id.imageText)
        assertEquals(textView.text, "label")

        val imageShadow = shadowOf(textView.compoundDrawablesRelative[0])

        assertNotNull(imageShadow)
        assertEquals(android.R.drawable.ic_menu_report_image, imageShadow.createdFromResId)
    }

    @Test
    fun `browser menu ImageText with with no iconTintColorResource must not have an imageTintList`() {
        val item = BrowserMenuImageText(
            "label",
            android.R.drawable.ic_menu_report_image
        )

        val view = inflate(item)
        val textView = view.findViewById<TextView>(R.id.imageText)
        assertNull(textView.compoundDrawableTintList)
    }

    private fun inflate(item: BrowserMenuImageText): View {
        val view = LayoutInflater.from(context).inflate(item.getLayoutResource(), null)
        val mockMenu = mock(BrowserMenu::class.java)
        item.bind(mockMenu, view)
        return view
    }
}
