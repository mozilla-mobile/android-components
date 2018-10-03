/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.schedule.workmanager;

import android.content.Context;

import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.TelemetryHolder;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.ping.TelemetryPingBuilder;
import org.mozilla.telemetry.schedule.TelemetryJob;
import org.mozilla.telemetry.storage.TelemetryStorage;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import mozilla.components.support.base.log.logger.Logger;

public class TelemetryWorker extends Worker {
    private final Logger logger = new Logger("telemetry/service");
    private TelemetryJob job;

    public TelemetryWorker(Context context, WorkerParameters parameters) {
        this(new TelemetryJob(), context, parameters);
    }

    TelemetryWorker(TelemetryJob job, Context context, WorkerParameters parameters) {
        super(context, parameters);
        this.job = job;
    }

    @NonNull
    @Override
    public Result doWork() {
        return uploadPingsInBackground();
    }

    @VisibleForTesting
    public Result uploadPingsInBackground() {
        final Telemetry telemetry = TelemetryHolder.get();
        final TelemetryConfiguration configuration = telemetry.getConfiguration();
        final TelemetryStorage storage = telemetry.getStorage();

        for (TelemetryPingBuilder builder : telemetry.getBuilders()) {
            final String pingType = builder.getType();
            logger.debug("Performing upload of ping type: " + pingType, null);

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
                return job.hasReachedUploadLimit(configuration, pingType) ? Result.FAILURE : Result.RETRY;
            }
        }

        logger.debug("All uploads performed", null);
        return Result.SUCCESS;
    }
}
