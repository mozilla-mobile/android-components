/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.feature.autofill.handler.FillRequestHandler
import mozilla.components.feature.autofill.handler.MAX_LOGINS
import mozilla.components.feature.autofill.structure.ParsedStructure
import mozilla.components.feature.autofill.structure.getLookupDomain
import mozilla.components.feature.autofill.structure.parseStructure
import mozilla.components.feature.autofill.structure.toRawStructure

/**
 * Service responsible for implementing Android's Autofill framework.
 */
@RequiresApi(Build.VERSION_CODES.O)
abstract class AbstractAutofillService : AutofillService() {
    abstract val configuration: AutofillConfiguration

    private val fillHandler by lazy { FillRequestHandler(context = this, configuration) }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        // We are using GlobalScope here instead of a scope bound to the service since the service
        // seems to get destroyed before we invoke a method on the callback. So we need a scope that
        // lives longer than the service.
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            // You may be wondering why we translate the AssistStructure into a RawStructure and then
            // create a FillResponseBuilder that outputs the FillResponse. This is purely for testing.
            // Neither AssistStructure nor FillResponse can be created by us and they do not let us
            // inspect their data. So we create these intermediate objects that we can create and
            // inspect in unit tests.
            val structure = request.fillContexts.last().structure.toRawStructure()
            val responseBuilder = fillHandler.handle(
                structure,
                maxSuggestionCount = request.getMaxSuggestionCount()
            )
            val response = responseBuilder?.build(
                this@AbstractAutofillService,
                configuration,
                request.getInlinePresentationSpec()
            )
            callback.onSuccess(response)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        var username: String? = null
        var password: String? = null
        var parsedStructure: ParsedStructure? = null
        for (fillContext in request.fillContexts) {
            parsedStructure = parseStructure(this, fillContext.structure.toRawStructure())
            parsedStructure?.also { ps ->
                ps.username?.let { username = it }
                ps.password?.let { password = it }
            }
        }
        parsedStructure ?: return
        username ?: return
        password ?: return

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            val lookupDomain = parsedStructure.getLookupDomain(configuration.publicSuffixList)
            val loginEntry = LoginEntry(
                origin = "https://$lookupDomain",
                username = username!!,
                password = password!!,
                httpRealm = "https://$lookupDomain"
            )
            configuration.storage.addOrUpdate(loginEntry)
        }

        callback.onSuccess()
    }
}

internal fun FillRequest.getInlinePresentationSpec(): InlinePresentationSpec? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return inlineSuggestionsRequest?.inlinePresentationSpecs?.last()
    } else {
        return null
    }
}

internal fun FillRequest.getMaxSuggestionCount() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    (inlineSuggestionsRequest?.maxSuggestionCount ?: 1) - 1 // space for search chip
} else {
    MAX_LOGINS
}
