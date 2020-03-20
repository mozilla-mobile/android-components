/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import android.content.res.Resources
import kotlinx.coroutines.Deferred
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.feature.awesomebar.R
import mozilla.components.feature.tabs.TabsUseCases
import java.util.UUID

/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides suggestions based on the sessions in the
 * [SessionManager] (Open tabs).
 */
class SessionSuggestionProvider(
    private val resources: Resources,
    private val store: BrowserStore,
    private val selectTabUseCase: TabsUseCases.SelectTabUseCase,
    private val icons: BrowserIcons? = null,
    private val excludeSelectedSession: Boolean = false
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val state = store.state
        val tabs = state.tabs

        val suggestions = mutableListOf<AwesomeBar.Suggestion>()
        val iconRequests: List<Deferred<Icon>?> = tabs.map { icons?.loadIcon(IconRequest(it.content.url)) }

        tabs.zip(iconRequests) { result, icon ->
            if (
                result.contains(text) &&
                !result.content.private &&
                shouldIncludeSelectedTab(state, result)
            ) {
                suggestions.add(
                        AwesomeBar.Suggestion(
                            provider = this,
                            id = result.id,
                            title = result.content.title,
                            description = resources.getString(R.string.switch_to_tab_description),
                            icon = icon?.await()?.bitmap,
                            onSuggestionClicked = { selectTabUseCase(result.id) }
                        )
                )
            }
        }
        return suggestions
    }

    private fun TabSessionState.contains(text: String) =
            (content.url.contains(text, ignoreCase = true) || content.title.contains(text, ignoreCase = true))

    private fun shouldIncludeSelectedTab(state: BrowserState, tab: TabSessionState): Boolean {
        return if (excludeSelectedSession) {
            tab.id != state.selectedTabId
        } else {
            true
        }
    }
}
