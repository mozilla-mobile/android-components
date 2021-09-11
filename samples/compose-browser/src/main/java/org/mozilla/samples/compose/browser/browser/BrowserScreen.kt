/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.compose.browser.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import mozilla.components.browser.state.helper.Target
import mozilla.components.compose.browser.awesomebar.AwesomeBar
import mozilla.components.compose.browser.toolbar.BrowserToolbar
import mozilla.components.compose.engine.WebContent
import mozilla.components.compose.tabstray.TabCounterButton
import mozilla.components.compose.tabstray.TabList
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.feature.awesomebar.provider.ClipboardSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchActionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.lib.state.Store
import mozilla.components.lib.state.ext.composableStore
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.samples.compose.browser.BrowserComposeActivity.Companion.ROUTE_SETTINGS
import org.mozilla.samples.compose.browser.components

/**
 * The main browser screen.
 */
@Composable
fun BrowserScreen(navController: NavController) {
    val target = Target.SelectedTab

    val store = composableStore<BrowserScreenState, BrowserScreenAction> { restoredState ->
        BrowserScreenStore(restoredState ?: BrowserScreenState())
    }

    val editState = store.observeAsComposableState { state -> state.editMode }
    val editUrl = store.observeAsComposableState { state -> state.editText }
    val loadUrl = components().sessionUseCases.loadUrl
    val showTabs = store.observeAsComposableState { state -> state.showTabs }

    BackHandler(enabled = editState.value == true) {
        store.dispatch(BrowserScreenAction.ToggleEditMode(false))
    }

    Box {
        Column {
            BrowserToolbar(
                components().store,
                target,
                editMode = editState.value!!,
                onDisplayMenuClicked = {
                    navController.navigate(ROUTE_SETTINGS)
                },
                onTextCommit = { text ->
                    store.dispatch(BrowserScreenAction.ToggleEditMode(false))
                    loadUrl(text)
                },
                onTextEdit = { text -> store.dispatch(BrowserScreenAction.UpdateEditText(text)) },
                onDisplayToolbarClick = {
                    store.dispatch(BrowserScreenAction.ToggleEditMode(true))
                },
                editText = editUrl.value,
                hint = "Search or enter address",
                browserActions = {
                    TabCounterButton(
                        components().store,
                        onClicked = { store.dispatch(BrowserScreenAction.ShowTabs) }
                    )
                }
            )

            Box {
                WebContent(
                    components().engine,
                    components().store,
                    Target.SelectedTab
                )

                val url = editUrl.value
                if (editState.value == true && url != null) {
                    Suggestions(
                        url,
                        onSuggestionClicked = { suggestion ->
                            store.dispatch(BrowserScreenAction.ToggleEditMode(false))
                            suggestion.onSuggestionClicked?.invoke()
                        }
                    )
                }
            }
        }

        if (showTabs.value == true) {
            TabsTray(store)
        }
    }
}

/**
 * Shows the lit of tabs.
 */
@Composable
fun TabsTray(
    store: Store<BrowserScreenState, BrowserScreenAction>
) {
    val components = components()

    BackHandler(onBack = { store.dispatch(BrowserScreenAction.HideTabs) })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = ContentAlpha.medium))
            .clickable {
                store.dispatch(BrowserScreenAction.HideTabs)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(fraction = 0.8f)
                .align(Alignment.BottomStart)
        ) {
            TabList(
                store = components().store,
                onTabSelected = { tab ->
                    components.tabsUseCases.selectTab(tab.id)
                    store.dispatch(BrowserScreenAction.HideTabs)
                },
                onTabClosed = { tab ->
                    components.tabsUseCases.removeTab(tab.id)
                },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    components.tabsUseCases.addTab(
                        url = "about:blank",
                        selectTab = true
                    )
                    store.dispatch(BrowserScreenAction.HideTabs)
                    store.dispatch(BrowserScreenAction.ToggleEditMode(true))
                }
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun Suggestions(
    url: String,
    onSuggestionClicked: (AwesomeBar.Suggestion) -> Unit
) {
    val context = LocalContext.current
    val components = components()

    val sessionSuggestionProvider = remember(context) {
        SessionSuggestionProvider(
            context.resources,
            components.store,
            components.tabsUseCases.selectTab
        )
    }

    val searchActionProvider = remember {
        SearchActionProvider(components.store, components.searchUseCases.defaultSearch)
    }

    val searchSuggestionProvider = remember(context) {
        SearchSuggestionProvider(
            context,
            components.store,
            components.searchUseCases.defaultSearch,
            components.client,
            mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
            engine = components.engine,
            filterExactMatch = true
        )
    }

    val clipboardSuggestionProvider = remember(context) {
        ClipboardSuggestionProvider(
            context,
            components.sessionUseCases.loadUrl
        )
    }

    AwesomeBar(
        url,
        providers = listOf(
            sessionSuggestionProvider,
            searchActionProvider,
            searchSuggestionProvider,
            clipboardSuggestionProvider
        ),
        onSuggestionClicked = { suggestion -> onSuggestionClicked(suggestion) }
    )
}
