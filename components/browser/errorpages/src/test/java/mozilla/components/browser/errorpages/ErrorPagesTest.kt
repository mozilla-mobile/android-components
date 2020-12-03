/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.errorpages

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.errorpages.ErrorPages.createUrlEncodedErrorPage
import mozilla.components.support.ktx.kotlin.urlEncode
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy

@RunWith(AndroidJUnit4::class)
class ErrorPagesTest {

    @Test
    fun `createUrlEncodedErrorPage should encoded error information into the URL`() {
        assertUrlEncodingIsValid(ErrorType.UNKNOWN)
        assertUrlEncodingIsValid(ErrorType.ERROR_SECURITY_SSL)
        assertUrlEncodingIsValid(ErrorType.ERROR_SECURITY_BAD_CERT)
        assertUrlEncodingIsValid(ErrorType.ERROR_NET_INTERRUPT)
        assertUrlEncodingIsValid(ErrorType.ERROR_NET_TIMEOUT)
        assertUrlEncodingIsValid(ErrorType.ERROR_CONNECTION_REFUSED)
        assertUrlEncodingIsValid(ErrorType.ERROR_UNKNOWN_SOCKET_TYPE)
        assertUrlEncodingIsValid(ErrorType.ERROR_REDIRECT_LOOP)
        assertUrlEncodingIsValid(ErrorType.ERROR_OFFLINE)
        assertUrlEncodingIsValid(ErrorType.ERROR_PORT_BLOCKED)
        assertUrlEncodingIsValid(ErrorType.ERROR_NET_RESET)
        assertUrlEncodingIsValid(ErrorType.ERROR_UNSAFE_CONTENT_TYPE)
        assertUrlEncodingIsValid(ErrorType.ERROR_CORRUPTED_CONTENT)
        assertUrlEncodingIsValid(ErrorType.ERROR_CONTENT_CRASHED)
        assertUrlEncodingIsValid(ErrorType.ERROR_INVALID_CONTENT_ENCODING)
        assertUrlEncodingIsValid(ErrorType.ERROR_UNKNOWN_HOST)
        assertUrlEncodingIsValid(ErrorType.ERROR_MALFORMED_URI)
        assertUrlEncodingIsValid(ErrorType.ERROR_UNKNOWN_PROTOCOL)
        assertUrlEncodingIsValid(ErrorType.ERROR_FILE_NOT_FOUND)
        assertUrlEncodingIsValid(ErrorType.ERROR_FILE_ACCESS_DENIED)
        assertUrlEncodingIsValid(ErrorType.ERROR_PROXY_CONNECTION_REFUSED)
        assertUrlEncodingIsValid(ErrorType.ERROR_UNKNOWN_PROXY_HOST)
        assertUrlEncodingIsValid(ErrorType.ERROR_SAFEBROWSING_MALWARE_URI)
        assertUrlEncodingIsValid(ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI)
        assertUrlEncodingIsValid(ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI)
        assertUrlEncodingIsValid(ErrorType.ERROR_SAFEBROWSING_PHISHING_URI)
    }

    private fun assertUrlEncodingIsValid(errorType: ErrorType) {
        val context = spy(testContext)

        val htmlFilename = "htmlResource.html"

        val uri = "sampleUri"

        val errorPage = createUrlEncodedErrorPage(
            context,
            errorType,
            uri,
            htmlFilename
        )

        val expectedImageName = if (errorType.imageNameRes != null) {
            context.resources.getString(errorType.imageNameRes!!) + ".svg"
        } else {
            ""
        }

        assertTrue(errorPage.startsWith("resource://android/assets/$htmlFilename"))
        assertTrue(errorPage.contains("&button=${context.resources.getString(errorType.refreshButtonRes).urlEncode()}"))

        val description = context.resources.getString(errorType.messageRes, uri).replace("<ul>", "<ul role=\"presentation\">")

        assertTrue(errorPage.contains("&description=${description.urlEncode()}"))
        assertTrue(errorPage.contains("&image=$expectedImageName"))
    }
}
