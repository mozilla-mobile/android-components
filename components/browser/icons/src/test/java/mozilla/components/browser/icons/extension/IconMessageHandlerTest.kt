/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.extension

import android.graphics.Bitmap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.session.Session
import mozilla.components.support.test.any
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconMessageHandlerTest {
    @Test
    fun `Complex message (TheVerge) is transformed into IconRequest and loaded`() = runBlocking {
        val session: Session = mock()
        doReturn("https://www.theverge.com/").`when`(session).url

        val bitmap: Bitmap = mock()
        val icon = Icon(bitmap, source = Icon.Source.DOWNLOAD)
        val deferredIcon = GlobalScope.async { icon }

        val icons: BrowserIcons = mock()
        doReturn(deferredIcon).`when`(icons).loadIcon(any())

        val handler = IconMessageHandler(session, icons)

        val message = """
            {
              "url": "https:\/\/www.theverge.com\/",
              "icons": [
                {
                  "mimeType": "image\/png",
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7395367\/favicon-16x16.0.png",
                  "type": "icon",
                  "sizes": [
                    "16x16"
                  ]
                },
                {
                  "mimeType": "image\/png",
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7395363\/favicon-32x32.0.png",
                  "type": "icon",
                  "sizes": [
                    "32x32"
                  ]
                },
                {
                  "mimeType": "image\/png",
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7395365\/favicon-96x96.0.png",
                  "type": "icon",
                  "sizes": [
                    "96x96"
                  ]
                },
                {
                  "mimeType": "image\/png",
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7395351\/android-chrome-192x192.0.png",
                  "type": "icon",
                  "sizes": [
                    "192x192"
                  ]
                },
                {
                  "mimeType": "",
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7395361\/favicon-64x64.0.ico",
                  "type": "shortcut icon",
                  "sizes": []
                },
                {
                  "mimeType": "",
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7395359\/ios-icon.0.png",
                  "type": "apple-touch-icon",
                  "sizes": [
                    "180x180"
                  ]
                },
                {
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/9672633\/VergeOG.0_1200x627.0.png",
                  "type": "og:image"
                },
                {
                  "href": "https:\/\/cdn.vox-cdn.com\/community_logos\/52803\/VER_Logomark_175x92..png",
                  "type": "twitter:image"
                },
                {
                  "href": "https:\/\/cdn.vox-cdn.com\/uploads\/chorus_asset\/file\/7396113\/221a67c8-a10f-11e6-8fae-983107008690.0.png",
                  "type": "msapplication-TileImage"
                }
              ]
            }
        """.trimIndent()

        handler.onMessage(JSONObject(message), source = null)

        assertNotNull(handler.lastJob)
        handler.lastJob!!.join()

        // Examine IconRequest
        val captor = argumentCaptor<IconRequest>()
        verify(icons).loadIcon(captor.capture())

        val request = captor.value
        assertEquals("https://www.theverge.com/", request.url)
        assertEquals(9, request.resources.size)

        with(request.resources[0]) {
            assertEquals("image/png", mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395367/favicon-16x16.0.png", url)
            assertEquals(IconRequest.Resource.Type.FAVICON, type)
            assertEquals(1, sizes.size)
            assertEquals(IconRequest.Resource.Size(16, 16), sizes[0])
        }

        with(request.resources[1]) {
            assertEquals("image/png", mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395363/favicon-32x32.0.png", url)
            assertEquals(IconRequest.Resource.Type.FAVICON, type)
            assertEquals(1, sizes.size)
            assertEquals(IconRequest.Resource.Size(32, 32), sizes[0])
        }

        with(request.resources[2]) {
            assertEquals("image/png", mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395365/favicon-96x96.0.png", url)
            assertEquals(IconRequest.Resource.Type.FAVICON, type)
            assertEquals(1, sizes.size)
            assertEquals(IconRequest.Resource.Size(96, 96), sizes[0])
        }

        with(request.resources[3]) {
            assertEquals("image/png", mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395351/android-chrome-192x192.0.png", url)
            assertEquals(IconRequest.Resource.Type.FAVICON, type)
            assertEquals(1, sizes.size)
            assertEquals(IconRequest.Resource.Size(192, 192), sizes[0])
        }

        with(request.resources[4]) {
            assertNull(mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395361/favicon-64x64.0.ico", url)
            assertEquals(IconRequest.Resource.Type.FAVICON, type)
            assertEquals(0, sizes.size)
        }

        with(request.resources[5]) {
            assertNull(mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7395359/ios-icon.0.png", url)
            assertEquals(IconRequest.Resource.Type.APPLE_TOUCH_ICON, type)
            assertEquals(1, sizes.size)
            assertEquals(IconRequest.Resource.Size(180, 180), sizes[0])
        }

        with(request.resources[6]) {
            assertNull(mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/9672633/VergeOG.0_1200x627.0.png", url)
            assertEquals(IconRequest.Resource.Type.OPENGRAPH, type)
            assertEquals(0, sizes.size)
        }

        with(request.resources[7]) {
            assertNull(mimeType)
            assertEquals("https://cdn.vox-cdn.com/community_logos/52803/VER_Logomark_175x92..png", url)
            assertEquals(IconRequest.Resource.Type.TWITTER, type)
            assertEquals(0, sizes.size)
        }

        with(request.resources[8]) {
            assertNull(mimeType)
            assertEquals("https://cdn.vox-cdn.com/uploads/chorus_asset/file/7396113/221a67c8-a10f-11e6-8fae-983107008690.0.png", url)
            assertEquals(IconRequest.Resource.Type.MICROSOFT_TILE, type)
            assertEquals(0, sizes.size)
        }

        // Loaded icon will be set on session
        verify(session).icon = bitmap
    }
}
