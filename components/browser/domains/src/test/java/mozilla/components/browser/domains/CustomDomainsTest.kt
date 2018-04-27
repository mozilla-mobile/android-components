/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.domains

import android.content.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CustomDomainsTest {
    @Before
    fun setUp() {
        RuntimeEnvironment.application
                .getSharedPreferences("custom_autocomplete", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
    }

    @Test
    fun testCustomListIsEmptyByDefault() {
        val domains = runBlocking {
            CustomDomains.load(RuntimeEnvironment.application)
        }

        assertEquals(0, domains.size)
    }

    @Test
    fun testSaveAndRemoveDomains() {
        CustomDomains.save(RuntimeEnvironment.application, listOf(
                "mozilla.org",
                "example.org",
                "example.com"
        ))

        var domains = CustomDomains.load(RuntimeEnvironment.application)
        assertEquals(3, domains.size)

        CustomDomains.remove(RuntimeEnvironment.application, listOf("example.org", "example.com"))
        domains = CustomDomains.load(RuntimeEnvironment.application)
        assertEquals(1, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
    }

    @Test
    fun testAddAndLoadDomains() {
        CustomDomains.add(RuntimeEnvironment.application, "mozilla.org")
        val domains = CustomDomains.load(RuntimeEnvironment.application)
        assertEquals(1, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
    }

    @Test
    fun testSaveAndLoadDomains() {
        CustomDomains.save(RuntimeEnvironment.application, listOf(
                "mozilla.org",
                "example.org",
                "example.com"
        ))

        val domains = CustomDomains.load(RuntimeEnvironment.application)

        assertEquals(3, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
        assertEquals("example.org", domains.elementAt(1))
        assertEquals("example.com", domains.elementAt(2))
    }
}