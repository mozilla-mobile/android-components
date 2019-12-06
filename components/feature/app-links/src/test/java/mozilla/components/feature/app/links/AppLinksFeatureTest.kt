/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.app.links

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLinksFeatureTest {
    private lateinit var mockContext: Context
    private lateinit var mockUseCases: AppLinksUseCases
    private lateinit var mockGetRedirect: AppLinksUseCases.GetAppLinkRedirect
    private lateinit var mockEngineSession: EngineSession
    private lateinit var appLinksFeature: AppLinksFeature

    private val currentWebUrl = "https://moilla.org"
    private val webUrl = "https://example.com"
    private val webUrlWithAppLink = "https://soundcloud.com"
    private val intentUrl = "zxing://scan"
    private val fallbackUrl = "https://getpocket.com"
    private val marketplaceUrl = "market://details?id=example.com"

    @Before
    fun setup() {
        mockContext = mock()
        mockUseCases = mock()
        mockEngineSession = mock()
        mockGetRedirect = mock()
        whenever(mockUseCases.interceptedAppLinkRedirect).thenReturn(mockGetRedirect)

        val webRedirect = AppLinkRedirect(null, webUrl, null)
        val appRedirect = AppLinkRedirect(Intent.parseUri(intentUrl, 0), null, null)
        val appRedirectFromWebUrl = AppLinkRedirect(Intent.parseUri(webUrlWithAppLink, 0), null, null)
        val fallbackRedirect = AppLinkRedirect(null, fallbackUrl, null)
        val marketRedirect = AppLinkRedirect(null, null, Intent.parseUri(marketplaceUrl, 0))

        whenever(mockGetRedirect.invoke(webUrl)).thenReturn(webRedirect)
        whenever(mockGetRedirect.invoke(intentUrl)).thenReturn(appRedirect)
        whenever(mockGetRedirect.invoke(webUrlWithAppLink)).thenReturn(appRedirectFromWebUrl)
        whenever(mockGetRedirect.invoke(fallbackUrl)).thenReturn(fallbackRedirect)
        whenever(mockGetRedirect.invoke(marketplaceUrl)).thenReturn(marketRedirect)

        appLinksFeature = AppLinksFeature(
            context = mockContext,
            interceptLinkClicks = true,
            useCases = mockUseCases
        )
    }

    @Test
    fun `external app is opened by user clicking on a link`() {
        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, false, currentWebUrl,
            webUrlWithAppLink, true)
        assert(response is RequestInterceptor.InterceptionResponse.AppIntent)
    }

    @Test
    fun `external app is not opened when not user clicking on a link`() {
        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, false, currentWebUrl,
            webUrlWithAppLink, false)
        assertEquals(null, response)
    }

    @Test
    fun `external app is not opened when in private mode`() {
        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, true, currentWebUrl,
            webUrlWithAppLink, true)
        assertEquals(null, response)
    }

    @Test
    fun `external app is not opened if the current session is already on the same host`() {
        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, false, webUrlWithAppLink,
            webUrlWithAppLink, true)
        assertEquals(null, response)
    }

    @Test
    fun `white and black listed schemes when triggered by user clicking on a link`() {
        val engineSession: EngineSession = mock()
        val whitelistedScheme = "whitelisted"
        val blacklistedScheme = "blacklisted"
        val feature = AppLinksFeature(
            context = mockContext,
            interceptLinkClicks = false,
            alwaysAllowedSchemes = setOf(whitelistedScheme),
            alwaysDeniedSchemes = setOf(blacklistedScheme),
            useCases = mockUseCases
        )

        val blackListedUrl = "$blacklistedScheme://example.com"
        val blacklistedRedirect = AppLinkRedirect(Intent.parseUri(blackListedUrl, 0), blackListedUrl, null)
        whenever(mockGetRedirect.invoke(blackListedUrl)).thenReturn(blacklistedRedirect)
        var response = feature.interceptor.onLoadRequest(engineSession, false, currentWebUrl, blackListedUrl, true)
        assertEquals(response, null)

        val whiteListedUrl = "$whitelistedScheme://example.com"
        val whitelistedRedirect = AppLinkRedirect(Intent.parseUri(whiteListedUrl, 0), whiteListedUrl, null)
        whenever(mockGetRedirect.invoke(whiteListedUrl)).thenReturn(whitelistedRedirect)
        response = feature.interceptor.onLoadRequest(engineSession, false, currentWebUrl, whiteListedUrl, true)
        assert(response is RequestInterceptor.InterceptionResponse.AppIntent)
    }

    @Test
    fun `an external app is not opened for URLs with javascript scheme`() {
        val javascriptUri = "javascript:;"

        val appRedirect = AppLinkRedirect(Intent.parseUri(javascriptUri, 0), null, null)
        whenever(mockGetRedirect.invoke(javascriptUri)).thenReturn(appRedirect)

        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, false, currentWebUrl,
            javascriptUri, true)
        assertEquals(null, response)
    }

    @Test
    fun `Use the fallback URL when no non-browser app is installed`() {
        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, false, currentWebUrl,
            fallbackUrl, true)
        assert(response is RequestInterceptor.InterceptionResponse.Url)
    }

    @Test
    fun `use the market intent if target app is not installed`() {
        val response = appLinksFeature.interceptor.onLoadRequest(mockEngineSession, false, currentWebUrl,
            marketplaceUrl, true)
        assert(response is RequestInterceptor.InterceptionResponse.AppIntent)
    }
}
