/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.icons.loader;

import android.content.Context;
import mozilla.components.browser.icons.IconDescriptor;
import mozilla.components.browser.icons.IconRequest;
import mozilla.components.browser.icons.IconResponse;
import mozilla.components.browser.icons.Icons;
import mozilla.components.browser.icons.storage.FailureCache;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class TestIconDownloader {
    /**
     * Scenario: A request with a non HTTP URL (data:image/*) is executed.
     *
     * Verify that:
     *  * No download is performed.
     */
    @Test
    public void testDownloaderDoesNothingForNonHttpUrls() throws Exception {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl("http://www.mozilla.org")
                .icon(IconDescriptor.createGenericIcon(
                        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAEklEQVR4AWP4z8AAxCDiP8N/AB3wBPxcBee7AAAAAElFTkSuQmCC"))
                .build();

        final IconDownloader downloader = spy(new IconDownloader());
        IconResponse response = downloader.load(request);

        Assert.assertNull(response);

        verify(downloader, never()).downloadAndDecodeImage(any(Context.class), anyString());
        verify(downloader, never()).connectTo(anyString());
    }

    /**
     * Scenario: Request contains an URL and server returns 301 with location header (always the same URL).
     *
     * Verify that:
     *  * Download code stops and does not loop forever.
     */
    @Test
    public void testRedirectsAreFollowedButNotInCircles() throws Exception {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl("http://www.mozilla.org")
                .icon(IconDescriptor.createFavicon(
                        "https://www.mozilla.org/media/img/favicon.52506929be4c.ico",
                        32,
                        "image/x-icon"))
                .build();

        HttpURLConnection mockedConnection = mock(HttpURLConnection.class);
        doReturn(301).when(mockedConnection).getResponseCode();
        doReturn("http://example.org/favicon.ico").when(mockedConnection).getHeaderField("Location");

        final IconDownloader downloader = spy(new IconDownloader());
        doReturn(mockedConnection).when(downloader).connectTo(anyString());
        IconResponse response = downloader.load(request);

        Assert.assertNull(response);

        verify(downloader).connectTo("https://www.mozilla.org/media/img/favicon.52506929be4c.ico");
        verify(downloader).connectTo("http://example.org/favicon.ico");
    }

    /**
     * Scenario: Request contains an URL and server returns HTTP 404.
     *
     * Verify that:
     *  * URL is added to failure cache.
     */
    @Test
    public void testUrlIsAddedToFailureCacheIfServerReturnsClientError() throws Exception {
        final String faviconUrl = "https://www.mozilla.org/404.ico";

        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl("http://www.mozilla.org")
                .icon(IconDescriptor.createFavicon(faviconUrl, 32, "image/x-icon"))
                .build();

        HttpURLConnection mockedConnection = mock(HttpURLConnection.class);
        doReturn(404).when(mockedConnection).getResponseCode();

        Assert.assertFalse(FailureCache.get().isKnownFailure(faviconUrl));

        final IconDownloader downloader = spy(new IconDownloader());
        doReturn(mockedConnection).when(downloader).connectTo(anyString());
        IconResponse response = downloader.load(request);

        Assert.assertNull(response);

        Assert.assertTrue(FailureCache.get().isKnownFailure(faviconUrl));
    }

    /**
     * Scenario: Connected to successfully to server but reading the response code throws an exception.
     *
     * Verify that:
     *  * disconnect() is called on HttpUrlConnection
     */
    @Test
    public void testConnectionIsClosedWhenReadingResponseCodeThrows() throws Exception {
        final IconRequest request = Icons.with(RuntimeEnvironment.application)
                .pageUrl("http://www.mozilla.org")
                .icon(IconDescriptor.createFavicon(
                        "https://www.mozilla.org/media/img/favicon.52506929be4c.ico",
                        32,
                        "image/x-icon"))
                .build();

        HttpURLConnection mockedConnection = mock(HttpURLConnection.class);
        doThrow(new IOException()).when(mockedConnection).getResponseCode();

        final IconDownloader downloader = spy(new IconDownloader());
        doReturn(mockedConnection).when(downloader).connectTo(anyString());
        IconResponse response = downloader.load(request);

        Assert.assertNull(response);

        verify(mockedConnection).disconnect();
    }
}
