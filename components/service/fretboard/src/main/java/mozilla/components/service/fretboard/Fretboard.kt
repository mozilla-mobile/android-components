/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard

import android.content.Context
import mozilla.components.support.base.log.logger.Logger

/**
 * Entry point of the library
 *
 * @property source experiment remote source
 * @property storage experiment local storage mechanism
 * @param valuesProvider provider for the device's values
 */
class Fretboard(
    private val source: ExperimentSource,
    private val storage: ExperimentStorage,
    valuesProvider: ValuesProvider = ValuesProvider()
) {
    private var experimentsResult: ExperimentsSnapshot = ExperimentsSnapshot(listOf(), null)
    private var experimentsLoaded: Boolean = false
    private val evaluator = ExperimentEvaluator(valuesProvider)
    private val logger = Logger(LOG_TAG)

    /**
     * Provides the list of experiments (active or not)
     */
    val experiments: List<Experiment>
        get() = experimentsResult.experiments.toList()

    /**
     * Loads experiments from local storage
     */
    @Synchronized
    fun loadExperiments() {
        experimentsResult = storage.retrieve()
        experimentsLoaded = true
    }

    /**
     * Requests new experiments from the server and
     * saves them to local storage
     */
    @Synchronized
    fun updateExperiments() {
        if (!experimentsLoaded) {
            loadExperiments()
        }
        try {
            val serverExperiments = source.getExperiments(experimentsResult)
            experimentsResult = serverExperiments
            storage.save(serverExperiments)
        } catch (e: ExperimentDownloadException) {
            // Keep using the local experiments
            logger.error(e.message, e)
        }
    }

    /**
     * Checks if the user is part of
     * the specified experiment
     *
     * @param context context
     * @param descriptor descriptor of the experiment to check
     *
     * @return true if the user is part of the specified experiment, false otherwise
     */
    fun isInExperiment(context: Context, descriptor: ExperimentDescriptor): Boolean {
        return evaluator.evaluate(context, descriptor, experimentsResult.experiments) != null
    }

    /**
     * Performs an action if the user is part of the specified experiment
     *
     * @param context context
     * @param descriptor descriptor of the experiment to check
     * @param block block of code to be executed if the user is part of the experiment
     */
    fun withExperiment(context: Context, descriptor: ExperimentDescriptor, block: (Experiment) -> Unit) {
        evaluator.evaluate(context, descriptor, experimentsResult.experiments)?.let { block(it) }
    }

    /**
     * Gets the metadata associated with the specified experiment, even if the user is not part of it
     *
     * @param descriptor descriptor of the experiment
     *
     * @return metadata associated with the experiment
     */
    fun getExperiment(descriptor: ExperimentDescriptor): Experiment? {
        return evaluator.getExperiment(descriptor, experimentsResult.experiments)
    }

    /**
     * Provides the list of active experiments
     *
     * @param context context
     *
     * @return active experiments
     */
    fun getActiveExperiments(context: Context): List<Experiment> {
        return experiments.filter { isInExperiment(context, ExperimentDescriptor(it.name)) }.toList()
    }

    /**
     * Overrides a specified experiment
     *
     * @param context context
     * @param descriptor descriptor of the experiment
     * @param active overridden value for the experiment, true to activate it, false to deactivate
     */
    fun setOverride(context: Context, descriptor: ExperimentDescriptor, active: Boolean) {
        evaluator.setOverride(context, descriptor, active)
    }

    /**
     * Clears an override for a specified experiment
     *
     * @param context context
     * @param descriptor descriptor of the experiment
     */
    fun clearOverride(context: Context, descriptor: ExperimentDescriptor) {
        evaluator.clearOverride(context, descriptor)
    }

    /**
     * Clears all experiment overrides
     *
     * @param context context
     */
    fun clearAllOverrides(context: Context) {
        evaluator.clearAllOverrides(context)
    }

    companion object {
        private const val LOG_TAG = "fretboard"
    }
}
