/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.search.custom

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.search.provider.custom.CustomSearchEngineWriter
import mozilla.components.support.test.mock
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomSearchEngineWriterTest {
    @Test
    fun `buildSearchEngineXML builds correct custom search engine xml`() {
        val icon = Icon(mock(), source = Icon.Source.GENERATOR)
        val searchEngineName = "TestTestGo"
        val searchQuery = "http://test.com"
        val customSearchEngineWriter = CustomSearchEngineWriter()

        val searchEngineXML = customSearchEngineWriter.buildSearchEngineXML(searchEngineName, searchQuery, icon.bitmap)
        assertTrue(searchEngineXML!!.contains(searchEngineName))
        assertTrue(searchEngineXML.contains(searchQuery))
    }
}
