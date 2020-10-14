/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.readerview.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.feature.readerview.ReaderViewFeature
import mozilla.components.feature.readerview.ReaderViewFeature.Companion.COLOR_SCHEME_KEY
import mozilla.components.feature.readerview.ReaderViewFeature.Companion.FONT_SIZE_DEFAULT
import mozilla.components.feature.readerview.ReaderViewFeature.Companion.FONT_SIZE_KEY
import mozilla.components.feature.readerview.ReaderViewFeature.Companion.FONT_TYPE_KEY
import mozilla.components.support.test.eq
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class ReaderViewConfigTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sendConfigMessage: (JSONObject) -> Unit
    private lateinit var config: ReaderViewConfig

    @Before
    fun setup() {
        context = mock()
        prefs = mock()
        editor = mock()
        sendConfigMessage = mock()

        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)

        config = ReaderViewConfig(context, sendConfigMessage)
    }

    @Test
    fun `color scheme should read from shared prefs`() {
        whenever(prefs.getString(COLOR_SCHEME_KEY, "LIGHT")).thenReturn("SEPIA")
        verify(prefs, never()).getString(eq(COLOR_SCHEME_KEY), anyString())

        assertEquals(ReaderViewFeature.ColorScheme.SEPIA, config.colorScheme)
        verify(prefs, times(1)).getString(eq(COLOR_SCHEME_KEY), anyString())

        assertEquals(ReaderViewFeature.ColorScheme.SEPIA, config.colorScheme)
        verify(prefs, times(1)).getString(eq(COLOR_SCHEME_KEY), anyString())
    }

    @Test
    fun `font type should read from shared prefs`() {
        whenever(prefs.getString(FONT_TYPE_KEY, "SERIF")).thenReturn("SANSSERIF")
        verify(prefs, never()).getString(eq(FONT_TYPE_KEY), anyString())

        assertEquals(ReaderViewFeature.FontType.SANSSERIF, config.fontType)
        verify(prefs, times(1)).getString(eq(FONT_TYPE_KEY), anyString())

        assertEquals(ReaderViewFeature.FontType.SANSSERIF, config.fontType)
        verify(prefs, times(1)).getString(eq(FONT_TYPE_KEY), anyString())
    }

    @Test
    fun `font size should read from shared prefs`() {
        whenever(prefs.getInt(FONT_SIZE_KEY, FONT_SIZE_DEFAULT)).thenReturn(4)
        verify(prefs, never()).getInt(eq(FONT_SIZE_KEY), anyInt())

        assertEquals(4, config.fontSize)
        verify(prefs, times(1)).getInt(eq(FONT_SIZE_KEY), anyInt())

        assertEquals(4, config.fontSize)
        verify(prefs, times(1)).getInt(eq(FONT_SIZE_KEY), anyInt())
    }
}
