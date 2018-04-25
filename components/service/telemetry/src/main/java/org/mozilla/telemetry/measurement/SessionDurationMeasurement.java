/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.telemetry.measurement;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import org.mozilla.telemetry.config.TelemetryConfiguration;

import java.util.concurrent.TimeUnit;

public class SessionDurationMeasurement extends TelemetryMeasurement {
    private static final String FIELD_NAME = "durations";

    private static final String PREFERENCE_DURATION = "session_duration";

    private final TelemetryConfiguration configuration;

    private boolean sessionStarted = false;
    private long timeAtSessionStartNano = -1;

    public SessionDurationMeasurement(TelemetryConfiguration configuration) {
        super(FIELD_NAME);

        this.configuration = configuration;
    }

    public synchronized void recordSessionStart() {
        if (sessionStarted) {
            throw new IllegalStateException("Trying to start session but it is already started");
        }

        sessionStarted = true;
        timeAtSessionStartNano = getSystemTimeNano();
    }

    public synchronized void recordSessionEnd() {
        if (!sessionStarted) {
            throw new IllegalStateException("Expected session to be started before session end is called");
        }

        sessionStarted = false;

        final long sessionElapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(getSystemTimeNano() - timeAtSessionStartNano);

        final SharedPreferences preferences = configuration.getSharedPreferences();

        final long totalElapsedSeconds = preferences.getLong(PREFERENCE_DURATION, 0);

        preferences.edit()
                .putLong(PREFERENCE_DURATION, totalElapsedSeconds + sessionElapsedSeconds)
                .apply();
    }

    @Override
    public Object flush() {
        return getAndResetDuration();
    }

    private synchronized long getAndResetDuration() {
        final SharedPreferences preferences = configuration.getSharedPreferences();

        long duration = preferences.getLong(PREFERENCE_DURATION, 0);

        preferences.edit()
                .putLong(PREFERENCE_DURATION, 0)
                .apply();

        return duration;
    }

    /**
     * Returns (roughly) the system uptime in nanoseconds.
     */
    @VisibleForTesting long getSystemTimeNano() {
        return System.nanoTime();
    }
}
