/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.prompt

import mozilla.components.concept.engine.prompt.PromptRequest.SingleChoice
import mozilla.components.concept.engine.prompt.PromptRequest.MultipleChoice
import mozilla.components.concept.engine.prompt.PromptRequest.MenuChoice
import mozilla.components.concept.engine.prompt.PromptRequest.Alert
import mozilla.components.support.test.mock
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class PromptRequestTest {

    @Test
    fun `Create prompt requests`() {

        val single = SingleChoice(emptyArray()) {}
        single.onSelect(Choice(id = "", label = ""))
        assertNotNull(single.choices)

        val multiple = MultipleChoice(emptyArray()) {}
        multiple.onSelect(arrayOf(Choice(id = "", label = "")))
        assertNotNull(multiple.choices)

        val menu = MenuChoice(emptyArray()) {}
        menu.onSelect(Choice(id = "", label = ""))
        assertNotNull(menu.choices)

        val alert = Alert("title", "message", true, {}) {}
        assertEquals(alert.title, "title")
        assertEquals(alert.message, "message")
        assertEquals(alert.hasShownManyDialogs, true)
        alert.onDismiss()
        alert.onShouldShowNoMoreDialogs(true)

        val dateRequest = PromptRequest.Date("title", Date(), Date(), Date(), {}) {}
        assertEquals(dateRequest.title, "title")
        assertNotNull(dateRequest.initialDate)
        assertNotNull(dateRequest.minimumDate)
        assertNotNull(dateRequest.maximumDate)
        dateRequest.onSelect(Date())
        dateRequest.onClear()

        val filePickerRequest = PromptRequest.File(emptyArray(), true, { _, _ -> }, { _, _ -> }) {}
        assertTrue(filePickerRequest.mimeTypes.isEmpty())
        assertTrue(filePickerRequest.isMultipleFilesSelection)
        filePickerRequest.onSingleFileSelected(mock(), mock())
        filePickerRequest.onMultipleFilesSelected(mock(), emptyArray())
        filePickerRequest.onDismiss()

        val promptRequest = PromptRequest.Authentication(
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
}