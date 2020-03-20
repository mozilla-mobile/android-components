/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.testing.WorkManagerTestInitHelper;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import mozilla.components.service.glean.config.Configuration;

@RunWith(RobolectricTestRunner.class)
public class GleanFromJavaTest {
    // The only purpose of these tests is to make sure the Glean API is
    // callable from Java. If something goes wrong, it should complain about missing
    // methods at build-time.

    @Test
    public void testInitGleanWithDefaults() {
        Context context = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context);
        Glean.INSTANCE.initialize(context, true);
    }

    @Test
    public void testInitGleanWithConfiguration() {
        Context context = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context);
        Configuration config =
                new Configuration(Configuration.DEFAULT_TELEMETRY_ENDPOINT, "test-channel");
        Glean.INSTANCE.initialize(context, true, config);
    }

    @Test
    public void testGleanExperimentsAPIWithDefaults() {
        Glean.INSTANCE.setExperimentActive("test-exp-id-1", "test-branch-1");
    }

    @Test
    public void testGleanExperimentsAPIWithOptional() {
        Map<String, String> experimentProperties = new HashMap<>();
        experimentProperties.put("test-prop1", "test-prop-result1");

        Glean.INSTANCE.setExperimentActive(
                "test-exp-id-1",
                "test-branch-1",
                experimentProperties
        );
    }
}
