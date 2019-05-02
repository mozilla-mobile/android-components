/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.browser.tabstray.thumbnail

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TabThumbnailViewTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `view should always use Matrix ScaleType`() {
        val view = TabThumbnailView(context, mock())
        assertEquals(ImageView.ScaleType.MATRIX, view.scaleType)
    }

    @Test
    fun `view updates matrix when changed`() {
        val view = TabThumbnailView(context, mock())
        val matrix = view.imageMatrix
        val drawable: Drawable = mock()

        `when`(drawable.intrinsicWidth).thenReturn(5)
        `when`(drawable.intrinsicHeight).thenReturn(5)

        view.setImageDrawable(drawable)
        view.setFrame(5, 5, 5, 5)

        val matrix2 = view.imageMatrix

        assertNotEquals(matrix, matrix2)
    }

    @Test
    fun `view updates don't change matrix if no changes to frame`() {
        val view = TabThumbnailView(context, mock())
        val drawable: Drawable = mock()

        `when`(drawable.intrinsicWidth).thenReturn(5)
        `when`(drawable.intrinsicHeight).thenReturn(5)

        view.setImageDrawable(drawable)
        view.setFrame(5, 5, 5, 5)

        val matrix = view.imageMatrix

        view.setFrame(5, 5, 5, 5)

        val matrix2 = view.imageMatrix

        assertEquals(matrix, matrix2)
    }
}
