/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.awesomebar

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import mozilla.components.compose.browser.awesomebar.AwesomeBar
import mozilla.components.compose.browser.awesomebar.legacy.LegacyAwesomeBarView
import mozilla.components.concept.awesomebar.AwesomeBar

/**
 * This wrapper wraps the `AwesomeBar()` composable and exposes it as a `View` and `concept-awesomebar`
 * implementation.
 */
class AwesomeBarWrapper@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LegacyAwesomeBarView(context, attrs, defStyleAttr) {
    @Composable
    override fun Render(
        providers: List<AwesomeBar.SuggestionProvider>,
        text: String,
        onEditSuggestionListener: ((String) -> Unit)?,
        onStopListener: (() -> Unit)?
    ) {
        AwesomeBar(
            text = text,
            providers = providers,
            onSuggestionClicked = { suggestion ->
                suggestion.onSuggestionClicked?.invoke()
                onStopListener?.invoke()
            },
            onAutoComplete = { suggestion ->
                onEditSuggestionListener?.invoke(suggestion.editSuggestion!!)
            }
        )
    }
}
