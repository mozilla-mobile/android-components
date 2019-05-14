/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.domains

import android.content.Context
import kotlinx.coroutines.runBlocking
import mozilla.components.support.test.robolectric.applicationContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomDomainsTest {

    private val context by applicationContext()

    @Before
    fun setUp() {
        context.getSharedPreferences("custom_autocomplete", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun customListIsEmptyByDefault() {
        val domains = runBlocking {
            CustomDomains.load(context)
        }

        assertEquals(0, domains.size)
    }

    @Test
    fun saveAndRemoveDomains() {
        CustomDomains.save(context, listOf(
            "mozilla.org",
            "example.org",
            "example.com"
        ))

        var domains = CustomDomains.load(context)
        assertEquals(3, domains.size)

        CustomDomains.remove(context, listOf("example.org", "example.com"))
        domains = CustomDomains.load(context)
        assertEquals(1, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
    }

    @Test
    fun addAndLoadDomains() {
        CustomDomains.add(context, "mozilla.org")
        val domains = CustomDomains.load(context)
        assertEquals(1, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
    }

    @Test
    fun saveAndLoadDomains() {
        CustomDomains.save(context, listOf(
            "mozilla.org",
            "example.org",
            "example.com"
        ))

        val domains = CustomDomains.load(context)

        assertEquals(3, domains.size)
        assertEquals("mozilla.org", domains.elementAt(0))
        assertEquals("example.org", domains.elementAt(1))
        assertEquals("example.com", domains.elementAt(2))
    }
}