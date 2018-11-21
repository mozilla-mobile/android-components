package mozilla.components.service.sync.logins

import mozilla.appservices.logins.ServerPassword
import mozilla.appservices.logins.SyncUnlockInfo

/**
 * Entrypoint for integrating with Firefox Sync for logins.
 *
 * @property store store to save login information.
 */
class SyncLoginsClient(private val store: AsyncLoginsStorage) {

    /**
     * Perform a sync, and then get the local password list. Unlocks the store if necessary. Note
     * that this may reject with a [SyncAuthInvalidException], in which case the oauth token may
     * need to be refreshed.
     *
     * @param accessToken This should be produced by completing an OAuth
     * flow using FxA (service-firefox-accounts) for flows begun with `wantsKeys = true`,
     * that requested (at least) the scopes `https://identity.mozilla.com/apps/oldsync`
     * or `https://identity.mozilla.com/apps/lockbox`.
     * @param syncKeyId JWK key identifier for sync key.
     * @param syncKey JWK key data for sync key.
     * @param encryptionKey A secret key protecting the store.
     * @param tokenServerURL This should be the result of calling `getTokenServerEndpointURL` on
     * an authenticated Firefox Account.
     *
     * @return the list of logins ([ServerPassword]s).
     */
    suspend fun syncAndGetPasswords(
        accessToken: String,
        syncKeyId: String,
        syncKey: String,
        encryptionKey: String,
        tokenServerURL: String
    ): List<ServerPassword> {

        val unlockInfo = SyncUnlockInfo(
            kid = syncKeyId,
            fxaAccessToken = accessToken,
            syncKey = syncKey,
            tokenserverURL = tokenServerURL
        )

        val store = getUnlockedStore(encryptionKey)
        store.sync(unlockInfo).await()

        return getLocalPasswordList(encryptionKey)
    }

    private suspend fun getLocalPasswordList(encryptionKey: String): List<ServerPassword> {
        val store = getUnlockedStore(encryptionKey)
        return store.list().await()
    }

    private suspend fun getUnlockedStore(encryptionKey: String): AsyncLoginsStorage {
        if (this.store.isLocked()) {
            this.store.unlock(encryptionKey).await()
        }
        return this.store
    }
}
