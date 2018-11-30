/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.storages

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import mozilla.components.service.glean.Lifetime
import mozilla.components.service.glean.CommonMetricData
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject

/**
 * Defines an alias for a generic data storage to be used by
 * [GenericScalarStorageEngine]. This maps a metric name to
 * its data.
 */
internal typealias GenericDataStorage<T> = MutableMap<String, T>

/**
 * Defines an alias for a generic storage map to be used by
 * [GenericScalarStorageEngine]. This maps a store name to
 * the [GenericDataStorage] it holds.
 */
internal typealias GenericStorageMap<T> = MutableMap<String, GenericDataStorage<T>>

/**
 * Defines the typealias for the combiner function to be used by
 * [GenericScalarStorageEngine.recordScalar] when recording new values.
 */
internal typealias ScalarRecordingCombiner<T> = (currentValue: T?, newValue: T) -> T

/**
 * A base class for 'scalar' like metrics. This allows sharing the common
 * store managing and lifetime behaviours.
 */
abstract class GenericScalarStorageEngine<ScalarType> : StorageEngine {
    override lateinit var applicationContext: Context

    // Let derived class define a logger so that they can provide a proper name,
    // useful when debugging weird behaviours.
    abstract val logger: Logger

    protected val userLifetimeStorage: SharedPreferences by lazy { deserializeUserLifetime() }

    // Store a map for each lifetime as an array element:
    // Array[Lifetime] = Map[StorageName, ScalarType].
    protected val dataStores: Array<GenericStorageMap<ScalarType>> =
        Array(Lifetime.values().size) { mutableMapOf<String, GenericDataStorage<ScalarType>>() }

    /**
     * Implementor's provided function to convert deserialized 'user' lifetime
     * data to the destination ScalarType.
     *
     * @param value loaded from the storage as [Any]
     *
     * @return data as [ScalarType] or null if deserialization failed
     */
    protected abstract fun deserializeSingleMetric(value: Any?): ScalarType?

    /**
     * Implementor's provided function to serialized 'user' lifetime
     * data to [String].
     *
     * @param value loaded from the storage as [ScalarType]
     *
     * @return data as [String] or null if serialization failed
     */
    protected fun serializeSingleMetric(value: ScalarType): String? {
        return value.toString()
    }

    /**
     * Helper function to return the name of the stored metric.
     * This is used to support empty categories.
     */
    protected fun getStoredName(metricData: CommonMetricData): String {
        with(metricData) {
            return if (category.isEmpty()) {
                name
            } else {
                "$category.$name"
            }
        }
    }

    /**
     * Deserialize the metrics with a lifetime = User that are on disk.
     * This will be called the first time a metric is used or before a snapshot is
     * taken.
     *
     * @return A [SharedPreferences] reference that will be used to inititialize [userLifetimeStorage]
     */
    @Suppress("TooGenericExceptionCaught")
    open fun deserializeUserLifetime(): SharedPreferences {
        val prefs =
            applicationContext.getSharedPreferences(this.javaClass.simpleName, Context.MODE_PRIVATE)

        val metrics = try {
            prefs.all.entries
        } catch (e: NullPointerException) {
            // If we fail to deserialize, we can log the problem but keep on going.
            logger.error("Failed to deserialize metric with 'user' lifetime")
            return prefs
        }

        for ((metricStoragePath, metricValue) in metrics) {
            if (!metricStoragePath.contains('#')) {
                continue
            }

            // Split the stored name in 2: we expect it to be in the format
            // store#metric.name
            val (storeName, metricName) =
                metricStoragePath.split('#', limit = 2)
            if (storeName.isEmpty()) {
                continue
            }

            val storeData = dataStores[Lifetime.User.ordinal].getOrPut(storeName) { mutableMapOf() }
            // Only set the stored value if we're able to deserialize the persisted data.
            deserializeSingleMetric(metricValue)?.let {
                storeData[metricName] = it
            } ?: logger.warn("Failed to deserialize $metricStoragePath")
        }

        return prefs
    }

