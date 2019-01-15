/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.awesomebar.provider

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.utils.WebURLFinder

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

/**
 * An [AwesomeBar.SuggestionProvider] implementation that returns a suggestions for an URL in the clipboard (if there's
 * any).
 */
class ClipboardSuggestionProvider(
    context: Context,
    private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase
) : AwesomeBar.SuggestionProvider {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        val url = getTextFromClipboard(clipboardManager)?.let {
            findUrl(it)
        } ?: return emptyList()

        return listOf(AwesomeBar.Suggestion(
            id = "mozac-feature-awesomebar-clipboard",
            description = url,
            flags = setOf(AwesomeBar.Suggestion.Flag.CLIPBOARD),
            onSuggestionClicked = {
                loadUrlUseCase.invoke(url)
            }
        ))
    }
}

private fun findUrl(text: String): String? {
    val finder = WebURLFinder(text)
    return finder.bestWebURL()
}

private fun getTextFromClipboard(clipboardManager: ClipboardManager): String? {
    if (clipboardManager.isPrimaryClipEmpty() || !clipboardManager.isPrimaryClipPlainText()) {
        // We only care about a primary clip with type "text/plain"
        return null
    }

    return clipboardManager.firstPrimaryClipItem?.text?.toString()
}

private fun ClipboardManager.isPrimaryClipPlainText() =
    primaryClipDescription?.hasMimeType(MIME_TYPE_TEXT_PLAIN) ?: false

private fun ClipboardManager.isPrimaryClipEmpty() = primaryClip?.itemCount == 0

private val ClipboardManager.firstPrimaryClipItem: ClipData.Item?
    get() = primaryClip?.getItemAt(0)
