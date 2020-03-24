/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.tabstray

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.concept.tabstray.Tab
import mozilla.components.concept.tabstray.Tabs
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class TabsAdapterTest {

    @Test
    fun `itemCount will reflect number of sessions`() {
        val adapter = TabsAdapter()
        assertEquals(0, adapter.itemCount)

        adapter.updateTabs(
            Tabs(
                list = listOf(
                    Tab("A", "https://www.mozilla.org"),
                    Tab("B", "https://www.firefox.com")
                ),
                selectedIndex = 0)
        )
        assertEquals(2, adapter.itemCount)

        adapter.updateTabs(
            Tabs(
                list = listOf(
                    Tab("A", "https://www.mozilla.org"),
                    Tab("B", "https://www.firefox.com"),
                    Tab("C", "https://getpocket.com")
                ),
                selectedIndex = 0)
        )

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `onBindViewHolder calls bind on matching holder`() {
        val adapter = TabsAdapter()
        adapter.tabsTray = mock()

        val holder: TabViewHolder = mock()

        val tab = Tab("A", "https://www.mozilla.org")

        adapter.updateTabs(
            Tabs(
                list = listOf(tab),
                selectedIndex = 0)
        )

        adapter.onBindViewHolder(holder, 0)

        verify(holder).bind(tab, true, adapter)
    }

    @Test
    fun `underlying adapter is notified about data set changes`() {
        val adapter = spy(TabsAdapter())
        adapter.tabsTray = mock()

        adapter.onTabsInserted(27, 101)
        verify(adapter).notifyItemRangeInserted(27, 101)

        adapter.onTabsRemoved(11, 202)
        verify(adapter).notifyItemRangeRemoved(11, 202)

        adapter.onTabsMoved(13, 23)
        verify(adapter).notifyItemMoved(13, 23)

        adapter.onTabsChanged(42, 78)
        verify(adapter).notifyItemRangeChanged(42, 78)
    }
}