    /**
     * Retrieves the [recorded metric data][ScalarType] for the provided
     * store name.
     *
     * Please note that the [Lifetime.Application] lifetime is handled implicitly
     * by never clearing its data. It will naturally clear out when restarting the
     * application.
     *
     * @param storeName the name of the desired store
     * @param clearStore whether or not to clear the requested store. Not that only
     *        metrics stored with a lifetime of [Lifetime.Ping] will be cleared.
     *
     * @return the [ScalarType] recorded in the requested store
     */
    @Synchronized
    fun getSnapshot(storeName: String, clearStore: Boolean): GenericDataStorage<ScalarType>? {
        val allLifetimes: GenericDataStorage<ScalarType> = mutableMapOf()

        // Make sure data with "user" lifetime is loaded before getting the snapshot.
        // We still need to catch exceptions here, as `getAll()` might throw.
        @Suppress("TooGenericExceptionCaught")
        try {
            userLifetimeStorage.all
        } catch (e: NullPointerException) {
            // Intentionally left blank. We just want to fall through.
        }

        // Get the metrics for all the supported lifetimes.
        for (store in dataStores) {
            store[storeName]?.let {
                allLifetimes.putAll(it)
            }
        }

        if (clearStore) {
            // We only allow clearing metrics with the "ping" lifetime.
            dataStores[Lifetime.Ping.ordinal].remove(storeName)
        }

        return if (allLifetimes.isNotEmpty()) allLifetimes else null
    }

    /**
     * Get a snapshot of the stored data as a JSON object.
     *
     * @param storeName the name of the desired store
     * @param clearStore whether or not to clearStore the requested store
     *
     * @return the [JSONObject] containing the recorded data.
     */
    override fun getSnapshotAsJSON(storeName: String, clearStore: Boolean): Any? {
        return getSnapshot(storeName, clearStore)?.let { dataMap ->
            return JSONObject(dataMap)
        }
    }

    /**
     * Helper function for the derived classes. It can be used to record
     * simple scalars to the internal storage. Internally, this calls [recordScalar]
     * with a custom `combine` function that only sets the new value.
     *
     * @param metricData the information about the metric
     * @param value the new value
     */
    protected fun recordScalar(
        metricData: CommonMetricData,
        value: ScalarType
    ) = recordScalar(metricData, value) { _, v -> v }

    /**
     * Helper function for the derived classes. It can be used to record
     * simple scalars to the internal storage.
     *
     * @param metricData the information about the metric
     * @param value the new value
     * @param combine a lambda function to combine the currently stored value and
     *        the new one; this allows to implement new behaviours such as adding.
     */
    protected fun recordScalar(
        metricData: CommonMetricData,
        value: ScalarType,
        combine: ScalarRecordingCombiner<ScalarType>
    ) {
        checkNotNull(applicationContext) { "No recording can take place without an application context" }

        // Record a copy of the metric in all the needed stores.
        @SuppressLint("CommitPrefEdits")
        val userPrefs: SharedPreferences.Editor? =
            if (metricData.lifetime == Lifetime.User) userLifetimeStorage.edit() else null
        metricData.getStorageNames().forEach {
            val storeData = dataStores[metricData.lifetime.ordinal].getOrPut(it) { mutableMapOf() }
            // We support empty categories for enabling the internal use of metrics
            // when assembling pings in [PingMaker].
            val entryName = getStoredName(metricData)
            val combinedValue = combine(storeData[entryName], value)
            storeData[entryName] = combinedValue
            // Persist data with "user" lifetime
            if (metricData.lifetime == Lifetime.User) {
                userPrefs?.putString("$it#$entryName", serializeSingleMetric(combinedValue))
            }
        }
        userPrefs?.apply()
    }

    internal open fun clearAllStores() = dataStores.forEach { it.clear() }
}
