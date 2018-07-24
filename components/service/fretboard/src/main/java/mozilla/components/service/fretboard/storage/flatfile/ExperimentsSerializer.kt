/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard.storage.flatfile

import mozilla.components.service.fretboard.Experiment
import mozilla.components.service.fretboard.JSONExperimentParser
import mozilla.components.service.fretboard.ExperimentsSnapshot
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper class for serializing experiments
 * into the json format used to save them
 * locally on the device
 */
internal class ExperimentsSerializer {
    /**
     * Transforms the given list of experiments to
     * its json file representation
     *
     * @param snapshot experiments to serialize
     * @return json file representation of the given experiments
     */
    fun toJson(snapshot: ExperimentsSnapshot): String {
        val experimentsJson = JSONObject()
        val experimentsJSONArray = JSONArray()
        val jsonParser = JSONExperimentParser()
        for (experiment in snapshot.experiments)
            experimentsJSONArray.put(jsonParser.toJson(experiment))
        experimentsJson.put(EXPERIMENTS_KEY, experimentsJSONArray)
        experimentsJson.put(LAST_MODIFIED_KEY, snapshot.lastModified)
        return experimentsJson.toString()
    }

    /**
     * Creates a list of experiments given json
     * representation, as stored locally inside a file
     *
     * @param json json file contents
     * @return list of experiments
     */
    fun fromJson(json: String): ExperimentsSnapshot {
        val experimentsJson = JSONObject(json)
        val experimentsJsonArray = experimentsJson.getJSONArray(EXPERIMENTS_KEY)
        val experiments = ArrayList<Experiment>()
        val jsonParser = JSONExperimentParser()
        for (i in 0 until experimentsJsonArray.length())
            experiments.add(jsonParser.fromJson(experimentsJsonArray[i] as JSONObject))
        val lastModified: Long?
        if (experimentsJson.has(LAST_MODIFIED_KEY)) {
            lastModified = experimentsJson.getLong(LAST_MODIFIED_KEY)
        } else {
            lastModified = null
        }
        return ExperimentsSnapshot(experiments, lastModified)
    }

    companion object {
        private const val EXPERIMENTS_KEY = "experiments"
        private const val LAST_MODIFIED_KEY = "last_modified"
    }
}
