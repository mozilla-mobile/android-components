/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket.api

import androidx.annotation.VisibleForTesting
import mozilla.components.service.pocket.logger
import mozilla.components.support.ktx.android.org.json.mapNotNull
import mozilla.components.support.ktx.android.org.json.tryGetInt
import mozilla.components.support.ktx.android.org.json.tryGetString
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws

private const val JSON_STORY_TITLE_KEY = "title"
private const val JSON_STORY_URL_KEY = "url"
@VisibleForTesting
internal const val JSON_STORY_IMAGE_URL_KEY = "imageUrl"
private const val JSON_STORY_PUBLISHER_KEY = "publisher"
private const val JSON_STORY_CATEGORY_KEY = "category"
private const val JSON_STORY_TIME_TO_READ_KEY = "timeToRead"

/**
 * Holds functions that parse the JSON returned by the Pocket API and converts them to more usable Kotlin types.
 */
internal class PocketJSONParser {
    /**
     * @return The stories, removing entries that are invalid, or null on error; the list will never be empty.
     */
    fun jsonToPocketApiStories(json: String): List<PocketApiStory>? = try {
        val rawJSON = JSONObject(json)
        val storiesJSON = rawJSON.getJSONArray(KEY_ARRAY_ITEMS)
        val stories = storiesJSON.mapNotNull(JSONArray::getJSONObject) { jsonToPocketApiStory(it) }

        // We return null, rather than the empty list, because devs might forget to check an empty list.
        if (stories.isNotEmpty()) stories else null
    } catch (e: JSONException) {
        logger.warn("invalid JSON from the Pocket endpoint", e)
        null
    }

    private fun jsonToPocketApiStory(json: JSONObject): PocketApiStory? = try {
        PocketApiStory(
            // These three properties are required for any valid recommendation.
            title = json.getString(JSON_STORY_TITLE_KEY),
            url = json.getString(JSON_STORY_URL_KEY),
            imageUrl = getValidImageUrl(json),
            // The following three properties are optional.
            publisher = json.tryGetString(JSON_STORY_PUBLISHER_KEY) ?: STRING_NOT_FOUND_DEFAULT_VALUE,
            category = json.tryGetString(JSON_STORY_CATEGORY_KEY) ?: STRING_NOT_FOUND_DEFAULT_VALUE,
            timeToRead = json.tryGetInt(JSON_STORY_TIME_TO_READ_KEY) ?: INT_NOT_FOUND_DEFAULT_VALUE
        )
    } catch (e: JSONException) {
        null
    }

    /**
     * Validate and return the Pocket story imageUrl only if it has an actual image path.
     * The JSON object representing a Pocket story might contains a url representing the `imageUrl`
     * but that might not be valid.
     *
     * @param [jsonStory] JSON respecting the schema of a Pocket story. Expected to contain [JSON_STORY_IMAGE_URL_KEY].
     *
     * @return the full imageUrl from [jsonStory] if it does not end in "/null".
     *
     * @throws JSONException if a value for [JSON_STORY_IMAGE_URL_KEY] is not found or ends with "/null".
     */
    @Throws(JSONException::class)
    @VisibleForTesting
    internal fun getValidImageUrl(jsonStory: JSONObject): String {
        val imageUrl = jsonStory.getString(JSON_STORY_IMAGE_URL_KEY)
        val imagePath = imageUrl.substringAfterLast("/")

        if (imagePath == "null") {
            throw JSONException("Null image path")
        }

        return imageUrl
    }

    companion object {
        @VisibleForTesting const val KEY_ARRAY_ITEMS = "recommendations"

        /**
         * Returns a new instance of [PocketJSONParser].
         */
        fun newInstance(): PocketJSONParser {
            return PocketJSONParser()
        }
    }
}
