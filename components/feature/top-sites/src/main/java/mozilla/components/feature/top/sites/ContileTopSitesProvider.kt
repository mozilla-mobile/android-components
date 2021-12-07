/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.org.json.asSequence
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

internal const val contileEndpointUrl = "https://contile.services.mozilla.com/v1/tiles"

/**
 * Provide access to the Contile services API.
 *
 * @property client [Client] used for interacting with the Contile HTTP API.
 */
class ContileTopSitesProvider(
    private val client: Client
) : TopSitesProvider {

    private val logger = Logger("ContileTopSitesProvider")

    override suspend fun getTopSites(): List<TopSite> {
        return try {
            fetchTopSites()
        } catch (e: IOException) {
            logger.error("Failed to fetch contile top sites", e)
            throw e
        }
    }

    private fun fetchTopSites(): List<TopSite> {
        client.fetch(
            Request(
                url = contileEndpointUrl
            )
        ).use { response ->
            if (response.isSuccess) {
                val responseBody = response.body.string(Charsets.UTF_8)

                return try {
                    JSONObject(responseBody).getTopSites()
                } catch (e: JSONException) {
                    throw IOException(e)
                }
            } else {
                val errorMessage = "Failed to fetch contile top sites. Status code: ${response.status}"
                logger.error(errorMessage)
                throw IOException(errorMessage)
            }
        }
    }
}

internal fun JSONObject.getTopSites(): List<TopSite> =
    getJSONArray("tiles")
        .asSequence { i -> getJSONObject(i) }
        .mapNotNull { it.toTopSite() }
        .toList()

private fun JSONObject.toTopSite(): TopSite? {
    return try {
        TopSite.Contile(
            id = getLong("id"),
            title = getString("name"),
            url = getString("url"),
            clickUrl = getString("click_url"),
            imageUrl = getString("image_url"),
            impressionUrl = getString("impression_url"),
            position = getInt("position"),
            createdAt = null
        )
    } catch (e: JSONException) {
        null
    }
}
