/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.display

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.support.ktx.android.view.forEach
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DisplayToolbarTest {
    @Test
    fun `clicking on the URL switches the toolbar to editing mode`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val urlView = extractUrlView(displayToolbar)
        assertTrue(urlView.performClick())

        verify(toolbar).editMode()
    }

    @Test
    fun `progress is forwarded to progress bar`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val progressView = extractProgressView(displayToolbar)

        displayToolbar.updateProgress(10)
        assertEquals(10, progressView.progress)

        displayToolbar.updateProgress(50)
        assertEquals(50, progressView.progress)

        displayToolbar.updateProgress(75)
        assertEquals(75, progressView.progress)

        displayToolbar.updateProgress(100)
        assertEquals(100, progressView.progress)
    }

    @Test
    fun `icon view will use square size`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val iconView = extractIconView(displayToolbar)

        assertEquals(56, iconView.measuredWidth)
        assertEquals(56, iconView.measuredHeight)
    }

    @Test
    fun `progress view will use full width and 3dp height`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val progressView = extractProgressView(displayToolbar)

        assertEquals(1024, progressView.measuredWidth)
        assertEquals(3, progressView.measuredHeight)
    }

    @Test
    fun `menu view is gone by default`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val menuView = extractMenuView(displayToolbar)
        assertNotNull(menuView)
        assertTrue(menuView.visibility == View.GONE)
    }

    @Test
    fun `menu view becomes visible once a menu builder is set`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val menuView = extractMenuView(displayToolbar)
        assertNotNull(menuView)

        assertTrue(menuView.visibility == View.GONE)

        displayToolbar.menuBuilder = BrowserMenuBuilder(emptyList())

        assertTrue(menuView.visibility == View.VISIBLE)

        displayToolbar.menuBuilder = null

        assertTrue(menuView.visibility == View.GONE)
    }

    @Test
    fun `no menu builder is set by default`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        assertNull(displayToolbar.menuBuilder)
    }

    @Test
    fun `menu builder will be used to create and show menu when button is clicked`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)
        val menuView = extractMenuView(displayToolbar)

        val menuBuilder = mock(BrowserMenuBuilder::class.java)
        val menu = mock(BrowserMenu::class.java)
        doReturn(menu).`when`(menuBuilder).build(RuntimeEnvironment.application)

        displayToolbar.menuBuilder = menuBuilder

        verify(menuBuilder, never()).build(RuntimeEnvironment.application)
        verify(menu, never()).show(menuView)

        menuView.performClick()

        verify(menuBuilder).build(RuntimeEnvironment.application)
        verify(menu).show(menuView)
    }

    @Test
    fun `browser action gets added as view to toolbar`() {
        val contentDescription = "Mozilla"

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        assertNull(extractActionView(displayToolbar, contentDescription))

        val action = BrowserToolbar.Button(mock(), contentDescription) {}
        displayToolbar.addBrowserAction(action)

        val view = extractActionView(displayToolbar, contentDescription)
        assertNotNull(view)
        assertEquals(contentDescription, view?.contentDescription)
    }

    @Test
    fun `clicking browser action view triggers listener of action`() {
        var callbackExecuted = false

        val action = BrowserToolbar.Button(mock(), "Button") {
            callbackExecuted = true
        }

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)
        displayToolbar.addBrowserAction(action)

        val view = extractActionView(displayToolbar, "Button")
        assertNotNull(view)

        assertFalse(callbackExecuted)

        view?.performClick()

        assertTrue(callbackExecuted)
    }

    @Test
    fun `browser action view will use square size`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val action = BrowserToolbar.Button(mock(), "action") {}
        displayToolbar.addBrowserAction(action)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val view = extractActionView(displayToolbar, "action")!!

        assertEquals(56, view.measuredWidth)
        assertEquals(56, view.measuredHeight)
    }

    @Test
    fun `page actions will be added as view to the toolbar`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        assertNull(extractActionView(displayToolbar, "Reader Mode"))

        val action = BrowserToolbar.Button(mock(), "Reader Mode") {}
        displayToolbar.addPageAction(action)

        assertNotNull(extractActionView(displayToolbar, "Reader Mode"))
    }

    @Test
    fun `clicking a page action view will execute the listener of the action`() {
        var listenerExecuted = false

        val action = BrowserToolbar.Button(mock(), "Reload") {
            listenerExecuted = true
        }

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)
        displayToolbar.addPageAction(action)

        assertFalse(listenerExecuted)

        val view = extractActionView(displayToolbar, "Reload")
        assertNotNull(view)
        view!!.performClick()

        assertTrue(listenerExecuted)
    }

    @Test
    fun `views for page actions will have a square shape`() {
        val action = BrowserToolbar.Button(mock(), "Open app") {}

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)
        displayToolbar.addPageAction(action)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val view = extractActionView(displayToolbar, "Open app")!!

        assertEquals(56, view.measuredWidth)
        assertEquals(56, view.measuredHeight)
    }

    @Test
    fun `navigation actions will be added as view to the toolbar`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        assertNull(extractActionView(displayToolbar, "Back"))
        assertNull(extractActionView(displayToolbar, "Forward"))

        displayToolbar.addNavigationAction(BrowserToolbar.Button(mock(), "Back") {})
        displayToolbar.addNavigationAction(BrowserToolbar.Button(mock(), "Forward") {})

        assertNotNull(extractActionView(displayToolbar, "Back"))
        assertNotNull(extractActionView(displayToolbar, "Forward"))
    }

    @Test
    fun `clicking on navigation action will execute listener of the action`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        var listenerExecuted = false
        val action = BrowserToolbar.Button(mock(), "Back") {
            listenerExecuted = true
        }

        displayToolbar.addNavigationAction(action)

        assertFalse(listenerExecuted)

        extractActionView(displayToolbar, "Back")!!
            .performClick()

        assertTrue(listenerExecuted)
    }

    @Test
    fun `navigation action view will have a square shape`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        displayToolbar.addNavigationAction(
            BrowserToolbar.Button(mock(), "Back") {})

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val view = extractActionView(displayToolbar, "Back")!!

        assertEquals(56, view.measuredWidth)
        assertEquals(56, view.measuredHeight)
    }

    @Test
    fun `view of not visible navigation action gets removed after invalidating`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        var shouldActionBeDisplayed = true

        val action = BrowserToolbar.Button(
            mock(),
            "Back",
            visible = { shouldActionBeDisplayed }
        ) { /* Do nothing */ }

        displayToolbar.addNavigationAction(action)

        assertNotNull(extractActionView(displayToolbar, "Back"))

        shouldActionBeDisplayed = false
        displayToolbar.invalidateActions()

        assertNull(extractActionView(displayToolbar, "Back"))

        shouldActionBeDisplayed = true
        displayToolbar.invalidateActions()

        assertNotNull(extractActionView(displayToolbar, "Back"))
    }

    @Test
    fun `toolbar should call bind with view argument on action after invalidating`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val action = spy(BrowserToolbar.Button(mock(), "Reload") {})

        displayToolbar.addPageAction(action)

        val view = extractActionView(displayToolbar, "Reload")

        verify(action, never()).bind(view!!)

        displayToolbar.invalidateActions()

        verify(action).bind(view)
    }

    @Test
    fun `page action will not be added if visible lambda of action returns false`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val visibleAction = BrowserToolbar.Button(mock(), "Reload") {}
        val invisibleAction = BrowserToolbar.Button(
            mock(),
            "Reader Mode",
            visible = { false }) {}

        displayToolbar.addPageAction(visibleAction)
        displayToolbar.addPageAction(invisibleAction)

        assertNotNull(extractActionView(displayToolbar, "Reload"))
        assertNull(extractActionView(displayToolbar, "Reader Mode"))
    }

    @Test
    fun `browser action will not be added if visible lambda of action returns false`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val visibleAction = BrowserToolbar.Button(mock(), "Tabs") {}
        val invisibleAction = BrowserToolbar.Button(
            mock(),
                "Settings",
                visible = { false }) {}

        displayToolbar.addBrowserAction(visibleAction)
        displayToolbar.addBrowserAction(invisibleAction)

        assertNotNull(extractActionView(displayToolbar, "Tabs"))
        assertNull(extractActionView(displayToolbar, "Settings"))
    }

    @Test
    fun `navigation action will not be added if visible lambda of action returns false`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val visibleAction = BrowserToolbar.Button(mock(), "Forward") {}
        val invisibleAction = BrowserToolbar.Button(
            mock(),
                "Back",
                visible = { false }) {}

        displayToolbar.addNavigationAction(visibleAction)
        displayToolbar.addNavigationAction(invisibleAction)

        assertNotNull(extractActionView(displayToolbar, "Forward"))
        assertNull(extractActionView(displayToolbar, "Back"))
    }

    @Test
    fun `toolbar will honor minimum width of action view`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        val normalAction = BrowserToolbar.Button(mock(), "Forward") {}
        val backAction = object : BrowserToolbar.Button(mock(), "Back", listener = {}) {
            override fun createView(parent: ViewGroup): View {
                return super.createView(parent).apply {
                    minimumWidth = 500
                }
            }
        }

        displayToolbar.addNavigationAction(normalAction)
        displayToolbar.addNavigationAction(backAction)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(56, View.MeasureSpec.EXACTLY)

        displayToolbar.measure(widthSpec, heightSpec)

        val forwardView = extractActionView(displayToolbar, "Forward")!!
        val backView = extractActionView(displayToolbar, "Back")!!

        assertEquals(56, forwardView.measuredWidth)
        assertEquals(56, forwardView.measuredHeight)

        assertEquals(500, backView.measuredWidth)
        assertEquals(56, backView.measuredHeight)
    }

    @Test
    fun `url box view will be added and removed from display layout`() {
        val view = TextView(RuntimeEnvironment.application)

        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        view assertNotIn displayToolbar

        displayToolbar.urlBoxView = view

        view assertIn displayToolbar

        displayToolbar.urlBoxView = null

        view assertNotIn displayToolbar
    }

    @Test
    fun `url box size matches url and page actions size`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reload") {})
        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reader Mode") {})

        val view = TextView(RuntimeEnvironment.application)
        displayToolbar.urlBoxView = view

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)

        val urlView = extractUrlView(displayToolbar)
        val reloadView = extractActionView(displayToolbar, "Reload")!!
        val readerView = extractActionView(displayToolbar, "Reader Mode")!!

        assertTrue(view.measuredWidth > 0)

        assertEquals(view.measuredWidth, urlView.measuredWidth + reloadView.measuredWidth + readerView.measuredWidth)
        assertEquals(200, view.measuredHeight)
    }

    @Test
    fun `url box position is enclosing url and page actions`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reload") {})
        displayToolbar.addPageAction(BrowserToolbar.Button(mock(), "Reader Mode") {})

        val view = TextView(RuntimeEnvironment.application)
        displayToolbar.urlBoxView = view

        val widthSpec = View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST)

        displayToolbar.measure(widthSpec, heightSpec)
        displayToolbar.layout(0, 0, 1024, 200)

        val urlView = extractUrlView(displayToolbar)
        val reloadView = extractActionView(displayToolbar, "Reload")!!
        val readerView = extractActionView(displayToolbar, "Reader Mode")!!

        val viewRect = Rect(view.left, view.top, view.right, view.bottom)
        val urlViewRect = Rect(urlView.left, urlView.top, urlView.right, urlView.bottom)
        val reloadViewRect = Rect(reloadView.left, reloadView.top, reloadView.right, reloadView.bottom)
        val readerViewRect = Rect(readerView.left, readerView.top, readerView.right, readerView.bottom)

        assertTrue(viewRect.width() > 0)
        assertTrue(viewRect.height() > 0)
        assertTrue(viewRect.contains(urlViewRect))
        assertTrue(viewRect.contains(reloadViewRect))
        assertTrue(viewRect.contains(readerViewRect))
        assertEquals(urlViewRect.width() + reloadViewRect.width() + readerViewRect.width(), viewRect.width())
    }

    @Test
    fun `toolbar only switches to editing mode if onUrlClicked returns true`() {
        val toolbar = mock(BrowserToolbar::class.java)
        val displayToolbar = DisplayToolbar(RuntimeEnvironment.application, toolbar)

        displayToolbar.urlView.performClick()

        verify(toolbar).editMode()

        reset(toolbar)
        displayToolbar.onUrlClicked = { false }
        displayToolbar.urlView.performClick()

        verify(toolbar, never()).editMode()

        reset(toolbar)
        displayToolbar.onUrlClicked = { true }
        displayToolbar.urlView.performClick()

        verify(toolbar).editMode()
    }

    companion object {
        private fun extractUrlView(displayToolbar: DisplayToolbar): TextView {
            var textView: TextView? = null

            displayToolbar.forEach {
                if (it is TextView) {
                    textView = it
                    return@forEach
                }
            }

            return textView ?: throw AssertionError("Could not find URL view")
        }

        private fun extractProgressView(displayToolbar: DisplayToolbar): ProgressBar {
            var progressView: ProgressBar? = null

            displayToolbar.forEach {
                if (it is ProgressBar) {
                    progressView = it
                    return@forEach
                }
            }

            return progressView ?: throw AssertionError("Could not find URL view")
        }

        private fun extractIconView(displayToolbar: DisplayToolbar): ImageView {
            var iconView: ImageView? = null

            displayToolbar.forEach {
                if (it is ImageView) {
                    iconView = it
                    return@forEach
                }
            }

            return iconView ?: throw AssertionError("Could not find URL view")
        }

        private fun extractMenuView(displayToolbar: DisplayToolbar): ImageButton {
            var menuButton: ImageButton? = null

            displayToolbar.forEach {
                if (it is ImageButton) {
                    menuButton = it
                    return@forEach
                }
            }

            return menuButton ?: throw AssertionError("Could not find menu view")
        }

        private fun extractActionView(
            displayToolbar: DisplayToolbar,
            contentDescription: String
        ): ImageButton? {
            var actionView: ImageButton? = null

            displayToolbar.forEach {
                if (it is ImageButton && it.contentDescription == contentDescription) {
                    actionView = it
                    return@forEach
                }
            }

            return actionView
        }
    }
}

infix fun View.assertIn(group: ViewGroup) {
    var found = false

    group.forEach {
        if (this == it) {
            println("Checking $this == $it")
            found = true
        }
    }

    if (!found) {
        throw AssertionError("View not found in ViewGroup")
    }
}

infix fun View.assertNotIn(group: ViewGroup) {
    group.forEach {
        if (this == it) {
            throw AssertionError("View should not be in ViewGroup")
        }
    }
}
