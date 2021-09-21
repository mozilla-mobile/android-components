/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.autofill.response.dataset

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.BlendMode
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.InlinePresentation
import android.text.TextUtils
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import android.widget.inline.InlinePresentationSpec
import androidx.annotation.RequiresApi
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import mozilla.components.concept.storage.Login
import mozilla.components.feature.autofill.AutofillConfiguration
//import mozilla.components.feature.autofill.R
import mozilla.components.feature.autofill.handler.EXTRA_LOGIN_ID
import mozilla.components.feature.autofill.structure.ParsedStructure

@RequiresApi(Build.VERSION_CODES.O)
internal data class LoginDatasetBuilder(
    val parsedStructure: ParsedStructure,
    val login: Login,
    val needsConfirmation: Boolean,
    val requestOffset: Int = 0
) : DatasetBuilder {

    @SuppressLint("NewApi")
    override fun build(
        context: Context,
        configuration: AutofillConfiguration,
        imeSpec: InlinePresentationSpec?
    ): Dataset {
        val dataset = Dataset.Builder()

        val usernamePresentation = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        usernamePresentation.setTextViewText(android.R.id.text1, login.usernamePresentationOrFallback(context))
        val passwordPresentation = RemoteViews(context.packageName, android.R.layout.simple_list_item_1)
        passwordPresentation.setTextViewText(android.R.id.text1, login.passwordPresentation(context))

        var usernameInlinePresentation: InlinePresentation? = null
        var passwordInlinePresentation: InlinePresentation? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && imeSpec != null
            && canUseInlineSuggestions(imeSpec)
        ) {
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            //val icon: Icon = Icon.createWithResource(context, R.drawable.ic_cc_logo_mastercard)
            val usernameSlice = createSlice(
                login.usernamePresentationOrFallback(context),
                //startIcon = icon,
                attribution = pendingIntent
            )
            val passwordSlice = createSlice(
                login.passwordPresentation(context),
                //startIcon = icon,
                attribution = pendingIntent
            )
            usernameInlinePresentation = InlinePresentation(usernameSlice, imeSpec, false)
            passwordInlinePresentation = InlinePresentation(passwordSlice, imeSpec, false)
        }

        parsedStructure.usernameId?.let { id ->
            dataset.setValue(
                id,
                if (needsConfirmation) null else AutofillValue.forText(login.username),
                usernamePresentation,
                usernameInlinePresentation
            )
        }

        parsedStructure.passwordId?.let { id ->
            dataset.setValue(
                id,
                if (needsConfirmation) null else AutofillValue.forText(login.password),
                passwordPresentation,
                passwordInlinePresentation
            )
        }

        if (needsConfirmation) {
            val confirmIntent = Intent(context, configuration.confirmActivity)
            confirmIntent.putExtra(EXTRA_LOGIN_ID, login.guid)

            val intentSender: IntentSender = PendingIntent.getActivity(
                context,
                configuration.activityRequestCode + requestOffset,
                confirmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            ).intentSender

            dataset.setAuthentication(intentSender)
        }

        return dataset.build()
    }
}

internal fun Login.usernamePresentationOrFallback(context: Context): String {
    return if (username.isNotEmpty()) {
        username
    } else {
        context.getString(mozilla.components.feature.autofill.R.string.mozac_feature_autofill_popup_no_username)
    }
}

private fun Login.passwordPresentation(context: Context): String {
    return context.getString(
        mozilla.components.feature.autofill.R.string.mozac_feature_autofill_popup_password,
        usernamePresentationOrFallback(context)
    )
}

@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.R)
fun createSlice(
    title: CharSequence,
    subtitle: CharSequence = "",
    startIcon: Icon? = null,
    endIcon: Icon? = null,
    contentDescription: CharSequence = "",
    attribution: PendingIntent
): Slice {
    // Build the content for the v1 UI.
    val builder = InlineSuggestionUi.newContentBuilder(attribution)
        .setContentDescription(contentDescription);
    if (!TextUtils.isEmpty(title)) {
        builder.setTitle(title);
    }
    if (!TextUtils.isEmpty(subtitle)) {
        builder.setSubtitle(subtitle);
    }
    if (startIcon != null) {
        startIcon.setTintBlendMode(BlendMode.DST)
        builder.setStartIcon(startIcon);
    }
    if (endIcon != null) {
        builder.setEndIcon(endIcon);
    }
    return builder.build().slice;
}

@RequiresApi(Build.VERSION_CODES.R)
fun canUseInlineSuggestions(imeSpec: InlinePresentationSpec): Boolean {
    return UiVersions.getVersions(imeSpec.style).contains(UiVersions.INLINE_UI_VERSION_1)
}

@RequiresApi(Build.VERSION_CODES.O)
fun Dataset.Builder.setValue(
    id: AutofillId,
    value: AutofillValue?,
    presentation: RemoteViews,
    inlinePresentation: InlinePresentation? = null
) : Dataset.Builder {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && inlinePresentation != null) {
        this.setValue(id, value, presentation, inlinePresentation)
    } else {
        this.setValue(id, value, presentation)
    }
}
