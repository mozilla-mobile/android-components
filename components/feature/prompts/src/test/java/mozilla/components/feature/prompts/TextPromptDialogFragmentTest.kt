/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts

import android.content.DialogInterface.BUTTON_POSITIVE
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.feature.prompts.R.id
import mozilla.components.support.test.mock
import mozilla.ext.appCompatContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class TextPromptDialogFragmentTest {

    @Test
    fun `build dialog`() {

        val fragment = spy(
            TextPromptDialogFragment.newInstance("sessionId", "title", "label", "defaultValue", true)
        )

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)

        dialog.show()

        val titleTextView = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
        val inputLabel = dialog.findViewById<TextView>(id.input_label)
        val inputValue = dialog.findViewById<TextView>(id.input_value)
        val checkBox = dialog.findViewById<CheckBox>(id.no_more_dialogs_check_box)

        assertEquals(fragment.sessionId, "sessionId")
        assertEquals(fragment.title, "title")
        assertEquals(fragment.labelInput, "label")
        assertEquals(fragment.defaultInputValue, "defaultValue")
        assertEquals(fragment.hasShownManyDialogs, true)

        assertEquals(titleTextView.text, "title")
        assertEquals(fragment.title, "title")
        assertEquals(inputLabel.text, "label")
        assertEquals(inputValue.text.toString(), "defaultValue")
        assertTrue(checkBox.isVisible)

        checkBox.isChecked = true
        assertTrue(fragment.userSelectionNoMoreDialogs)

        inputValue.text = "NewValue"
        assertEquals(inputValue.text.toString(), "NewValue")
    }

    @Test
    fun `TextPrompt with hasShownManyDialogs equals false should not have a checkbox`() {

        val fragment = spy(
            TextPromptDialogFragment.newInstance("sessionId", "title", "label", "defaultValue", false)
        )

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)

        dialog.show()

        val checkBox = dialog.findViewById<CheckBox>(id.no_more_dialogs_check_box)

        assertFalse(checkBox.isVisible)
    }

    @Test
    fun `Clicking on positive button notifies the feature`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            TextPromptDialogFragment.newInstance("sessionId", "title", "label", "defaultValue", false)
        )

        fragment.feature = mockFeature

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val positiveButton = (dialog as AlertDialog).getButton(BUTTON_POSITIVE)
        positiveButton.performClick()

        verify(mockFeature).onConfirm("sessionId", false to "defaultValue")
    }

    @Test
    fun `After checking no more dialogs checkbox feature onNoMoreDialogsChecked must be called`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            TextPromptDialogFragment.newInstance("sessionId", "title", "label", "defaultValue", true)
        )

        fragment.feature = mockFeature

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val checkBox = dialog.findViewById<CheckBox>(id.no_more_dialogs_check_box)

        checkBox.isChecked = true

        val positiveButton = (dialog as AlertDialog).getButton(BUTTON_POSITIVE)
        positiveButton.performClick()

        verify(mockFeature).onConfirm("sessionId", true to "defaultValue")
    }

    @Test
    fun `touching outside of the dialog must notify the feature onCancel`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            TextPromptDialogFragment.newInstance("sessionId", "title", "label", "defaultValue", true)
        )

        fragment.feature = mockFeature

        doReturn(appCompatContext).`when`(fragment).requireContext()

        fragment.onCancel(null)

        verify(mockFeature).onCancel("sessionId")
    }
}
