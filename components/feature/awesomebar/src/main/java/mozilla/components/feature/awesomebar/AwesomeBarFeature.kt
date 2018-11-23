/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar

import android.view.View
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases

/**
 * Connects an [AwesomeBar] with a [Toolbar] and allows adding multiple [AwesomeBar.SuggestionProvider] implementations.
 */
class AwesomeBarFeature(
    private val awesomeBar: AwesomeBar,
    private val toolbar: Toolbar,
    private val engineView: EngineView? = null
) {
    init {
        toolbar.setOnEditListener(object : mozilla.components.concept.toolbar.Toolbar.OnEditListener {
            override fun onTextChanged(text: String) = awesomeBar.onInputChanged(text)

            override fun onStartEditing() = showAwesomeBar()

            override fun onStopEditing() = hideAwesomeBar()
        })

        awesomeBar.setOnStopListener { toolbar.displayMode() }
    }

    /**
     * Add a [AwesomeBar.SuggestionProvider] for "Open tabs" to the [AwesomeBar].
     */
    fun addSessionProvider(
        sessionManager: SessionManager,
        selectTabUseCase: TabsUseCases.SelectTabUseCase
    ): AwesomeBarFeature {
        val provider = SessionSuggestionProvider(sessionManager, selectTabUseCase)
        awesomeBar.addProviders(provider)
        return this
    }

    /**
     * Add a [AwesomeBar.SuggestionProvider] for search engine suggestions to the [AwesomeBar].
     */
    fun addSearchProvider(
        searchEngine: SearchEngine,
        searchUseCase: SearchUseCases.DefaultSearchUseCase
    ): AwesomeBarFeature {
        awesomeBar.addProviders(SearchSuggestionProvider(searchEngine, searchUseCase))
        return this
    }

    /**
     * Add a [AwesomeBar.SuggestionProvider] for browsing history to the [AwesomeBar].
     */
    fun addHistoryProvider(
        historyStorage: HistoryStorage,
        loadUrlUseCase: SessionUseCases.LoadUrlUseCase
    ): AwesomeBarFeature {
        awesomeBar.addProviders(HistoryStorageSuggestionProvider(historyStorage, loadUrlUseCase))
        return this
    }

    private fun showAwesomeBar() {
        awesomeBar.asView().visibility = View.VISIBLE
        engineView?.asView()?.visibility = View.GONE
        awesomeBar.onInputStarted()
    }

    private fun hideAwesomeBar() {
        awesomeBar.asView().visibility = View.GONE
        engineView?.asView()?.visibility = View.VISIBLE
        awesomeBar.onInputCancelled()
    }
}
