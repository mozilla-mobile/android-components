/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.nimbus.ext

import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.experiments.nimbus.Variables

/**
 * Gets the branch name of an experiment acting on the feature given [featureId], and transforms it
 * with given closure.
 *
 * You are probably looking for [withVariables].
 *
 * If we're enrolled in an experiment, the transform is passed the branch id/slug as a `String`.
 *
 * If we're not enrolled in the experiment, or the experiment is not valid then the transform
 * is passed a `null`.
 */
fun <T> NimbusApi.withExperiment(featureId: String, transform: (String?) -> T): T {
    return transform(withExperiment(featureId))
}

/**
 * The synonym for [NimbusApi.getExperimentBranch] to complement [withExperiment].
 *
 * Short-hand for [NimbusApi.getExperimentBranch].
 */
@Suppress("TooGenericExceptionCaught")
fun NimbusApi.withExperiment(featureId: String) =
    try {
        getExperimentBranch(featureId)
    } catch (e: Throwable) {
        Logger.error("Failed to getExperimentBranch($featureId)", e)
        null
    }

/**
 * A synonym for [NimbusApi.getVariables].
 *
 * This exists as a complement to the `withVariable(featureId, sendExposureEvent, transform)` method.
 *
 * @param featureId the id of the feature as it appears in `Experimenter`
 * @param sendExposureEvent by default `true`. This logs an event that the user was exposed to an experiment
 *      involving this feature.
 * @return a [Variables] object providing typed accessors to a remotely configured JSON object.
 */
fun NimbusApi.withVariables(featureId: String, sendExposureEvent: Boolean = true) =
    getVariables(featureId, sendExposureEvent)

/**
 * Get a [Variables] object for this feature and use that to configure the feature itself or a
 * more type safe configuration object.
 *
 * @param featureId the id of the feature as it appears in the Experimenter platform.
 * @param sendExposureEvent by default `true`. This logs an event that the user was exposed to an experiment
 *      involving this feature.
 */
fun <T> NimbusApi.withVariables(featureId: String, sendExposureEvent: Boolean = true, transform: (Variables) -> T) =
    transform(getVariables(featureId, sendExposureEvent))
