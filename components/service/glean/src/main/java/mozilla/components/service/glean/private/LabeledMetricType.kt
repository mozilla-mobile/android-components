/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean.private

import mozilla.components.service.glean.storages.StorageEngine
import mozilla.components.service.glean.storages.StorageEngineManager
import mozilla.components.service.glean.error.ErrorRecording.ErrorType
import mozilla.components.service.glean.error.ErrorRecording.recordError
import mozilla.components.support.base.log.logger.Logger

/**
 * This implements the developer facing API for labeled metrics.
 *
 * Instances of this class type are automatically generated by the parsers at build time,
 * allowing developers to record values that were previously registered in the metrics.yaml file.
 *
 * Unlike most metric types, LabeledMetricType does not have its own corresponding storage engine,
 * but records metrics for the underlying metric type T in the storage engine for that type.  The
 * only difference is that labeled metrics are stored with the special key `$category.$name/$label`.
 * The |StorageEngineManager.collect| method knows how to pull these special values back out of the
 * individual storage engines and rearrange them correctly in the ping.
 */
data class LabeledMetricType<T>(
    override val disabled: Boolean,
    override val category: String,
    override val lifetime: Lifetime,
    override val name: String,
    override val sendInPings: List<String>,
    val subMetric: T,
    val labels: Set<String>? = null
) : CommonMetricData {

    private val logger = Logger("glean/LabeledMetricType")

    companion object {
        private const val MAX_LABELS = 16
        private const val OTHER_LABEL = "__other__"

        // This regex is used for matching against labels and should allow for dots, underscores,
        // and/or hyphens. Labels are also limited to starting with either a letter or an
        // underscore character.
        private val labelRegex = Regex("^[a-z_][a-z0-9_-]{0,29}(\\.[a-z0-9_-]{0,29})*$")
        // Some examples of good and bad labels:
        //
        // Good:
        //   this.is.fine
        //   this_is_fine_too
        //   this.is_still_fine
        //   thisisfine
        //   this.is_fine.2
        //   _.is_fine
        //   this.is-fine
        //   this-is-fine
        // Bad:
        //   this.is.not_fine_due_tu_the_length_being_too_long_i_thing.i.guess
        //   1.not_fine
        //   this.$isnotfine
        //   -.not_fine

        private const val MAX_LABEL_LENGTH = 61
    }

    private val seenLabels: MutableSet<String> = mutableSetOf()

    // TimespanMetricType holds a state now (the private `timerId`), which must be preserved
    // when looking up the labels. Each time we request a label for a timespan, we cache the
    // generated type in this map.
    private val seenTimespans: MutableMap<String, TimespanMetricType> = mutableMapOf()

    /**
     * Handles the label in the case where labels are predefined.
     *
     * If the given label is not in the predefined set of labels, returns [OTHER_LABEL], otherwise
     * returns the label verbatim.
     *
     * @param label The label, as specified by the user
     * @return adjusted label, possibly set to [OTHER_LABEL]
     */
    private fun getFinalStaticLabel(label: String): String {
        return if (labels!!.contains(label)) label else OTHER_LABEL
    }

    /**
     * Handles the label in the case where labels aren't predefined.
     *
     * If we've already seen more than [MAX_LABELS] unique labels, returns [OTHER_LABEL].
     *
     * Also validates any unseen labels to make sure they are snake_case and under 30 characters.
     * If not, returns [OTHER_LABEL].
     *
     * @param label The label, as specified by the user
     * @return adjusted label, possibly set to [OTHER_LABEL]
     */
    @Suppress("ReturnCount")
    private fun getFinalDynamicLabel(label: String): String {
        if (lifetime != Lifetime.Application && seenLabels.size == 0) {
            // TODO 1530733: This might cause I/O on the main thread if this is the
            // first thing being stored to the given storage engine after app restart.
            getStorageEngineForMetric()?.let {
                val identifier = (subMetric as CommonMetricData).identifier
                val prefix = "$identifier/"
                seenLabels.addAll(
                    it.getIdentifiersInStores((subMetric as CommonMetricData).sendInPings)
                        .filter { it.startsWith(prefix) }
                        .map { it.substring(prefix.length) }
                )
            }
        }

        if (!seenLabels.contains(label)) {
            if (seenLabels.size >= MAX_LABELS) {
                return OTHER_LABEL
            } else {
                if (label.length > MAX_LABEL_LENGTH) {
                    recordError(
                        this,
                        ErrorType.InvalidLabel,
                        "label length ${label.length} exceeds maximum of $MAX_LABEL_LENGTH",
                        logger
                    )
                    return OTHER_LABEL
                }

                // Labels must be snake_case.
                if (!labelRegex.matches(label)) {
                    recordError(
                        this,
                        ErrorType.InvalidLabel,
                        "label must be dotted snake_case, got '$label'",
                        logger
                    )
                    return OTHER_LABEL
                }
                seenLabels.add(label)
            }
        }
        return label
    }

    /**
     * Get a copy of the subMetric with the name changed to the given `newName`.
     *
     * @param newName The new name for the metric.
     * @return A copy of subMetric with the new name.
     * @throws IllegalStateException If this metric type does not support labels.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun getMetricWithNewName(newName: String): T {
        // function is "internal" so we can mock it in testing

        // Every metric that supports labels needs an entry here
        return when (subMetric) {
            is BooleanMetricType -> subMetric.copy(name = newName) as T
            is CounterMetricType -> subMetric.copy(name = newName) as T
            is DatetimeMetricType -> subMetric.copy(name = newName) as T
            is StringSetMetricType -> subMetric.copy(name = newName) as T
            is StringMetricType -> subMetric.copy(name = newName) as T
            is TimespanMetricType -> subMetric.copy(name = newName) as T
            is UuidMetricType -> subMetric.copy(name = newName) as T
            else -> throw IllegalStateException(
                "Can not create a labeled version of this metric type"
            )
        }
    }

    /**
     * Delegates to [StorageEngineManager.getStorageEngineForMetric].
     * Provided here so it can be mocked for testing.
     */
    internal fun getStorageEngineForMetric(): StorageEngine? {
        return StorageEngineManager.getStorageEngineForMetric(subMetric)
    }

    /**
     * Get the specific metric for a given label.
     *
     * If a set of acceptable labels were specified in the metrics.yaml file,
     * and the given label is not in the set, it will be recorded under the
     * special [OTHER_LABEL].
     *
     * If a set of acceptable labels was not specified in the metrics.yaml file,
     * only the first 16 unique labels will be used. After that, any additional
     * labels will be recorded under the special [OTHER_LABEL] label.
     *
     * Labels must be snake_case and less than 30 characters. If an invalid label
     * is used, the metric will be recorded in the special [OTHER_LABEL] label.
     *
     * @param label The label
     * @return The specific metric for that label
     */
    operator fun get(label: String): T {
        val actualLabel = labels?.let {
            getFinalStaticLabel(label)
        } ?: run {
            getFinalDynamicLabel(label)
        }

        val newMetricName = "$name/$actualLabel"
        if (subMetric is TimespanMetricType) {
            // If this is a timespan, try to look it up from the cache.
            // If it's there, return it. If it isn't, create a new TimespanMetricType
            // and add it to the cache then return it.
            // This needs to be synchronized as access to the map is not thread safe.
            synchronized(this) {
                @Suppress("UNCHECKED_CAST")
                return seenTimespans.getOrPut(newMetricName) {
                    getMetricWithNewName(newMetricName) as TimespanMetricType
                } as T
            }
        }

        return getMetricWithNewName(newMetricName)
    }
}
