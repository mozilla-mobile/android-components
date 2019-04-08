/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.os.SystemClock

/**
 * A helper class for recording timespans for the [TimespanMetricType] and
 * [TimingDistributionMetricType].
 *
 * To get an instance of a [Timespan] class, use [TimespanMetricType.start] or
 * [TimingDistributionMetricType.start].  Once you have one, when you are
 * done timing, call [Timespan.stop].  This will record the result to the
 * appropriate metric.
 *
 * @param recorder A function to record the resulting time, in nanoseconds
 * @param recordError A function to call to record an error, currently if stop is called twice
 */
class Timespan internal constructor(
    private val recorder: ((Long) -> Unit),
    private val recordError: (() -> Unit)
) {
    private var startTime: Long? = getElapsedNanos()

    companion object {
        /**
         * Helper function used for getting the elapsed time, since the process
         * started, using a monotonic clock.
         * We need to have this as an helper so that we can override it in tests.
         *
         * @return the time, in nanoseconds, since the process started.
         */
        internal var getElapsedNanos = { SystemClock.elapsedRealtimeNanos() }
    }

    /**
     * Call to stop the timespan and record in the appropriate metric.
     */
    fun stop() {
        startTime?.let {
            val endTime = getElapsedNanos()
            val time = endTime - it
            recorder(time)
            startTime = null
        } ?: run {
            recordError()
        }
    }
}
