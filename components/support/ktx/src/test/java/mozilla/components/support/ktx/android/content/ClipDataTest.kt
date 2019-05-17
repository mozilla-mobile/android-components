/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.content

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.EditText
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipDataTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun pasteAsPlainText() {
        val view = EditText(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val spannableString = SpannableString("This is some bold text.").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
        }

        clipboard.primaryClip = ClipData.newPlainText("bold text", spannableString)
        clipboard.primaryClip?.pasteAsPlainText(view)
        val spans = view.text.getSpans(0, view.length(), StyleSpan::class.java)
        assertTrue(spans.isEmpty())
        assertEquals("This is some bold text.", view.text.toString())
    }

    @Test
    fun pasteAsPlainText_withSelection() {
        val view = EditText(context)
        val text = "some text"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val spannableString = SpannableString("bold text").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, 0)
        }

        clipboard.primaryClip = ClipData.newPlainText("bold text", spannableString)
        view.setText(text)
        view.setSelection(5, text.length)
        clipboard.primaryClip?.pasteAsPlainText(view)
        val spans = view.text.getSpans(0, view.length(), StyleSpan::class.java)
        assertTrue(spans.isEmpty())
        assertEquals("some bold text", view.text.toString())
    }
}