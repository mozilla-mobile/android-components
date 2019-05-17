/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.android.content

import android.content.ClipData
import android.widget.EditText

fun ClipData.pasteAsPlainText(editText: EditText) {
    var paste = ""
    for (i in 0 until itemCount) {
        paste += getItemAt(i).coerceToText(editText.context)
    }
    editText.apply {
        val data = buildString {
            append(text.substring(0, selectionStart))
            append(paste)
            append(text.substring(selectionEnd, text.length))
        }
        setText(data)
    }
}
