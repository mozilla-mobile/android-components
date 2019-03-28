/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.storage

/**
 * An interface which defines read/write operations for bookmarks data.
 */
interface BookmarksStorage : Storage {

    /**
     * Produces a bookmarks tree for the given guid string.
     *
     * @param guid The bookmark guid to obtain.
     * @param recursive Whether to recurse and obtain all levels of children.
     * @return The populated root starting from the guid.
     */
    suspend fun getTree(guid: String, recursive: Boolean = false): BookmarkNode?

    /**
     * Searches bookmarks with a query string.
     *
     * @param query The query string to search.
     * @param limit The maximum number of entries to return.
     * @return The list of matching bookmark nodes up to the limit number of items.
     */
    suspend fun searchBookmarks(query: String, limit: Int = defaultBookmarkSearchLimit): List<BookmarkNode>

    /**
     * Adds a new bookmark item to a given node.
     *
     * Sync behavior: will add new bookmark item to remote devices.
     *
     * @param parentGuid The parent guid of the new node.
     * @param url The URL of the bookmark item to add.
     * @param title The title of the bookmark item to add.
     * @param position The optional position to add the new node or null to append.
     * @return The guid of the newly inserted bookmark item.
     */
    suspend fun addItem(parentGuid: String, url: String, title: String, position: Int?): String

    /**
     * Adds a new bookmark folder to a given node.
     *
     * Sync behavior: will add new separator to remote devices.
     *
     * @param parentGuid The parent guid of the new node.
     * @param title The title of the bookmark folder to add.
     * @param position The optional position to add the new node or null to append.
     * @return The guid of the newly inserted bookmark item.
     */
    suspend fun addFolder(parentGuid: String, title: String, position: Int? = null): String

    /**
     * Adds a new bookmark separator to a given node.
     *
     * Sync behavior: will add new separator to remote devices.
     *
     * @param parentGuid The parent guid of the new node.
     * @param position The optional position to add the new node or null to append.
     * @return The guid of the newly inserted bookmark item.
     */
    suspend fun addSeparator(parentGuid: String, position: Int?): String

    /**
     * Edits the properties of an existing bookmark item and/or moves an existing one underneath a new parent guid.
     *
     * Sync behavior: will alter bookmark item on remote devices.
     *
     * @param guid The guid of the item to update.
     * @param info The info to change in the bookmark.
     */
    suspend fun updateNode(guid: String, info: BookmarkInfo)

    /**
     * Deletes a bookmark node and all of its children, if any.
     *
     * Sync behavior: will remove bookmark from remote devices.
     *
     * @return Whether the bookmark existed or not.
     */
    suspend fun deleteNode(guid: String): Boolean

    companion object {
        const val defaultBookmarkSearchLimit = 10
    }
}

/**
 * Class for holding metadata about any bookmark node
 */
data class BookmarkNode(
    val type: BookmarkNodeType,
    val guid: String,
    val parentGuid: String?,
    val position: Int?,
    val title: String?,
    val url: String?,
    val children: List<BookmarkNode?>?
)

/**
 * Class for making alterations to any bookmark node
 */
data class BookmarkInfo(
    val parentGuid: String?,
    val position: Int?,
    val title: String?,
    val url: String?
)

/**
 * The types of bookmark nodes
 */
enum class BookmarkNodeType {
    ITEM, FOLDER, SEPARATOR
}
