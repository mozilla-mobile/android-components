/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataStorage
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.session.SessionUseCases
import java.util.UUID

/**
 * Return 5 history suggestions by default.
 */
const val DEFAULT_METADATA_SUGGESTION_LIMIT = 5

/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides suggestions based on [HistoryMetadata].
 *
 * @param historyStorage an instance of the [HistoryStorage] used
 * to query matching metadata records.
 * @param loadUrlUseCase the use case invoked to load the url when the
 * user clicks on the suggestion.
 * @param icons optional instance of [BrowserIcons] to load fav icons
 * for [HistoryMetadata] URLs.
 * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
 * highest scored suggestion URL.
 * @param maxNumberOfSuggestions optional parameter to specify the maximum number of returned suggestions,
 * defaults to [DEFAULT_METADATA_SUGGESTION_LIMIT].
 */
class HistoryMetadataSuggestionProvider(
    private val historyStorage: HistoryMetadataStorage,
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
    private val icons: BrowserIcons? = null,
    internal val engine: Engine? = null,
    @VisibleForTesting internal val maxNumberOfSuggestions: Int = DEFAULT_METADATA_SUGGESTION_LIMIT
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isNullOrBlank()) {
            return emptyList()
        }

        val suggestions = historyStorage
            .queryHistoryMetadata(text, maxNumberOfSuggestions)
            .filter { it.totalViewTime > 0 }

        suggestions.firstOrNull()?.key?.url?.let { url -> engine?.speculativeConnect(url) }
        return suggestions.into(this, icons, loadUrlUseCase)
    }
}

internal suspend fun Iterable<HistoryMetadata>.into(
    provider: AwesomeBar.SuggestionProvider,
    icons: BrowserIcons?,
    loadUrlUseCase: SessionUseCases.LoadUrlUseCase
): List<AwesomeBar.Suggestion> = coroutineScope {
    return@coroutineScope this@into.map {
        async {
            AwesomeBar.Suggestion(
                provider = provider,
                icon = icons?.loadIcon(IconRequest(it.key.url))?.await()?.bitmap,
                title = it.title,
                description = it.key.url,
                editSuggestion = it.key.url,
                onSuggestionClicked = {
                    loadUrlUseCase.invoke(it.key.url)
                }
            )
        }
    }.awaitAll()
}
