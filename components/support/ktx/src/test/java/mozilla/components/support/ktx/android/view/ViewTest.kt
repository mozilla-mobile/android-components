/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.view

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import mozilla.components.support.base.android.Padding
import mozilla.components.support.test.any
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class ViewTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `showKeyboard should request focus`() {
        val view = EditText(context)
        assertFalse(view.hasFocus())

        view.showKeyboard()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(view.hasFocus())
    }

    @Test
    fun `visibility helper methods`() {
        val view = TextView(context)

        view.visibility = View.GONE

        assertTrue(view.isGone())
        assertFalse(view.isVisible())
        assertFalse(view.isInvisible())

        view.visibility = View.VISIBLE

        assertFalse(view.isGone())
        assertTrue(view.isVisible())
        assertFalse(view.isInvisible())

        view.visibility = View.INVISIBLE

        assertFalse(view.isGone())
        assertFalse(view.isVisible())
        assertTrue(view.isInvisible())
    }

    @Test
    fun `setPadding should set padding`() {
        val view = TextView(context)

        assertEquals(view.paddingLeft, 0)
        assertEquals(view.paddingTop, 0)
        assertEquals(view.paddingRight, 0)
        assertEquals(view.paddingBottom, 0)

        view.setPadding(Padding(16, 20, 24, 28))

        assertEquals(view.paddingLeft, 16)
        assertEquals(view.paddingTop, 20)
        assertEquals(view.paddingRight, 24)
        assertEquals(view.paddingBottom, 28)
    }

    @Test
    fun `getRectWithViewLocation should transform getLocationInWindow method values`() {
        val view = spy(View(context))
        doAnswer { invocation ->
            val locationInWindow = (invocation.getArgument(0) as IntArray)
            locationInWindow[0] = 100
            locationInWindow[1] = 200
            locationInWindow
        }.`when`(view).getLocationInWindow(any())

        `when`(view.width).thenReturn(150)
        `when`(view.height).thenReturn(250)

        val outRect = view.getRectWithViewLocation()

        assertEquals(100, outRect.left)
        assertEquals(200, outRect.top)
        assertEquals(250, outRect.right)
        assertEquals(450, outRect.bottom)
    }
}
