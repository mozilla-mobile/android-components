/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.prompts

import android.content.DialogInterface
import android.view.View.GONE
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.ext.appCompatContext
import mozilla.components.feature.prompts.R.id
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class AuthenticationDialogFragmentTest {

    @Test
    fun `build dialog`() {

        val fragment = spy(
            AuthenticationDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "username",
                "password",
                false
            )
        )

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)

        dialog.show()

        val titleTextView = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)
        val messageTextView = dialog.findViewById<TextView>(android.R.id.message)
        val usernameEditText = dialog.findViewById<EditText>(id.username)
        val passwordEditText = dialog.findViewById<EditText>(id.password)

        assertEquals(fragment.sessionId, "sessionId")
        assertEquals(fragment.title, "title")
        assertEquals(fragment.message, "message")
        assertEquals(fragment.username, "username")
        assertEquals(fragment.password, "password")
        assertEquals(fragment.onlyShowPassword, false)

        assertEquals(titleTextView.text, "title")
        assertEquals(messageTextView.text, "message")
        assertEquals(usernameEditText.text.toString(), "username")
        assertEquals(passwordEditText.text.toString(), "password")

        usernameEditText.setText("new_username")
        passwordEditText.setText("new_password")

        assertEquals(usernameEditText.text.toString(), "new_username")
        assertEquals(passwordEditText.text.toString(), "new_password")
    }

    @Test
    fun `dialog with onlyShowPassword must not have a username field`() {

        val fragment = spy(
            AuthenticationDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "username",
                "password",
                true
            )
        )

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)

        dialog.show()

        val usernameEditText = dialog.findViewById<EditText>(id.username)

        assertEquals(usernameEditText.visibility, GONE)
    }

    @Test
    fun `when the title is not provided the dialog must has a default value`() {

        val fragment = spy(
            AuthenticationDialogFragment.newInstance(
                "sessionId",
                "",
                "message",
                "username",
                "password",
                true
            )
        )

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)

        dialog.show()

        val titleTextView = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)

        val defaultTitle = appCompatContext.getString(AuthenticationDialogFragment.DEFAULT_TITLE)
        assertEquals(titleTextView.text.toString(), defaultTitle)
    }

    @Test
    fun `Clicking on positive button notifies the feature`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            AuthenticationDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "username",
                "password",
                false
            )
        )

        fragment.feature = mockFeature

        doReturn(appCompatContext).`when`(fragment).requireContext()

        val dialog = fragment.onCreateDialog(null)
        dialog.show()

        val positiveButton = (dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.performClick()

        verify(mockFeature).onConfirm("sessionId", "username" to "password")
    }

    @Test
    fun `touching outside of the dialog must notify the feature onCancel`() {

        val mockFeature: PromptFeature = mock()

        val fragment = spy(
            AuthenticationDialogFragment.newInstance(
                "sessionId",
                "title",
                "message",
                "username",
                "password",
                false
            )
        )

        fragment.feature = mockFeature

        doReturn(appCompatContext).`when`(fragment).requireContext()

        fragment.onCancel(null)

        verify(mockFeature).onCancel("sessionId")
    }
}