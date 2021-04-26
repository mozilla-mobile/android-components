/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.prompt

import mozilla.components.concept.engine.prompt.PromptRequest.Alert
import mozilla.components.concept.engine.prompt.PromptRequest.Authentication
import mozilla.components.concept.engine.prompt.PromptRequest.Color
import mozilla.components.concept.engine.prompt.PromptRequest.Confirm
import mozilla.components.concept.engine.prompt.PromptRequest.File
import mozilla.components.concept.engine.prompt.PromptRequest.MenuChoice
import mozilla.components.concept.engine.prompt.PromptRequest.MultipleChoice
import mozilla.components.concept.engine.prompt.PromptRequest.Popup
import mozilla.components.concept.engine.prompt.PromptRequest.Repost
import mozilla.components.concept.engine.prompt.PromptRequest.SaveLoginPrompt
import mozilla.components.concept.engine.prompt.PromptRequest.SelectLoginPrompt
import mozilla.components.concept.engine.prompt.PromptRequest.SingleChoice
import mozilla.components.concept.engine.prompt.PromptRequest.TextPrompt
import mozilla.components.concept.engine.prompt.PromptRequest.TimeSelection
import mozilla.components.concept.engine.prompt.PromptRequest.TimeSelection.Type
import mozilla.components.concept.storage.Login
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class PromptRequestTest {

    @Test
    fun `SingleChoice`() {
        val single = SingleChoice(emptyArray()) {}
        single.onConfirm(Choice(id = "", label = ""))
        assertNotNull(single.choices)
    }

    @Test
    fun `MultipleChoice`() {
        val multiple = MultipleChoice(emptyArray()) {}
        multiple.onConfirm(arrayOf(Choice(id = "", label = "")))
        assertNotNull(multiple.choices)
    }

    @Test
    fun `MenuChoice`() {
        val menu = MenuChoice(emptyArray()) {}
        menu.onConfirm(Choice(id = "", label = ""))
        assertNotNull(menu.choices)
    }

    @Test
    fun `Alert`() {
        val alert = Alert("title", "message", true, {}) {}

        assertEquals(alert.title, "title")
        assertEquals(alert.message, "message")
        assertEquals(alert.hasShownManyDialogs, true)

        alert.onDismiss()
        alert.onConfirm(true)

        assertEquals(alert.title, "title")
        assertEquals(alert.message, "message")
        assertEquals(alert.hasShownManyDialogs, true)

        alert.onDismiss()
        alert.onConfirm(true)
    }

    @Test
    fun `TextPrompt`() {
        val textPrompt = TextPrompt(
            "title",
            "label",
            "value",
            true,
            {}) { _, _ -> }

        assertEquals(textPrompt.title, "title")
        assertEquals(textPrompt.inputLabel, "label")
        assertEquals(textPrompt.inputValue, "value")
        assertEquals(textPrompt.hasShownManyDialogs, true)

        textPrompt.onDismiss()
        textPrompt.onConfirm(true, "")
    }

    @Test
    fun `TimeSelection`() {
        val dateRequest = TimeSelection(
            "title",
            Date(),
            Date(),
            Date(),
            Type.DATE,
            {}) {}

        assertEquals(dateRequest.title, "title")
        assertEquals(dateRequest.type, Type.DATE)
        assertNotNull(dateRequest.initialDate)
        assertNotNull(dateRequest.minimumDate)
        assertNotNull(dateRequest.maximumDate)

        dateRequest.onConfirm(Date())
        dateRequest.onClear()
    }

    @Test
    fun `File`() {
        val filePickerRequest = File(
            emptyArray(),
            true,
            PromptRequest.File.FacingMode.NONE,
            { _, _ -> },
            { _, _ -> }
        ) {}

        assertTrue(filePickerRequest.mimeTypes.isEmpty())
        assertTrue(filePickerRequest.isMultipleFilesSelection)
        assertEquals(filePickerRequest.captureMode, PromptRequest.File.FacingMode.NONE)

        filePickerRequest.onSingleFileSelected(mock(), mock())
        filePickerRequest.onMultipleFilesSelected(mock(), emptyArray())
        filePickerRequest.onDismiss()
    }

    @Test
    fun `Authentication`() {
        val promptRequest = Authentication(
            "title",
            "message",
            "username",
            "password",
            PromptRequest.Authentication.Method.HOST,
            PromptRequest.Authentication.Level.NONE,
            false,
            false,
            false,
            { _, _ -> }) {
        }

        assertEquals(promptRequest.title, "title")
        assertEquals(promptRequest.message, "message")
        assertEquals(promptRequest.userName, "username")
        assertEquals(promptRequest.password, "password")
        assertFalse(promptRequest.onlyShowPassword)
        assertFalse(promptRequest.previousFailed)
        assertFalse(promptRequest.isCrossOrigin)

        promptRequest.onConfirm("", "")
        promptRequest.onDismiss()
    }

    @Test
    fun `Color`() {
        val onConfirm: (String) -> Unit = {}
        val onDismiss: () -> Unit = {}

        val colorRequest = Color("defaultColor", onConfirm, onDismiss)

        assertEquals(colorRequest.defaultColor, "defaultColor")

        colorRequest.onConfirm("")
        colorRequest.onDismiss()
    }

    @Test
    fun `Popup`() {
        val popupRequest = Popup("http://mozilla.slack.com/", {}, {})

        assertEquals(popupRequest.targetUri, "http://mozilla.slack.com/")

        popupRequest.onAllow()
        popupRequest.onDeny()
    }

    @Test
    fun `Confirm`() {
        val onConfirmPositiveButton: (Boolean) -> Unit = {}
        val onConfirmNegativeButton: (Boolean) -> Unit = {}
        val onConfirmNeutralButton: (Boolean) -> Unit = {}

        val confirmRequest = Confirm(
            "title",
            "message",
            false,
            "positive",
            "negative",
            "neutral",
            onConfirmPositiveButton,
            onConfirmNegativeButton,
            onConfirmNeutralButton
        ) {}

        assertEquals(confirmRequest.title, "title")
        assertEquals(confirmRequest.message, "message")
        assertEquals(confirmRequest.positiveButtonTitle, "positive")
        assertEquals(confirmRequest.negativeButtonTitle, "negative")
        assertEquals(confirmRequest.neutralButtonTitle, "neutral")

        confirmRequest.onConfirmPositiveButton(true)
        confirmRequest.onConfirmNegativeButton(true)
        confirmRequest.onConfirmNeutralButton(true)
    }

    @Test
    fun `SaveLoginPrompt`() {
        val onLoginDismiss: () -> Unit = {}
        val onLoginConfirm: (Login) -> Unit = {}
        val login = Login(null, "origin", username = "username", password = "password")

        val loginSaveRequest = SaveLoginPrompt(0, listOf(login), onLoginDismiss, onLoginConfirm)

        assertEquals(loginSaveRequest.logins, listOf(login))
        assertEquals(loginSaveRequest.hint, 0)

        loginSaveRequest.onConfirm(login)
        loginSaveRequest.onDismiss()
    }

    @Test
    fun `SelectLoginPrompt`() {
        val onLoginDismiss: () -> Unit = {}
        val onLoginConfirm: (Login) -> Unit = {}
        val login = Login(null, "origin", username = "username", password = "password")

        val loginSelectRequest =
            SelectLoginPrompt(listOf(login), onLoginDismiss, onLoginConfirm)

        assertEquals(loginSelectRequest.logins, listOf(login))

        loginSelectRequest.onConfirm(login)
        loginSelectRequest.onDismiss()
    }

    @Test
    fun `Repost`() {
        var onAcceptWasCalled = false
        var onDismissWasCalled = false

        val repostRequest = Repost(
            onConfirm = {
                onAcceptWasCalled = true
            },
            onDismiss = {
                onDismissWasCalled = true
            }
        )

        repostRequest.onConfirm()
        repostRequest.onDismiss()

        assertTrue(onAcceptWasCalled)
        assertTrue(onDismissWasCalled)
    }
}
