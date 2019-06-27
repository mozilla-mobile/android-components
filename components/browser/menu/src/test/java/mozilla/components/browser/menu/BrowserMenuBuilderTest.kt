/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class BrowserMenuBuilderTest {

    @Test
    fun `items are forwarded from builder to menu`() {
        val builder = BrowserMenuBuilder(listOf(mockMenuItem(), mockMenuItem()))

        val menu = builder.build(testContext)

        val anchor = ImageButton(testContext)
        val popup = menu.show(anchor)

        val recyclerView: RecyclerView = popup.contentView.findViewById(R.id.mozac_browser_menu_recyclerView)
        assertNotNull(recyclerView)

        val recyclerAdapter = recyclerView.adapter!!
        assertNotNull(recyclerAdapter)
        assertEquals(2, recyclerAdapter.itemCount)
    }

    @Test
    fun `menu is shown on click when attached`() {
        var shown = false
        val builder = spy(BrowserMenuBuilder(emptyList()))
        val view = ImageButton(testContext)

        builder.attachTo(view, onShow = { shown = true })

        verify(builder, never()).build(testContext)
        assertFalse(shown)

        view.performClick()
        verify(builder).build(testContext)
        assertTrue(shown)
    }

    @Test
    fun `menu is shown on long click when attached`() {
        var shown = false
        val builder = spy(BrowserMenuBuilder(emptyList()))
        val view = ImageButton(testContext)

        builder.attachTo(view, onShow = { shown = true })

        verify(builder, never()).build(testContext)
        assertFalse(shown)

        view.performLongClick()
        verify(builder).build(testContext)
        assertTrue(shown)
    }

    private fun mockMenuItem() = object : BrowserMenuItem {
        override val visible: () -> Boolean = { true }

        override fun getLayoutResource() = R.layout.mozac_browser_menu_item_simple

        override fun bind(menu: BrowserMenu, view: View) {}
    }
}
