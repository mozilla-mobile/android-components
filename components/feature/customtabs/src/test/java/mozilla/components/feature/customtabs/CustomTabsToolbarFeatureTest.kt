/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.customtabs

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.view.forEach
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.state.CustomTabActionButtonConfig
import mozilla.components.browser.state.state.CustomTabConfig
import mozilla.components.browser.state.state.CustomTabMenuItem
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class CustomTabsToolbarFeatureTest {

    private val sessionId = "custom-tab-session-id"
    @Mock private lateinit var sessionManager: SessionManager
    @Mock private lateinit var session: Session

    @Before
    fun setup() {
        sessionManager = spy(SessionManager(mock()))
        session = mock()

        `when`(sessionManager.findSessionById(sessionId)).thenReturn(session)
    }

    @Test
    fun `start without sessionId invokes nothing`() {
        val feature = spy(CustomTabsToolbarFeature(sessionManager, mock(), sessionId = null) {})

        feature.start()

        verify(sessionManager, never()).findSessionById(anyString())
        verify(feature, never()).initialize(eq(session), any())
    }

    @Test
    fun `start calls initialize with the sessionId`() {
        val config: CustomTabConfig = mock()
        val feature = spy(CustomTabsToolbarFeature(sessionManager, mock(), sessionId) {})

        `when`(session.customTabConfig).thenReturn(config)
        doNothing().`when`(feature).addCloseButton(session, null)

        feature.start()

        verify(feature).initialize(session, config)
        assertTrue(feature.initialized)

        // Calling start again should NOT call init again

        feature.start()

        verify(feature, times(1)).initialize(session, config)
    }

    @Test
    fun `stop calls unregister`() {
        val feature = CustomTabsToolbarFeature(sessionManager, mock(), sessionId) {}

        feature.stop()

        verify(session).unregister(any())
    }

    @Test
    fun `initialized it set to true if session is a customtab`() {
        val toolbar = spy(BrowserToolbar(testContext))
        var feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) {})
        `when`(session.customTabConfig).thenReturn(null)

        feature.start()

        assertFalse(feature.initialized)

        feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) {})
        `when`(session.customTabConfig).thenReturn(mock())

        feature.start()

        assertTrue(feature.initialized)
    }

    @Test
    fun `initialize updates toolbar`() {
        val toolbar = spy(BrowserToolbar(testContext))
        val config: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(config)

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId) {})

        feature.initialize(session, config)

        assertFalse(toolbar.onUrlClicked.invoke())
    }

    @Test
    fun `initialize calls updateToolbarColor`() {
        val toolbar = spy(BrowserToolbar(testContext))
        val config: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(config)

        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) {})

        feature.initialize(session, config)

        verify(feature).updateToolbarColor(anyInt(), anyInt())
    }

    @Test
    fun `updateToolbarColor changes background and textColor`() {
        val toolbar: BrowserToolbar = mock()
        `when`(session.customTabConfig).thenReturn(mock())

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId) {})

        feature.updateToolbarColor(null, null)

        verify(toolbar, never()).setBackgroundColor(anyInt())
        verify(toolbar, never()).textColor = anyInt()
        verify(toolbar, never()).trackingProtectionColor = anyInt()

        feature.updateToolbarColor(123, 456)

        verify(toolbar).setBackgroundColor(anyInt())
        verify(toolbar).trackingProtectionColor = anyInt()
        verify(toolbar).textColor = anyInt()
    }

    @Test
    fun `updateToolbarColor changes status bar color`() {
        val toolbar: BrowserToolbar = mock()
        val window: Window = mock()
        `when`(session.customTabConfig).thenReturn(mock())
        `when`(window.decorView).thenReturn(mock())

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId, window = window) {})

        feature.updateToolbarColor(null, null)

        verify(window, never()).statusBarColor = anyInt()
        verify(window, never()).navigationBarColor = anyInt()

        feature.updateToolbarColor(123, 456)

        verify(window).statusBarColor = 123
        verify(window).navigationBarColor = 456
    }

    @Test
    fun `initialize calls addCloseButton`() {
        val session: Session = mock()
        val toolbar = BrowserToolbar(testContext)
        val config = CustomTabConfig()
        `when`(session.customTabConfig).thenReturn(config)

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId) {})

        feature.initialize(session, config)

        verify(feature).addCloseButton(session, null)
    }

    @Test
    fun `initialize doesn't call addCloseButton if the button should be hidden`() {
        val session: Session = mock()
        val toolbar = BrowserToolbar(testContext)
        val config = CustomTabConfig(showCloseButton = false)
        `when`(session.customTabConfig).thenReturn(config)

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId) {})

        feature.initialize(session, config)

        verify(feature, never()).addCloseButton(session, null)
    }

    @Test
    fun `close button invokes callback and removes session`() {
        val toolbar = BrowserToolbar(testContext)
        var closeClicked = false
        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) { closeClicked = true })
        val config = CustomTabConfig()
        `when`(session.customTabConfig).thenReturn(config)

        feature.initialize(session, config)

        val button = extractActionView(toolbar, testContext.getString(R.string.mozac_feature_customtabs_exit_button))
        button?.performClick()

        assertTrue(closeClicked)
        verify(sessionManager).remove(session)
    }

    @Test
    fun `initialize calls addShareButton`() {
        val toolbar = BrowserToolbar(testContext)
        val config: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(config)
        `when`(session.url).thenReturn("https://mozilla.org")

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId) {})

        feature.initialize(session, config)

        verify(feature, never()).addShareButton(session)

        // Show share menu only if config.showShareMenuItem has true
        `when`(config.showShareMenuItem).thenReturn(true)

        feature.initialize(session, config)

        verify(feature).addShareButton(session)
    }

    @Test
    fun `share button uses custom share listener`() {
        val toolbar = spy(BrowserToolbar(testContext))
        val captor = argumentCaptor<Toolbar.ActionButton>()
        var clicked = false
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, sessionId, shareListener = { clicked = true }) {})

        `when`(session.customTabConfig).thenReturn(mock())

        feature.addShareButton(session)

        verify(toolbar).addBrowserAction(captor.capture())

        val button = captor.value.createView(FrameLayout(testContext))
        button.performClick()
        assertTrue(clicked)
    }

    @Test
    fun `initialize calls addActionButton`() {
        val toolbar = BrowserToolbar(testContext)
        val config: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(config)

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        feature.initialize(session, config)

        verify(feature).addActionButton(any(), any())
    }

    @Test
    fun `add action button is invoked`() {
        val toolbar = spy(BrowserToolbar(testContext))
        val captor = argumentCaptor<Toolbar.ActionButton>()
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})
        val customTabConfig: CustomTabConfig = mock()
        val actionConfig: CustomTabActionButtonConfig = mock()
        val size = 24
        val closeButtonIcon = Bitmap.createBitmap(IntArray(size * size), size, size, Bitmap.Config.ARGB_8888)
        val pendingIntent: PendingIntent = mock()

        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(session.url).thenReturn("https://example.com")

        feature.addActionButton(session, null)

        verify(toolbar, never()).addBrowserAction(any())

        // Show action button only when CustomTabActionButtonConfig is not null
        `when`(customTabConfig.actionButtonConfig).thenReturn(actionConfig)
        `when`(actionConfig.description).thenReturn("test desc")
        `when`(actionConfig.pendingIntent).thenReturn(pendingIntent)
        `when`(actionConfig.icon).thenReturn(closeButtonIcon)

        feature.addActionButton(session, actionConfig)

        verify(toolbar).addBrowserAction(captor.capture())

        val button = captor.value.createView(FrameLayout(testContext))
        button.performClick()
        verify(pendingIntent).send(any(), anyInt(), any())
    }

    @Test
    fun `initialize calls addMenuItems when config has items`() {
        val toolbar = BrowserToolbar(testContext)
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        feature.initialize(session, customTabConfig)

        verify(feature, never()).addMenuItems(any(), anyList(), anyInt())

        `when`(customTabConfig.menuItems).thenReturn(listOf(CustomTabMenuItem("Share", mock())))

        feature.initialize(session, customTabConfig)

        verify(feature).addMenuItems(any(), anyList(), anyInt())
    }

    @Test
    fun `initialize calls addMenuItems when menuBuilder has items`() {
        val menuBuilder: BrowserMenuBuilder = mock()
        val toolbar = BrowserToolbar(testContext)
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())
        `when`(menuBuilder.items).thenReturn(listOf(mock(), mock()))

        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "", menuBuilder) {})

        feature.initialize(session, customTabConfig)

        verify(feature).addMenuItems(any(), anyList(), anyInt())
    }

    @Test
    fun `initialize never calls addMenuItems when no config or builder items available`() {
        val session: Session = mock()
        val menuBuilder: BrowserMenuBuilder = mock()
        val toolbar = BrowserToolbar(testContext)
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)

        // With NO config or builder.
        var feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})

        feature.initialize(session, customTabConfig)

        verify(feature, never()).addMenuItems(any(), anyList(), anyInt())

        // With only builder but NO items.
        feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "", menuBuilder) {})

        feature.initialize(session, customTabConfig)

        // With only builder and items.
        `when`(menuBuilder.items).thenReturn(listOf())

        feature.initialize(session, customTabConfig)

        verify(feature, never()).addMenuItems(any(), anyList(), anyInt())

        // With config with NO items and builder with items.
        `when`(customTabConfig.menuItems).thenReturn(emptyList())

        feature.initialize(session, customTabConfig)

        verify(feature, never()).addMenuItems(any(), anyList(), anyInt())
    }

    @Test
    fun `menu items added WITHOUT current items`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(testContext))
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "") {})
        val captor = argumentCaptor<BrowserMenuBuilder>()
        val customTabConfig: CustomTabConfig = mock()

        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())

        feature.addMenuItems(session, listOf(CustomTabMenuItem("Share", mock())), 0)

        verify(toolbar).setMenuBuilder(captor.capture())
        assertEquals(1, captor.value.items.size)
    }

    @Test
    fun `menu items added WITH current items`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(testContext))
        val builder: BrowserMenuBuilder = mock()
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "", builder) {})
        val captor = argumentCaptor<BrowserMenuBuilder>()
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())
        `when`(builder.items).thenReturn(listOf(mock(), mock()))

        feature.addMenuItems(session, listOf(CustomTabMenuItem("Share", mock())), 0)

        verify(toolbar).setMenuBuilder(captor.capture())

        assertEquals(3, captor.value.items.size)
    }

    @Test
    fun `menu item added at specified index`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(testContext))
        val builder: BrowserMenuBuilder = mock()
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "", builder) {})
        val captor = argumentCaptor<BrowserMenuBuilder>()
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())
        `when`(builder.items).thenReturn(listOf(mock(), mock()))

        feature.addMenuItems(session, listOf(CustomTabMenuItem("Share", mock())), 1)

        verify(toolbar).setMenuBuilder(captor.capture())

        assertEquals(3, captor.value.items.size)
        assertTrue(captor.value.items[1] is SimpleBrowserMenuItem)
    }

    @Test
    fun `menu item added appended if index too large`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(testContext))
        val builder: BrowserMenuBuilder = mock()
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "", builder) {})
        val captor = argumentCaptor<BrowserMenuBuilder>()
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())
        `when`(builder.items).thenReturn(listOf(mock(), mock()))

        feature.addMenuItems(session, listOf(CustomTabMenuItem("Share", mock())), 4)

        verify(toolbar).setMenuBuilder(captor.capture())

        assertEquals(3, captor.value.items.size)
        assertTrue(captor.value.items[2] is SimpleBrowserMenuItem)
    }

    @Test
    fun `menu item added appended if index too small`() {
        val session: Session = mock()
        val toolbar = spy(BrowserToolbar(testContext))
        val builder: BrowserMenuBuilder = mock()
        val feature = spy(CustomTabsToolbarFeature(mock(), toolbar, "", builder) {})
        val captor = argumentCaptor<BrowserMenuBuilder>()
        val customTabConfig: CustomTabConfig = mock()
        `when`(session.customTabConfig).thenReturn(customTabConfig)
        `when`(customTabConfig.menuItems).thenReturn(emptyList())
        `when`(builder.items).thenReturn(listOf(mock(), mock()))

        feature.addMenuItems(session, listOf(CustomTabMenuItem("Share", mock())), -4)

        verify(toolbar).setMenuBuilder(captor.capture())

        assertEquals(3, captor.value.items.size)
        assertTrue(captor.value.items[2] is SimpleBrowserMenuItem)
    }

    @Test
    fun `onBackPressed removes initialized session`() {
        val sessionId = "123"
        val session: Session = mock()
        val toolbar = BrowserToolbar(testContext)
        val sessionManager: SessionManager = mock()
        var closeExecuted = false
        `when`(session.customTabConfig).thenReturn(mock())
        `when`(sessionManager.findSessionById(anyString())).thenReturn(session)

        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) { closeExecuted = true })
        feature.initialized = true

        val result = feature.onBackPressed()

        assertTrue(result)
        assertTrue(closeExecuted)
    }

    @Test
    fun `onBackPressed without a session does nothing`() {
        val sessionId = null
        val session: Session = mock()
        val toolbar = BrowserToolbar(testContext)
        val sessionManager: SessionManager = mock()
        var closeExecuted = false
        `when`(session.customTabConfig).thenReturn(mock())
        `when`(sessionManager.findSessionById(anyString())).thenReturn(session)

        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) { closeExecuted = true })

        val result = feature.onBackPressed()

        assertFalse(result)
        assertFalse(closeExecuted)
    }

    @Test
    fun `onBackPressed with uninitialized feature returns false`() {
        val sessionId = "123"
        val session: Session = mock()
        val toolbar = BrowserToolbar(testContext)
        val sessionManager: SessionManager = mock()
        var closeExecuted = false
        `when`(sessionManager.findSessionById(anyString())).thenReturn(session)

        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, sessionId) {
            closeExecuted = true
        })
        feature.initialized = false

        val result = feature.onBackPressed()

        assertFalse(result)
        assertFalse(closeExecuted)
    }

    @Test
    fun `keep readableColor if toolbarColor is provided`() {
        val sessionManager: SessionManager = mock()
        val toolbar = BrowserToolbar(testContext)
        val session: Session = mock()
        val customTabConfig: CustomTabConfig = mock()
        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, "") {})

        assertEquals(Color.WHITE, feature.readableColor)

        `when`(sessionManager.findSessionById(anyString())).thenReturn(session)
        `when`(session.customTabConfig).thenReturn(customTabConfig)

        feature.initialize(session, customTabConfig)

        assertEquals(Color.WHITE, feature.readableColor)

        `when`(customTabConfig.toolbarColor).thenReturn(Color.WHITE)

        feature.initialize(session, customTabConfig)

        assertEquals(Color.BLACK, feature.readableColor)
    }

    @Test
    fun `show title only if not empty`() {
        val sessionManager: SessionManager = mock()
        val toolbar = BrowserToolbar(testContext)
        val session = spy(Session("https://mozilla.org"))
        val customTabConfig: CustomTabConfig = mock()
        val feature = spy(CustomTabsToolbarFeature(sessionManager, toolbar, "") {})
        val title = "Internet for people, not profit - Mozilla"

        `when`(sessionManager.findSessionById(anyString())).thenReturn(session)
        `when`(session.customTabConfig).thenReturn(customTabConfig)

        feature.start()

        session.notifyObservers { onTitleChanged(session, "") }

        assertEquals("", toolbar.title)

        session.notifyObservers { onTitleChanged(session, title) }

        assertEquals(title, toolbar.title)
    }

    private fun extractActionView(
        browserToolbar: BrowserToolbar,
        contentDescription: String
    ): ImageButton? {
        var actionView: ImageButton? = null

        browserToolbar.forEach { group ->
            val viewGroup = group as ViewGroup

            viewGroup.forEach inner@{
                if (it is ImageButton && it.contentDescription == contentDescription) {
                    actionView = it
                    return@inner
                }
            }
        }

        return actionView
    }
}
