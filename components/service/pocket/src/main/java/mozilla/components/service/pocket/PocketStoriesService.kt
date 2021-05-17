/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.pocket

import android.content.Context
import mozilla.components.service.pocket.stories.PocketStoriesUseCases
import mozilla.components.service.pocket.stories.update.PocketStoriesRefreshScheduler

/**
 * Allows for getting a list of pocket stories based on the provided [PocketStoriesConfig]
 *
 * @param context Android Context. Prefer sending application context to limit the possibility of even small leaks.
 * @param pocketStoriesConfig configuration for how and what pocket stories to get.
 */
class PocketStoriesService(
    private val context: Context,
    private val pocketStoriesConfig: PocketStoriesConfig
) {
    internal var scheduler = PocketStoriesRefreshScheduler(pocketStoriesConfig)
    internal var getStoriesUsecase = PocketStoriesUseCases().GetPocketStories(context)

    /**
     * Entry point to start fetching Pocket stories in the background.
     *
     * Use this at an as high as possible level in your application.
     * Must be paired in a similar way with the [stopPeriodicStoriesRefresh] method.
     *
     * This starts the process of downloading and caching Pocket stories in the background,
     * making them available for the [getStories] method.
     */
    fun startPeriodicStoriesRefresh() {
        PocketStoriesUseCases.initialize(pocketStoriesConfig.pocketApiKey, pocketStoriesConfig.client)
        scheduler.schedulePeriodicRefreshes(context)
    }

    /**
     * Single stopping point for the "get Pocket stories" functionality.
     *
     * Use this at an as high as possible level in your application.
     * Must be paired in a similar way with the [startPeriodicStoriesRefresh] method.
     *
     * This stops the process of downloading and caching Pocket stories in the background.
     */
    fun stopPeriodicStoriesRefresh() {
        scheduler.stopPeriodicRefreshes(context)
        PocketStoriesUseCases.reset()
    }

    /**
     * Get a list of Pocket recommended stories based on the initial configuration.
     *
     * To be called after [startPeriodicStoriesRefresh] to ensure the recommendations are up-to-date.
     * Might return an empty list or a list of older than expected stories if
     * [startPeriodicStoriesRefresh] hasn't yet completed.
     */
    suspend fun getStories(): List<PocketRecommendedStory> {
        return getStoriesUsecase.invoke()
    }
}
