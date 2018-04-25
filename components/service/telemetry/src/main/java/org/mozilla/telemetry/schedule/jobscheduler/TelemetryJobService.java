/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.schedule.jobscheduler;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.TelemetryHolder;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.net.TelemetryClient;
import org.mozilla.telemetry.ping.TelemetryPingBuilder;
import org.mozilla.telemetry.storage.TelemetryStorage;

import java.util.Calendar;

public class TelemetryJobService extends JobService {
    private static final String LOG_TAG = "TelemetryJobService";

    private static final String PREFERENCE_UPLOAD_COUNT_PREFIX = "upload_count_";
    private static final String PREFERENCE_LAST_UPLOAD_PREFIX = "last_uploade_";

    private UploadPingsTask uploadTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        uploadTask = new UploadPingsTask();
        uploadTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (uploadTask != null) {
            uploadTask.cancel(true);
        }
        return true;
    }

    @SuppressLint("StaticFieldLeak") // This needs to be fixed (#111)
    private class UploadPingsTask extends AsyncTask<JobParameters, Void, Void> {
        @Override
        protected Void doInBackground(JobParameters... params) {
            final JobParameters parameters = params[0];
            uploadPingsInBackground(this, parameters);
            return null;
        }
    }

    @VisibleForTesting public void uploadPingsInBackground(AsyncTask task, JobParameters parameters) {
        final Telemetry telemetry = TelemetryHolder.get();
        final TelemetryConfiguration configuration = telemetry.getConfiguration();
        final TelemetryStorage storage = telemetry.getStorage();

        for (TelemetryPingBuilder builder : telemetry.getBuilders()) {
            final String pingType = builder.getType();
            Log.d(LOG_TAG, "Performing upload of ping type: " + pingType);

            if (task.isCancelled()) {
                Log.d(LOG_TAG, "Job stopped. Exiting.");
                return; // Job will be rescheduled from onStopJob().
            }

            if (storage.countStoredPings(pingType) == 0) {
                Log.d(LOG_TAG, "No pings of type " + pingType + " to upload");
                continue;
            }

            if (hasReachedUploadLimit(configuration, pingType)) {
                Log.d(LOG_TAG, "Daily upload limit for type " + pingType + " reached");
                continue;
            }

            if (!performPingUpload(telemetry, pingType)) {
                Log.i(LOG_TAG, "Upload aborted. Rescheduling job if limit not reached.");
                jobFinished(parameters, !hasReachedUploadLimit(configuration, pingType));
                return;
            }
        }

        Log.d(LOG_TAG, "All uploads performed");
        jobFinished(parameters, false);
    }

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
    private boolean hasReachedUploadLimit(TelemetryConfiguration configuration, String pingType) {
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

    @VisibleForTesting long now() {
        return System.currentTimeMillis();
    }

    private boolean performPingUpload(Telemetry telemetry, final String pingType) {
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
