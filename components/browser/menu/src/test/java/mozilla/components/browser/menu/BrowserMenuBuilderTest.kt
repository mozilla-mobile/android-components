/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.concept.menu.Menu
import mozilla.components.concept.menu.MenuItem
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserMenuBuilderTest {

    @Test
    fun `items are forwarded from builder to menu`() {
        val builder = BrowserMenuBuilder(mutableListOf(mockMenuItem(), mockMenuItem()))

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
    fun `menu item added appended if index too large`() {
        val builder = BrowserMenuBuilder(mutableListOf(mockMenuItem(), mockMenuItem()))

        builder.addAllMenus(4, listOf(mockMenuItem(1234)))

        assertEquals(3, builder.items.size)
        assertEquals(1234, builder.items[2].getLayoutResource())
    }

    @Test
    fun `menu item added appended if index too small`() {
        val builder = BrowserMenuBuilder(mutableListOf(mockMenuItem(), mockMenuItem()))

        builder.addAllMenus(-4, listOf(mockMenuItem(4567)))

        assertEquals(3, builder.items.size)
        assertEquals(4567, builder.items[0].getLayoutResource())
    }

    @Test
    fun `menu item added with empty menu`() {
        val builder = BrowserMenuBuilder(mutableListOf())

        builder.addAllMenus(4, listOf(mockMenuItem(4567)))

        assertEquals(1, builder.items.size)
        assertEquals(4567, builder.items[0].getLayoutResource())
    }

    @Test
    fun `menu facts added`() {
        val builder = BrowserMenuBuilder(mutableListOf(mockMenuItem(), mockMenuItem()))

        builder.addFactExtra("customtab", true)

        assertTrue(builder.extras["customtab"] as Boolean)
    }

    private fun mockMenuItem(layoutRes: Int = R.layout.mozac_browser_menu_item_simple) = object : MenuItem {
        override val visible: () -> Boolean = { true }

        override fun getLayoutResource() = layoutRes

        override fun bind(menu: Menu, view: View) {}
    }
}
