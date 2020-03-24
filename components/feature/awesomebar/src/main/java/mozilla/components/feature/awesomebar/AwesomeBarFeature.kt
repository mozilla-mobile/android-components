/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar

import android.content.Context
import android.content.res.Resources
import android.view.View
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.SearchEngineManager
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.feature.awesomebar.provider.ClipboardSuggestionProvider
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
    private val engineView: EngineView? = null,
    private val icons: BrowserIcons? = null,
    onEditStart: (() -> Unit)? = null,
    onEditComplete: (() -> Unit)? = null
) {
    init {
        toolbar.setOnEditListener(ToolbarEditListener(
            awesomeBar,
            onEditStart,
            onEditComplete,
            ::showAwesomeBar,
            ::hideAwesomeBar
        ))

        awesomeBar.setOnStopListener { toolbar.displayMode() }
    }

    /**
     * Add a [AwesomeBar.SuggestionProvider] for "Open tabs" to the [AwesomeBar].
     */
    fun addSessionProvider(
        resources: Resources,
        store: BrowserStore,
        selectTabUseCase: TabsUseCases.SelectTabUseCase
    ): AwesomeBarFeature {
        val provider = SessionSuggestionProvider(resources, store, selectTabUseCase, icons)
        awesomeBar.addProviders(provider)
        return this
    }

    /**
     * Adds a [AwesomeBar.SuggestionProvider] for search engine suggestions to the [AwesomeBar].
     *
     * @param searchEngine The search engine to request suggestions from.
     * @param searchUseCase The use case to invoke for searches.
     * @param fetchClient The HTTP client for requesting suggestions from the search engine.
     * @param limit The maximum number of suggestions that should be returned. It needs to be >= 1.
     * @param mode Whether to return a single search suggestion (with chips) or one suggestion per item.
     * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
     * highest scored search suggestion URL.
     */
    @Suppress("LongParameterList")
    fun addSearchProvider(
        searchEngine: SearchEngine,
        searchUseCase: SearchUseCases.SearchUseCase,
        fetchClient: Client,
        limit: Int = 15,
        mode: SearchSuggestionProvider.Mode = SearchSuggestionProvider.Mode.SINGLE_SUGGESTION,
        engine: Engine? = null
    ): AwesomeBarFeature {
        awesomeBar.addProviders(SearchSuggestionProvider(searchEngine, searchUseCase, fetchClient, limit, mode, engine))
        return this
    }

    /**
     * Adds a [AwesomeBar.SuggestionProvider] for search engine suggestions to the [AwesomeBar].
     * If the default search engine is to be used for fetching search engine suggestions then
     * this method is preferable over [addSearchProvider], as it will lazily load the default
     * search engine using the provided [SearchEngineManager].
     *
     * @param context the activity or application context, required to load search engines.
     * @param searchEngineManager The search engine manager to look up search engines.
     * @param searchUseCase The use case to invoke for searches.
     * @param fetchClient The HTTP client for requesting suggestions from the search engine.
     * @param limit The maximum number of suggestions that should be returned. It needs to be >= 1.
     * @param mode Whether to return a single search suggestion (with chips) or one suggestion per item.
     * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
     * highest scored search suggestion URL.
     */
    @Suppress("LongParameterList")
    fun addSearchProvider(
        context: Context,
        searchEngineManager: SearchEngineManager,
        searchUseCase: SearchUseCases.SearchUseCase,
        fetchClient: Client,
        limit: Int = 15,
        mode: SearchSuggestionProvider.Mode = SearchSuggestionProvider.Mode.SINGLE_SUGGESTION,
        engine: Engine? = null
    ): AwesomeBarFeature {
        awesomeBar.addProviders(
            SearchSuggestionProvider(context, searchEngineManager, searchUseCase, fetchClient, limit, mode, engine)
        )
        return this
    }

    /**
     * Add a [AwesomeBar.SuggestionProvider] for browsing history to the [AwesomeBar].
     *
     * @param historyStorage and instance of the [BookmarksStorage] used to query matching bookmarks.
     * @param loadUrlUseCase the use case invoked to load the url when the user clicks on the suggestion.
     * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
     * highest scored suggestion URL.
     */
    fun addHistoryProvider(
        historyStorage: HistoryStorage,
        loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
        engine: Engine? = null
    ): AwesomeBarFeature {
        awesomeBar.addProviders(HistoryStorageSuggestionProvider(historyStorage, loadUrlUseCase, icons, engine))
        return this
    }

    /**
     * Add a [AwesomeBar.SuggestionProvider] for clipboard items to the [AwesomeBar].
     *
     * @param context the activity or application context, required to look up the clipboard manager.
     * @param loadUrlUseCase the use case invoked to load the url when
     * the user clicks on the suggestion.
     * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
     * highest scored suggestion URL.
     */
    fun addClipboardProvider(
        context: Context,
        loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
        engine: Engine? = null
    ): AwesomeBarFeature {
        awesomeBar.addProviders(ClipboardSuggestionProvider(context, loadUrlUseCase, engine = engine))
        return this
    }

    private fun showAwesomeBar() {
        awesomeBar.asView().visibility = View.VISIBLE
        engineView?.asView()?.visibility = View.GONE
    }

    private fun hideAwesomeBar() {
        awesomeBar.asView().visibility = View.GONE
        engineView?.asView()?.visibility = View.VISIBLE
    }
}

internal class ToolbarEditListener(
    private val awesomeBar: AwesomeBar,
    private val onEditStart: (() -> Unit)? = null,
    private val onEditComplete: (() -> Unit)? = null,
    private val showAwesomeBar: () -> Unit,
    private val hideAwesomeBar: () -> Unit
) : Toolbar.OnEditListener {
    private var inputStarted = false

    override fun onTextChanged(text: String) {
        if (inputStarted) {
            awesomeBar.onInputChanged(text)
        }
    }

    override fun onStartEditing() {
        onEditStart?.invoke() ?: showAwesomeBar()
        awesomeBar.onInputStarted()
        inputStarted = true
    }

    override fun onStopEditing() {
        onEditComplete?.invoke() ?: hideAwesomeBar()
        awesomeBar.onInputCancelled()
        inputStarted = false
    }

    override fun onCancelEditing(): Boolean {
        return true
    }
}
