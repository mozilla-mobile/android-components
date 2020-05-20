/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.search.custom

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.search.provider.custom.CustomSearchEngineProvider
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CustomSearchEngineProviderTest {

    @Test
    fun `addSearchEngine successfully adds a new searchEngine`() = runBlockingTest {
        val icon = Icon(mock(), source = Icon.Source.GENERATOR)
        val icons: BrowserIcons = mock()
        whenever(icons.loadIcon(any())).thenReturn(CompletableDeferred(icon))

        val storage = CustomSearchEngineProvider.SearchEngineStorage(testContext)
        val customSearchEngineProvider = CustomSearchEngineProvider()

        val expectedSearchEngineXML =
            """<?xml version="1.0" encoding="UTF-8" standalone="no"?><OpenSearchDescription xmlns="http://a9.com/-/spec/opensearch/1.1/"><ShortName>TestTestGo</ShortName><Image height="16" width="16">data:image/png;base64,</Image><Description>TestTestGo</Description><Url template="http://test.com" type="text/html"/></OpenSearchDescription>"""
        val searchEngineName = "TestTestGo"
        val searchQuery = "http://test.com"
        customSearchEngineProvider.addSearchEngine(
            testContext,
            searchEngineName,
            searchQuery,
            icons
        )
        assertEquals(expectedSearchEngineXML, storage[searchEngineName])
    }

    @Test
    fun isCustomSearchEngine() {
        val icon = Icon(mock(), source = Icon.Source.GENERATOR)
        val icons: BrowserIcons = mock()
        whenever(icons.loadIcon(any())).thenReturn(CompletableDeferred(icon))

        val customSearchEngineProvider = CustomSearchEngineProvider()
        val testSearchEngineName = "TestTestGo"
        val searchQuery = "http://test.com"
        assertFalse(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))

        runBlockingTest {
            customSearchEngineProvider.addSearchEngine(
                testContext,
                testSearchEngineName,
                searchQuery,
                icons
            )
            assertTrue(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))
        }
    }

    @Test
    fun removeSearchEngine() {
        val icon = Icon(mock(), source = Icon.Source.GENERATOR)
        val icons: BrowserIcons = mock()
        whenever(icons.loadIcon(any())).thenReturn(CompletableDeferred(icon))

        val customSearchEngineProvider = CustomSearchEngineProvider()
        val testSearchEngineName = "TestTestGo"
        val searchQuery = "http://test.com"
        assertFalse(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))

        runBlockingTest {
            customSearchEngineProvider.addSearchEngine(
                testContext,
                testSearchEngineName,
                searchQuery,
                icons
            )
            assertTrue(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))

            customSearchEngineProvider.removeSearchEngine(
                testContext,
                testSearchEngineName
            )
            assertFalse(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))
        }
    }

    @Test
    fun replaceSearchEngine() {
        val icon = Icon(mock(), source = Icon.Source.GENERATOR)
        val icons: BrowserIcons = mock()
        whenever(icons.loadIcon(any())).thenReturn(CompletableDeferred(icon))

        val customSearchEngineProvider = CustomSearchEngineProvider()
        val testSearchEngineName = "TestTestGo"
        val searchQuery = "http://test.com"
        assertFalse(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))

        runBlockingTest {
            customSearchEngineProvider.addSearchEngine(
                testContext,
                testSearchEngineName,
                searchQuery,
                icons
            )
            assertTrue(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))

            val replaceSearchEngineName = "ReplaceReplaceGo"
            val replaceQuery = "http://replace.com"
            customSearchEngineProvider.replaceSearchEngine(
                testContext,
                testSearchEngineName,
                replaceSearchEngineName,
                replaceQuery,
                icons
            )
            assertFalse(customSearchEngineProvider.isCustomSearchEngine(testContext, testSearchEngineName))
            assertTrue(customSearchEngineProvider.isCustomSearchEngine(testContext, replaceSearchEngineName))
        }
    }
}
