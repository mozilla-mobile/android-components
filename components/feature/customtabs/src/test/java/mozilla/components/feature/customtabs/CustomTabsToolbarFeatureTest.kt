/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.customtabs

import android.graphics.Color
import android.graphics.drawable.Drawable
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CustomTabsToolbarFeatureTest {

    @Test
    fun `start without sessionId invokes nothing`() {
        val sessionManager: SessionManager = spy(SessionManager(mock()))
        val session: Session = mock()

        val feature = spy(CustomTabsToolbarFeature(sessionManager, mock()) {})

        feature.start()

        verify(sessionManager, never()).findSessionById(anyString())
        verify(feature, never()).initialize(session)
    }

    @Test
    fun `start calls initialize with the sessionId`() {
        val sessionManager: SessionManager = mock()
        val session: Session = mock()
        val feature = spy(CustomTabsToolbarFeature(sessionManager, mock(), "") {})

        `when`(sessionManager.findSessionById(anyString())).thenReturn(session)
        `when`(session.customTabConfig).thenReturn(mock())
        doNothing().`when`(feature).addCloseButton(null)

        feature.start()

        verify(feature).initialize(session)

        // Calling start again should NOT call init again

        feature.start()

        verify(feature, times(1)).initialize(session)
    }

    @Test
    fun `initialize returns true if session is a customtab`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(RuntimeEnvironment.application))
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        var initialized = feature.initialize(session)

        assertFalse(initialized)

        `when`(session.customTabConfig).thenReturn(mock())

        initialized = feature.initialize(session)

        assertTrue(initialized)
    }

    @Test
    fun `initialize updates toolbar`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(RuntimeEnvironment.application))
        `when`(session.customTabConfig).thenReturn(mock())

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        feature.initialize(session)

        assertFalse(toolbar.onUrlClicked.invoke())
    }

    @Test
    fun `initialize calls updateToolbarColor`() {
        val sessionManager: SessionManager = mock()
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(RuntimeEnvironment.application))
        `when`(session.customTabConfig).thenReturn(mock())

        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, "") {})

        feature.initialize(session)

        verify(feature).updateToolbarColor(anyInt())
    }

    @Test
    fun `updateToolbarColor changes background and textColor`() {
        val session: Session = mock()
        val toolbar: BrowserToolbar = mock()
        `when`(session.customTabConfig).thenReturn(mock())

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        feature.updateToolbarColor(null)

        verify(toolbar, never()).setBackgroundColor(anyInt())
        verify(toolbar, never()).textColor = anyInt()

        feature.updateToolbarColor(123)

        verify(toolbar).setBackgroundColor(anyInt())
        verify(toolbar).textColor = anyInt()
    }

    @Test
    fun getReadableTextColor() {
        // White text color for a black background
        val white = CustomTabsToolbarFeature.getReadableTextColor(0)
        assertEquals(Color.WHITE, white)

        // Black text color for a white background
        val black = CustomTabsToolbarFeature.getReadableTextColor(0xFFFFFF)
        assertEquals(Color.BLACK, black)
    }

    @Test
    fun `initialize calls addCloseButton`() {
        val session: Session = mock()
        val toolbar = BrowserToolbar(RuntimeEnvironment.application)
        `when`(session.customTabConfig).thenReturn(mock())

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        feature.initialize(session)

        verify(feature).addCloseButton(null)
    }

    @Test
    fun isSmallerThan() {
        val toolbar = spy(BrowserToolbar(RuntimeEnvironment.application))
        val drawable: Drawable = mock()
        with(CustomTabsToolbarFeature(mock(), toolbar, "") {}) {
            `when`(drawable.minimumWidth).thenReturn(84)
            `when`(drawable.minimumHeight).thenReturn(84)
            assertFalse(drawable.isSmallerThan(CustomTabsToolbarFeature.MAX_CLOSE_BUTTON_SIZE_DP))

            `when`(drawable.minimumWidth).thenReturn(24)
            `when`(drawable.minimumHeight).thenReturn(24)
            assertTrue(drawable.isSmallerThan(CustomTabsToolbarFeature.MAX_CLOSE_BUTTON_SIZE_DP))

            `when`(drawable.minimumWidth).thenReturn(24)
            `when`(drawable.minimumHeight).thenReturn(84)
            assertFalse(drawable.isSmallerThan(CustomTabsToolbarFeature.MAX_CLOSE_BUTTON_SIZE_DP))
        }
    }
}