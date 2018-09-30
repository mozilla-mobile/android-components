/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.mozilla.telemetry.schedule.jobscheduler.TelemetryJobService;

public class TestUtils {
    /**
     * Wait for the internal executor service to execute all scheduled runnables.
     */
    public static void waitForExecutor(Telemetry telemetry) throws ExecutionException, InterruptedException {
        waitForExecutor(telemetry.getExecutor());
    }

    /**
     * Wait for the internal executor service to execute all scheduled runnables.
     */
    public static void waitForExecutor(TelemetryJobService jobService) throws ExecutionException, InterruptedException {
        waitForExecutor(jobService.getExecutor());
    }

    /**
     * Wait for runnable computation to complete.
     */
    private static void waitForExecutor(ExecutorService executor) throws ExecutionException, InterruptedException {
        executor.submit(new Runnable() {
            @Override
            public void run() {

            }
        }).get();
    }
}
