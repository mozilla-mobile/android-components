/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.extension

import mozilla.components.browser.icons.IconRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconMessageKtTest {
    @Test
    fun `Serializing and deserializing icon resources`() {
        val resources = listOf(
            IconRequest.Resource(
                url = "https://www.mozilla.org/icon64.png",
                sizes = listOf(IconRequest.Resource.Size(64, 64)),
                mimeType = "image/png",
                type = IconRequest.Resource.Type.FAVICON
            ),
            IconRequest.Resource(
                url = "https://www.mozilla.org/icon128.png",
                sizes = listOf(IconRequest.Resource.Size(128, 128)),
                mimeType = "image/png",
                type = IconRequest.Resource.Type.FAVICON
            ),
            IconRequest.Resource(
                url = "https://www.mozilla.org/icon128.png",
                sizes = listOf(IconRequest.Resource.Size(180, 180)),
                type = IconRequest.Resource.Type.APPLE_TOUCH_ICON
            )
        )

        val json = resources.toJSON()
        assertEquals(3, json.length())

        val restoredResources = json.toIconResources()
        assertEquals(resources, restoredResources)
    }
}
