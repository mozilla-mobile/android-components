/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.action

import android.graphics.Bitmap
import mozilla.components.browser.state.selector.findCustomTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SecurityInfoState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy

class ContentActionTest {
    private lateinit var store: BrowserStore
    private lateinit var tabId: String
    private lateinit var otherTabId: String

    private val tab: TabSessionState
        get() = store.state.tabs.find { it.id == tabId }!!

    private val otherTab: TabSessionState
        get() = store.state.tabs.find { it.id == otherTabId }!!

    @Before
    fun setUp() {
        val state = BrowserState(tabs = listOf(
            createTab(url = "https://www.mozilla.org").also {
                tabId = it.id
            },
            createTab(url = "https://www.firefox.com").also {
                otherTabId = it.id
            }
        ))

        store = BrowserStore(state)
    }

    @Test
    fun `UpdateUrlAction updates URL`() {
        val newUrl = "https://www.example.org"

        assertNotEquals(newUrl, tab.content.url)
        assertNotEquals(newUrl, otherTab.content.url)

        store.dispatch(
            ContentAction.UpdateUrlAction(tab.id, newUrl)
        ).joinBlocking()

        assertEquals(newUrl, tab.content.url)
        assertNotEquals(newUrl, otherTab.content.url)
    }

    @Test
    fun `UpdateLoadingStateAction updates loading state`() {
        assertFalse(tab.content.loading)
        assertFalse(otherTab.content.loading)

        store.dispatch(
            ContentAction.UpdateLoadingStateAction(tab.id, true)
        ).joinBlocking()

        assertTrue(tab.content.loading)
        assertFalse(otherTab.content.loading)

        store.dispatch(
            ContentAction.UpdateLoadingStateAction(tab.id, false)
        ).joinBlocking()

        assertFalse(tab.content.loading)
        assertFalse(otherTab.content.loading)

        store.dispatch(
            ContentAction.UpdateLoadingStateAction(tab.id, true)
        ).joinBlocking()

        store.dispatch(
            ContentAction.UpdateLoadingStateAction(otherTab.id, true)
        ).joinBlocking()

        assertTrue(tab.content.loading)
        assertTrue(otherTab.content.loading)
    }

    @Test
    fun `UpdateTitleAction updates title`() {
        val newTitle = "This is a title"

        assertNotEquals(newTitle, tab.content.title)
        assertNotEquals(newTitle, otherTab.content.title)

        store.dispatch(
            ContentAction.UpdateTitleAction(tab.id, newTitle)
        ).joinBlocking()

        assertEquals(newTitle, tab.content.title)
        assertNotEquals(newTitle, otherTab.content.title)
    }

    @Test
    fun `UpdateProgressAction updates progress`() {
        assertEquals(0, tab.content.progress)
        assertEquals(0, otherTab.content.progress)

        store.dispatch(ContentAction.UpdateProgressAction(tab.id, 75)).joinBlocking()

        assertEquals(75, tab.content.progress)
        assertEquals(0, otherTab.content.progress)

        store.dispatch(ContentAction.UpdateProgressAction(otherTab.id, 25)).joinBlocking()
        store.dispatch(ContentAction.UpdateProgressAction(tab.id, 85)).joinBlocking()

        assertEquals(85, tab.content.progress)
        assertEquals(25, otherTab.content.progress)
    }

    @Test
    fun `UpdateSearchTermsAction updates URL`() {
        val searchTerms = "Hello World"

        assertNotEquals(searchTerms, tab.content.searchTerms)
        assertNotEquals(searchTerms, otherTab.content.searchTerms)

        store.dispatch(
            ContentAction.UpdateSearchTermsAction(tab.id, searchTerms)
        ).joinBlocking()

        assertEquals(searchTerms, tab.content.searchTerms)
        assertNotEquals(searchTerms, otherTab.content.searchTerms)
    }

    @Test
    fun `UpdateSecurityInfo updates searchInfo`() {
        val newSecurityInfo = SecurityInfoState(true, "mozilla.org", "The Mozilla Team")

        assertNotEquals(newSecurityInfo, tab.content.securityInfo)
        assertNotEquals(newSecurityInfo, otherTab.content.securityInfo)

        store.dispatch(
            ContentAction.UpdateSecurityInfo(tab.id, newSecurityInfo)
        ).joinBlocking()

        assertEquals(newSecurityInfo, tab.content.securityInfo)
        assertNotEquals(newSecurityInfo, otherTab.content.securityInfo)

        assertEquals(true, tab.content.securityInfo.secure)
        assertEquals("mozilla.org", tab.content.securityInfo.host)
        assertEquals("The Mozilla Team", tab.content.securityInfo.issuer)
    }

    @Test
    fun `UpdateThumbnailAction updates thumbnail`() {
        val thumbnail = spy(Bitmap::class.java)

        assertNotEquals(thumbnail, tab.content.thumbnail)
        assertNotEquals(thumbnail, otherTab.content.thumbnail)

        store.dispatch(
                ContentAction.UpdateThumbnailAction(tab.id, thumbnail)
        ).joinBlocking()

        assertEquals(thumbnail, tab.content.thumbnail)
        assertNotEquals(thumbnail, otherTab.content.thumbnail)
    }

    @Test
    fun `RemoveThumbnailAction removes thumbnail`() {
        val thumbnail = spy(Bitmap::class.java)

        assertNotEquals(thumbnail, tab.content.thumbnail)

        store.dispatch(
                ContentAction.UpdateThumbnailAction(tab.id, thumbnail)
        ).joinBlocking()

        assertEquals(thumbnail, tab.content.thumbnail)

        store.dispatch(
                ContentAction.RemoveThumbnailAction(tab.id)
        ).joinBlocking()

        assertNull(tab.content.thumbnail)
    }

    @Test
    fun `Updating custom tab`() {
        val customTab = createCustomTab("https://getpocket.com")
        val otherCustomTab = createCustomTab("https://www.google.com")

        store.dispatch(CustomTabListAction.AddCustomTabAction(customTab)).joinBlocking()
        store.dispatch(CustomTabListAction.AddCustomTabAction(otherCustomTab)).joinBlocking()

        store.dispatch(ContentAction.UpdateUrlAction(customTab.id, "https://www.example.org")).joinBlocking()
        store.dispatch(ContentAction.UpdateTitleAction(customTab.id, "I am a custom tab")).joinBlocking()

        val updatedCustomTab = store.state.findCustomTab(customTab.id)!!
        val updatedOtherCustomTab = store.state.findCustomTab(otherCustomTab.id)!!

        assertEquals("https://www.example.org", updatedCustomTab.content.url)
        assertNotEquals("https://www.example.org", updatedOtherCustomTab.content.url)
        assertNotEquals("https://www.example.org", tab.content.url)
        assertNotEquals("https://www.example.org", otherTab.content.url)

        assertEquals("I am a custom tab", updatedCustomTab.content.title)
        assertNotEquals("I am a custom tab", updatedOtherCustomTab.content.title)
        assertNotEquals("I am a custom tab", tab.content.title)
        assertNotEquals("I am a custom tab", otherTab.content.title)
    }
}