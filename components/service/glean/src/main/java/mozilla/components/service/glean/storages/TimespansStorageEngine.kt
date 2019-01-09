/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.storages

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.SystemClock
import mozilla.components.service.glean.CommonMetricData
import mozilla.components.service.glean.TimeUnit

import mozilla.components.support.base.log.logger.Logger
import org.json.JSONArray
import java.util.concurrent.TimeUnit as AndroidTimeUnit

/**
 * This singleton handles the in-memory storage logic for timespans. It is meant to be used by
 * the Specific Timespan API and the ping assembling objects. No validation on the stored data
 * is performed at this point: validation must be performed by the Specific Timespan API.
 *
 * This class contains a reference to the Android application Context. While the IDE warns
 * us that this could leak, the application context lives as long as the application and this
 * object. For this reason, we should be safe to suppress the IDE warning.
 */
@SuppressLint("StaticFieldLeak")
internal object TimespansStorageEngine : TimespansStorageEngineImplementation()

internal open class TimespansStorageEngineImplementation(
    override val logger: Logger = Logger("glean/TimespansStorageEngine")
) : GenericScalarStorageEngine<Long>() {

    /**
     * A map that stores the start times from the API consumers, not yet
     * committed to any store (i.e. [start] was called but no [stopAndSum] yet).
     */
    private val uncommittedStartTimes = mutableMapOf<String, Long>()

    /**
     * An internal map to keep track of the desired time units for the recorded timespans.
     * We need this in order to get a snapshot of the data, with the right time unit,
     * later on.
     */
    private val timeUnitsMap = mutableMapOf<String, TimeUnit>()

    override fun deserializeSingleMetric(metricName: String, value: Any?): Long? {
        val jsonArray = (value as? String)?.let {
            return@let try {
                JSONArray(it)
            } catch (e: org.json.JSONException) {
                null
            }
        }

        // In order to perform timeunit conversion when taking a snapshot, we persisted
        // the desired time unit together with the raw values. We unpersist the first element
        // in the array as the time unit, the second as the raw Long value.
        if (jsonArray == null || jsonArray.length() != 2) {
            // TODO report errors through other special metrics handled by the SDK. See bug 1499761.
            logger.error("Unexpected format found when deserializing $metricName")
            return null
        }

        return try {
            val timeUnit = jsonArray.getInt(0)
            val rawValue = jsonArray.getLong(1)
            // If nothing threw, make sure our time unit is within the enum's range
            // and finally set/return the values.
            TimeUnit.values().getOrNull(timeUnit)?.let {
                timeUnitsMap[metricName] = it
                rawValue
            }
        } catch (e: org.json.JSONException) {
            null
        }
    }

    override fun serializeSingleMetric(
        userPreferences: SharedPreferences.Editor?,
        storeName: String,
        value: Long,
        extraSerializationData: Any?
    ) {
        // To support converting to the desired time unit when taking a snapshot, we need a way
        // to know the time unit for timespans that are loaded off the disk, for user lifetime.
        // To do that, instead of simply persisting a Long, we instead persist a JSONArray. The
        // first item in this array is the the time unit, the second is the long value.

        // We expect to have received the time unit as extraSerializationData. There's
        // no point in persisting if we didn't.
        if (extraSerializationData == null ||
            extraSerializationData !is TimeUnit) {
            logger.error("Unexpected or missing extra data for time unit serialization")
            return
        }

        val tuple = JSONArray()
        tuple.put(extraSerializationData.ordinal)
        tuple.put(value)
        userPreferences?.putString(storeName, tuple.toString())
    }

    /**
     * Helper function used for getting the elapsed time, since the process
     * started, using a monotonic clock.
     * We need to have this as an helper so that we can override it in tests.
     *
     * @return the time, in nanoseconds, since the process started.
     */
    internal fun getElapsedNanos(): Long = SystemClock.elapsedRealtimeNanos()

    /**
     * Convenience method to get a time in a different, supported time unit.
     *
     * @param timeUnit the required time unit, one in [TimeUnit]
     * @param elapsedNanos a time in nanoseconds
     *
     * @return the time in the desired time unit
     */
    private fun getAdjustedTime(timeUnit: TimeUnit, elapsedNanos: Long): Long {
        return when (timeUnit) {
            TimeUnit.Nanosecond -> elapsedNanos
            TimeUnit.Microsecond -> AndroidTimeUnit.NANOSECONDS.toMicros(elapsedNanos)
            TimeUnit.Millisecond -> AndroidTimeUnit.NANOSECONDS.toMillis(elapsedNanos)
            TimeUnit.Second -> AndroidTimeUnit.NANOSECONDS.toSeconds(elapsedNanos)
            TimeUnit.Minute -> AndroidTimeUnit.NANOSECONDS.toMinutes(elapsedNanos)
            TimeUnit.Hour -> AndroidTimeUnit.NANOSECONDS.toHours(elapsedNanos)
            TimeUnit.Day -> AndroidTimeUnit.NANOSECONDS.toDays(elapsedNanos)
        }
    }

    /**
     * Start tracking time for the provided metric. This records an error if it’s
     * already tracking time (i.e. start was already called with no corresponding
     * [stopAndSum]): in that case the original start time will be preserved.
     *
     * @param metricData the metric information for the timespan
     */
    fun start(metricData: CommonMetricData) {
        val timespanName = getStoredName(metricData)

        if (timespanName in uncommittedStartTimes) {
            // TODO report errors if already tracking time through internal metrics. See bug 1499761.
            logger.error("$timespanName already started")
            return
        }

        synchronized(this) {
            uncommittedStartTimes[timespanName] = getElapsedNanos()
        }
    }

    /**
     * Stop tracking time for the provided metric. Add the elapsed time to the time currently
     * stored in the metric. This will record an error if no [start] was called.
     *
     * @param metricData the metric information for the timespan
     * @param timeUnit the time unit we want the data in when snapshotting
     */
    @Synchronized
    fun stopAndSum(
        metricData: CommonMetricData,
        timeUnit: TimeUnit
    ) {
        // TODO report errors if not tracking time through internal metrics. See bug 1499761.
        // Look for the start time: if it's there, commit the timespan.
        val timespanName = getStoredName(metricData)
        uncommittedStartTimes.remove(timespanName)?.let { startTime ->
            val elapsedNanos = getElapsedNanos() - startTime

            // Store the time unit: we'll need it when snapshotting.
            timeUnitsMap[timespanName] = timeUnit

            // Use a custom combiner to sum the new timespan to the one already stored. We
            // can't adjust the time unit before storing so that we still allow for values
            // lower than the desired time unit to accumulate.
            super.recordScalar(metricData, elapsedNanos, timeUnit) { currentValue, newValue ->
                currentValue?.let {
                    it + newValue
                } ?: newValue
            }
        }
    }

    /**
     * Abort a previous [start] call. No error is recorded if no [start] was called.
     *
     * @param metricData the metric information for the timespan
     */
    @Synchronized
    fun cancel(metricData: CommonMetricData) {
        uncommittedStartTimes.remove(getStoredName(metricData))
    }

    /**
     * Get a snapshot of the stored timespans and adjust it to the desired time units.
     *
     * @param storeName the name of the desired store
     * @param clearStore whether or not to clear the requested store. Not that only
     *        metrics stored with a lifetime of [Lifetime.Ping] will be cleared.
     *
     * @return the [Long] recorded in the requested store
     */
    @Synchronized
    override fun getSnapshot(storeName: String, clearStore: Boolean): GenericDataStorage<Long>? {
        val adjustedData = super.getSnapshot(storeName, clearStore)?.mapValues {
            // Convert to the expected time unit.
            if (it.key !in timeUnitsMap) {
                logger.error("Can't find the time unit for ${it.key}. Reporting raw value.")
            }

            timeUnitsMap[it.key]?.let { timeUnit ->
                getAdjustedTime(timeUnit, it.value)
            } ?: it.value
        }

        // Clear the time unit map if needed: we need to check all the stores
        // for all the lifetimes.
        if (clearStore) {
            // Get a list of the metrics that are still stored. We'll drop the time units for all the
            // metrics that are not in this set.
            val unclearedMetricNames =
                dataStores.flatMap { lifetime -> lifetime.entries }.flatMap { it -> it.value.keys }.toSet()

            timeUnitsMap.keys.retainAll { it in unclearedMetricNames }
        }

        return adjustedData?.toMutableMap()
    }

    /**
     * Test-only method used to clear the timespans stores.
     */
    override fun clearAllStores() {
        super.clearAllStores()
        timeUnitsMap.clear()
    }
}
