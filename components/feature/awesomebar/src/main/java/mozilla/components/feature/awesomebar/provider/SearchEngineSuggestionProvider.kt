/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.support.base.log.logger.Logger
import java.lang.Integer.MAX_VALUE
import java.util.UUID

/**
 * [AwesomeBar.SuggestionProvider] implementation that provides suggestions based on the search engine list.
 *
 * @property searchEnginesList a search engine list used to search
 * @property selectShortcutEngine the use case invoked to temporarily change engine used for search
 * @property title String resource for the title to be shown for the suggestion(s), it
 * includes a placeholder for engine name
 * @property description the description to be shown for the suggestion(s), same description for all
 * @property searchIcon the icon to be shown for the suggestion(s), same icon for all
 * @property maxSuggestions the maximum number of suggestions to be provided
 * @property charactersThreshold the minimum typed characters used to match to a search engine name
 */
@Suppress("LongParameterList")
class SearchEngineSuggestionProvider(
    private val context: Context,
    private val searchEnginesList: List<SearchEngine>,
    private val selectShortcutEngine: (engine: SearchEngine) -> Unit,
    @StringRes
    private val title: Int,
    private val description: String?,
    private val searchIcon: Bitmap?,
    @VisibleForTesting
    internal val maxSuggestions: Int = DEFAULT_MAX_SUGGESTIONS,
    @VisibleForTesting
    internal val charactersThreshold: Int = DEFAULT_CHARACTERS_THRESHOLD
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()
    private val logger = Logger("SearchEngineSuggestionProvider")

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        logger.debug("onInputChanged(${text.length} characters) called")

        if (text.isEmpty() || text.length < charactersThreshold) {
            return emptyList()
        }

        val suggestions = searchEnginesList
            .also { logger.debug("\tonInputChanged: ${it.size} suggestions available") }
            .filter { it.name.contains(text, true) }.take(maxSuggestions)
            .also { logger.debug("\tonInputChanged: ${it.size} suggestions filtered") }

        return if (suggestions.isNotEmpty()) {
            suggestions.into()
        } else {
            return emptyList()
        }.also {
            logger.debug("\tonInputChanged: ${it.size} suggestions returned")
        }
    }

    /**
     *  Generates a list of [AwesomeBar.Suggestion] from a [SearchEngine] list
     */
    private fun List<SearchEngine>.into(): List<AwesomeBar.Suggestion> {

        return this.map {
            val now = System.currentTimeMillis()

            AwesomeBar.Suggestion(
                provider = this@SearchEngineSuggestionProvider,
                id = it.id,
                icon = searchIcon,
                flags = setOf(AwesomeBar.Suggestion.Flag.BOOKMARK),
                title = context.getString(title, it.name),
                description = description,
                onSuggestionClicked = { selectShortcutEngine(it) },
                score = MAX_VALUE
            ).also {
                logger.debug("\tinto() mapped this suggestion after ${System.currentTimeMillis() - now} millis")
            }
        }
    }

    companion object {
        internal const val DEFAULT_MAX_SUGGESTIONS = 1
        internal const val DEFAULT_CHARACTERS_THRESHOLD = 1
    }
}
