/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.VisibleForTesting
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import mozilla.components.concept.engine.prompt.Choice

private const val KEY_CHOICES = "KEY_CHOICES"
private const val KEY_DIALOG_TYPE = "KEY_DIALOG_TYPE"

/**
 * [android.support.v4.app.DialogFragment] implementation to display choice(options,optgroup and menu)
 * web content in native dialogs.
 */
class ChoiceDialogFragment : PromptDialogFragment() {

    @VisibleForTesting
    internal val choices: Array<Choice> by lazy { safeArguments.getParcelableArray(KEY_CHOICES)!!.toArrayOfChoices() }

    @VisibleForTesting
    internal val dialogType: Int by lazy { safeArguments.getInt(KEY_DIALOG_TYPE) }

    internal val isSingleChoice get() = dialogType == SINGLE_CHOICE_DIALOG_TYPE

    internal val isMenuChoice get() = dialogType == MENU_CHOICE_DIALOG_TYPE

    internal val mapSelectChoice by lazy { HashMap<Choice, Choice>() }

    private val safeArguments get() = requireNotNull(arguments)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return when (dialogType) {
            SINGLE_CHOICE_DIALOG_TYPE -> createSingleChoiceDialog()
            MULTIPLE_CHOICE_DIALOG_TYPE -> createMultipleChoiceDialog()
            MENU_CHOICE_DIALOG_TYPE -> createSingleChoiceDialog()
            else -> throw IllegalArgumentException(" $dialogType is not a valid choice dialog type")
        }
    }

    companion object {

        fun newInstance(choices: Array<Choice>, sessionId: String, dialogType: Int): ChoiceDialogFragment {
            val fragment = ChoiceDialogFragment()
            val arguments = fragment.arguments ?: Bundle()

            with(arguments) {
                putParcelableArray(KEY_CHOICES, choices)
                putString(KEY_SESSION_ID, sessionId)
                putInt(KEY_DIALOG_TYPE, dialogType)
            }

            fragment.arguments = arguments

            return fragment
        }

        const val SINGLE_CHOICE_DIALOG_TYPE = 0
        const val MULTIPLE_CHOICE_DIALOG_TYPE = 1
        const val MENU_CHOICE_DIALOG_TYPE = 2
    }

    @SuppressLint("InflateParams")
    internal fun createDialogContentView(inflater: LayoutInflater): View {
        val view = inflater.inflate(R.layout.mozac_feature_choice_dialogs, null)
        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = ChoiceAdapter(this@ChoiceDialogFragment, inflater)
        }
        return view
    }

    fun onSelect(selectedChoice: Choice) {
        feature?.onSingleChoiceSelect(sessionId, selectedChoice)
        dismiss()
    }

    private fun createSingleChoiceDialog(): AlertDialog {

        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = createDialogContentView(inflater)

        return builder.setView(view)
            .setOnDismissListener {
                feature?.onCancel(sessionId)
            }.create()
    }

    private fun createMultipleChoiceDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = createDialogContentView(inflater)

        return builder.setView(view)
            .setNegativeButton(R.string.mozac_feature_prompts_cancel) { _, _ ->
                feature?.onCancel(sessionId)
            }
            .setPositiveButton(R.string.mozac_feature_prompts_ok) { _, _ ->
                feature?.onMultipleChoiceSelect(sessionId, mapSelectChoice.keys.toTypedArray())
            }.setOnDismissListener {
                feature?.onCancel(sessionId)
            }.create()
    }

    @Suppress("UNCHECKED_CAST")
    private fun Array<Parcelable>.toArrayOfChoices(): Array<Choice> {
        return if (this.isArrayOf<Choice>()) {
            this as Array<Choice>
        } else {
            Array(this.size) { index ->
                this[index] as Choice
            }
        }
    }
}
