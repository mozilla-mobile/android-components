/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.autofill.response.fill

import android.content.Context
import android.os.Build
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import mozilla.components.concept.storage.Login
import mozilla.components.feature.autofill.AutofillConfiguration
import mozilla.components.feature.autofill.response.dataset.LoginDatasetBuilder
import mozilla.components.feature.autofill.response.dataset.SearchDatasetBuilder
import mozilla.components.feature.autofill.structure.ParsedStructure

/**
 * [FillResponseBuilder] implementation that creates a [FillResponse] containing logins for
 * autofilling.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal data class LoginFillResponseBuilder(
    val parsedStructure: ParsedStructure,
    val logins: List<Login>,
    val needsConfirmation: Boolean
) : FillResponseBuilder {
    private val searchDatasetBuilder = SearchDatasetBuilder(parsedStructure)

    override fun build(
        context: Context,
        configuration: AutofillConfiguration,
        imeSpec: InlinePresentationSpec?
    ): FillResponse {
        val builder = FillResponse.Builder()

        logins.forEachIndexed { index, login ->
            val datasetBuilder = LoginDatasetBuilder(
                parsedStructure,
                login,
                needsConfirmation,
                requestOffset = index
            )

            val dataset = datasetBuilder.build(
                context,
                configuration,
                imeSpec
            )

            builder.addDataset(dataset)
        }

        builder.addDataset(
            searchDatasetBuilder.build(context, configuration, imeSpec)
        )

        builder.handleSaveInfo(parsedStructure)

        return builder.build()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun FillResponse.Builder.handleSaveInfo(ps: ParsedStructure) {
    val ids = arrayOf(ps.usernameId, ps.passwordId).filterNotNull()
    if (ids.isNotEmpty()) {
        setSaveInfo(
            SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                ids.toTypedArray()
            ).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ps.passwordId == null) {
                    // Continue workflow until password field is reached
                    it.setFlags(SaveInfo.FLAG_DELAY_SAVE)
                }
            }.build()
        )
    }
}
