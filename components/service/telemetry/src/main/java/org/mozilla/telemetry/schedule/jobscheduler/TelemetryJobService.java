/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.schedule.jobscheduler;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;

import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.TelemetryHolder;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.ping.TelemetryPingBuilder;
import org.mozilla.telemetry.schedule.TelemetryJob;
import org.mozilla.telemetry.storage.TelemetryStorage;

import androidx.annotation.VisibleForTesting;
import mozilla.components.support.base.log.logger.Logger;

public class TelemetryJobService extends JobService {
    private final Logger logger = new Logger("telemetry/service");
    @VisibleForTesting TelemetryJob job = new TelemetryJob();
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

    @VisibleForTesting
    public void uploadPingsInBackground(AsyncTask task, JobParameters parameters) {
        final Telemetry telemetry = TelemetryHolder.get();
        final TelemetryConfiguration configuration = telemetry.getConfiguration();
        final TelemetryStorage storage = telemetry.getStorage();

        for (TelemetryPingBuilder builder : telemetry.getBuilders()) {
            final String pingType = builder.getType();
            logger.debug("Performing upload of ping type: " + pingType, null);

            if (task.isCancelled()) {
                logger.debug("Job stopped. Exiting.", null);
                return; // Job will be rescheduled from onStopJob().
            }

            if (storage.countStoredPings(pingType) == 0) {
                logger.debug("No pings of type " + pingType + " to upload", null);
                continue;
            }

            if (job.hasReachedUploadLimit(configuration, pingType)) {
                logger.debug("Daily upload limit for type " + pingType + " reached", null);
                continue;
            }

            if (!job.performPingUpload(telemetry, pingType)) {
                logger.info("Upload aborted. Rescheduling job if limit not reached.", null);
                jobFinished(parameters, !job.hasReachedUploadLimit(configuration, pingType));
                return;
            }
        }

        logger.debug("All uploads performed", null);
        jobFinished(parameters, false);
    }
}
