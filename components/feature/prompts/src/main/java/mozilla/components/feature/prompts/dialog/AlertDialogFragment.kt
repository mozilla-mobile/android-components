/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

/**
 * [android.support.v4.app.DialogFragment] implementation to display web Alerts with native dialogs.
 */
internal class AlertDialogFragment : AbstractPromptTextDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onPositiveClickAction()
            }
        return setCustomMessageView(builder)
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        feature?.onCancel(sessionId)
    }

    private fun onPositiveClickAction() {
        if (!userSelectionNoMoreDialogs) {
            feature?.onCancel(sessionId)
        } else {
            feature?.onConfirm(sessionId, userSelectionNoMoreDialogs)
        }
    }

    companion object {
        /**
         * A builder method for creating a [AlertDialogFragment]
         * @param sessionId to create the dialog.
         * @param title the title of the dialog.
         * @param message the message of the dialog.
         * @param hasShownManyDialogs tells if this [sessionId] has shown many dialogs
         * in a short period of time, if is true a checkbox will be part of the dialog, for the user
         * to choose if wants to prevent this [sessionId] continuing showing dialogs.
         */
        fun newInstance(
            sessionId: String,
            title: String,
            message: String,
            hasShownManyDialogs: Boolean
        ): AlertDialogFragment {

            val fragment = AlertDialogFragment()
            val arguments = fragment.arguments ?: Bundle()

            with(arguments) {
                putString(KEY_SESSION_ID, sessionId)
                putString(KEY_TITLE, title)
                putString(KEY_MESSAGE, message)
                putBoolean(KEY_MANY_ALERTS, hasShownManyDialogs)
            }

            fragment.arguments = arguments
            return fragment
        }
    }
}
