[android-components](../../index.md) / [mozilla.components.browser.storage.sync](../index.md) / [PlacesBookmarksStorage](./index.md)

# PlacesBookmarksStorage

`open class PlacesBookmarksStorage : `[`PlacesStorage`](../-places-storage/index.md)`, `[`BookmarksStorage`](../../mozilla.components.concept.storage/-bookmarks-storage/index.md)`, `[`SyncableStore`](../../mozilla.components.concept.sync/-syncable-store/index.md) [(source)](https://github.com/mozilla-mobile/android-components/blob/master/components/browser/storage-sync/src/main/java/mozilla/components/browser/storage/sync/PlacesBookmarksStorage.kt#L28)

Implementation of the [BookmarksStorage](../../mozilla.components.concept.storage/-bookmarks-storage/index.md) which is backed by a Rust Places lib via [PlacesApi](#).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `PlacesBookmarksStorage(context: `[`Context`](https://developer.android.com/reference/android/content/Context.html)`)`<br>Implementation of the [BookmarksStorage](../../mozilla.components.concept.storage/-bookmarks-storage/index.md) which is backed by a Rust Places lib via [PlacesApi](#). |

### Properties

| Name | Summary |
|---|---|
| [logger](logger.md) | `open val logger: `[`Logger`](../../mozilla.components.support.base.log.logger/-logger/index.md) |

### Functions

| Name | Summary |
|---|---|
| [addFolder](add-folder.md) | `open suspend fun addFolder(parentGuid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, title: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Adds a new bookmark folder to a given node. |
| [addItem](add-item.md) | `open suspend fun addItem(parentGuid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, title: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Adds a new bookmark item to a given node. |
| [addSeparator](add-separator.md) | `open suspend fun addSeparator(parentGuid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, position: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`?): `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Adds a new bookmark separator to a given node. |
| [deleteNode](delete-node.md) | `open suspend fun deleteNode(guid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Deletes a bookmark node and all of its children, if any. |
| [getBookmark](get-bookmark.md) | `open suspend fun getBookmark(guid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`BookmarkNode`](../../mozilla.components.concept.storage/-bookmark-node/index.md)`?`<br>Obtains the details of a bookmark without children, if one exists with that guid. Otherwise, null. |
| [getBookmarksWithUrl](get-bookmarks-with-url.md) | `open suspend fun getBookmarksWithUrl(url: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`BookmarkNode`](../../mozilla.components.concept.storage/-bookmark-node/index.md)`>`<br>Produces a list of all bookmarks with the given URL. |
| [getTree](get-tree.md) | `open suspend fun getTree(guid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, recursive: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): `[`BookmarkNode`](../../mozilla.components.concept.storage/-bookmark-node/index.md)`?`<br>Produces a bookmarks tree for the given guid string. |
| [searchBookmarks](search-bookmarks.md) | `open suspend fun searchBookmarks(query: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, limit: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`BookmarkNode`](../../mozilla.components.concept.storage/-bookmark-node/index.md)`>`<br>Searches bookmarks with a query string. |
| [sync](sync.md) | `open suspend fun sync(authInfo: `[`AuthInfo`](../../mozilla.components.concept.sync/-auth-info/index.md)`): `[`SyncStatus`](../../mozilla.components.concept.sync/-sync-status/index.md)<br>Runs syncBookmarks() method on the places Connection |
| [updateNode](update-node.md) | `open suspend fun updateNode(guid: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, info: `[`BookmarkInfo`](../../mozilla.components.concept.storage/-bookmark-info/index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Edits the properties of an existing bookmark item and/or moves an existing one underneath a new parent guid. |

### Inherited Functions

| Name | Summary |
|---|---|
| [cleanup](../-places-storage/cleanup.md) | `open fun cleanup(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Cleans up background work and database connections |
| [ignoreUrlExceptions](../-places-storage/ignore-url-exceptions.md) | `fun ignoreUrlExceptions(operation: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Runs [block](../-places-storage/ignore-url-exceptions.md#mozilla.components.browser.storage.sync.PlacesStorage$ignoreUrlExceptions(kotlin.String, kotlin.Function0((kotlin.Unit)))/block) described by [operation](../-places-storage/ignore-url-exceptions.md#mozilla.components.browser.storage.sync.PlacesStorage$ignoreUrlExceptions(kotlin.String, kotlin.Function0((kotlin.Unit)))/operation), ignoring and logging any thrown [UrlParseFailed](#) exceptions. |
| [runMaintenance](../-places-storage/run-maintenance.md) | `open suspend fun runMaintenance(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Internal database maintenance tasks. Ideally this should be called once a day. |
| [syncAndHandleExceptions](../-places-storage/sync-and-handle-exceptions.md) | `fun syncAndHandleExceptions(syncBlock: () -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`SyncStatus`](../../mozilla.components.concept.sync/-sync-status/index.md)<br>Runs a [syncBlock](../-places-storage/sync-and-handle-exceptions.md#mozilla.components.browser.storage.sync.PlacesStorage$syncAndHandleExceptions(kotlin.Function0((kotlin.Unit)))/syncBlock), re-throwing any panics that may be encountered. |
