/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.behavior

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.ScaleGestureDetector
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class BrowserGestureDetectorTest {
    // Robolectric currently (April 17th 2020) only offer a stub in it's `ShadowScaleGestureDetector`
    // so unit tests based on the actual implementation of `ScaleGestureListener` are not possible.

    // Used spies and not mocks as it was observed that verifying more of the below as mocks
    // will fail the tests because of "UnfinishedVerificationException"
    private val scrollListener = spy { _: Float, _: Float -> run {} }
    private val verticalScrollListener = spy { _: Float -> run {} }
    private val horizontalScrollListener = spy { _: Float -> run {} }
    private val scaleBeginListener = spy { _: Float -> run {} }
    private val scaleInProgressListener = spy { _: Float -> run {} }
    private val scaleEndListener = spy { _: Float -> run {} }
    private val gesturesListener = BrowserGestureDetector.GesturesListener(
        onScroll = scrollListener,
        onVerticalScroll = verticalScrollListener,
        onHorizontalScroll = horizontalScrollListener,
        onScaleBegin = scaleBeginListener,
        onScale = scaleInProgressListener,
        onScaleEnd = scaleEndListener
    )

    @Test
    fun `Detector should not attempt to detect zoom if MotionEvent's action is ACTION_CANCEL`() {
        val detector = spy(BrowserGestureDetector(testContext, mock()))
        val scaleDetector: ScaleGestureDetector = mock()
        doReturn(scaleDetector).`when`(detector).scaleGestureDetector

        val downEvent = TestUtils.getMotionEvent(ACTION_DOWN)
        val cancelEvent = TestUtils.getMotionEvent(ACTION_CANCEL, previousEvent = downEvent)
        val moveEvent = TestUtils.getMotionEvent(ACTION_MOVE, previousEvent = downEvent)
        detector.handleTouchEvent(downEvent)
        detector.handleTouchEvent(cancelEvent)
        detector.handleTouchEvent(downEvent)
        detector.handleTouchEvent(moveEvent)

        verify(scaleDetector, times(3)).onTouchEvent(any<MotionEvent>())
    }

    @Test
    fun `Detector should not attempt to detect scrolls if a zoom gesture is in progress`() {
        val detector = spy(BrowserGestureDetector(testContext, mock()))
        val scrollDetector: GestureDetector = mock()
        val scaleDetector: ScaleGestureDetector = mock()
        doReturn(scrollDetector).`when`(detector).gestureDetector
        doReturn(scaleDetector).`when`(detector).scaleGestureDetector
        `when`(scaleDetector.isInProgress).thenReturn(true)

        detector.handleTouchEvent(TestUtils.getMotionEvent(ACTION_DOWN))

        verify(scrollDetector, never()).onTouchEvent(any<MotionEvent>())
    }

    @Test
    fun `Detector's handleTouchEvent returns false if the event was not handled`() {
        val detector = spy(BrowserGestureDetector(testContext, mock()))
        val unhandledEvent = TestUtils.getMotionEvent(ACTION_DOWN)

        // Neither the scale detector, nor the scroll detector should be interested
        // in a one of a time ACTION_CANCEL MotionEvent
        val wasEventHandled = detector.handleTouchEvent(
            TestUtils.getMotionEvent(ACTION_CANCEL, previousEvent = unhandledEvent)
        )

        assertFalse(wasEventHandled)
    }

    @Test
    fun `Detector's handleTouchEvent returns true if the event was handled`() {
        val detector = spy(BrowserGestureDetector(testContext, mock()))
        val downEvent = TestUtils.getMotionEvent(ACTION_DOWN)
        val moveEvent = TestUtils.getMotionEvent(ACTION_MOVE, 0f, 0f, previousEvent = downEvent)
        val moveEvent2 = TestUtils.getMotionEvent(ACTION_MOVE, 100f, 100f, previousEvent = moveEvent)

        detector.handleTouchEvent(downEvent)
        detector.handleTouchEvent(moveEvent)
        val wasEventHandled = detector.handleTouchEvent(moveEvent2)

        assertTrue(wasEventHandled)
    }

    @Test
    fun `Detector should inform about scroll and vertical scrolls events`() {
        val detector = spy(BrowserGestureDetector(testContext, gesturesListener))
        val downEvent = TestUtils.getMotionEvent(ACTION_DOWN)
        val moveEvent = TestUtils.getMotionEvent(ACTION_MOVE, 0f, 0f, previousEvent = downEvent)
        val moveEvent2 = TestUtils.getMotionEvent(ACTION_MOVE, 100f, 200f, previousEvent = moveEvent)

        detector.handleTouchEvent(downEvent)
        detector.handleTouchEvent(moveEvent)
        detector.handleTouchEvent(moveEvent2)

        // If the movement was more on the Y axis both "onScroll" and "onVerticalScroll" callbacks
        // should be called but no others.
        verify(scrollListener).invoke(-100f, -200f)
        verify(verticalScrollListener).invoke(-200f)
        verify(horizontalScrollListener, never()).invoke(anyFloat())
        verify(scaleBeginListener, never()).invoke(anyFloat())
        verify(scaleInProgressListener, never()).invoke(anyFloat())
        verify(scaleEndListener, never()).invoke(anyFloat())
    }

    @Test
    fun `Detector should prioritize vertical scrolls over horizontal scrolls`() {
        val detector = spy(BrowserGestureDetector(testContext, gesturesListener))
        val downEvent = TestUtils.getMotionEvent(ACTION_DOWN)
        val moveEvent = TestUtils.getMotionEvent(ACTION_MOVE, 0f, 0f, previousEvent = downEvent)
        val moveEvent2 = TestUtils.getMotionEvent(ACTION_MOVE, 100f, 100f, previousEvent = moveEvent)

        detector.handleTouchEvent(downEvent)
        detector.handleTouchEvent(moveEvent)
        detector.handleTouchEvent(moveEvent2)

        // If the movement was for the same amount on both the Y axis and the X axis
        // both "onScroll" and "onVerticalScroll" callbacks should be called but no others.
        verify(scrollListener).invoke(-100f, -100f)
        verify(verticalScrollListener).invoke(-100f)
        verify(horizontalScrollListener, never()).invoke(anyFloat())
        verify(scaleBeginListener, never()).invoke(anyFloat())
        verify(scaleInProgressListener, never()).invoke(anyFloat())
        verify(scaleEndListener, never()).invoke(anyFloat())
    }

    @Test
    fun `Detector should inform about scroll and horizontal scrolls events`() {
        val detector = spy(BrowserGestureDetector(testContext, gesturesListener))
        val downEvent = TestUtils.getMotionEvent(ACTION_DOWN)
        val moveEvent = TestUtils.getMotionEvent(ACTION_MOVE, 0f, 0f, previousEvent = downEvent)
        val moveEvent2 = TestUtils.getMotionEvent(ACTION_MOVE, 101f, 100f, previousEvent = moveEvent)

        detector.handleTouchEvent(downEvent)
        detector.handleTouchEvent(moveEvent)
        detector.handleTouchEvent(moveEvent2)

        // If the movement was for the same amount on both the Y axis and the X axis
        // both "onScroll" and "onVerticalScroll" callbacks should be called but no others.
        verify(scrollListener).invoke(-101f, -100f)
        verify(horizontalScrollListener).invoke(-101f)
        verify(verticalScrollListener, never()).invoke(anyFloat())
        verify(scaleBeginListener, never()).invoke(anyFloat())
        verify(scaleInProgressListener, never()).invoke(anyFloat())
        verify(scaleEndListener, never()).invoke(anyFloat())
    }
}
