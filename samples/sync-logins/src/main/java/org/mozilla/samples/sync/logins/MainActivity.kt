/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.sync.logins

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.v4.content.ContextCompat
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import mozilla.components.service.fxa.Config
import mozilla.components.service.fxa.FirefoxAccount
import mozilla.components.service.fxa.FxaException
import mozilla.components.service.sync.logins.AsyncLoginsStorageAdapter
import mozilla.components.service.sync.logins.SyncLoginsClient
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineContext

open class MainActivity : AppCompatActivity(), LoginFragment.OnLoginCompleteListener, CoroutineScope {

    private var scopes: Array<String> = arrayOf("profile", "https://identity.mozilla.com/apps/oldsync")
    private var wantsKeys: Boolean = true

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var activityContext: MainActivity
    private lateinit var whenAccount: Deferred<FirefoxAccount>

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        const val CLIENT_ID = "12cc4070a481bc73"
        const val REDIRECT_URL = "fxaclient://android.redirect"
        const val CONFIG_URL = "https://latest.dev.lcip.org"
        const val FXA_STATE_PREFS_KEY = "fxaAppState"
        const val FXA_STATE_KEY = "fxaState"
        const val STORE_PATH = "/logins.sqlite"
        const val SCOPE = "https://identity.mozilla.com/apps/oldsync"
        // Hardcoded to keep the example simple. IRL this should be
        // fetched from a secure storage (or the user)
        const val SECRET_KEY = "my_cool_secret_key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        job = Job()

        listView = findViewById(R.id.logins_list_view)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        activityContext = this

        whenAccount = async {
            try {
                val savedJSON = getSharedPreferences(FXA_STATE_PREFS_KEY, Context.MODE_PRIVATE)
                        .getString(FXA_STATE_KEY, "")
                FirefoxAccount.fromJSONString(savedJSON)
            } catch (e: FxaException) {
                Config.custom(CONFIG_URL).await().use { config ->
                    FirefoxAccount(config, CLIENT_ID, REDIRECT_URL)
                }
            }
        }

        findViewById<View>(R.id.buttonWebView).setOnClickListener {
            launch {
                val url = whenAccount.await().beginOAuthFlow(scopes, wantsKeys).await()
                openWebView(url)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (whenAccount.isCompleted) {
            whenAccount.getCompleted().close()
        } else {
            whenAccount.cancel()
        }
        job.cancel()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        val data = intent.dataString

        if (Intent.ACTION_VIEW == action && data != null) {
            val url = Uri.parse(data)
            val code = url.getQueryParameter("code")
            val state = url.getQueryParameter("state")
            displayAndPersistProfile(code, state)
        }
    }

    private fun openWebView(url: String) {
        supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.container, LoginFragment.create(url, REDIRECT_URL))
            addToBackStack(null)
            commit()
        }
    }

    override fun onLoginComplete(code: String, state: String, fragment: LoginFragment) {
        displayAndPersistProfile(code, state)
        supportFragmentManager?.popBackStack()
    }

    private fun displayAndPersistProfile(code: String, state: String) {
        launch {
            val account = whenAccount.await()
            val oauthInfo = account.completeOAuthFlow(code, state).await()

            val permissionCheck = ContextCompat.checkSelfPermission(this@MainActivity, WRITE_EXTERNAL_STORAGE)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED && VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE), 1)
            }

            val dbPath = "${applicationContext.getExternalFilesDir(null).absolutePath}$STORE_PATH"
            AsyncLoginsStorageAdapter.forDatabase(dbPath).use { store ->
                val keyInfo = oauthInfo.keys?.get(SCOPE) ?: throw IllegalStateException("Key info is missing")

                val syncedLogins = SyncLoginsClient(store).syncAndGetPasswords(
                    oauthInfo.accessToken,
                    keyInfo.kid,
                    keyInfo.k,
                    SECRET_KEY,
                    account.getTokenServerEndpointURL()
                )

                Toast.makeText(this@MainActivity, "Logins success", Toast.LENGTH_SHORT).show()
                syncedLogins.forEach {
                    adapter.add("Login: " + it.hostname)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}
