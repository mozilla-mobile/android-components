/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.session.storage.serialize

import android.util.AtomicFile
import android.util.JsonReader
import android.util.JsonToken
import mozilla.components.browser.session.storage.RecoverableBrowserState
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.recover.RecoverableTab
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSessionState
import mozilla.components.support.ktx.android.util.nextBooleanOrNull
import mozilla.components.support.ktx.android.util.nextStringOrNull
import mozilla.components.support.ktx.util.readJSON
import java.util.UUID

/**
 * Reads a [RecoverableBrowserState] (partial, serialized [BrowserState]) or a single [RecoverableTab]
 * (partial, serialized [TabSessionState]) from disk.
 */
class BrowserStateReader {
    /**
     * Reads a serialized [RecoverableBrowserState] from the given [AtomicFile].
     *
     * @param engine The [Engine] implementation for restoring the engine state.
     * @param file The [AtomicFile] to read the the recoverable state from.
     * @param predicate an optional predicate applied to each tab to determine if it should be restored.
     */
    fun read(
        engine: Engine,
        file: AtomicFile,
        predicate: (RecoverableTab) -> Boolean = { true }
    ): RecoverableBrowserState? {
        return file.readJSON { browsingSession(
            engine,
            restoreSessionId = true,
            restoreParentId = true,
            predicate = predicate)
        }
    }

    /**
     * Reads a single [RecoverableTab] from the given [file].
     *
     * @param engine The [Engine] implementation for restoring the engine state.
     * @param restoreSessionId Whether the original tab ID should be restored or whether a new ID
     * should be generated for the tab.
     * @param restoreParentId Whether the original parent tab ID should be restored or whether it
     * should be set to `null`.
     */
    fun readTab(
        engine: Engine,
        file: AtomicFile,
        restoreSessionId: Boolean = true,
        restoreParentId: Boolean = true
    ): RecoverableTab? {
        return file.readJSON { tab(engine, restoreSessionId, restoreParentId) }
    }
}

@Suppress("ComplexMethod")
private fun JsonReader.browsingSession(
    engine: Engine,
    restoreSessionId: Boolean = true,
    restoreParentId: Boolean = true,
    predicate: (RecoverableTab) -> Boolean = { true }
): RecoverableBrowserState? {
    beginObject()

    var version = 1 // Initially we didn't save a version. If there's none then we assume it is version 1.
    var tabs: List<RecoverableTab>? = null
    var selectedIndex: Int? = null
    var selectedTabId: String? = null

    while (hasNext()) {
        when (nextName()) {
            Keys.VERSION_KEY -> version = nextInt()
            Keys.SELECTED_SESSION_INDEX_KEY -> selectedIndex = nextInt()
            Keys.SELECTED_TAB_ID_KEY -> selectedTabId = nextStringOrNull()
            Keys.SESSION_STATE_TUPLES_KEY -> tabs = tabs(engine, restoreSessionId, restoreParentId, predicate)
        }
    }

    endObject()

    if (selectedTabId == null && version == 1 && selectedIndex != null) {
        // In the first version we only saved the selected index in the list of all tabs instead
        // of the ID of the selected tab. If we come across such an older version then we try
        // to find the tab and determine the selected ID ourselves.
        selectedTabId = tabs?.getOrNull(selectedIndex)?.id
    }

    return if (tabs != null && tabs.isNotEmpty()) {
        // Check if selected tab still exists after restoring/filtering and
        // use most recently accessed tab otherwise.
        if (tabs.find { it.id == selectedTabId } == null) {
            selectedTabId = tabs.sortedByDescending { it.lastAccess }.first().id
        }

        RecoverableBrowserState(tabs, selectedTabId)
    } else {
        null
    }
}

private fun JsonReader.tabs(
    engine: Engine,
    restoreSessionId: Boolean = true,
    restoreParentId: Boolean = true,
    predicate: (RecoverableTab) -> Boolean = { true }
): List<RecoverableTab> {
    beginArray()

    val tabs = mutableListOf<RecoverableTab>()
    while (peek() != JsonToken.END_ARRAY) {
        val tab = tab(engine, restoreSessionId, restoreParentId)
        if (tab != null && predicate(tab)) {
            tabs.add(tab)
        }
    }

    endArray()

    return tabs
}

private fun JsonReader.tab(
    engine: Engine,
    restoreSessionId: Boolean = true,
    restoreParentId: Boolean = true
): RecoverableTab? {
    beginObject()

    var engineSessionState: EngineSessionState? = null
    var tab: RecoverableTab? = null

    while (hasNext()) {
        when (nextName()) {
            Keys.SESSION_KEY -> tab = tabSession()
            Keys.ENGINE_SESSION_KEY -> engineSessionState = engine.createSessionStateFrom(this)
        }
    }

    endObject()

    return tab?.copy(
        state = engineSessionState,
        id = if (restoreSessionId) tab.id else UUID.randomUUID().toString(),
        parentId = if (restoreParentId) tab.parentId else null
    )
}

@Suppress("ComplexMethod")
private fun JsonReader.tabSession(): RecoverableTab? {
    var id: String? = null
    var parentId: String? = null
    var url: String? = null
    var title: String? = null
    var contextId: String? = null
    var lastAccess: Long? = null

    var readerStateActive: Boolean? = null
    var readerActiveUrl: String? = null

    beginObject()

    while (hasNext()) {
        when (val name = nextName()) {
            Keys.SESSION_URL_KEY -> url = nextString()
            Keys.SESSION_UUID_KEY -> id = nextString()
            Keys.SESSION_CONTEXT_ID_KEY -> contextId = nextStringOrNull()
            Keys.SESSION_PARENT_UUID_KEY -> parentId = nextStringOrNull()?.takeIf { it.isNotEmpty() }
            Keys.SESSION_TITLE -> title = nextStringOrNull() ?: ""
            Keys.SESSION_READER_MODE_KEY -> readerStateActive = nextBooleanOrNull()
            Keys.SESSION_READER_MODE_ACTIVE_URL_KEY -> readerActiveUrl = nextStringOrNull()
            Keys.SESSION_LAST_ACCESS -> lastAccess = nextLong()
            Keys.SESSION_SOURCE_KEY -> nextString()
            else -> throw IllegalArgumentException("Unknown session key: $name")
        }
    }

    endObject()

    return RecoverableTab(
        id = requireNotNull(id),
        parentId = parentId,
        url = requireNotNull(url),
        title = requireNotNull(title),
        contextId = contextId,
        state = null, // This will be deserialized and added separately
        readerState = ReaderState(
            active = readerStateActive ?: false,
            activeUrl = readerActiveUrl
        ),
        private = false, // We never serialize private sessions
        lastAccess = lastAccess ?: 0
    )
}
