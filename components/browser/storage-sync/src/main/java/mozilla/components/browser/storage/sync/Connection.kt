/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.storage.sync

import androidx.annotation.GuardedBy
import mozilla.appservices.places.PlacesApi
import mozilla.appservices.places.PlacesReaderConnection
import mozilla.appservices.places.PlacesWriterConnection
import mozilla.appservices.sync15.FailureName
import mozilla.appservices.sync15.FailureReason
import mozilla.appservices.sync15.SyncTelemetryPing
import mozilla.components.browser.storage.sync.GleanMetrics.BookmarksSync
import mozilla.components.browser.storage.sync.GleanMetrics.HistorySync
import mozilla.components.browser.storage.sync.GleanMetrics.Pings
import java.io.Closeable
import java.io.File

const val DB_NAME = "places.sqlite"
const val MAX_FAILURE_REASON_LENGTH = 100

/**
 * A slight abstraction over [PlacesApi].
 *
 * A single reader is assumed here, which isn't a limitation placed on use by [PlacesApi].
 * We can switch to pooling multiple readers as the need arises. Underneath, these are connections
 * to a SQLite database, and so opening and maintaining them comes with a memory/IO burden.
 *
 * Writer is always the same, as guaranteed by [PlacesApi].
 */
internal interface Connection : Closeable {
    fun reader(): PlacesReaderConnection
    fun writer(): PlacesWriterConnection

    // Until we get a real SyncManager in application-services libraries, we'll have to live with this
    // strange split that doesn't quite map all that well to our internal storage model.
    fun syncHistory(syncInfo: SyncAuthInfo)
    fun syncBookmarks(syncInfo: SyncAuthInfo)

    // These are implemented as default methods on `Connection` instead of
    // `RustPlacesConnection` to make testing easier.
    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun assembleHistoryPing(ping: SyncTelemetryPing) {
        ping.syncs.forEach eachSync@{ sync ->
            sync.failureReason?.let {
                recordHistoryFailureReason(it)
                sendHistoryPing()
                return@eachSync
            }
            sync.engines.forEach eachEngine@{ engine ->
                if (engine.name != "history") {
                    return@eachEngine
                }
                HistorySync.apply {
                    val base = BaseGleanSyncPing.fromEngineInfo(ping.uid, engine)
                    uid.set(base.uid)
                    startedAt.set(base.startedAt)
                    finishedAt.set(base.finishedAt)
                    if (base.applied > 0) {
                        // Since all Sync ping counters have `lifetime: ping`, and
                        // we send the ping immediately after, we don't need to
                        // reset the counters before calling `add`.
                        incoming["applied"].add(base.applied)
                    }
                    if (base.failedToApply > 0) {
                        incoming["failed_to_apply"].add(base.failedToApply)
                    }
                    if (base.reconciled > 0) {
                        incoming["reconciled"].add(base.reconciled)
                    }
                    if (base.uploaded > 0) {
                        outgoing["uploaded"].add(base.uploaded)
                    }
                    if (base.failedToUpload > 0) {
                        outgoing["failed_to_upload"].add(base.failedToUpload)
                    }
                    if (base.outgoingBatches > 0) {
                        outgoingBatches.add(base.outgoingBatches)
                    }
                    base.failureReason?.let {
                        recordHistoryFailureReason(it)
                    }
                }
                sendHistoryPing()
            }
        }
    }
    fun sendHistoryPing() {}

