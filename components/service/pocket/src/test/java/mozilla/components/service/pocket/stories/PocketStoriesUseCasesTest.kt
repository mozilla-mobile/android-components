/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket.stories

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.components.concept.fetch.Client
import mozilla.components.service.pocket.PocketRecommendedStory
import mozilla.components.service.pocket.api.PocketEndpoint
import mozilla.components.service.pocket.api.PocketResponse
import mozilla.components.service.pocket.helpers.PocketTestResource
import mozilla.components.service.pocket.helpers.assertClassVisibility
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import kotlin.reflect.KVisibility

@RunWith(AndroidJUnit4::class)
class PocketStoriesUseCasesTest {
    private val usecases = spy(PocketStoriesUseCases())
    private val pocketRepo: PocketRecommendationsRepository = mock()
    private val pocketEndoint: PocketEndpoint = mock()

    @Before
    fun setup() {
        doReturn(pocketEndoint).`when`(usecases).getPocketEndpoint(any())
        doReturn(pocketRepo).`when`(usecases).getPocketRepository(any())
    }

    @After
    fun cleanup() {
        PocketStoriesUseCases.reset()
    }

    @Test
    fun `GIVEN a PocketStoriesUseCases THEN its visibility is public`() {
        assertClassVisibility(PocketStoriesUseCases::class, KVisibility.PUBLIC)
    }

    @Test
    fun `GIVEN a RefreshPocketStories THEN its visibility is internal`() {
        assertClassVisibility(
            PocketStoriesUseCases.RefreshPocketStories::class,
            KVisibility.INTERNAL
        )
    }

    @Test
    fun `GIVEN a GetPocketStories THEN its visibility is public`() {
        assertClassVisibility(PocketStoriesUseCases.GetPocketStories::class, KVisibility.INTERNAL)
    }

    @Test
    fun `GIVEN an uninitialized PocketStoriesUseCases WHEN initialize is called THEN api key and client are set`() {
        val client: Client = mock()

        PocketStoriesUseCases.initialize(client)

        assertSame(client, PocketStoriesUseCases.fetchClient)
    }

    @Test
    fun `GIVEN an already initialized PocketStoriesUseCases WHEN initialize is called THEN the new api key and client overwrite the old values`() {
        PocketStoriesUseCases.fetchClient = mock()
        val newClient: Client = mock()

        PocketStoriesUseCases.initialize(newClient)

        assertSame(newClient, PocketStoriesUseCases.fetchClient)
    }

    @Test
    fun `GIVEN PocketStoriesUseCases WHEN reset is called THEN the api key and fetch client references are freed`() {
        PocketStoriesUseCases.fetchClient = mock()

        PocketStoriesUseCases.reset()

        assertNull(PocketStoriesUseCases.fetchClient)
    }

    @Test
    fun `GIVEN PocketStoriesUseCases WHEN RefreshPocketStories is called THEN it should download stories from API and return early if unsuccessful response`() {
        PocketStoriesUseCases.initialize(mock())
        val refreshUsecase = spy(
            usecases.RefreshPocketStories(testContext)
        )
        val successfulResponse = getSuccessfulPocketStories()

        doReturn(successfulResponse).`when`(pocketEndoint).getTopStories()

        val result = runBlocking {
            refreshUsecase.invoke()
        }

        assertTrue(result)
        verify(pocketEndoint).getTopStories()
        runBlocking {
            verify(pocketRepo).addAllPocketPocketRecommendedStories((successfulResponse as PocketResponse.Success).data)
        }
    }

    @Test
    fun `GIVEN PocketStoriesUseCases WHEN RefreshPocketStories is called THEN it should download stories from API and save a successful response locally`() {
        PocketStoriesUseCases.initialize(mock())
        val refreshUsecase = spy(usecases.RefreshPocketStories(testContext))
        val successfulResponse = getFailedPocketStories()

        doReturn(successfulResponse).`when`(pocketEndoint).getTopStories()

        val result = runBlocking {
            refreshUsecase.invoke()
        }

        assertFalse(result)
        verify(pocketEndoint).getTopStories()
        runBlocking {
            verify(pocketRepo, never()).addAllPocketPocketRecommendedStories(any())
        }
    }

    @Test
    fun `GIVEN PocketStoriesUseCases WHEN GetPocketStories is called THEN it should delegate the repository to return locally stored stories`() =
        runBlocking {
            val getStoriesUsecase = spy(usecases.GetPocketStories(testContext))

            doReturn(emptyList<PocketRecommendedStory>())
                .`when`(pocketRepo).getPocketRecommendedStories()
            var result = getStoriesUsecase.invoke()
            verify(pocketRepo).getPocketRecommendedStories()
            assertTrue(result.isEmpty())

            val stories = JSONObject(
                PocketTestResource.FIVE_POCKET_STORIES_RECOMMENDATIONS_POCKET_RESPONSE.get()
            ).toPocketRecommendedStories()
            doReturn(stories).`when`(pocketRepo).getPocketRecommendedStories()
            result = getStoriesUsecase.invoke()
            // getPocketRecommendedStories() should've been called 2 times. Once in the above check, once now.
            verify(pocketRepo, times(2)).getPocketRecommendedStories()
            assertEquals(result, stories)
        }

    private fun getSuccessfulPocketStories() =
        PocketResponse.wrap(PocketTestResource.FIVE_POCKET_STORIES_RECOMMENDATIONS_POCKET_RESPONSE.get())

    private fun getFailedPocketStories() = PocketResponse.wrap(null)
}
