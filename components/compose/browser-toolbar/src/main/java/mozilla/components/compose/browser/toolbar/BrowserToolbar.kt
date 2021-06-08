/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.browser.toolbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import mozilla.components.browser.state.helper.Target
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.session.SessionUseCases

/**
 * A customizable toolbar for browsers.
 *
 * The toolbar can switch between two modes: display and edit. The display mode displays the current
 * URL and controls for navigation. In edit mode the current URL can be edited. Those two modes are
 * implemented by the [BrowserDisplayToolbar] and [BrowserEditToolbar] composables.
 */
@Composable
fun BrowserToolbar(
    store: BrowserStore,
    useCases: SessionUseCases,
    target: Target
) {
    val editMode = remember { mutableStateOf(false) }
    val selectedTab: SessionState? by target.observeAsStateFrom(
        store = store,
        observe = { tab -> tab?.content?.url }
    )

    if (editMode.value) {
        BrowserEditToolbar(
            url = selectedTab?.content?.url ?: "<empty>",
            onUrlCommitted = { text ->
                useCases.loadUrl(text)
                editMode.value = false
            }
        )
    } else {
        BrowserDisplayToolbar(
            url = selectedTab?.content?.url ?: "<empty>",
            onUrlClicked = { editMode.value = true }
        )
    }
}
