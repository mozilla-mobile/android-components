/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.content

import android.content.ClipData
import android.text.Spanned
import android.widget.EditText

fun ClipData.pasteAsPlainText(editText: EditText) {
    var paste = ""
    for (i in 0 until itemCount) {
        val text = getItemAt(i).coerceToText(editText.context)
        paste += if (text is Spanned) text.toString() else text
    }
    val data = editText.text.substring(0, editText.selectionStart) + paste +
            editText.text.substring(editText.selectionEnd, editText.text.length)
    editText.setText(data)
}
