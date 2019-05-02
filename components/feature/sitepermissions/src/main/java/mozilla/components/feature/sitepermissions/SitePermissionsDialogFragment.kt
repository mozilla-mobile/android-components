package mozilla.components.feature.sitepermissions

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.support.v4.graphics.ColorUtils
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE

import android.widget.CheckBox
import android.view.WindowManager.LayoutParams.*
import android.widget.ImageView
import android.widget.TextView

internal const val KEY_SESSION_ID = "KEY_SESSION_ID"
internal const val KEY_TITLE = "KEY_TITLE"

private const val KEY_USER_CHECK_BOX = "KEY_USER_CHECK_BOX"
private const val KEY_DIALOG_GRAVITY = "KEY_DIALOG_GRAVITY"
private const val KEY_DIALOG_WIDTH_MATCH_PARENT = "KEY_DIALOG_WIDTH_MATCH_PARENT"
private const val KEY_TITLE_ICON = "KEY_TITLE_ICON"
private const val KEY_SHOULD_INCLUDE_CHECKBOX= "KEY_SHOULD_INCLUDE_CHECKBOX"
private const val DIALOG_GRAVITY_NONE = -1

class SitePermissionsDialogFragment : AppCompatDialogFragment() {

    internal val feature: SitePermissionsFeature? = null

    internal val sessionId: String by lazy { safeArguments.getString(KEY_SESSION_ID) }

    internal val title: String by lazy { safeArguments.getString(KEY_TITLE) }

    internal val dialogGravity: Int by lazy { safeArguments.getInt(KEY_DIALOG_GRAVITY, DIALOG_GRAVITY_NONE) }

    internal val dialogShouldWidthMatchParent: Boolean by lazy { safeArguments.getBoolean(KEY_DIALOG_WIDTH_MATCH_PARENT) }

    internal val shouldIncludeCheckBox: Boolean by lazy { safeArguments.getBoolean(KEY_SHOULD_INCLUDE_CHECKBOX) }

    val safeArguments get() = requireNotNull(arguments)

    internal val icon: Int by lazy { safeArguments.getInt(KEY_TITLE_ICON, DIALOG_GRAVITY_NONE) }

    internal var userSelectionNoMoreDialogs: Boolean
        get() = safeArguments.getBoolean(KEY_USER_CHECK_BOX)
        set(value) {
            safeArguments.putBoolean(KEY_USER_CHECK_BOX, value)
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = if (dialogGravity != DIALOG_GRAVITY_NONE) {
            AlertDialog.Builder(requireContext(), R.style.Mozac_SitePermissions_dialog)
        } else {
            AlertDialog.Builder(requireContext())
        }

        val rootView = LayoutInflater.from(context).inflate(
            R.layout.mozac_site_permissions_prompt,
            null,
            false)

        rootView.findViewById<TextView>(R.id.title).text = title
        rootView.findViewById<ImageView>(R.id.icon).setImageResource(icon)

        builder.setView(rootView)

        builder.setCancelable(true)
            .setPositiveButton(R.string.mozac_feature_sitepermissions_allow) { _, _ ->
                //feature?.onContentPermissionGranted()
            }
            .setNegativeButton(R.string.mozac_feature_sitepermissions_not_allow){  _, _ ->
                //feature?.onContentPermissionDeny()
            }

        if (shouldIncludeCheckBox){
            addCheckbox(rootView)
        }

        val dialog = builder.create()
        applyDialogStyles(dialog)
        return dialog
    }

    private fun applyDialogStyles(dialog: Dialog) {
        if (dialogGravity != DIALOG_GRAVITY_NONE) {
            dialog.window?.setGravity(dialogGravity)
            dialog.window?.addFlags(FLAG_DIM_BEHIND)
            dialog.window?.setDimAmount(0.7f)

            activity?.window?.navigationBarColor?.apply {
                dialog.window?.navigationBarColor = ColorUtils.blendARGB(this, Color.BLACK, 0.5f)
            }
        }

        if (dialogShouldWidthMatchParent) {
            dialog.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
        }
    }

    @SuppressLint("InflateParams")
    private fun addCheckbox(containerView: View) {

       val checkBox =  containerView.findViewById<CheckBox>(R.id.do_not_ask_again)
        checkBox.visibility = VISIBLE
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            userSelectionNoMoreDialogs = isChecked
        }
    }

    companion object {
        fun newInstance(
            sessionId: String,
            title: String,
            titleIcon: Int,
            feature: SitePermissionsFeature,
            shouldIncludeCheckBox: Boolean
        ): SitePermissionsDialogFragment {

            val fragment = SitePermissionsDialogFragment()
            val arguments = fragment.arguments ?: Bundle()

            with(arguments) {
                putString(KEY_SESSION_ID, sessionId)
                putString(KEY_TITLE, title)
                putInt(KEY_TITLE_ICON, titleIcon)
                putBoolean(KEY_SHOULD_INCLUDE_CHECKBOX, shouldIncludeCheckBox)

                feature.promptsStyling?.apply {
                    putInt(KEY_DIALOG_GRAVITY, gravity)
                    putBoolean(KEY_DIALOG_WIDTH_MATCH_PARENT, shouldWithMatchParent)
                }
            }

            fragment.arguments = arguments
            return fragment
        }
    }
}