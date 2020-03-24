/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageButton
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.browser.toolbar.edit.EditToolbar
import mozilla.components.concept.toolbar.AutocompleteDelegate
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.concept.toolbar.Toolbar.SiteSecurity
import mozilla.components.concept.toolbar.Toolbar.SiteTrackingProtection
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.Robolectric
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class BrowserToolbarTest {

    @Test
    fun `display toolbar is visible by default`() {
        val toolbar = BrowserToolbar(testContext)
        assertTrue(toolbar.display.rootView.visibility == View.VISIBLE)
        assertTrue(toolbar.edit.rootView.visibility == View.GONE)
    }

    @Test
    fun `calling editMode() makes edit toolbar visible`() {
        val toolbar = BrowserToolbar(testContext)
        assertTrue(toolbar.display.rootView.visibility == View.VISIBLE)
        assertTrue(toolbar.edit.rootView.visibility == View.GONE)

        toolbar.editMode()

        assertTrue(toolbar.display.rootView.visibility == View.GONE)
        assertTrue(toolbar.edit.rootView.visibility == View.VISIBLE)
    }

    @Test
    fun `calling displayMode() makes display toolbar visible`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.editMode()

        assertTrue(toolbar.display.rootView.visibility == View.GONE)
        assertTrue(toolbar.edit.rootView.visibility == View.VISIBLE)

        toolbar.displayMode()

        assertTrue(toolbar.display.rootView.visibility == View.VISIBLE)
        assertTrue(toolbar.edit.rootView.visibility == View.GONE)
    }

    @Test
    fun `back presses will not be handled in display mode`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.displayMode()

        assertFalse(toolbar.onBackPressed())

        assertTrue(toolbar.display.rootView.visibility == View.VISIBLE)
        assertTrue(toolbar.edit.rootView.visibility == View.GONE)
    }

    @Test
    fun `back presses will switch from edit mode to display mode`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.editMode()

        assertTrue(toolbar.display.rootView.visibility == View.GONE)
        assertTrue(toolbar.edit.rootView.visibility == View.VISIBLE)

        assertTrue(toolbar.onBackPressed())

        assertTrue(toolbar.display.rootView.visibility == View.VISIBLE)
        assertTrue(toolbar.edit.rootView.visibility == View.GONE)
    }

    @Test
    fun `displayUrl will be forwarded to display toolbar immediately`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()
        val edit: EditToolbar = mock()

        toolbar.display = display
        toolbar.edit = edit

        toolbar.url = "https://www.mozilla.org"

        verify(display).url = "https://www.mozilla.org"
        verify(edit, never()).updateUrl(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean())
    }

    @Test
    fun `last URL will be forwarded to edit toolbar when switching mode`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.edit = spy(toolbar.edit)

        toolbar.url = "https://www.mozilla.org"
        verify(toolbar.edit, never()).updateUrl("https://www.mozilla.org", true)

        toolbar.editMode()

        verify(toolbar.edit).updateUrl("https://www.mozilla.org", true)
    }

    @Test
    fun `displayProgress will send accessibility events`() {
        val toolbar = BrowserToolbar(testContext)
        val root = mock(ViewParent::class.java)
        Shadows.shadowOf(toolbar).setMyParent(root)
        `when`(root.requestSendAccessibilityEvent(any(), any())).thenReturn(false)

        Shadows.shadowOf(testContext.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).setEnabled(true)

        toolbar.displayProgress(10)
        toolbar.displayProgress(50)
        toolbar.displayProgress(100)

        val captor = ArgumentCaptor.forClass(AccessibilityEvent::class.java)

        verify(root, times(4)).requestSendAccessibilityEvent(any(), captor.capture())

        assertEquals(AccessibilityEvent.TYPE_ANNOUNCEMENT, captor.allValues[0].eventType)
        assertEquals(testContext.getString(R.string.mozac_browser_toolbar_progress_loading), captor.allValues[0].text[0])

        assertEquals(AccessibilityEvent.TYPE_VIEW_SCROLLED, captor.allValues[1].eventType)
        assertEquals(10, captor.allValues[1].scrollY)
        assertEquals(100, captor.allValues[1].maxScrollY)

        assertEquals(AccessibilityEvent.TYPE_VIEW_SCROLLED, captor.allValues[2].eventType)
        assertEquals(50, captor.allValues[2].scrollY)
        assertEquals(100, captor.allValues[2].maxScrollY)

        assertEquals(AccessibilityEvent.TYPE_VIEW_SCROLLED, captor.allValues[3].eventType)
        assertEquals(100, captor.allValues[3].scrollY)
        assertEquals(100, captor.allValues[3].maxScrollY)
    }

    @Test
    fun `displayProgress will be forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()

        toolbar.display = display

        toolbar.displayProgress(10)
        toolbar.displayProgress(50)
        toolbar.displayProgress(75)
        toolbar.displayProgress(100)

        verify(display).updateProgress(10)
        verify(display).updateProgress(50)
        verify(display).updateProgress(75)
        verify(display).updateProgress(100)

        verifyNoMoreInteractions(display)
    }

    @Test
    fun `internal onUrlEntered callback will be forwarded to urlChangeListener`() {
        val toolbar = BrowserToolbar(testContext)

        val mockedListener = object {
            var called = false
            var url: String? = null

            fun invoke(url: String): Boolean {
                this.called = true
                this.url = url
                return true
            }
        }

        toolbar.setOnUrlCommitListener(mockedListener::invoke)
        toolbar.onUrlEntered("https://www.mozilla.org")

        assertTrue(mockedListener.called)
        assertEquals("https://www.mozilla.org", mockedListener.url)
    }

    /*
    @Test
    fun `internal onEditCancelled callback will be forwarded to editListener`() {
        val toolbar = BrowserToolbar(testContext)
        val listener: Toolbar.OnEditListener = mock()
        toolbar.setOnEditListener(listener)
        assertEquals(toolbar.edit.editListener, listener)

        toolbar.edit.views.url.onKeyPreIme(
            KeyEvent.KEYCODE_BACK,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
        )
        verify(listener, times(1)).onCancelEditing()
    }*/

    @Test
    fun `toolbar measure will use full width and fixed 56dp height`() {
        val toolbar = BrowserToolbar(testContext)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST)

        toolbar.measure(widthSpec, heightSpec)

        assertEquals(1024, toolbar.measuredWidth)
        assertEquals(56, toolbar.measuredHeight)
    }

    @Test
    fun `toolbar will use provided height with EXACTLY measure spec`() {
        val toolbar = BrowserToolbar(testContext)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)

        toolbar.measure(widthSpec, heightSpec)

        assertEquals(1024, toolbar.measuredWidth)
        assertEquals(800, toolbar.measuredHeight)
    }

    @Test
    fun `display and edit toolbar will use full size of browser toolbar`() {
        val toolbar = BrowserToolbar(testContext)

        assertEquals(0, toolbar.display.rootView.measuredWidth)
        assertEquals(0, toolbar.display.rootView.measuredHeight)
        assertEquals(0, toolbar.edit.rootView.measuredWidth)
        assertEquals(0, toolbar.edit.rootView.measuredHeight)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST)

        toolbar.measure(widthSpec, heightSpec)

        assertEquals(1024, toolbar.display.rootView.measuredWidth)
        assertEquals(56, toolbar.display.rootView.measuredHeight)
        assertEquals(1024, toolbar.edit.rootView.measuredWidth)
        assertEquals(56, toolbar.edit.rootView.measuredHeight)
    }

    @Test
    fun `toolbar will switch back to display mode after an URL has been entered`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.editMode()

        assertTrue(toolbar.display.rootView.visibility == View.GONE)
        assertTrue(toolbar.edit.rootView.visibility == View.VISIBLE)

        toolbar.onUrlEntered("https://www.mozilla.org")

        assertTrue(toolbar.display.rootView.visibility == View.VISIBLE)
        assertTrue(toolbar.edit.rootView.visibility == View.GONE)
    }

    @Test
    fun `toolbar will switch back to display mode if URL commit listener returns true`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.setOnUrlCommitListener { true }
        toolbar.editMode()

        assertTrue(toolbar.display.rootView.isGone)
        assertTrue(toolbar.edit.rootView.isVisible)

        toolbar.onUrlEntered("https://www.mozilla.org")

        assertTrue(toolbar.display.rootView.isVisible)
        assertTrue(toolbar.edit.rootView.isGone)
    }

    @Test
    fun `toolbar will stay in edit mode if URL commit listener returns false`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.setOnUrlCommitListener { false }
        toolbar.editMode()

        assertTrue(toolbar.display.rootView.isGone)
        assertTrue(toolbar.edit.rootView.isVisible)

        toolbar.onUrlEntered("https://www.mozilla.org")

        assertTrue(toolbar.display.rootView.isGone)
        assertTrue(toolbar.edit.rootView.isVisible)
    }

    @Test
    fun `add browser action will be forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()

        toolbar.display = display

        val action = BrowserToolbar.Button(mock(), "Hello") {
            // Do nothing
        }

        toolbar.addBrowserAction(action)

        verify(display).addBrowserAction(action)
    }

    @Test
    fun `remove browser action will be forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()

        toolbar.display = display

        val action = BrowserToolbar.Button(mock(), "Hello") {
            // Do nothing
        }

        toolbar.removeBrowserAction(action)

        verify(display).removeBrowserAction(action)
    }

    @Test
    fun `remove page action will be forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()

        toolbar.display = display

        val action = BrowserToolbar.Button(mock(), "Hello") {
            // Do nothing
        }

        toolbar.removePageAction(action)

        verify(display).removePageAction(action)
    }

    @Test
    fun `add page action will be forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)

        val display: DisplayToolbar = mock()

        toolbar.display = display

        val action = BrowserToolbar.Button(mock(), "World") {
            // Do nothing
        }

        toolbar.addPageAction(action)

        verify(display).addPageAction(action)
    }

    @Test
    fun `add edit action will be forwarded to edit toolbar`() {
        val toolbar = BrowserToolbar(testContext)

        val edit: EditToolbar = mock()
        toolbar.edit = edit

        val action = BrowserToolbar.Button(mock(), "QR code scanner") {
            // Do nothing
        }

        toolbar.addEditAction(action)

        verify(edit).addEditAction(action)
    }

    @Test
    fun `cast to view`() {
        // Given
        val toolbar = BrowserToolbar(testContext)

        // When
        val view = toolbar.asView()

        // Then
        assertNotNull(view)
    }

    @Test
    fun `URL update does not override search terms in edit mode`() {
        val toolbar = BrowserToolbar(testContext)

        toolbar.display = spy(toolbar.display)
        toolbar.edit = spy(toolbar.edit)

        toolbar.setSearchTerms("mozilla android")
        toolbar.url = "https://www.mozilla.com"
        toolbar.editMode()
        verify(toolbar.display).url = "https://www.mozilla.com"
        verify(toolbar.edit).updateUrl("mozilla android", false)

        toolbar.setSearchTerms("")
        toolbar.url = "https://www.mozilla.org"
        toolbar.editMode()
        verify(toolbar.display).url = "https://www.mozilla.org"
        verify(toolbar.edit).updateUrl("https://www.mozilla.org", true)
    }

    @Test
    fun `add navigation action will be forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()
        toolbar.display = display

        val action = BrowserToolbar.Button(mock(), "Back") {
            // Do nothing
        }

        toolbar.addNavigationAction(action)

        verify(display).addNavigationAction(action)
    }

    @Test
    fun `invalidate actions is forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        val display: DisplayToolbar = mock()
        toolbar.display = display

        verify(display, never()).invalidateActions()

        toolbar.invalidateActions()

        verify(display).invalidateActions()
    }

    @Test
    fun `search terms (if set) are forwarded to edit toolbar instead of URL`() {
        val toolbar = BrowserToolbar(testContext)

        toolbar.edit = spy(toolbar.edit)

        toolbar.url = "https://www.mozilla.org"
        toolbar.setSearchTerms("Mozilla Firefox")

        verify(toolbar.edit, never()).updateUrl("https://www.mozilla.org")
        verify(toolbar.edit, never()).updateUrl("Mozilla Firefox")

        toolbar.editMode()

        verify(toolbar.edit, never()).updateUrl("https://www.mozilla.org")
        verify(toolbar.edit).updateUrl("Mozilla Firefox")
    }

    @Test
    fun `editListener is set on edit`() {
        val toolbar = BrowserToolbar(testContext)
        assertNull(toolbar.edit.editListener)

        val listener: Toolbar.OnEditListener = mock()
        toolbar.setOnEditListener(listener)

        assertEquals(listener, toolbar.edit.editListener)
    }

    @Test
    fun `editListener is invoked when switching between modes`() {
        val toolbar = BrowserToolbar(testContext)

        val listener: Toolbar.OnEditListener = mock()
        toolbar.setOnEditListener(listener)

        toolbar.editMode()

        verify(listener).onStartEditing()
        verifyNoMoreInteractions(listener)

        toolbar.displayMode()

        verify(listener).onStopEditing()
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `editListener is invoked when text changes`() {
        val toolbar = BrowserToolbar(testContext)

        val listener: Toolbar.OnEditListener = mock()
        toolbar.setOnEditListener(listener)

        toolbar.edit.views.url.onAttachedToWindow()

        toolbar.editMode()

        toolbar.edit.views.url.setText("Hello")
        toolbar.edit.views.url.setText("Hello World")

        verify(listener).onStartEditing()
        verify(listener).onTextChanged("Hello")
        verify(listener).onTextChanged("Hello World")
    }

    @Test
    fun `titleView visibility is based on being set`() {
        val toolbar = BrowserToolbar(testContext)

        assertEquals(toolbar.display.views.origin.titleView.visibility, View.GONE)
        toolbar.title = "Mozilla"
        assertEquals(toolbar.display.views.origin.titleView.visibility, View.VISIBLE)
        toolbar.title = ""
        assertEquals(toolbar.display.views.origin.titleView.visibility, View.GONE)
    }

    @Test
    fun `titleView text is set properly`() {
        val toolbar = BrowserToolbar(testContext)

        toolbar.title = "Mozilla"
        assertEquals("Mozilla", toolbar.display.views.origin.titleView.text)
        assertEquals("Mozilla", toolbar.title)
    }

    @Test
    fun `titleView fading is set properly with non-null attrs`() {
        val attributeSet: AttributeSet = Robolectric.buildAttributeSet().build()

        val toolbar = BrowserToolbar(testContext, attributeSet)
        val titleView = toolbar.display.views.origin.titleView
        val edgeLength = testContext.resources.getDimensionPixelSize(R.dimen.mozac_browser_toolbar_url_fading_edge_size)

        assertTrue(titleView.isHorizontalFadingEdgeEnabled)
        assertEquals(edgeLength, titleView.horizontalFadingEdgeLength)
    }

    @Test
    fun `Button constructor with drawable`() {
        val buttonDefault = BrowserToolbar.Button(mock(), "imageDrawable") {}

        assertEquals(true, buttonDefault.visible())
        assertEquals(BrowserToolbar.DEFAULT_PADDING, buttonDefault.padding)
        assertEquals("imageDrawable", buttonDefault.contentDescription)

        val button = BrowserToolbar.Button(mock(), "imageDrawable", visible = { false }) {}

        assertEquals(false, button.visible())
    }

    @Test
    fun `ToggleButton constructor with drawable`() {
        val buttonDefault =
            BrowserToolbar.ToggleButton(mock(), mock(), "imageDrawable", "imageSelectedDrawable") {}

        assertEquals(true, buttonDefault.visible())
        assertEquals(BrowserToolbar.DEFAULT_PADDING, buttonDefault.padding)

        val button = BrowserToolbar.ToggleButton(
            mock(),
            mock(),
            "imageDrawable",
            "imageSelectedDrawable",
            visible = { false }) {}

        assertEquals(false, button.visible())
    }

    @Test
    fun `ReloadPageAction visibility changes update image`() {
        val reloadImage: Drawable = mock()
        val stopImage: Drawable = mock()
        val view: ImageButton = mock()
        var reloadPageAction = BrowserToolbar.TwoStateButton(reloadImage, "reload", stopImage, "stop") {}
        assertFalse(reloadPageAction.enabled)
        reloadPageAction.bind(view)
        verify(view).setImageDrawable(stopImage)
        verify(view).contentDescription = "stop"

        reloadPageAction = BrowserToolbar.TwoStateButton(reloadImage, "reload", stopImage, "stop", { false }) {}
        reloadPageAction.bind(view)
        verify(view).setImageDrawable(stopImage)
        verify(view).contentDescription = "reload"
    }

    @Test
    fun `siteSecure updates the display`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.display = spy(toolbar.display)
        assertEquals(SiteSecurity.INSECURE, toolbar.siteSecure)

        toolbar.siteSecure = SiteSecurity.SECURE

        verify(toolbar.display).siteSecurity = SiteSecurity.SECURE
    }

    @Test
    fun `siteTrackingProtection updates the display`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.display = spy(toolbar.display)
        assertEquals(SiteTrackingProtection.OFF_GLOBALLY, toolbar.siteTrackingProtection)

        toolbar.siteTrackingProtection = SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED

        verify(toolbar.display).setTrackingProtectionState(SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED)

        toolbar.siteTrackingProtection = SiteTrackingProtection.ON_NO_TRACKERS_BLOCKED
        verifyNoMoreInteractions(toolbar.display)
    }

    @Test
    fun `private flag sets IME_FLAG_NO_PERSONALIZED_LEARNING on url edit view`() {
        val toolbar = BrowserToolbar(testContext)
        val edit = toolbar.edit

        // By default "private mode" is off.
        assertEquals(0, edit.views.url.imeOptions and
            EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(false, toolbar.private)

        // Turning on private mode sets flag
        toolbar.private = true
        assertNotEquals(0, edit.views.url.imeOptions and
            EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertTrue(toolbar.private)

        // Turning private mode off again - should remove flag
        toolbar.private = false
        assertEquals(0, edit.views.url.imeOptions and
            EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(false, toolbar.private)
    }

    @Test
    fun `setAutocompleteListener is forwarded to edit toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.edit = mock()

        val filter: suspend (String, AutocompleteDelegate) -> Unit = { _, _ ->
            // Do nothing
        }

        toolbar.setAutocompleteListener(filter)

        verify(toolbar.edit).setAutocompleteListener(filter)
    }

    @Test
    fun `onStop is forwarded to display toolbar`() {
        val toolbar = BrowserToolbar(testContext)
        toolbar.display = mock()

        toolbar.onStop()

        verify(toolbar.display).onStop()
    }
}
