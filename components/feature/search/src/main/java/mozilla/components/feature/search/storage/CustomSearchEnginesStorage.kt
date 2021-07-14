/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.search.storage

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.feature.search.middleware.SearchMiddleware
import java.io.File
import kotlin.coroutines.CoroutineContext

internal const val SEARCH_FILE_EXTENSION = ".xml"
internal const val SEARCH_DIR_NAME = "search-engines"

/**
 * A storage implementation for organizing [SearchEngine]s. Its primary use case is for persisting
 * custom search engines added by users.
 */
internal class CustomSearchEngineStorage(
    private val context: Context,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : SearchMiddleware.CustomStorage {
    private val reader = SearchEngineReader(SearchEngine.Type.CUSTOM)
    private val writer = SearchEngineWriter()

    override suspend fun loadSearchEngineList(): List<SearchEngine> = withContext(coroutineContext) {
        val result = getFileDirectory().listFiles()?.map {
            val filename = it.name.removeSuffix(SEARCH_FILE_EXTENSION)
            val identifier = String(Base64.decode(filename, Base64.NO_WRAP or Base64.URL_SAFE))
            loadSearchEngineAsync(identifier)
        } ?: emptyList()

        result.awaitAll()
    }

    suspend fun loadSearchEngine(identifier: String): SearchEngine = withContext(coroutineContext) {
        loadSearchEngineAsync(identifier).await()
    }

    override suspend fun saveSearchEngine(searchEngine: SearchEngine): Boolean = withContext(coroutineContext) {
        writer.saveSearchEngineXML(searchEngine, getSearchFile(searchEngine.id))
    }

    override suspend fun removeSearchEngine(identifier: String) = withContext(coroutineContext) {
        getSearchFile(identifier).delete()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getSearchFile(identifier: String): AtomicFile {
        val encodedId = Base64.encodeToString(identifier.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
        return AtomicFile(File(getFileDirectory(), encodedId + SEARCH_FILE_EXTENSION))
    }

    @WorkerThread
    private fun loadSearchEngineAsync(
        identifier: String
    ): Deferred<SearchEngine> = GlobalScope.async(coroutineContext) {
        reader.loadFile(identifier, getSearchFile(identifier))
    }

    private fun getFileDirectory(): File =
        File(context.filesDir, SEARCH_DIR_NAME).also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
}
