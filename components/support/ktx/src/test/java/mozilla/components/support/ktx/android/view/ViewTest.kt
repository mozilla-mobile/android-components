/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.view

import android.app.Activity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.support.base.android.Padding
import mozilla.components.support.test.any
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ViewTest {
    @Before
    @ExperimentalCoroutinesApi
    fun setUp() {
        // We create a separate thread for the main dispatcher so that we do not deadlock our test
        // thread.
        Dispatchers.setMain(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    }

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `showKeyboard should request focus`() {
        val view = EditText(testContext)
        assertFalse(view.hasFocus())

        view.showKeyboard()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertTrue(view.hasFocus())
    }

    @Suppress("Deprecation")
    @Test
    fun `visibility helper methods`() {
        val view = TextView(testContext)

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
        val view = TextView(testContext)

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
        val view = spy(View(testContext))
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

    @Test
    fun `called after next layout`() {
        val view = View(testContext)

        var callbackInvoked = false
        view.onNextGlobalLayout {
            callbackInvoked = true
        }

        assertFalse(callbackInvoked)

        view.viewTreeObserver.dispatchOnGlobalLayout()

        assertTrue(callbackInvoked)
    }

    @Test
    fun `remove listener after next layout`() {
        val view = spy(View(testContext))
        val viewTreeObserver = spy(view.viewTreeObserver)
        doReturn(viewTreeObserver).`when`(view).viewTreeObserver

        view.onNextGlobalLayout {}

        verify(viewTreeObserver, never()).removeOnGlobalLayoutListener(any())

        viewTreeObserver.dispatchOnGlobalLayout()

        verify(viewTreeObserver).removeOnGlobalLayoutListener(any())
    }

    @Test(expected = IllegalStateException::class)
    fun `toScope throws if view is not attached`() {
        val view = View(testContext)
        view.toScope()
    }

    @Test
    fun `can dispatch coroutines to view scope`() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val view = View(testContext)
        activity.windowManager.addView(view, WindowManager.LayoutParams(100, 100))

        assertTrue(view.isAttachedToWindow)

        val latch = CountDownLatch(1)
        var coroutineExecuted = false

        view.toScope().launch {
            coroutineExecuted = true
            latch.countDown()
        }

        latch.await(10, TimeUnit.SECONDS)

        assertTrue(coroutineExecuted)
    }

    @Test
    fun `scope is cancelled when view is detached`() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val view = View(testContext)
        activity.windowManager.addView(view, WindowManager.LayoutParams(100, 100))

        val scope = view.toScope()

        assertTrue(view.isAttachedToWindow)
        assertTrue(scope.isActive)

        activity.windowManager.removeView(view)

        assertFalse(view.isAttachedToWindow)
        assertFalse(scope.isActive)

        val latch = CountDownLatch(1)
        var coroutineExecuted = false

        scope.launch {
            coroutineExecuted = true
            latch.countDown()
        }

        assertFalse(latch.await(5, TimeUnit.SECONDS))
        assertFalse(coroutineExecuted)
    }
}
