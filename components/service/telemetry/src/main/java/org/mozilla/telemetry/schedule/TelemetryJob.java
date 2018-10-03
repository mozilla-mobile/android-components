/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.schedule;

import android.content.SharedPreferences;

import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.net.TelemetryClient;
import org.mozilla.telemetry.storage.TelemetryStorage;

import java.util.Calendar;

import androidx.annotation.VisibleForTesting;

public class TelemetryJob {
    private static final String PREFERENCE_UPLOAD_COUNT_PREFIX = "upload_count_";
    private static final String PREFERENCE_LAST_UPLOAD_PREFIX = "last_uploade_";

    /**
     * Increment the upload counter for this ping type.
     */
    private boolean incrementUploadCount(TelemetryConfiguration configuration, String pingType) {
        final SharedPreferences preferences = configuration.getSharedPreferences();

        final long lastUpload = preferences.getLong(PREFERENCE_LAST_UPLOAD_PREFIX + pingType, 0);
        final long now = now();

        final long count = isSameDay(lastUpload, now)
                ? preferences.getLong(PREFERENCE_UPLOAD_COUNT_PREFIX + pingType, 0) + 1
                : 1;

        preferences.edit()
                .putLong(PREFERENCE_LAST_UPLOAD_PREFIX + pingType, now)
                .putLong(PREFERENCE_UPLOAD_COUNT_PREFIX + pingType, count)
                .apply();

        return true;
    }

    /**
     * Return true if the upload limit for this ping type has been reached.
     */
    public boolean hasReachedUploadLimit(TelemetryConfiguration configuration, String pingType) {
        final SharedPreferences preferences = configuration.getSharedPreferences();

        final long lastUpload = preferences.getLong(PREFERENCE_LAST_UPLOAD_PREFIX + pingType, 0);
        final long count = preferences.getLong(PREFERENCE_UPLOAD_COUNT_PREFIX + pingType, 0);

        return isSameDay(lastUpload, now())
                && count >= configuration.getMaximumNumberOfPingUploadsPerDay();
    }

    @VisibleForTesting boolean isSameDay(long timestamp1, long timestamp2) {
        final Calendar calendar1 = Calendar.getInstance();
        calendar1.setTimeInMillis(timestamp1);

        final Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(timestamp2);

        return (calendar1.get(Calendar.ERA) == calendar2.get(Calendar.ERA) &&
                calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR));
    }

    @VisibleForTesting public long now() {
        return System.currentTimeMillis();
    }

    public boolean performPingUpload(Telemetry telemetry, final String pingType) {
        final TelemetryConfiguration configuration = telemetry.getConfiguration();
        final TelemetryStorage storage = telemetry.getStorage();
        final TelemetryClient client = telemetry.getClient();

        return storage.process(pingType, new TelemetryStorage.TelemetryStorageCallback() {
            @Override
            public boolean onTelemetryPingLoaded(String path, String serializedPing) {
                return !hasReachedUploadLimit(configuration, pingType)
                        && client.uploadPing(configuration, path, serializedPing)
                        && incrementUploadCount(configuration, pingType);
            }
        });
    }
}
