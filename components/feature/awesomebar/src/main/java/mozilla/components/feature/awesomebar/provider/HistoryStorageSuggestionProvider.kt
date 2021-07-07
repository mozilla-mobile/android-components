/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import androidx.annotation.VisibleForTesting
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.storage.SearchResult
import mozilla.components.feature.awesomebar.facts.emitHistorySuggestionClickedFact
import mozilla.components.feature.session.SessionUseCases
import java.util.UUID

/**
 * Return 20 history suggestions by default.
 */
const val DEFAULT_HISTORY_SUGGESTION_LIMIT = 20

/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides suggestions based on the browsing
 * history stored in the [HistoryStorage].
 *
 * @param historyStorage and instance of the [HistoryStorage] used
 * to query matching history records.
 * @param loadUrlUseCase the use case invoked to load the url when the
 * user clicks on the suggestion.
 * @param icons optional instance of [BrowserIcons] to load fav icons
 * for history URLs.
 * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
 * highest scored suggestion URL.
 * @param maxNumberOfSuggestions optional parameter to specify the maximum number of returned suggestions,
 * defaults to [DEFAULT_HISTORY_SUGGESTION_LIMIT]
 */
class HistoryStorageSuggestionProvider(
    private val historyStorage: HistoryStorage,
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
    private val icons: BrowserIcons? = null,
    internal val engine: Engine? = null,
    @VisibleForTesting internal val maxNumberOfSuggestions: Int = DEFAULT_HISTORY_SUGGESTION_LIMIT
) : AwesomeBar.SuggestionProvider {

    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }

        // In case of duplicates we want to pick the suggestion with the highest score.
        // See: https://github.com/mozilla/application-services/issues/970
        val suggestions = historyStorage.getSuggestions(text, maxNumberOfSuggestions)
            .sortedByDescending { it.score }
            .distinctBy { it.id }

        suggestions.firstOrNull()?.url?.let { url -> engine?.speculativeConnect(url) }

        return suggestions.into()
    }

    override val shouldClearSuggestions: Boolean
        // We do not want the suggestion of this provider to disappear and re-appear when text changes.
        get() = false

    private suspend fun Iterable<SearchResult>.into(): List<AwesomeBar.Suggestion> {
        val iconRequests = this.map { icons?.loadIcon(IconRequest(it.url)) }
        return this.zip(iconRequests) { result, icon ->
            AwesomeBar.Suggestion(
                provider = this@HistoryStorageSuggestionProvider,
                id = result.id,
                icon = icon?.await()?.bitmap,
                title = result.title,
                description = result.url,
                editSuggestion = result.url,
                score = result.score,
                onSuggestionClicked = {
                    loadUrlUseCase.invoke(result.url)
                    emitHistorySuggestionClickedFact()
                }
            )
        }
    }
}
