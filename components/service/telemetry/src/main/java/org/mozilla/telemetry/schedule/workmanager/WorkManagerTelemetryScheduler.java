/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.schedule.workmanager;

import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.schedule.TelemetryScheduler;

import java.util.concurrent.TimeUnit;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/**
 * TelemetryScheduler implementation that uses Android's WorkManager API to schedule ping uploads.
 */
public class WorkManagerTelemetryScheduler implements TelemetryScheduler {
    @Override
    public void scheduleUpload(TelemetryConfiguration configuration) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(TelemetryWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, configuration.getInitialBackoffForUpload(), TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance().enqueue(workRequest);
    }
}