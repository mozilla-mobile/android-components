/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.menu.BrowserMenu.Orientation.DOWN
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.menu.view.DynamicWidthRecyclerView
import mozilla.components.browser.menu.view.ExpandableLayout
import mozilla.components.concept.menu.MenuStyle
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay

@RunWith(AndroidJUnit4::class)
class BrowserMenuTest {

    @Test
    fun `show returns non-null popup window`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)

        assertNotNull(popup)
    }

    @Test
    fun `show assigns currAnchor and isShown`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)

        assertNotNull(popup)
        assertEquals(anchor, menu.currAnchor)
        assertTrue(menu.isShown)
    }

    @Test
    fun `show assigns width and background color`() {
        val items = listOf(SimpleBrowserMenuItem("Hello") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor, style = MenuStyle(
            backgroundColor = Color.RED,
            minWidth = 20,
            maxWidth = 500
        ))

        assertNotNull(popup)
        assertEquals(anchor, menu.currAnchor)
        assertTrue(menu.isShown)

        val cardView = popup.contentView.findViewById<CardView>(R.id.mozac_browser_menu_menuView)
        val recyclerView = popup.contentView.findViewById<DynamicWidthRecyclerView>(R.id.mozac_browser_menu_recyclerView)

        assertEquals(ColorStateList.valueOf(Color.RED), cardView.cardBackgroundColor)
        assertEquals(20, recyclerView.minWidth)
        assertEquals(500, recyclerView.maxWidth)
    }

    @Test
    fun `dismiss sets isShown to false`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)
        popup.dismiss()

        assertFalse(menu.isShown)
    }

    @Test
    fun `recyclerview adapter will have items for every menu item`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)

        val recyclerView: RecyclerView = popup.contentView.findViewById(R.id.mozac_browser_menu_recyclerView)
        assertNotNull(recyclerView)

        val recyclerAdapter = recyclerView.adapter!!
        assertNotNull(recyclerAdapter)
        assertEquals(2, recyclerAdapter.itemCount)
    }

    @Test
    fun `endOfMenuAlwaysVisible will be forwarded to recyclerview layoutManager`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = spy(BrowserMenuAdapter(testContext, items))
        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor, endOfMenuAlwaysVisible = true)

        val recyclerView: RecyclerView = popup.contentView.findViewById(R.id.mozac_browser_menu_recyclerView)
        assertNotNull(recyclerView)

        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        assertTrue(layoutManager.stackFromEnd)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M])
    fun `endOfMenuAlwaysVisible will be forwarded to scrollOnceToTheBottom on devices with Android M and below`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)
        val menu = spy(BrowserMenu(adapter))
        doNothing().`when`(menu).scrollOnceToTheBottom(any())

        val anchor = Button(testContext)
        val popup = menu.show(anchor, endOfMenuAlwaysVisible = true)

        val recyclerView: RecyclerView = popup.contentView.findViewById(R.id.mozac_browser_menu_recyclerView)
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        assertFalse(layoutManager.stackFromEnd)
        verify(menu).scrollOnceToTheBottom(any())
    }

    @Test
    fun `invalidate will be forwarded to recyclerview adapter`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = spy(BrowserMenuAdapter(testContext, items))

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)

        val recyclerView: RecyclerView = popup.contentView.findViewById(R.id.mozac_browser_menu_recyclerView)
        assertNotNull(recyclerView)
        assertNotNull(recyclerView.adapter)

        menu.invalidate()
        Mockito.verify(adapter).invalidate(recyclerView)
    }

    @Test
    fun `invalidate is a no-op if the menu is closed`() {
        val items = listOf(SimpleBrowserMenuItem("Hello") {})
        val menu = BrowserMenu(BrowserMenuAdapter(testContext, items))

        menu.invalidate()
    }

    @Test
    fun `created popup window is displayed automatically`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)

        assertTrue(popup.isShowing)
    }

    @Test
    fun `dismissing the browser menu will dismiss the popup`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {})

        val adapter = BrowserMenuAdapter(testContext, items)

        val menu = BrowserMenu(adapter)

        val anchor = Button(testContext)
        val popup = menu.show(anchor)

        assertTrue(popup.isShowing)

        menu.dismiss()

        assertFalse(popup.isShowing)
    }

    @Test
    fun `determineMenuOrientation returns Orientation-DOWN by default`() {
        assertEquals(
            BrowserMenu.Orientation.DOWN,
            BrowserMenu.determineMenuOrientation(mock())
        )
    }

    @Test
    fun `determineMenuOrientation returns Orientation-UP for views with bottom gravity in CoordinatorLayout`() {
        val params = CoordinatorLayout.LayoutParams(100, 100)
        params.gravity = Gravity.BOTTOM

        val view = View(testContext)
        view.layoutParams = params

        assertEquals(
            BrowserMenu.Orientation.UP,
            BrowserMenu.determineMenuOrientation(view)
        )
    }

    @Test
    fun `determineMenuOrientation returns Orientation-DOWN for views with top gravity in CoordinatorLayout`() {
        val params = CoordinatorLayout.LayoutParams(100, 100)
        params.gravity = Gravity.TOP

        val view = View(testContext)
        view.layoutParams = params

        assertEquals(
            BrowserMenu.Orientation.DOWN,
            BrowserMenu.determineMenuOrientation(view)
        )
    }

    @Test
    fun `Popup#show will initialize the menuPositioningData`() {
        val adapter = BrowserMenuAdapter(testContext, emptyList())
        val menu = BrowserMenu(adapter)
        val anchor = Button(testContext)
        setScreenHeight(100)

        menu.show(anchor)

        val expected = MenuPositioningData(
            BrowserMenuPlacement.AnchoredToTop.Dropdown(anchor), DOWN, false, true, 0, 100, 28
        )
        assertEquals(expected, menu.menuPositioningData)
    }

    @Test
    fun `configureExpandableMenu will setup a new ExpandabeLayout for a AnchoredToBottom#ManualAnchoring menu`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World", isCollapsingMenuLimit = true) {}
        )
        val adapter = BrowserMenuAdapter(testContext, items)
        val menu = BrowserMenu(adapter)
        val view = FrameLayout(testContext)
        val anchor = Button(testContext)
        menu.menuPositioningData = MenuPositioningData(BrowserMenuPlacement.AnchoredToBottom.Dropdown(anchor))

        val result = menu.configureExpandableMenu(view, true)

        assertTrue(result is ExpandableLayout)
        assertTrue(result.getChildAt(0) == view)
    }

    @Test
    fun `configureExpandableMenu will setup a new ExpandabeLayout for a AnchoredToBottom#Dropdown menu`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World", isCollapsingMenuLimit = true) {}
        )
        val adapter = BrowserMenuAdapter(testContext, items)
        val menu = BrowserMenu(adapter)
        val view = FrameLayout(testContext)
        val anchor = Button(testContext)
        menu.menuPositioningData = MenuPositioningData(BrowserMenuPlacement.AnchoredToBottom.ManualAnchoring(anchor))

        val result = menu.configureExpandableMenu(view, true)

        assertTrue(result is ExpandableLayout)
        assertTrue(result.getChildAt(0) == view)
    }

    @Test
    fun `configureExpandableMenu will not setup a new ExpandableLayout if none of the items can serve as a collapsingMenuLimit`() {
        val items = listOf(
            SimpleBrowserMenuItem("Hello") {},
            SimpleBrowserMenuItem("World") {}
        )
        val adapter = BrowserMenuAdapter(testContext, items)
        val menu = BrowserMenu(adapter)
        val view = FrameLayout(testContext)
        val anchor = Button(testContext)
        menu.menuPositioningData = MenuPositioningData(BrowserMenuPlacement.AnchoredToBottom.ManualAnchoring(anchor))

        val result = menu.configureExpandableMenu(view, true)

        assertFalse(result is ExpandableLayout)
        assertTrue(result == view)
    }

    @Test
    fun `GIVEN a not expandable menu WHEN configureExpandableMenu is called for one which should not be scrolled to bottom THEN the same menu is returned`() {
        val menu = spy(BrowserMenu(mock()))
        menu.menuPositioningData = MenuPositioningData(BrowserMenuPlacement.AnchoredToTop.Dropdown(mock()))
        val viewGroup: ViewGroup = mock()

        val result = menu.configureExpandableMenu(viewGroup, false)

        assertSame(viewGroup, result)
        verify(menu, never()).showMenuBottom(any())
    }

    @Test
    fun `GIVEN a not expandable menu WHEN configureExpandableMenu is called for one which should be scrolled to bottom THEN the layout manager is updated for this`() {
        val menu = spy(BrowserMenu(mock()))
        menu.menuPositioningData = MenuPositioningData(BrowserMenuPlacement.AnchoredToTop.Dropdown(mock()))
        val menuList = RecyclerView(testContext)
        menu.menuList = menuList

        val result = menu.configureExpandableMenu(menuList, true)

        assertSame(menuList, result)
        verify(menu).showMenuBottom(menuList)
    }

    @Test
    fun `GIVEN a menu that should be scrolled to the bottom WHEN showMenuBottom is called THEN it replaces the layout manager and sets stackFromEnd`() {
        val menu = spy(BrowserMenu(mock()))
        // Call show to have a default layout manager set
        menu.show(View(testContext))
        val initialLayoutManager = menu.menuList!!.layoutManager

        menu.showMenuBottom(menu.menuList!!)

        assertNotSame(initialLayoutManager, menu.menuList!!.layoutManager)
        assertTrue((menu.menuList!!.layoutManager as LinearLayoutManager).stackFromEnd)
    }

    @Test
    fun `getNewPopupWindow will return a PopupWindow with MATCH_PARENT height if the view is ExpandableLayout`() {
        val expandableLayout = ExpandableLayout.wrapContentInExpandableView(FrameLayout(testContext), 0) { }

        val result = BrowserMenu(mock()).getNewPopupWindow(expandableLayout)

        assertSame(expandableLayout, result.contentView)
        assertTrue(result.height == MATCH_PARENT)
        assertTrue(result.width == WRAP_CONTENT)
    }

    @Test
    fun `getNewPopupWindow will return a PopupWindow with WRAP_CONTENT height if the view is not ExpandableLayout`() {
        val notExpandableLayout = FrameLayout(testContext)

        val result = BrowserMenu(mock()).getNewPopupWindow(notExpandableLayout)

        assertSame(notExpandableLayout, result.contentView)
        assertTrue(result.height == WRAP_CONTENT)
        assertTrue(result.width == WRAP_CONTENT)
    }

    @Test
    fun `popup is dismissed when anchor is detached`() {
        val items = listOf(
            SimpleBrowserMenuItem("Mock") {},
            SimpleBrowserMenuItem("Menu") {})
        val adapter = BrowserMenuAdapter(testContext, items)
        val menu = BrowserMenu(adapter)
        val anchor = Button(testContext)
        val popupWindow = menu.show(anchor)

        assertTrue(popupWindow.isShowing)

        menu.onViewDetachedFromWindow(anchor)

        assertFalse(popupWindow.isShowing)
    }

    private fun setScreenHeight(value: Int) {
        val display = ShadowDisplay.getDefaultDisplay()
        val shadow = Shadows.shadowOf(display)
        shadow.setHeight(value)
    }
}
