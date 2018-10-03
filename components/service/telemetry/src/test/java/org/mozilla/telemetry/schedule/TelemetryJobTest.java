/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.schedule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.TelemetryHolder;
import org.mozilla.telemetry.TestUtils;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.net.TelemetryClient;
import org.mozilla.telemetry.ping.TelemetryCorePingBuilder;
import org.mozilla.telemetry.ping.TelemetryPingBuilder;
import org.mozilla.telemetry.serialize.JSONPingSerializer;
import org.mozilla.telemetry.serialize.TelemetryPingSerializer;
import org.mozilla.telemetry.storage.FileTelemetryStorage;
import org.mozilla.telemetry.storage.TelemetryStorage;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(RobolectricTestRunner.class)
public class TelemetryJobTest {
    @Test
    public void testDailyLimitIsEnforced() throws Exception {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(RuntimeEnvironment.application)
                .setMaximumNumberOfPingUploadsPerDay(2);

        final TelemetryPingSerializer serializer = new JSONPingSerializer();

        final TelemetryStorage storage = new FileTelemetryStorage(configuration, serializer);

        final TelemetryClient client = mock(TelemetryClient.class);
        doReturn(true).when(client).uploadPing(eq(configuration), anyString(), anyString());

        final TelemetryScheduler scheduler = mock(TelemetryScheduler.class);
        final Telemetry telemetry = new Telemetry(configuration, storage, client, scheduler)
                .addPingBuilder(new TelemetryCorePingBuilder(configuration));
        TelemetryHolder.set(telemetry);

        telemetry.queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryCorePingBuilder.TYPE)
                .queuePing(TelemetryCorePingBuilder.TYPE);

        TestUtils.waitForExecutor(telemetry);

        assertEquals(5, storage.countStoredPings(TelemetryCorePingBuilder.TYPE));

        final TelemetryJob job = spy(new TelemetryJob());

        doReturn(1337L).when(job).now();

        uploadPingsInBackground(telemetry, configuration, storage, job);

        // 3 pings are still in the storage
        assertEquals(3, storage.countStoredPings(TelemetryCorePingBuilder.TYPE));
    }

    private void uploadPingsInBackground(
            Telemetry telemetry,
            TelemetryConfiguration configuration,
            TelemetryStorage storage,
            TelemetryJob job
    ) {
        for (TelemetryPingBuilder builder : telemetry.getBuilders()) {
            final String pingType = builder.getType();

            if (storage.countStoredPings(pingType) == 0) {
                continue;
            }

            if (job.hasReachedUploadLimit(configuration, pingType)) {
                continue;
            }

            if (!job.performPingUpload(telemetry, pingType)) {
                return;
            }
        }
    }
}
