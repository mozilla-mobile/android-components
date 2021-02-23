/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.intent.processing

import android.app.SearchManager
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.ACTION_SEARCH
import android.content.Intent.ACTION_WEB_SEARCH
import android.content.Intent.EXTRA_TEXT
import android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED
import mozilla.components.browser.state.state.SessionState.Source
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.WebURLFinder

/**
 * Processor for intents which should trigger session-related actions.
 *
 * @property tabsUseCases An instance of [TabsUseCases] used to open new tabs.
 * @property loadUrlUseCase A reference to [SessionUseCases.DefaultLoadUrlUseCase] used to load URLs.
 * @property newTabSearchUseCase A reference to [SearchUseCases.NewTabSearchUseCase] to be used for
 * ACTION_SEND intents if the provided text is not a URL.
 * @property isPrivate Whether a processed intent should open a new tab as private
 */
class TabIntentProcessor(
    private val tabsUseCases: TabsUseCases,
    private val loadUrlUseCase: SessionUseCases.DefaultLoadUrlUseCase,
    private val newTabSearchUseCase: SearchUseCases.NewTabSearchUseCase,
    private val isPrivate: Boolean = false
) : IntentProcessor {

    /**
     * Loads a URL from a view intent in a new session.
     */
    private fun processViewIntent(intent: SafeIntent): Boolean {
        val url = intent.dataString

        return if (url.isNullOrEmpty()) {
            false
        } else {
            tabsUseCases.selectOrAddTab(
                url,
                private = isPrivate,
                source = Source.ACTION_VIEW,
                flags = LoadUrlFlags.external()
            )
            true
        }
    }

    /**
     * Processes a send intent and tries to load [EXTRA_TEXT] as a URL.
     * If its not a URL, a search is run instead.
     */
    private fun processSendIntent(intent: SafeIntent): Boolean {
        val extraText = intent.getStringExtra(EXTRA_TEXT)

        return if (extraText.isNullOrBlank()) {
            false
        } else {
            val url = WebURLFinder(extraText).bestWebURL()
            if (url != null) {
                addNewTab(url, Source.ACTION_SEND)
            } else {
                newTabSearchUseCase(extraText, Source.ACTION_SEND)
            }
            true
        }
    }

    private fun processSearchIntent(intent: SafeIntent): Boolean {
        val searchQuery = intent.getStringExtra(SearchManager.QUERY)

        return if (searchQuery.isNullOrBlank()) {
            false
        } else {
            if (searchQuery.isUrl()) {
                addNewTab(searchQuery, Source.ACTION_SEARCH)
            } else {
                newTabSearchUseCase(searchQuery, Source.ACTION_SEARCH)
            }
            true
        }
    }

    private fun addNewTab(url: String, source: Source) {
        if (isPrivate) {
            tabsUseCases.addPrivateTab(url, source = source, flags = LoadUrlFlags.external())
        } else {
            tabsUseCases.addTab(url, source = source, flags = LoadUrlFlags.external())
        }
    }

    /**
     * Processes the given intent by invoking the registered handler.
     *
     * @param intent the intent to process
     * @return true if the intent was processed, otherwise false.
     */
    override fun process(intent: Intent): Boolean {
        val safeIntent = SafeIntent(intent)
        return when (safeIntent.action) {
            ACTION_VIEW, ACTION_NDEF_DISCOVERED -> processViewIntent(safeIntent)
            ACTION_SEND -> processSendIntent(safeIntent)
            ACTION_SEARCH, ACTION_WEB_SEARCH -> processSearchIntent(safeIntent)
            else -> false
        }
    }
}
