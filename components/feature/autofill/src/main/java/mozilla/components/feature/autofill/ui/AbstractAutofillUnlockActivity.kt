/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.autofill.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.autofill.FillResponse
import android.view.autofill.AutofillManager
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mozilla.components.feature.autofill.AutofillConfiguration
import mozilla.components.feature.autofill.authenticator.Authenticator
import mozilla.components.feature.autofill.authenticator.createAuthenticator
import mozilla.components.feature.autofill.facts.emitAutofillLock
import mozilla.components.feature.autofill.handler.FillRequestHandler
import mozilla.components.feature.autofill.handler.MAX_LOGINS
import mozilla.components.feature.autofill.structure.ParsedStructure

/**
 * Activity responsible for unlocking the autofill service by asking the user to verify with a
 * fingerprint or alternative device unlocking mechanism.
 */
@RequiresApi(Build.VERSION_CODES.O)
abstract class AbstractAutofillUnlockActivity : FragmentActivity() {
    abstract val configuration: AutofillConfiguration

    private var fillResponse: Deferred<FillResponse?>? = null
    private val fillHandler by lazy { FillRequestHandler(context = this, configuration) }
    private val authenticator: Authenticator? by lazy { createAuthenticator(this, configuration) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parsedStructure = intent.getParcelableExtra<ParsedStructure>(EXTRA_PARSED_STRUCTURE)
        val imeSpec = intent.getImeSpec()
        val maxSuggestionCount = intent.getIntExtra(EXTRA_MAX_SUGGESTION_COUNT, MAX_LOGINS)
        // While the user is asked to authenticate, we already try to build the fill response asynchronously.
        if (parsedStructure != null) {
            fillResponse = lifecycleScope.async(Dispatchers.IO) {
                val builder = fillHandler.handle(parsedStructure, forceUnlock = true, maxSuggestionCount)
                val result = builder.build(this@AbstractAutofillUnlockActivity, configuration, imeSpec)
                result
            }
        }

        if (authenticator == null) {
            // If no authenticator is available then we just bail here. Instead we should ask the user to
            // enroll, or show an error message instead.
            // https://github.com/mozilla-mobile/android-components/issues/9756
            setResult(RESULT_CANCELED)
            finish()
        } else {
            authenticator!!.prompt(this, PromptCallback())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authenticator?.onActivityResult(requestCode, resultCode)
    }

    internal inner class PromptCallback : Authenticator.Callback {
        override fun onAuthenticationError() {
            fillResponse?.cancel()

            emitAutofillLock(unlocked = false)

            setResult(RESULT_CANCELED)
            finish()
        }

        override fun onAuthenticationSucceeded() {
            configuration.lock.unlock()

            val replyIntent = Intent().apply {
                // At this point it should be safe to block since the fill response should be ready once
                // the user has authenticated.
                runBlocking { putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse?.await()) }
            }

            emitAutofillLock(unlocked = true)

            setResult(RESULT_OK, replyIntent)
            finish()
        }

        override fun onAuthenticationFailed() {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    companion object {
        const val EXTRA_PARSED_STRUCTURE = "parsed_structure"
        const val EXTRA_IME_SPEC = "ime_spec"
        const val EXTRA_MAX_SUGGESTION_COUNT = "max_suggestion_count"
    }
}

internal fun Intent.getImeSpec(): InlinePresentationSpec? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return getParcelableExtra(AbstractAutofillUnlockActivity.EXTRA_IME_SPEC)
    } else {
        return null
    }
}
