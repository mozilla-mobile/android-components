/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.ping;

import android.text.TextUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;
import java.util.UUID;

import static mozilla.components.support.test.robolectric.ExtensionsKt.getTestContext;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class TelemetryMobileMetricsPingBuilderTest {

    @Test
    public void testBuildingEmptyPing() {
        final TelemetryConfiguration configuration = new TelemetryConfiguration(getTestContext());
        final TelemetryMobileMetricsPingBuilder builder = new TelemetryMobileMetricsPingBuilder(new JSONObject(), configuration);

        final TelemetryPing ping = builder.build();
        assertNotNull(ping);
        assertUUID(ping.getDocumentId());
        assertFalse(TextUtils.isEmpty(ping.getUploadPath()));
        assertEquals("mobile-metrics", ping.getType());

        final Map<String, Object> results = ping.getMeasurementResults();
        assertNotNull(results);

        assertTrue(results.containsKey("seq"));
        assertEquals(1L, results.get("seq"));

        assertTrue(results.containsKey("locale"));
        assertEquals("en-US", results.get("locale"));

        assertTrue(results.containsKey("os"));
        assertEquals("Android", results.get("os"));

        assertTrue(results.containsKey("osversion"));
        assertEquals("28", results.get("osversion"));

        assertTrue(results.containsKey("device"));
        assertFalse(TextUtils.isEmpty((String) results.get("device")));

        assertTrue(results.containsKey("createdDate"));

        assertTrue(results.containsKey("createdTimestamp"));

        assertTrue(results.containsKey("tz"));

        assertTrue(results.containsKey("arch")); 

        assertTrue(results.containsKey("profileDate"));

        assertTrue(results.containsKey("metrics"));
    }

    private void assertUUID(String uuid) {
        //noinspection ResultOfMethodCallIgnored
        UUID.fromString(uuid); // Should throw IllegalArgumentException if parameter does not conform
    }
}