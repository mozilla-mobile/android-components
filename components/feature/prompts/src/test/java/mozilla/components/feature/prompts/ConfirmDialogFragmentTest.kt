/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts

import android.content.Context
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfirmDialogFragmentTest {

    private val context: Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            android.support.v7.appcompat.R.style.Theme_AppCompat
        )

    @Test
    fun `build dialog`() {

        val fragment = spy(
            ConfirmDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "positiveLabel",
                "negativeLabel"
            )
        )

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)

        dialog.show()

        val titleTextView = dialog.findViewById<TextView>(android.support.v7.appcompat.R.id.alertTitle)
        val messageTextView = dialog.findViewById<TextView>(android.R.id.message)

        assertEquals(fragment.sessionId, "sessionId")
        assertEquals(fragment.message, "message")

        val positiveButton = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
        val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)

        assertEquals(titleTextView.text, "title")
        assertEquals(messageTextView.text, "message")
        assertEquals(positiveButton.text, "positiveLabel")
        assertEquals(negativeButton.text, "negativeLabel")
    }

    @Test
    fun `clicking on positive button notifies the feature`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            ConfirmDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "positiveLabel",
                "negativeLabel"
            )
        )

        fragment.feature = mockFeature

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val positiveButton = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.performClick()

        verify(mockFeature).onConfirm("sessionId")
    }

    @Test
    fun `clicking on negative button notifies the feature`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            ConfirmDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "positiveLabel",
                "negativeLabel"
            )
        )

        fragment.feature = mockFeature

        doReturn(context).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val negativeButton = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_NEGATIVE)
        negativeButton.performClick()

        verify(mockFeature).onCancel("sessionId")
    }
}
