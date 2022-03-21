/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.service.pocket.api.PocketJSONParser.Companion.KEY_ARRAY_ITEMS
import mozilla.components.service.pocket.helpers.PocketTestResources
import mozilla.components.service.pocket.helpers.assertClassVisibility
import mozilla.components.support.ktx.android.org.json.toList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KVisibility

@RunWith(AndroidJUnit4::class)
class PocketJSONParserTest {

    private lateinit var parser: PocketJSONParser

    @Before
    fun setUp() {
        parser = PocketJSONParser()
    }

    @Test
    fun `GIVEN a PocketJSONParser THEN its visibility is internal`() {
        assertClassVisibility(PocketJSONParser::class, KVisibility.INTERNAL)
    }

    @Test
    fun `GIVEN PocketJSONParser WHEN parsing valid stories recommendations THEN PocketApiStories are returned`() {
        val expectedStories = PocketTestResources.apiExpectedPocketStoriesRecommendations
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val actualStories = parser.jsonToPocketApiStories(pocketJSON)

        assertNotNull(actualStories)
        assertEquals(5, actualStories!!.size)
        assertEquals(expectedStories, actualStories)
    }

    @Test
    fun `WHEN parsing stories recommendations with missing titles THEN those entries are dropped`() {
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val expectedStoriesIfMissingTitle = ArrayList(PocketTestResources.apiExpectedPocketStoriesRecommendations)
            .apply { removeAt(4) }
        val pocketJsonWithMissingTitle = removeJsonFieldFromArrayIndex("title", 4, pocketJSON)

        val result = parser.jsonToPocketApiStories(pocketJsonWithMissingTitle)

        assertEquals(4, result!!.size)
        assertEquals(expectedStoriesIfMissingTitle.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations with missing urls THEN those entries are dropped`() {
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val expectedStoriesIfMissingUrl = ArrayList(PocketTestResources.apiExpectedPocketStoriesRecommendations)
            .apply { removeAt(3) }
        val pocketJsonWithMissingUrl = removeJsonFieldFromArrayIndex("url", 3, pocketJSON)

        val result = parser.jsonToPocketApiStories(pocketJsonWithMissingUrl)

        assertEquals(4, result!!.size)
        assertEquals(expectedStoriesIfMissingUrl.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations with missing imageUrls THEN those entries are dropped`() {
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val expectedStoriesIfMissingImageUrl = ArrayList(PocketTestResources.apiExpectedPocketStoriesRecommendations)
            .apply { removeAt(2) }
        val pocketJsonWithMissingImageUrl = removeJsonFieldFromArrayIndex("imageUrl", 2, pocketJSON)

        val result = parser.jsonToPocketApiStories(pocketJsonWithMissingImageUrl)

        assertEquals(4, result!!.size)
        assertEquals(expectedStoriesIfMissingImageUrl.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations with missing image paths THEN those entries are dropped`() {
        val pocketStoriesWithMissingImagePathJSON = JSONObject(PocketTestResources.pocketEndointFiveStoriesResponse).apply {
            val storiesWithOneMissingImagePath = JSONArray(
                this.getJSONArray(KEY_ARRAY_ITEMS).toList<JSONObject>()
                    .mapIndexed { index, jsonObject ->
                        if (index == 2) {
                            jsonObject.put(
                                JSON_STORY_IMAGE_URL_KEY,
                                jsonObject.getString(JSON_STORY_IMAGE_URL_KEY) + "/null"
                            )
                        } else {
                            jsonObject
                        }
                    }
            )
            put(KEY_ARRAY_ITEMS, storiesWithOneMissingImagePath)
        }
        val expectedStoriesIfMissingImagePath = ArrayList(PocketTestResources.apiExpectedPocketStoriesRecommendations)
            .apply { removeAt(2) }

        val result = parser.jsonToPocketApiStories(pocketStoriesWithMissingImagePathJSON.toString())

        assertEquals(4, result!!.size)
        assertEquals(expectedStoriesIfMissingImagePath.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations with missing publishers THEN those entries are kept but with default values`() {
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val expectedStoriesIfMissingPublishers = PocketTestResources.apiExpectedPocketStoriesRecommendations
            .mapIndexed { index, story ->
                if (index == 2) {
                    story.copy(publisher = STRING_NOT_FOUND_DEFAULT_VALUE)
                } else {
                    story
                }
            }
        val pocketJsonWithMissingPublisher = removeJsonFieldFromArrayIndex("publisher", 2, pocketJSON)

        val result = parser.jsonToPocketApiStories(pocketJsonWithMissingPublisher)

        assertEquals(5, result!!.size)
        assertEquals(expectedStoriesIfMissingPublishers.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations with missing categories THEN those entries are kept but with default values`() {
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val expectedStoriesIfMissingCategories = PocketTestResources.apiExpectedPocketStoriesRecommendations
            .mapIndexed { index, story ->
                if (index == 3) {
                    story.copy(category = STRING_NOT_FOUND_DEFAULT_VALUE)
                } else {
                    story
                }
            }
        val pocketJsonWithMissingCategories = removeJsonFieldFromArrayIndex("category", 3, pocketJSON)

        val result = parser.jsonToPocketApiStories(pocketJsonWithMissingCategories)

        assertEquals(5, result!!.size)
        assertEquals(expectedStoriesIfMissingCategories.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations with missing timeToRead THEN those entries are kept but with default values`() {
        val pocketJSON = PocketTestResources.pocketEndointFiveStoriesResponse
        val expectedStoriesIfMissingTimeToRead = PocketTestResources.apiExpectedPocketStoriesRecommendations
            .mapIndexed { index, story ->
                if (index == 4) {
                    story.copy(timeToRead = INT_NOT_FOUND_DEFAULT_VALUE)
                } else {
                    story
                }
            }
        val pocketJsonWithMissingTimeToRead = removeJsonFieldFromArrayIndex("timeToRead", 4, pocketJSON)

        val result = parser.jsonToPocketApiStories(pocketJsonWithMissingTimeToRead)

        assertEquals(5, result!!.size)
        assertEquals(expectedStoriesIfMissingTimeToRead.joinToString(), result.joinToString())
    }

    @Test
    fun `WHEN parsing stories recommendations for an empty string THEN null is returned`() {
        assertNull(parser.jsonToPocketApiStories(""))
    }

    @Test
    fun `WHEN parsing stories recommendations for an invalid JSON String THEN null is returned`() {
        assertNull(parser.jsonToPocketApiStories("{!!}}"))
    }

    @Test
    fun `WHEN a valid image path is present in imageUrl THEN return the full imageUrl`() {
        val storyWithValidImagePath = PocketTestResources.pocketEndpointOneStoryJSONResponse

        val result = parser.getValidImageUrl(storyWithValidImagePath)

        assertSame(storyWithValidImagePath.getString(JSON_STORY_IMAGE_URL_KEY), result)
    }

    @Test(expected = JSONException::class)
    fun `WHEN the image path is missing from imageUrl THEN an exception is thrown`() {
        val storyWithNullImageUrl = PocketTestResources.pocketEndpointOneStoryJSONResponse.also {
            it.put(JSON_STORY_IMAGE_URL_KEY, it.getString(JSON_STORY_IMAGE_URL_KEY) + "/null")
        }

        parser.getValidImageUrl(storyWithNullImageUrl)
    }
}

private fun removeJsonFieldFromArrayIndex(fieldName: String, indexInArray: Int, json: String): String {
    val obj = JSONObject(json)
    val storiesJson = obj.getJSONArray(KEY_ARRAY_ITEMS)
    storiesJson.getJSONObject(indexInArray).remove(fieldName)
    return obj.toString()
}
