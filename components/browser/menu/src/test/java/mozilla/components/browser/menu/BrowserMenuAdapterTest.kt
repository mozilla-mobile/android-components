/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BrowserMenuAdapterTest {
    @Test
    fun `items that return false from the visible lambda will be filtered out`() {
        val items = listOf(
            createMenuItem(1) { true },
            createMenuItem(2) { true },
            createMenuItem(3) { false },
            createMenuItem(4) { false },
            createMenuItem(5) { true })

        val adapter = BrowserMenuAdapter(RuntimeEnvironment.application, items)

        assertEquals(3, adapter.visibleItems.size)

        adapter.visibleItems.assertTrueForOne { it.getLayoutResource() == 1 }
        adapter.visibleItems.assertTrueForOne { it.getLayoutResource() == 2 }
        adapter.visibleItems.assertTrueForOne { it.getLayoutResource() == 5 }

        adapter.visibleItems.assertTrueForAll { it.visible() }

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `layout resource ID is used as view type`() {
        val items = listOf(
                createMenuItem(23),
                createMenuItem(42))

        val adapter = BrowserMenuAdapter(RuntimeEnvironment.application, items)

        assertEquals(2, adapter.itemCount)

        assertEquals(23, adapter.getItemViewType(0))
        assertEquals(42, adapter.getItemViewType(1))
    }

    @Test
    fun `bind will be forwarded to item implementation`() {
        val item1 = spy(createMenuItem())
        val item2 = spy(createMenuItem())

        val menu = mock(BrowserMenu::class.java)

        val adapter = BrowserMenuAdapter(RuntimeEnvironment.application, listOf(item1, item2))
        adapter.menu = menu

        val view = mock(View::class.java)
        val holder = BrowserMenuItemViewHolder(view)

        adapter.onBindViewHolder(holder, 0)

        verify(item1).bind(menu, view)
        verify(item2, never()).bind(menu, view)

        reset(item1)
        reset(item2)

        adapter.onBindViewHolder(holder, 1)

        verify(item1, never()).bind(menu, view)
        verify(item2).bind(menu, view)
    }

    private fun List<BrowserMenuItem>.assertTrueForOne(predicate: (BrowserMenuItem) -> Boolean) {
        for (item in this) {
            if (predicate(item)) {
                return
            }
        }
        fail("Predicate false for all items")
    }

    private fun List<BrowserMenuItem>.assertTrueForAll(predicate: (BrowserMenuItem) -> Boolean) {
        for (item in this) {
            if (!predicate(item)) {
                fail("Predicate not true for all items")
            }
        }
    }

    private fun createMenuItem(
        layout: Int = 0,
        visible: () -> Boolean = { true }
    ): BrowserMenuItem {
        return object : BrowserMenuItem {
            override val visible = visible

            override fun getLayoutResource() = layout

            override fun bind(menu: BrowserMenu, view: View) {}
        }
    }
}
