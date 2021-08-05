/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.state.recover

import mozilla.components.browser.state.state.LastMediaAccessState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.SessionState.Source
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.concept.storage.HistoryMetadataKey

/**
 * A tab that is no longer open and in the list of tabs, but that can be restored (recovered) at
 * any time.
 *
 * The values of this data class are usually filled with the values of a [TabSessionState] when
 * getting closed.
 *
 * @property id Unique ID identifying this tab.
 * @property url The last URL of this tab.
 * @property parentId The unique ID of the parent tab if this tab was opened from another tab (e.g. via
 * the context menu).
 * @property title The last title of this tab (or an empty String).
 * @property contextId The context ID ("container") this tab used (or null).
 * @property state The [EngineSessionState] needed for restoring the previous state of this tab.
 * @property readerState The last [ReaderState] of the tab.
 * @property lastAccess The last time this tab was selected.
 * @property createdAt Timestamp of the tab's creation.
 * @property lastMediaAccessState Details about the last time was playing in this tab.
 * @property private If tab was private.
 * @property historyMetadata The last [HistoryMetadataKey] of the tab.
 * @property source The last [Source] of the tab.
 */
data class RecoverableTab(
    val id: String,
    val url: String,
    val parentId: String? = null,
    val title: String = "",
    val contextId: String? = null,
    val state: EngineSessionState? = null,
    val readerState: ReaderState = ReaderState(),
    val lastAccess: Long = 0,
    val createdAt: Long = 0,
    val lastMediaAccessState: LastMediaAccessState = LastMediaAccessState(),
    val private: Boolean = false,
    val historyMetadata: HistoryMetadataKey? = null,
    val source: Source = Source.Internal.None
)

/**
 * Creates a [RecoverableTab] from this [TabSessionState].
 */
fun TabSessionState.toRecoverableTab() = RecoverableTab(
    id = id,
    parentId = parentId,
    url = content.url,
    title = content.title,
    contextId = contextId,
    state = engineState.engineSessionState,
    readerState = readerState,
    lastAccess = lastAccess,
    createdAt = createdAt,
    lastMediaAccessState = lastMediaAccessState,
    private = content.private,
    historyMetadata = historyMetadata,
    source = source
)

/**
 * Creates a [TabSessionState] from this [RecoverableTab].
 */
fun RecoverableTab.toTabSessionState() = createTab(
    id = id,
    url = url,
    parentId = parentId,
    title = title,
    contextId = contextId,
    engineSessionState = state,
    readerState = readerState,
    lastAccess = lastAccess,
    createdAt = createdAt,
    lastMediaAccessState = lastMediaAccessState,
    historyMetadata = historyMetadata,
    source = source,
    restored = true
)

/**
 * Creates a list of [TabSessionState]s from a List of [RecoverableTab]s.
 */
fun List<RecoverableTab>.toTabSessionStates() = map { it.toTabSessionState() }
