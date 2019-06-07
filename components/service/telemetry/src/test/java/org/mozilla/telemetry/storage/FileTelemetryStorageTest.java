/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.storage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder;
import org.mozilla.telemetry.ping.TelemetryPing;
import org.mozilla.telemetry.serialize.JSONPingSerializer;
import org.mozilla.telemetry.serialize.TelemetryPingSerializer;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.UUID;

import static mozilla.components.support.test.robolectric.ExtensionsKt.getTestContext;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class FileTelemetryStorageTest {
    private static final String TEST_PING_TYPE = "test";
    private static final String TEST_UPLOAD_PATH = "/some/random/path/upload";
    private static final String TEST_SERIALIZED_PING = "Hello Test";

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testStoringAndReading() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext());

        final String documentId = UUID.randomUUID().toString();

        final TelemetryPing ping = mock(TelemetryPing.class);
        doReturn(TEST_PING_TYPE).when(ping).getType();
        doReturn(documentId).when(ping).getDocumentId();
        doReturn(TEST_UPLOAD_PATH).when(ping).getUploadPath();

        final TelemetryPingSerializer serializer = mock(TelemetryPingSerializer.class);
        doReturn(TEST_SERIALIZED_PING).when(serializer).serialize(ping);

        final FileTelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);
        storage.store(ping);

        TelemetryStorage.TelemetryStorageCallback callback = spy((path, serializedPing) -> {
            assertEquals(TEST_UPLOAD_PATH, path);
            assertEquals(TEST_SERIALIZED_PING, serializedPing);
            return true;
        });

        final boolean processed = storage.process(TEST_PING_TYPE, callback);

        assertTrue(processed);

        verify(callback).onTelemetryPingLoaded(TEST_UPLOAD_PATH, TEST_SERIALIZED_PING);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testReturningFalseInCallbackReturnsFalseFromProcess() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext());

        final String documentId = UUID.randomUUID().toString();

        final TelemetryPing ping = mock(TelemetryPing.class);
        doReturn(TEST_PING_TYPE).when(ping).getType();
        doReturn(documentId).when(ping).getDocumentId();
        doReturn(TEST_UPLOAD_PATH).when(ping).getUploadPath();

        final TelemetryPingSerializer serializer = mock(TelemetryPingSerializer.class);
        doReturn(TEST_SERIALIZED_PING).when(serializer).serialize(ping);

        final FileTelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);
        storage.store(ping);

        TelemetryStorage.TelemetryStorageCallback callback = spy((path, serializedPing) -> {
            assertEquals(TEST_UPLOAD_PATH, path);
            assertEquals(TEST_SERIALIZED_PING, serializedPing);
            return false;
        });

        final boolean processed = storage.process(TEST_PING_TYPE, callback);

        assertFalse(processed);

        verify(callback).onTelemetryPingLoaded(TEST_UPLOAD_PATH, TEST_SERIALIZED_PING);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testPingIsRemovedAfterProcessing() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext());

        final String documentId = UUID.randomUUID().toString();

        final TelemetryPing ping = mock(TelemetryPing.class);
        doReturn(TEST_PING_TYPE).when(ping).getType();
        doReturn(documentId).when(ping).getDocumentId();
        doReturn(TEST_UPLOAD_PATH).when(ping).getUploadPath();

        final TelemetryPingSerializer serializer = mock(TelemetryPingSerializer.class);
        doReturn(TEST_SERIALIZED_PING).when(serializer).serialize(ping);

        final FileTelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);
        storage.store(ping);

        TelemetryStorage.TelemetryStorageCallback callback = spy((path, serializedPing) -> {
            assertEquals(TEST_UPLOAD_PATH, path);
            assertEquals(TEST_SERIALIZED_PING, serializedPing);
            return true;
        });

        final boolean processed = storage.process(TEST_PING_TYPE, callback);

        assertTrue(processed);

        verify(callback).onTelemetryPingLoaded(TEST_UPLOAD_PATH, TEST_SERIALIZED_PING);

        TelemetryStorage.TelemetryStorageCallback callback2 = spy((path, serializedPing) -> true);

        final boolean processed2 = storage.process(TEST_PING_TYPE, callback2);

        assertTrue(processed2);

        verify(callback2, never()).onTelemetryPingLoaded(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testPingIsNotRemovedAfterUnsuccessfulProcessing() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext());

        final String documentId = UUID.randomUUID().toString();

        final TelemetryPing ping = mock(TelemetryPing.class);
        doReturn(TEST_PING_TYPE).when(ping).getType();
        doReturn(documentId).when(ping).getDocumentId();
        doReturn(TEST_UPLOAD_PATH).when(ping).getUploadPath();

        final TelemetryPingSerializer serializer = mock(TelemetryPingSerializer.class);
        doReturn(TEST_SERIALIZED_PING).when(serializer).serialize(ping);

        final FileTelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);
        storage.store(ping);

        TelemetryStorage.TelemetryStorageCallback callback = spy((path, serializedPing) -> {
            assertEquals(TEST_UPLOAD_PATH, path);
            assertEquals(TEST_SERIALIZED_PING, serializedPing);
            return false;
        });

        final boolean processed = storage.process(TEST_PING_TYPE, callback);

        assertFalse(processed);

        verify(callback).onTelemetryPingLoaded(TEST_UPLOAD_PATH, TEST_SERIALIZED_PING);

        TelemetryStorage.TelemetryStorageCallback callback2 = spy((path, serializedPing) -> true);

        final boolean processed2 = storage.process(TEST_PING_TYPE, callback2);

        assertTrue(processed2);

        verify(callback2).onTelemetryPingLoaded(TEST_UPLOAD_PATH, TEST_SERIALIZED_PING);
    }

    @Test
    public void testPingsAreRemovedIfLimitsIsReached() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext())
                .setMaximumNumberOfPingsPerType(2);

        final TelemetryPingSerializer serializer = new JSONPingSerializer();

        final FileTelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);

        final TelemetryCorePingBuilder builder = new TelemetryCorePingBuilder(configuration);

        storage.store(builder.build());
        storage.store(builder.build());
        storage.store(builder.build());
        storage.store(builder.build());

        // Only two should actually be stored.
        assertEquals(2, storage.countStoredPings(TelemetryCorePingBuilder.TYPE));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testOldestFilesAreRemovedFirst() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext())
                .setMaximumNumberOfPingsPerType(2);

        final TelemetryPingSerializer serializer = new JSONPingSerializer();

        final File file1 = mock(File.class);
        doReturn(10L).when(file1).lastModified();

        final File file2 = mock(File.class);
        doReturn(5L).when(file2).lastModified();

        final File file3 = mock(File.class);
        doReturn(20L).when(file3).lastModified();

        final FileTelemetryStorage storage = spy(new FileTelemetryStorage(configuration, serializer));
        doReturn(new File[] { file1, file2, file3 } ).when(storage).listPingFiles(anyString());

        final TelemetryCorePingBuilder builder = new TelemetryCorePingBuilder(configuration);
        storage.store(builder.build());

        verify(file1, never()).delete();
        verify(file2).delete();
        verify(file3, never()).delete();
    }
}