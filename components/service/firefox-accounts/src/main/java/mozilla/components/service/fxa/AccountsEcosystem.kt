package mozilla.components.service.fxa

import android.content.ContentProviderClient
import android.content.Context
import android.net.Uri
import mozilla.components.support.base.log.logger.Logger

data class EcosystemAccount(
    val email: String,
    val sessionToken: String? = null,
    val kSync: String? = null,
    val kXSCS: String? = null
)

private data class ProviderClient(val provider: String, val contentClient: ContentProviderClient)

private const val KEY_EMAIL = "email"
private const val KEY_SESSION_TOKEN = "sessionToken"
private const val KEY_KSYNC = "kSync"
private const val KEY_KXSCS = "kXSCS"

class AccountsEcosystem(context: Context, private val ecosystemProviders: List<String>) {
    private val logger = Logger("AccountsEcosystem")

    private val contentResolver = context.contentResolver

    fun queryAccount(): EcosystemAccount? {
        return ecosystemProviders.firstTrustedProviderClient()?.let { queryForAccount(it) }
    }

    private fun queryForAccount(client: ProviderClient): EcosystemAccount? {
        val authAuthority = client.provider.authAuthority()
        val authAuthorityUri = Uri.parse("content://$authAuthority")
        val authStateUri = Uri.withAppendedPath(authAuthorityUri, "state")

        client.contentClient.query(
            authStateUri,
            arrayOf(KEY_EMAIL, KEY_SESSION_TOKEN, KEY_KSYNC, KEY_KXSCS),
            null, null, null
        ).use { cursor ->
            // Could not read account from the provider. Either it's logged out, or in a bad state.
            if (cursor == null) {
                return null
            }

            cursor.moveToFirst()

            val email = cursor.getString(cursor.getColumnIndex(KEY_EMAIL))
            val sessionToken = cursor.getBlob(cursor.getColumnIndex(KEY_SESSION_TOKEN))
            val kSync = cursor.getBlob(cursor.getColumnIndex(KEY_KSYNC))
            val kXSCS = cursor.getString(cursor.getColumnIndex(KEY_KXSCS))

            if (email != null && sessionToken != null && kSync != null && kXSCS != null) {
                return EcosystemAccount(email, String(sessionToken), String(kSync), kXSCS)
            } else if (email != null) {
                return EcosystemAccount(email)
            }
        }
        return null
    }

    private fun List<String>.firstTrustedProviderClient(): ProviderClient? {
        for (testedProvider in this) {
            val c = testedProvider.contentProvider()
            if (!isTrustedProvider(testedProvider)) {
                logger.warn("$testedProvider failed a trust check")
                continue
            }
            if (c != null) {
                return ProviderClient(testedProvider, c)
            }
        }
        return null
    }

    private fun isTrustedProvider(provider: String): Boolean {
        // TODO see https://bugzilla.mozilla.org/show_bug.cgi?id=1545232 for how to implement this
        return false
    }

    private fun String.contentProvider(): ContentProviderClient? {
        return contentResolver.acquireContentProviderClient(this.authAuthority())
    }

    private fun String.authAuthority(): String {
        return "$this.fxa.auth"
    }
}