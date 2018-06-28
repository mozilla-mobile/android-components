/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.fxa

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import mozilla.components.fxa.FirefoxAccount
import android.support.customtabs.CustomTabsIntent
import android.view.View
import android.widget.Button
import mozilla.components.fxa.Config
import mozilla.components.fxa.FxaClient
import android.content.Intent
import android.content.SharedPreferences
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import android.widget.TextView

open class MainActivity : AppCompatActivity() {

    private var account: FirefoxAccount? = null
    private var config: Config? = null

    companion object {
        const val CLIENT_ID = "12cc4070a481bc73"
        const val REDIRECT_URL = "fxaclient://android.redirect"
        const val CONFIG_URL = "https://latest.dev.lcip.org"
        const val PREFS_NAME = "fxaAppState"
        const val FXA_STATE_KEY = "fxaState"

        init {
            FxaClient.init()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(FXA_STATE_KEY, "").let {
                Log.i("profile on startup", it)
                account = FirefoxAccount.fromJSONString(it)
                displayProfile()
            }
        } catch (e: IllegalArgumentException) {
            config = Config.custom(CONFIG_URL)
            config?.let {
                account = FirefoxAccount(it, CLIENT_ID)

                val btn = findViewById<View>(R.id.button) as Button
                btn.setOnClickListener {
                    account?.beginOAuthFlow(REDIRECT_URL, arrayOf("profile"), false)?.let {
                        openAuthTab(it)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        val data = intent.dataString

        if (Intent.ACTION_VIEW == action && data != null) {
            if (authenticate(data)) {
                displayProfile()
                account?.toJSONString().let {
                    Log.e("account logged in", it)
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(FXA_STATE_KEY, it).commit()
                }
            }
        }
    }

    private fun displayProfile() {
        val txtView: TextView = findViewById(R.id.txtView)
        val profile = account?.getProfile() ?: throw IllegalArgumentException("Profile expected")
        txtView.text = "${profile?.displayName ?: ""} ${profile?.email}"
    }

    private fun authenticate(redirectUrl: String): Boolean {
        val url = Uri.parse(redirectUrl)
        val code = url.getQueryParameter("code")
        val state = url.getQueryParameter("state")

        account?.completeOAuthFlow(code, state)
        return true
    }

    private fun openAuthTab(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
                .addDefaultShareMenuItem()
                .setShowTitle(true)
                .build()

        customTabsIntent.intent.data = Uri.parse(url)
        customTabsIntent.launchUrl(this@MainActivity, Uri.parse(url))
    }
}
