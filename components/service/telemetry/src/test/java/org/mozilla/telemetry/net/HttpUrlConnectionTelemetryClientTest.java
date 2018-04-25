/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.net;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.net.HttpURLConnectionTelemetryClient;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import okhttp3.internal.http.HttpMethod;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class HttpUrlConnectionTelemetryClientTest {
    private static final String TEST_PATH = "/some/random/path/not/important";
    private static final String TEST_PING = "{ 'ping': 'test' }";

    @Test
    public void testTimeoutsAreSet() throws Exception {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application)
                .setReadTimeout(7050)
                .setConnectTimeout(3050);

        final HttpURLConnection connection = mock(HttpURLConnection.class);

        final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
        doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());
        doReturn(200).when(client).upload(connection, TEST_PING);

        client.uploadPing(configuration, TEST_PATH, TEST_PING);

        verify(connection).setReadTimeout(7050);
        verify(connection).setConnectTimeout(3050);
    }

    @Test
    public void testUserAgentIsSet() throws Exception {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application)
                .setUserAgent("Telemetry/Test 25.0.2");
        final HttpURLConnection connection = mock(HttpURLConnection.class);

        final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
        doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());
        doReturn(200).when(client).upload(connection, TEST_PING);

        client.uploadPing(configuration, TEST_PATH, TEST_PING);

        verify(connection).setRequestProperty("User-Agent", "Telemetry/Test 25.0.2");
    }

    @Test
    public void testReturnsTrueFor200ResponseCode() throws Exception {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application);

        final HttpURLConnection connection = mock(HttpURLConnection.class);
        doReturn(200).when(connection).getResponseCode();
        doReturn(mock(OutputStream.class)).when(connection).getOutputStream();

        final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
        doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());

        assertTrue(client.uploadPing(configuration, TEST_PATH, TEST_PING));
    }

    @Test
    public void testReturnsFalseFor5XXResponseCodes() throws IOException {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application);

        for (int responseCode = 500; responseCode <= 527; responseCode++) {
            final HttpURLConnection connection = mock(HttpURLConnection.class);
            doReturn(responseCode).when(connection).getResponseCode();
            doReturn(mock(OutputStream.class)).when(connection).getOutputStream();

            final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
            doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());

            assertFalse(client.uploadPing(configuration, TEST_PATH, TEST_PING));
        }
    }

    @Test
    public void testReturnsTrueFor2XXResponseCodes() throws IOException {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application);

        for (int responseCode = 200; responseCode <= 226; responseCode++) {
            final HttpURLConnection connection = mock(HttpURLConnection.class);
            doReturn(responseCode).when(connection).getResponseCode();
            doReturn(mock(OutputStream.class)).when(connection).getOutputStream();

            final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
            doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());

            assertTrue(client.uploadPing(configuration, TEST_PATH, TEST_PING));
        }
    }

    @Test
    public void testReturnsTrueFor4XXResponseCodes() throws IOException {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application);

        for (int responseCode = 400; responseCode <= 451; responseCode++) {
            final HttpURLConnection connection = mock(HttpURLConnection.class);
            doReturn(responseCode).when(connection).getResponseCode();
            doReturn(mock(OutputStream.class)).when(connection).getOutputStream();

            final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
            doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());

            assertTrue(client.uploadPing(configuration, TEST_PATH, TEST_PING));
        }
    }

    @Test
    public void testUpload() throws Exception {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("OK"));

        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application)
                .setUserAgent("Telemetry/42.23")
                .setServerEndpoint("http://" + server.getHostName() + ":" + server.getPort());

        final HttpURLConnectionTelemetryClient client = new HttpURLConnectionTelemetryClient();
        assertTrue(client.uploadPing(configuration, TEST_PATH, TEST_PING));

        final RecordedRequest request = server.takeRequest();
        assertEquals(TEST_PATH, request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals(TEST_PING, request.getBody().readUtf8());
        assertEquals("Telemetry/42.23", request.getHeader("User-Agent"));
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"));

        server.shutdown();
    }

    @Test
    public void testMalformedUrl() throws Exception {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application);

        final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
        doThrow(new MalformedURLException()).when(client).openConnectionConnection(anyString(), anyString());

        // If the URL is malformed then there's nothing we can do to recover. Therefore this is treated
        // like a successful upload.
        assertTrue(client.uploadPing(configuration, "path", "ping"));
    }

    @Test
    public void testIOExceptionWhileUpload() throws Exception {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application);

        final OutputStream stream = mock(OutputStream.class);
        doThrow(new IOException()).when(stream).write(any(byte[].class));

        final HttpURLConnection connection = mock(HttpURLConnection.class);
        doReturn(stream).when(connection).getOutputStream();

        final HttpURLConnectionTelemetryClient client = spy(new HttpURLConnectionTelemetryClient());
        doReturn(connection).when(client).openConnectionConnection(anyString(), anyString());

        // And IOException during upload is a failed upload that we should retry. The client should
        // return false in this case.
        assertFalse(client.uploadPing(configuration, "path", "ping"));
    }
}
