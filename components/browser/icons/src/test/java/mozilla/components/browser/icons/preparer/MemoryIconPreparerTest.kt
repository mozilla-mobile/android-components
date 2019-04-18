/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.preparer

import mozilla.components.browser.icons.IconRequest
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.doReturn

class MemoryIconPreparerTest {
    @Test
    fun `Preparer will add resources from cache`() {
        val resources = listOf(
            IconRequest.Resource("https://www.mozilla.org", type = IconRequest.Resource.Type.FAVICON),
            IconRequest.Resource("https://www.firefox.com", type = IconRequest.Resource.Type.APPLE_TOUCH_ICON)
        )

        val cache: MemoryIconPreparer.PreparerMemoryCache = mock()
        doReturn(resources).`when`(cache).getResources(any())

        val preparer = MemoryIconPreparer(cache)

        val initialRequest = IconRequest(url = "example.org")

        val request = preparer.prepare(initialRequest)

        assertEquals(2, request.resources.size)
        assertEquals(
            listOf(
                "https://www.mozilla.org",
                "https://www.firefox.com"
            ),
            request.resources.map { it.url }
        )
    }

    @Test
    fun `Preparer will not add resources if request already has resources`() {
        val resources = listOf(
            IconRequest.Resource("https://www.mozilla.org", type = IconRequest.Resource.Type.FAVICON),
            IconRequest.Resource("https://www.firefox.com", type = IconRequest.Resource.Type.APPLE_TOUCH_ICON)
        )

        val cache: MemoryIconPreparer.PreparerMemoryCache = mock()
        doReturn(resources).`when`(cache).getResources(any())

        val preparer = MemoryIconPreparer(cache)

        val initialRequest = IconRequest(url = "https://www.example.org", resources = listOf(
            IconRequest.Resource("https://getpocket.com", type = IconRequest.Resource.Type.FAVICON)
        ))

        val request = preparer.prepare(initialRequest)

        assertEquals(
            listOf(
                "https://getpocket.com"
            ),
            request.resources.map { it.url }
        )
    }
}
