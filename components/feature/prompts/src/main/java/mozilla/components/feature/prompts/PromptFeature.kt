/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.annotation.VisibleForTesting
import android.support.annotation.VisibleForTesting.PRIVATE
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import mozilla.components.browser.session.SelectionAwareSessionObserver
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.prompt.Choice
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.engine.prompt.PromptRequest.TextPrompt
import mozilla.components.concept.engine.prompt.PromptRequest.Color
import mozilla.components.concept.engine.prompt.PromptRequest.Authentication
import mozilla.components.concept.engine.prompt.PromptRequest.Alert
import mozilla.components.concept.engine.prompt.PromptRequest.MultipleChoice
import mozilla.components.concept.engine.prompt.PromptRequest.SingleChoice
import mozilla.components.concept.engine.prompt.PromptRequest.MenuChoice
import mozilla.components.concept.engine.prompt.PromptRequest.File
import mozilla.components.feature.prompts.ChoiceDialogFragment.Companion.MULTIPLE_CHOICE_DIALOG_TYPE
import mozilla.components.feature.prompts.ChoiceDialogFragment.Companion.MENU_CHOICE_DIALOG_TYPE
import mozilla.components.feature.prompts.ChoiceDialogFragment.Companion.SINGLE_CHOICE_DIALOG_TYPE
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.android.content.isPermissionGranted
import java.security.InvalidParameterException
import mozilla.components.concept.engine.prompt.PromptRequest.TimeSelection

import java.util.Date

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal const val FRAGMENT_TAG = "mozac_feature_prompt_dialog"

typealias OnNeedToRequestPermissions = (session: Session, permissions: Array<String>, requestCode: Int) -> Unit

/**
 * Feature for displaying native dialogs for html elements like:
 * input type date,file,time,color, option, menu, authentication, confirmation and other alerts.
 *
 * There are some requests that are not handled with dialogs instead with intents like file choosers and others.
 * For this reason, you have to keep the feature aware of flow of requesting data from other apps, overriding
 * onActivityResult on your [Activity] or [Fragment] and forward its calls to [onActivityResult].
 *
 * This feature will subscribe to the currently selected [Session] and display a suitable native dialog based on
 * [Session.Observer.onPromptRequested] events. Once the dialog is closed or the user selects an item from the dialog
 * the related [PromptFeature] will be consumed.
 *
 * @property activity The [Activity] host of this feature, if the host is a [Fragment], just ignore this parameter
 * and pass a [fragment] parameter. Never [fragment] and [activity] should be both null or you will get
 * an [IllegalStateException].
 * @property fragment The [Fragment] host of this feature, if the host is an [Activity], just ignore this parameter
 * and pass [activity] parameter. Never [fragment] and [activity] should be both null or you will get
 * an [IllegalStateException].
 * @property sessionManager The [Fragment] instance in order to subscribe to the selected [Session].
 * @property fragmentManager The [FragmentManager] to be used when displaying a dialog (fragment).
 * @property onNeedToRequestPermissions A callback to let you know that there are some permissions that need to be
 * granted before performing a [PromptRequest]. You are in change of requesting these permissions and notify the feature
 * calling [onRequestPermissionsResult] method.
 */