    // This function is almost identical to `recordHistoryPing`, with additional
    // reporting for validation problems. Unfortunately, since the
    // `BookmarksSync` and `HistorySync` metrics are two separate objects, we
    // can't factor this out into a generic function.
    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun assembleBookmarksPing(ping: SyncTelemetryPing) {
        ping.syncs.forEach eachSync@{ sync ->
            sync.failureReason?.let {
                // If the entire sync fails, don't try to unpack the ping; just
                // report the error and bail.
                recordBookmarksFailureReason(it)
                sendBookmarksPing()
                return@eachSync
            }
            sync.engines.forEach eachEngine@{ engine ->
                if (engine.name != "bookmarks") {
                    return@eachEngine
                }
                BookmarksSync.apply {
                    val base = BaseGleanSyncPing.fromEngineInfo(ping.uid, engine)
                    uid.set(base.uid)
                    startedAt.set(base.startedAt)
                    finishedAt.set(base.finishedAt)
                    if (base.applied > 0) {
                        incoming["applied"].add(base.applied)
                    }
                    if (base.failedToApply > 0) {
                        incoming["failed_to_apply"].add(base.failedToApply)
                    }
                    if (base.reconciled > 0) {
                        incoming["reconciled"].add(base.reconciled)
                    }
                    if (base.uploaded > 0) {
                        outgoing["uploaded"].add(base.uploaded)
                    }
                    if (base.failedToUpload > 0) {
                        outgoing["failed_to_upload"].add(base.failedToUpload)
                    }
                    if (base.outgoingBatches > 0) {
                        outgoingBatches.add(base.outgoingBatches)
                    }
                    base.failureReason?.let {
                        recordBookmarksFailureReason(it)
                    }
                    engine.validation?.let {
                        it.problems.forEach {
                            remoteTreeProblems[it.name].add(it.count)
                        }
                    }
                }
                sendBookmarksPing()
            }
        }
    }
    fun sendBookmarksPing() {}
}

/**
 * A singleton implementation of the [Connection] interface backed by the Rust Places library.
 */
internal object RustPlacesConnection : Connection {
    @GuardedBy("this")
    private var api: PlacesApi? = null

    @GuardedBy("this")
    private var cachedReader: PlacesReaderConnection? = null

    /**
     * Creates a long-lived [PlacesApi] instance, and caches a reader connection.
     * Writer connection is maintained by [PlacesApi] itself, and is created upon its initialization.
     *
     * @param parentDir Location of the parent directory in which database is/will be stored.
     */
    fun init(parentDir: File) = synchronized(this) {
        if (api == null) {
            api = PlacesApi(File(parentDir, DB_NAME).canonicalPath)
        }
        cachedReader = api!!.openReader()
    }

    override fun reader(): PlacesReaderConnection = synchronized(this) {
        check(cachedReader != null) { "must call init first" }
        return cachedReader!!
    }

    override fun writer(): PlacesWriterConnection {
        check(api != null) { "must call init first" }
        return api!!.getWriter()
    }

    override fun syncHistory(syncInfo: SyncAuthInfo) {
        check(api != null) { "must call init first" }
        val ping = api!!.syncHistory(syncInfo)
        assembleHistoryPing(ping)
    }

    override fun sendHistoryPing() {
        Pings.historySync.send()
    }

    override fun syncBookmarks(syncInfo: SyncAuthInfo) {
        check(api != null) { "must call init first" }
        val ping = api!!.syncBookmarks(syncInfo)
        assembleBookmarksPing(ping)
    }

    override fun sendBookmarksPing() {
        Pings.bookmarksSync.send()
    }

    override fun close() = synchronized(this) {
        check(api != null) { "must call init first" }
        api!!.close()
        api = null
    }
}

private fun recordHistoryFailureReason(reason: FailureReason) {
    val metric = when (reason.name) {
        FailureName.Other, FailureName.Unknown -> HistorySync.failureReason["other"]
        FailureName.Unexpected, FailureName.Http -> HistorySync.failureReason["unexpected"]
        FailureName.Auth -> HistorySync.failureReason["auth"]
        FailureName.Shutdown -> return
    }
    val message = reason.message ?: "Unexpected error: ${reason.code}"
    metric.set(message.take(MAX_FAILURE_REASON_LENGTH))
}

private fun recordBookmarksFailureReason(reason: FailureReason) {
    val metric = when (reason.name) {
        FailureName.Other, FailureName.Unknown -> BookmarksSync.failureReason["other"]
        FailureName.Unexpected, FailureName.Http -> BookmarksSync.failureReason["unexpected"]
        FailureName.Auth -> BookmarksSync.failureReason["auth"]
        FailureName.Shutdown -> return
    }
    val message = reason.message ?: "Unexpected error: ${reason.code}"
    metric.set(message.take(MAX_FAILURE_REASON_LENGTH))
}
