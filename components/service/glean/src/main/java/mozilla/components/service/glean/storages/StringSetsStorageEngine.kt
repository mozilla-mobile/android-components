/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.storages

import android.annotation.SuppressLint
import android.content.SharedPreferences
import mozilla.components.service.glean.error.ErrorRecording.ErrorType
import mozilla.components.service.glean.error.ErrorRecording.recordError
import mozilla.components.service.glean.private.CommonMetricData
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.org.json.toList
import org.json.JSONArray

/**
 * This singleton handles the in-memory storage logic for string sets. It is meant to be used by
 * the Specific String Set API and the ping assembling objects.
 *
 * This class contains a reference to the Android application Context. While the IDE warns
 * us that this could leak, the application context lives as long as the application and this
 * object. For this reason, we should be safe to suppress the IDE warning.
 */
@SuppressLint("StaticFieldLeak")
internal object StringSetsStorageEngine : StringSetsStorageEngineImplementation()

internal open class StringSetsStorageEngineImplementation(
    override val logger: Logger = Logger("glean/StringsSetsStorageEngine")
) : GenericStorageEngine<Set<String>>() {
    companion object {
        // Maximum size of any set
        const val MAX_SET_SIZE_VALUE = 20
        // Maximum length of any string in the set
        const val MAX_STRING_LENGTH = 50
    }

    override fun deserializeSingleMetric(metricName: String, value: Any?): Set<String>? {
        /*
        Since SharedPreferences doesn't directly support storing of Set<> types, we must use
        an intermediate JSONArray which can be deserialized and converted back to Set<String>.
        Using JSONArray introduces a possible issue in that it's constructor will still properly
        convert a stringified JSONArray into an array of Strings regardless of whether the values
        have been properly quoted or not.  For example, [1,2,3] is as valid just like
        ["a","b","c"] is valid.

        The try/catch is necessary as JSONArray can throw a JSONException if it cannot parse the
        string into an array.
        */
        return (value as? String)?.let {
            try {
                return@let JSONArray(it).toList<String>().toSet()
            } catch (e: org.json.JSONException) {
                return@let null
            }
        }
    }

    override fun serializeSingleMetric(
        userPreferences: SharedPreferences.Editor?,
        storeName: String,
        value: Set<String>,
        extraSerializationData: Any?
    ) {
        // Since SharedPreferences doesn't directly support storing of Set<> types, we must use
        // an intermediate JSONArray which can be serialized to a String type and stored.
        val jsonArray = JSONArray(value)
        userPreferences?.putString(storeName, jsonArray.toString())
    }

    /**
     * Appends to an existing string set in the desired stores.  If the store or set doesn't exist
     * then it is created and added to the desired stores.
     *
     * @param metricData object with metric settings
     * @param value the string value to add
     */
    fun add(
        metricData: CommonMetricData,
        value: String
    ) {
        val truncatedValue = value.let {
            if (it.length > MAX_STRING_LENGTH) {
                recordError(
                    metricData,
                    ErrorType.InvalidValue,
                    "Individual value length ${it.length} exceeds maximum of $MAX_STRING_LENGTH",
                    logger
                )
                return@let it.substring(0, MAX_STRING_LENGTH)
            }
            it
        }

        // Use a custom combiner to add the string to the existing set rather than overwriting
        super.recordMetric(metricData, setOf(truncatedValue), null) { currentValue, newValue ->
            currentValue?.let {
                // Report an error when the set is too big.
                if (it.count() + 1 > MAX_SET_SIZE_VALUE) {
                    recordError(
                        metricData,
                        ErrorType.InvalidValue,
                        "String set length of ${it.count() + 1} exceeds maximum of $MAX_SET_SIZE_VALUE",
                        logger
                    )
                    // Return the unchanged set.
                    return@let it
                }
                // Add the item to the set.
                it + newValue
            } ?: newValue
        }
    }

    /**
     * Sets a string set in the desired stores. This function will replace the existing set or
     * create a new set if it doesn't already exist. To add or append to an existing set, use
     * [add] function.
     *
     * @param metricData object with metric settings
     * @param value the string set value to record
     */
    @Suppress("LongMethod")
    fun set(
        metricData: CommonMetricData,
        value: Set<String>
    ) {
        val stringSet = value.map {
            if (it.length > MAX_STRING_LENGTH) {
                recordError(
                    metricData,
                    ErrorType.InvalidValue,
                    "String too long ${it.length} > $MAX_STRING_LENGTH",
                    logger
                )
            }
            it.take(MAX_STRING_LENGTH)
        }

        if (stringSet.count() > MAX_SET_SIZE_VALUE) {
            recordError(
                metricData,
                ErrorType.InvalidValue,
                "String set length of ${value.count()} exceeds maximum of $MAX_SET_SIZE_VALUE",
                logger
            )
        }

        super.recordMetric(metricData, stringSet.take(MAX_SET_SIZE_VALUE).toSet())
    }
}
