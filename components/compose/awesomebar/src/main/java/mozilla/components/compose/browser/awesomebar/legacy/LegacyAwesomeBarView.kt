/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.browser.awesomebar.legacy

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import mozilla.components.concept.awesomebar.AwesomeBar

private typealias EditSuggestionListener = ((String) -> Unit)?
private typealias StopListener = (() -> Unit)?

/**
 * This class is a wrapper for the `AwesomeBar()` composable in order to render it inside a
 * [View]-based UI and interact with code depending on an [AwesomeBar] concept implementation.
 * It's primary use case is for switching to the `AwesomeBar()` composable implementation before
 * having refactored all UI code to use Jetpack Compose.
 */
abstract class LegacyAwesomeBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr), AwesomeBar {
    private val providers = mutableStateOf(emptyList<AwesomeBar.SuggestionProvider>())
    private val text = mutableStateOf("")
    private var onEditSuggestionListener = mutableStateOf<EditSuggestionListener>(null)
    private var onStopListener = mutableStateOf<StopListener>(null)

    @Composable
    override fun Content() {
        if (providers.value.isEmpty()) {
            return
        }

        Render(
            providers.value,
            text.value,
            onEditSuggestionListener.value,
            onStopListener.value
        )
    }

    /**
     * The Jetpack Compose UI content for this view. Subclasses are expected to call into the
     * `AwesomeBar()` composable from this code. This method will get recomposed if any of the
     * passed in values change.
     */
    @Composable
    abstract fun Render(
        providers: List<AwesomeBar.SuggestionProvider>,
        text: String,
        onEditSuggestionListener: ((String) -> Unit)?,
        onStopListener: (() -> Unit)?
    )

    override fun addProviders(vararg providers: AwesomeBar.SuggestionProvider) {
        val newProviders = this.providers.value.toMutableList()
        newProviders.addAll(providers)
        this.providers.value = newProviders
    }

    override fun containsProvider(provider: AwesomeBar.SuggestionProvider): Boolean {
        return providers.value.any { current -> current.id == provider.id }
    }

    override fun onInputChanged(text: String) {
        this.text.value = text
    }

    override fun removeAllProviders() {
        providers.value = emptyList()
    }

    override fun removeProviders(vararg providers: AwesomeBar.SuggestionProvider) {
        val newProviders = this.providers.value.toMutableList()
        newProviders.removeAll(providers)
        this.providers.value = newProviders
    }

    override fun setOnEditSuggestionListener(listener: (String) -> Unit) {
        onEditSuggestionListener.value = listener
    }

    override fun setOnStopListener(listener: () -> Unit) {
        onStopListener.value = listener
    }
}
