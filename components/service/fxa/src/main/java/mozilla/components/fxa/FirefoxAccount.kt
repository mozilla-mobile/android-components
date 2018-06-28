/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.fxa

import android.text.TextUtils
import android.util.Log

class FirefoxAccount(override var rawPointer: FxaClient.RawFxAccount?) : RustObject<FxaClient.RawFxAccount>() {

    constructor(config: Config, clientId: String): this(null) {
        val e = Error.ByReference()
        val result = FxaClient.INSTANCE.fxa_new(config.consumePointer(), clientId, e)
        if (e.isSuccess()) {
            this.rawPointer = result
        } else {
            Log.e("FirefoxAccount.init", e.consumeMessage())
            this.rawPointer = null
        }
    }

    override fun destroyPointer(fxa: FxaClient.RawFxAccount) {
        FxaClient.INSTANCE.fxa_free(fxa)
    }

    fun beginOAuthFlow(redirectURI: String, scopes: Array<String>, wantsKeys: Boolean): String? {
        val scope = TextUtils.join(" ", scopes)
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_begin_oauth_flow(this.validPointer()!!, redirectURI, scope, wantsKeys, e)
        if (e.isSuccess()) {
            return getAndConsumeString(p)
        } else {
            Log.e("fxa.beginOAuthFlow", e.consumeMessage())
            return null
        }
    }

    fun getProfile(ignoreCache: Boolean): Profile? {
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_profile(this.validPointer(), ignoreCache, e)
        if (e.isSuccess()) {
            return Profile(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    fun newAssertion(audience: String): String? {
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_assertion_new(this.validPointer(), audience, e)
        if (e.isSuccess()) {
            return getAndConsumeString(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    fun getTokenServerEndpointURL(): String? {
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_get_token_server_endpoint_url(this.validPointer(), e)
        if (e.isSuccess()) {
            return getAndConsumeString(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    fun toJSONString(): String? {
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_to_json(this.validPointer(), e)
        if (e.isSuccess()) {
            return getAndConsumeString(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    fun getSyncKeys(): SyncKeys? {
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_get_sync_keys(this.validPointer(), e)
        if (e.isSuccess()) {
            return SyncKeys(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    fun getProfile(): Profile? {
        return getProfile(false)
    }

    fun completeOAuthFlow(code: String, state: String): OAuthInfo? {
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_complete_oauth_flow(this.validPointer(), code, state, e)
        if (e.isSuccess()) {
            return OAuthInfo(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    fun getOAuthToken(scopes: Array<String>): OAuthInfo? {
        val scope = TextUtils.join(" ", scopes)
        val e = Error.ByReference()
        val p = FxaClient.INSTANCE.fxa_get_oauth_token(this.validPointer(), scope, e)
        if (e.isSuccess()) {
            return OAuthInfo(p)
        } else {
            Log.e("FirefoxAccount", e.consumeMessage())
            return null
        }
    }

    companion object {
        fun from(config: Config, clientId: String, webChannelResponse: String): FirefoxAccount? {
            val e = Error.ByReference()
            val raw = FxaClient.INSTANCE.fxa_from_credentials(config.consumePointer(), clientId, webChannelResponse, e)
            if (e.isSuccess()) {
                return FirefoxAccount(raw)
            } else {
                Log.e("fxa.from", e.consumeMessage())
                return null
            }
        }

        fun fromJSONString(json: String): FirefoxAccount? {
            val e = Error.ByReference()
            val raw = FxaClient.INSTANCE.fxa_from_json(json, e)
            if (e.isSuccess()) {
                return FirefoxAccount(raw)
            } else {
                Log.e("fxa.fromJSONString", e.consumeMessage())
                return null
            }
        }
    }
}
