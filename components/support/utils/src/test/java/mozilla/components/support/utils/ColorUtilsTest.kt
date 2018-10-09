/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils

import android.graphics.Color

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.junit.Assert.assertEquals

@RunWith(RobolectricTestRunner::class)
class ColorUtilsTest {
    @Test
    fun getReadableTextColor() {
        assertEquals(Color.BLACK.toLong(), ColorUtils.getReadableTextColor(Color.WHITE).toLong())
        assertEquals(Color.WHITE.toLong(), ColorUtils.getReadableTextColor(Color.BLACK).toLong())

        // Slack
        assertEquals(Color.BLACK.toLong(), ColorUtils.getReadableTextColor(-0x90b14).toLong())

        // Google+
        assertEquals(Color.WHITE.toLong(), ColorUtils.getReadableTextColor(-0x24bbc9).toLong())

        // Telegram
        assertEquals(Color.WHITE.toLong(), ColorUtils.getReadableTextColor(-0xad825d).toLong())

        // IRCCloud
        assertEquals(Color.BLACK.toLong(), ColorUtils.getReadableTextColor(-0xd0804).toLong())

        // Yahnac
        assertEquals(Color.WHITE.toLong(), ColorUtils.getReadableTextColor(-0xa8400).toLong())
    }
}
