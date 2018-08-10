/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard.source.kinto

import mozilla.components.service.fretboard.ExperimentDownloadException
import mozilla.components.service.fretboard.ExperimentSource
import mozilla.components.service.fretboard.ExperimentsSnapshot
import mozilla.components.service.fretboard.JSONExperimentParser
import org.json.JSONException
import org.json.JSONObject

/**
 * Class responsible for fetching and
 * parsing experiments from a Kinto server
 *
 * @property baseUrl Kinto server url
 * @property bucketName name of the bucket to fetch
 * @property collectionName name of the collection to fetch
 * @property client http client to use
 */
class KintoExperimentSource(
    private val baseUrl: String,
    private val bucketName: String,
    private val collectionName: String,
    private val client: HttpClient = HttpURLConnectionHttpClient()
) : ExperimentSource {
    override fun getExperiments(snapshot: ExperimentsSnapshot): ExperimentsSnapshot {
        val experimentsDiff = getExperimentsDiff(client, snapshot)
        return mergeExperimentsFromDiff(experimentsDiff, snapshot)
    }

    private fun getExperimentsDiff(client: HttpClient, snapshot: ExperimentsSnapshot): String {
        val lastModified = snapshot.lastModified
        val kintoClient = KintoClient(client, baseUrl, bucketName, collectionName)
        return if (lastModified != null) {
            kintoClient.diff(lastModified)
        } else {
            kintoClient.get()
        }
    }

    private fun mergeExperimentsFromDiff(experimentsDiff: String, snapshot: ExperimentsSnapshot): ExperimentsSnapshot {
        val experiments = snapshot.experiments
        var maxLastModified: Long? = snapshot.lastModified
        val mutableExperiments = experiments.toMutableList()
        val experimentParser = JSONExperimentParser()
        try {
            val diffJsonObject = JSONObject(experimentsDiff)
            val experimentsJsonArray = diffJsonObject.getJSONArray(DATA_KEY)
            for (i in 0 until experimentsJsonArray.length()) {
                val experimentJsonObject = experimentsJsonArray.getJSONObject(i)
                val experiment = mutableExperiments.singleOrNull { it.id == experimentJsonObject.getString(ID_KEY) }
                if (experiment != null) {
                    mutableExperiments.remove(experiment)
                }
                if (!experimentJsonObject.has(DELETED_KEY)) {
                    mutableExperiments.add(experimentParser.fromJson(experimentJsonObject))
                }
                val lastModifiedDate = experimentJsonObject.getLong(LAST_MODIFIED_KEY)
                if (maxLastModified == null || lastModifiedDate > maxLastModified) {
                    maxLastModified = lastModifiedDate
                }
            }
        } catch (e: JSONException) {
            throw ExperimentDownloadException(e.message)
        }
        return ExperimentsSnapshot(mutableExperiments, maxLastModified)
    }

    companion object {
        private const val ID_KEY = "id"
        private const val DATA_KEY = "data"
        private const val DELETED_KEY = "deleted"
        private const val LAST_MODIFIED_KEY = "last_modified"
    }
}
