package mozilla.components.service.fxa.sync

import mozilla.appservices.syncmanager.SyncAuthInfo

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