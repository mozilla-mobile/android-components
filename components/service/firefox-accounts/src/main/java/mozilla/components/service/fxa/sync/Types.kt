package mozilla.components.service.fxa.sync

import mozilla.appservices.syncmanager.SyncAuthInfo
import mozilla.components.service.fxa.SyncEngine

/**
 * Conversion from our SyncAuthInfo into its "native" version used at the interface boundary.
 */
internal fun mozilla.components.concept.sync.SyncAuthInfo.toNative(): SyncAuthInfo {
    return SyncAuthInfo(
        kid = this.kid,
        fxaAccessToken = this.fxaAccessToken,
        syncKey = this.syncKey,
        tokenserverURL = this.tokenServerUrl
    )
}

fun String.toSyncEngine(): SyncEngine {
    return when (this) {
        "history" -> SyncEngine.History
        "bookmarks" -> SyncEngine.Bookmarks
        "passwords" -> SyncEngine.Passwords
        else -> SyncEngine.Other(this)
    }
}

fun List<String>.toSyncEngines(): Set<SyncEngine> {
    return this.map { it.toSyncEngine() }.toSet()
}