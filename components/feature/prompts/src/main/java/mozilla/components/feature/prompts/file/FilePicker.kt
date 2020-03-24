/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts.file

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.Intent.EXTRA_INITIAL_INTENTS
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.MediaStore.EXTRA_OUTPUT
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.engine.prompt.PromptRequest.File
import mozilla.components.feature.prompts.PromptContainer
import mozilla.components.feature.prompts.consumePromptFrom
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.ktx.android.content.isPermissionGranted

/**
 * @property container The [Activity] or [Fragment] which hosts the file picker.
 * @property store The [BrowserStore] this feature should subscribe to.
 * @property onNeedToRequestPermissions a callback invoked when permissions
 * need to be requested before a prompt (e.g. a file picker) can be displayed.
 * Once the request is completed, [onPermissionsResult] needs to be invoked.
 */
internal class FilePicker(
    private val container: PromptContainer,
    private val store: BrowserStore,
    private var sessionId: String? = null,
    override val onNeedToRequestPermissions: OnNeedToRequestPermissions
) : PermissionsFeature {

    /**
     * The image capture intent doesn't return the URI where the image is saved,
     * so we track it here.
     */
    private var captureUri: Uri? = null

    @Suppress("ComplexMethod")
    fun handleFileRequest(promptRequest: File, requestPermissions: Boolean = true) {
        // Track which permissions are needed.
        val neededPermissions = mutableListOf<String>()
        // Build a list of intents for capturing media and opening the file picker to combine later.
        val intents = mutableListOf<Intent>()
        captureUri = null

        // Compare the accepted values against image/*, video/*, and audio/*
        for (type in MimeType.values()) {
            val hasPermission = container.context.isPermissionGranted(type.permission)
            // The captureMode attribute can be used if the accepted types are exactly for
            // image/*, video/*, or audio/*.
            if (hasPermission && type.shouldCapture(promptRequest.mimeTypes, promptRequest.captureMode)) {
                type.buildIntent(container.context, promptRequest)?.also {
                    saveCaptureUriIfPresent(it)
                    container.startActivityForResult(it, FILE_PICKER_ACTIVITY_REQUEST_CODE)
                    return
                }
            }
            // Otherwise, build the intent and create a chooser later
            if (type.matches(promptRequest.mimeTypes)) {
                if (hasPermission) {
                    type.buildIntent(container.context, promptRequest)?.also {
                        saveCaptureUriIfPresent(it)
                        intents.add(it)
                    }
                } else {
                    neededPermissions.add(type.permission)
                }
            }
        }

        val canSkipPermissionRequest = !requestPermissions && intents.isNotEmpty()

        if (neededPermissions.isEmpty() || canSkipPermissionRequest) {
            // Combine the intents together using a chooser.
            val lastIntent = intents.removeAt(intents.lastIndex)
            val chooser = Intent.createChooser(lastIntent, null).apply {
                putExtra(EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            }

            container.startActivityForResult(chooser, FILE_PICKER_ACTIVITY_REQUEST_CODE)
        } else {
            onNeedToRequestPermissions(neededPermissions.toTypedArray())
        }
    }

    /**
     * Notifies the feature of intent results for prompt requests handled by
     * other apps like file chooser requests.
     *
     * @param requestCode The code of the app that requested the intent.
     * @param intent The result of the request.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == FILE_PICKER_ACTIVITY_REQUEST_CODE) {
            store.consumePromptFrom(sessionId) {
                val request = it as File

                if (resultCode == RESULT_OK) {
                    handleFilePickerIntentResult(intent, request)
                } else {
                    request.onDismiss()
                }
            }
        }
    }

    /**
     * Notifies the feature that the permissions request was completed. It will then
     * either process or dismiss the prompt request.
     *
     * @param permissions List of permission requested.
     * @param grantResults The grant results for the corresponding permissions
     * @see [onNeedToRequestPermissions].
     */
    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults.all { it == PERMISSION_GRANTED }) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }

    /**
     * Used in conjunction with [onNeedToRequestPermissions], to notify the feature
     * that all the required permissions have been granted, and the pending [PromptRequest]
     * can be performed.
     *
     * If the required permission has not been granted
     * [onNeedToRequestPermissions] will be called.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun onPermissionsGranted() {
        store.consumePromptFrom(sessionId) { promptRequest ->
            handleFileRequest(promptRequest as File, requestPermissions = false)
        }
    }

    /**
     * Used in conjunction with [onNeedToRequestPermissions] to notify the feature that one
     * or more required permissions have been denied.
     */
    @VisibleForTesting(otherwise = PRIVATE)
    internal fun onPermissionsDenied() {
        store.consumePromptFrom(sessionId) { request ->
            if (request is File) request.onDismiss()
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    internal fun handleFilePickerIntentResult(intent: Intent?, request: File) {
        if (intent?.clipData != null && request.isMultipleFilesSelection) {
            intent.clipData?.run {
                val uris = Array<Uri>(itemCount) { index -> getItemAt(index).uri }
                request.onMultipleFilesSelected(container.context, uris)
            }
        } else {
            val uri = intent?.data ?: captureUri
            uri?.let {
                request.onSingleFileSelected(container.context, it)
            } ?: request.onDismiss
        }
    }

    private fun saveCaptureUriIfPresent(intent: Intent) =
        intent.getParcelableExtra<Uri>(EXTRA_OUTPUT)?.let { captureUri = it }

    companion object {
        const val FILE_PICKER_ACTIVITY_REQUEST_CODE = 7113
    }
}