@Suppress("TooManyFunctions")
class PromptFeature(
    private val activity: Activity? = null,
    private val fragment: Fragment? = null,
    private val sessionManager: SessionManager,
    private val fragmentManager: FragmentManager,
    private val onNeedToRequestPermissions: OnNeedToRequestPermissions

) : LifecycleAwareFeature {

    init {
        if (activity == null && fragment == null) {
            throw IllegalStateException(
                "activity and fragment references " +
                    "must not be both null, at least one must be initialized."
            )
        }
    }

    private val observer = PromptRequestObserver(sessionManager, feature = this)

    private val context get() = activity ?: requireNotNull(fragment).requireContext()

    /**
     * Start observing the selected session and when needed show native dialogs.
     */
    override fun start() {
        observer.observeSelected()

        fragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { fragment ->
            // There's still a [PromptDialogFragment] visible from the last time. Re-attach this feature so that the
            // fragment can invoke the callback on this feature once the user makes a selection. This can happen when
            // the app was in the background and on resume the activity and fragments get recreated.
            reattachFragment(fragment as PromptDialogFragment)
        }
    }

    /**
     * Stop observing the selected session incoming onPromptRequested events.
     */
    override fun stop() {
        observer.stop()
    }

    /**
     * Forward the calls to onActivityResult on your [Activity] or [Fragment], to let the feature know, the results
     * of intents for handling prompt requests, that need to be performed by other apps like file chooser requests.
     *
     * @param requestCode The code of the app that requested the intent.
     * @param intent The result of the request.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == FILE_PICKER_REQUEST) {
            sessionManager.selectedSession?.promptRequest?.consume {

                val request = it as File

                if (resultCode != RESULT_OK || intent == null) {
                    request.onDismiss()
                } else {
                    handleFilePickerIntentResult(intent, request)
                }
                true
            }
        }
    }

    /**
     * Forward the calls to onRequestPermissionsResult on your [Activity] or [Fragment], to let the feature know,
     * the results for the requested permissions that are needed for performing a [PromptRequest].
     *
     * @param requestCode The code of the app that requested the intent.
     * @param permissions List of permission requested.
     * @see [onNeedToRequestPermissions].
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            FILE_PICKER_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onPermissionsGranted()
                } else {
                    onPermissionsDeny()
                }
            }
        }
    }

    /**
     * Use in conjunction with [onNeedToRequestPermissions], to notify the feature that all the required permissions
     * have been granted, and it can perform the pending [PromptRequest] in the selected session.
     *
     * If the required permission has not been completely granted [onNeedToRequestPermissions] will be called.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun onPermissionsGranted() {
        sessionManager.selectedSession?.apply {
            promptRequest.consume { promptRequest ->
                onPromptRequested(this, promptRequest)
                false
            }
        }
    }

    /**
     * Use in conjunction with [onNeedToRequestPermissions] to notify the feature that one or more required permissions
     * have been denied.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun onPermissionsDeny() {
        sessionManager.selectedSession?.apply {
            promptRequest.consume { request ->
                if (request is File) {
                    request.onDismiss()
                }
                true
            }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun handleFilePickerIntentResult(
        intent: Intent,
        request: File
    ) {
        intent.apply {

            if (request.isMultipleFilesSelection) {
                handleMultipleFileSelections(request, this)
            } else {
                handleSingleFileSelection(request, this)
            }
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun handleSingleFileSelection(
        request: File,
        intent: Intent
    ) {
        intent.data?.apply {
            request.onSingleFileSelected(context, this)
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun handleMultipleFileSelections(
        request: File,
        intent: Intent
    ) {
        intent.clipData?.apply {
            val uris = Array<Uri>(itemCount) { index -> getItemAt(index).uri }
            request.onMultipleFilesSelected(context, uris)
        }
    }

    /**
     * Event that is triggered when a native dialog needs to be shown.
     * Displays suitable dialog for the type of the [promptRequest].
     *
     * @param session The session which requested the dialog.
     * @param promptRequest The session the request the dialog.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun onPromptRequested(session: Session, promptRequest: PromptRequest) {

        // Requests that are handle with intents
        when (promptRequest) {
            is File -> {
                handleFilePickerRequest(promptRequest, session)
                return
            }
        }
        handleDialogsRequest(promptRequest, session)
    }

    /**
     * Event that is called when a dialog is dismissed.
     * This consumes the [PromptFeature] value from the [Session] indicated by [sessionId].
     * @param sessionId this is the id of the session which requested the prompt.
     */
    internal fun onCancel(sessionId: String) {
        val session = sessionManager.findSessionById(sessionId) ?: return
        session.promptRequest.consume {
            when (it) {
                is Alert -> it.onDismiss()
                is Authentication -> it.onDismiss()
                is Color -> it.onDismiss()
                is TextPrompt -> it.onDismiss()
                is PromptRequest.Popup -> it.onDeny()
            }
            true
        }
    }

    /**
     * Event that is called when the user confirm the action on the dialog.
     * This consumes the [PromptFeature] value from the [Session] indicated by [sessionId].
     * @param sessionId that requested to show the dialog.
     * @param value an optional value provided by the dialog as a result of confirming the action.
     */
    @Suppress("UNCHECKED_CAST", "ComplexMethod")
    internal fun onConfirm(sessionId: String, value: Any? = null) {
        val session = sessionManager.findSessionById(sessionId) ?: return
        session.promptRequest.consume {
            when (it) {
                is TimeSelection -> it.onConfirm(value as Date)
                is Color -> it.onConfirm(value as String)
                is Alert -> it.onConfirm(value as Boolean)
                is SingleChoice -> it.onConfirm(value as Choice)
                is MenuChoice -> it.onConfirm(value as Choice)
                is PromptRequest.Popup -> it.onAllow()

                is MultipleChoice -> {
                    it.onConfirm(value as Array<Choice>)
                }

                is Authentication -> {
                    val pair = value as Pair<String, String>
                    it.onConfirm(pair.first, pair.second)
                }

                is TextPrompt -> {
                    val pair = value as Pair<Boolean, String>
                    it.onConfirm(pair.first, pair.second)
                }
            }
            true
        }
    }

    /**
     * Event that is called when the user is requesting to clear the selected value from the dialog.
     * This consumes the [PromptFeature] value from the [Session] indicated by [sessionId].
     * @param sessionId that requested to show the dialog.
     */
    internal fun onClear(sessionId: String) {
        val session = sessionManager.findSessionById(sessionId) ?: return
        session.promptRequest.consume {
            when (it) {
                is PromptRequest.TimeSelection -> it.onClear()
            }
            true
        }
    }

    /**
     * Re-attach a fragment that is still visible but not linked to this feature anymore.
     */
    private fun reattachFragment(fragment: PromptDialogFragment) {
        val session = sessionManager.findSessionById(fragment.sessionId)
        if (session == null || session.promptRequest.isConsumed()) {
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
            return
        }
        // Re-assign the feature instance so that the fragment can invoke us once the user makes a selection or cancels
        // the dialog.
        fragment.feature = this
    }

    internal fun handleFilePickerRequest(
        promptRequest: File,
        session: Session
    ) {
        if (context.isPermissionGranted(READ_EXTERNAL_STORAGE)) {
            val intent = buildFileChooserIntent(
                promptRequest.isMultipleFilesSelection,
                promptRequest.mimeTypes
            )
            startActivityForResult(intent, FILE_PICKER_REQUEST)
        } else {
            onNeedToRequestPermissions(session, arrayOf(READ_EXTERNAL_STORAGE), FILE_PICKER_REQUEST)
        }
    }

    internal fun startActivityForResult(intent: Intent, code: Int) {
        if (activity != null) {
            activity.startActivityForResult(intent, code)
        } else {
            requireNotNull(fragment).startActivityForResult(intent, code)
        }
    }

    internal fun buildFileChooserIntent(allowMultipleSelection: Boolean, mimeTypes: Array<out String>): Intent {
        return with(Intent(Intent.ACTION_GET_CONTENT)) {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            if (mimeTypes.isNotEmpty()) {
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleSelection)
        }
    }

    @Suppress("ComplexMethod")
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun handleDialogsRequest(
        promptRequest: PromptRequest,
        session: Session
    ) {
        // Requests that are handled with dialogs
        val dialog = when (promptRequest) {

            is SingleChoice -> {
                ChoiceDialogFragment.newInstance(
                    promptRequest.choices,
                    session.id, SINGLE_CHOICE_DIALOG_TYPE
                )
            }

            is MultipleChoice -> ChoiceDialogFragment.newInstance(
                promptRequest.choices, session.id, MULTIPLE_CHOICE_DIALOG_TYPE
            )

            is MenuChoice -> ChoiceDialogFragment.newInstance(
                promptRequest.choices, session.id, MENU_CHOICE_DIALOG_TYPE
            )

            is Alert -> {
                with(promptRequest) {
                    AlertDialogFragment.newInstance(session.id, title, message, hasShownManyDialogs)
                }
            }

            is PromptRequest.TimeSelection -> {

                val selectionType = when (promptRequest.type) {
                    TimeSelection.Type.DATE -> TimePickerDialogFragment.SELECTION_TYPE_DATE
                    TimeSelection.Type.DATE_AND_TIME -> TimePickerDialogFragment.SELECTION_TYPE_DATE_AND_TIME
                    TimeSelection.Type.TIME -> TimePickerDialogFragment.SELECTION_TYPE_TIME
                }

                with(promptRequest) {
                    TimePickerDialogFragment.newInstance(
                        session.id,
                        title,
                        initialDate,
                        minimumDate,
                        maximumDate,
                        selectionType
                    )
                }
            }

            is PromptRequest.TextPrompt -> {
                with(promptRequest) {
                    TextPromptDialogFragment.newInstance(session.id, title, inputLabel, inputValue, hasShownManyDialogs)
                }
            }

            is PromptRequest.Authentication -> {
                with(promptRequest) {
                    AuthenticationDialogFragment.newInstance(
                        session.id,
                        title,
                        message,
                        userName,
                        password,
                        onlyShowPassword
                    )
                }
            }

            is PromptRequest.Color -> {
                with(promptRequest) {
                    ColorPickerDialogFragment.newInstance(session.id, defaultColor)
                }
            }

            is PromptRequest.Popup -> {
                val title = context.getString(R.string.mozac_feature_prompts_popup_dialog_title)
                val positiveLabel = context.getString(R.string.mozac_feature_prompts_allow)
                val negativeLabel = context.getString(R.string.mozac_feature_prompts_deny)

                with(promptRequest) {
                    ConfirmDialogFragment.newInstance(
                        sessionId = session.id,
                        title = title,
                        message = targetUri,
                        positiveButtonText = positiveLabel,
                        negativeButtonText = negativeLabel
                    )
                }
            }

            else -> {
                throw InvalidParameterException("Not valid prompt request type")
            }
        }

        dialog.feature = this
        dialog.show(fragmentManager, FRAGMENT_TAG)
    }

    /**
     * Observes [Session.Observer.onPromptRequested] of the selected session and notifies the feature whenever a prompt
     * needs to be shown.
     */
    internal class PromptRequestObserver(
        sessionManager: SessionManager,
        private val feature: PromptFeature
    ) : SelectionAwareSessionObserver(sessionManager) {

        override fun onPromptRequested(session: Session, promptRequest: PromptRequest): Boolean {
            feature.onPromptRequested(session, promptRequest)
            return false
        }
    }

    companion object {
        const val FILE_PICKER_REQUEST = 1234
    }
}
