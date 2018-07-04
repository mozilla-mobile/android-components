/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.tabstray

import android.widget.LinearLayout
import mozilla.components.browser.session.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TabsAdapterTest {
    @Test
    fun `itemCount will reflect number of sessions`() {
        val adapter = TabsAdapter()
        assertEquals(0, adapter.itemCount)

        adapter.displaySessions(listOf(Session("A"), Session("B")), 0)
        assertEquals(2, adapter.itemCount)

        adapter.updateSessions(listOf(
            Session("A"),
            Session("B"),
            Session("C")), 0)

        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `adapter will ask holder to unbind if view gets recycled`() {
        val adapter = TabsAdapter()
        val holder: TabViewHolder = mock()

        adapter.onViewRecycled(holder)

        verify(holder).unbind()
    }

    @Test
    fun `holders get registered and unregistered from session`() {
        val adapter = TabsAdapter()

        val view = LinearLayout(RuntimeEnvironment.application)

        val holder1 = adapter.createViewHolder(view, 0)
        val holder2 = adapter.createViewHolder(view, 0)
        val holder3 = adapter.createViewHolder(view, 0)

        val session1: Session = mock()
        val session2: Session = mock()
        val session3: Session = mock()

        adapter.displaySessions(listOf(session1, session2, session3), 0)

        adapter.onBindViewHolder(holder1, 0)
        adapter.onBindViewHolder(holder2, 1)

        verify(session1).register(holder1)
        verify(session2).register(holder2)
        verifyNoMoreInteractions(session3)

        adapter.unsubscribeHolders()

        verify(session1).unregister(holder1)
        verify(session2).unregister(holder2)
        verifyNoMoreInteractions(session3)
    }
}
