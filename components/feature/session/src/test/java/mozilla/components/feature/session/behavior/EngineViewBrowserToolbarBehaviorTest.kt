/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session.behavior

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.selection.SelectionActionDelegate
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class EngineViewBrowserToolbarBehaviorTest {

    @Test
    fun `EngineView clipping and bottom toolbar offset are kept in sync`() {
        val engineView: EngineView = spy(FakeEngineView(testContext))
        val toolbar: View = mock()
        doReturn(100).`when`(toolbar).height
        doReturn(42f).`when`(toolbar).translationY

        val behavior = EngineViewBrowserToolbarBehavior(
            mock(), null, engineView.asView(), toolbar.height, ToolbarPosition.BOTTOM
        )

        behavior.onDependentViewChanged(mock(), mock(), toolbar)

        verify(engineView).setVerticalClipping(-42)
        assertEquals(0f, engineView.asView().translationY)
    }

    @Test
    fun `EngineView clipping and top toolbar offset are kept in sync`() {
        val engineView: EngineView = spy(FakeEngineView(testContext))
        val toolbar: View = mock()
        doReturn(100).`when`(toolbar).height
        doReturn(42f).`when`(toolbar).translationY

        val behavior = EngineViewBrowserToolbarBehavior(
            mock(), null, engineView.asView(), toolbar.height, ToolbarPosition.TOP
        )

        behavior.onDependentViewChanged(mock(), mock(), toolbar)

        verify(engineView).setVerticalClipping(42)
        assertEquals(142f, engineView.asView().translationY)
    }

    @Test
    fun `Behavior does not depend on normal views`() {
        val behavior = EngineViewBrowserToolbarBehavior(
            mock(), null, mock(), 0, ToolbarPosition.BOTTOM
        )

        assertFalse(behavior.layoutDependsOn(mock(), mock(), TextView(testContext)))
        assertFalse(behavior.layoutDependsOn(mock(), mock(), EditText(testContext)))
        assertFalse(behavior.layoutDependsOn(mock(), mock(), ImageView(testContext)))
    }

    @Test
    fun `Behavior depends on BrowserToolbar`() {
        val behavior = EngineViewBrowserToolbarBehavior(
            mock(), null, mock(), 0, ToolbarPosition.BOTTOM
        )

        assertTrue(behavior.layoutDependsOn(mock(), mock(), BrowserToolbar(testContext)))
    }
}

class FakeEngineView(context: Context) : TextView(context), EngineView {
    override fun render(session: EngineSession) {}

    override fun captureThumbnail(onFinish: (Bitmap?) -> Unit) {}

    override fun clearSelection() {}

    override fun setVerticalClipping(clippingHeight: Int) {}

    override fun setDynamicToolbarMaxHeight(height: Int) {}

    override fun release() {}

    override var selectionActionDelegate: SelectionActionDelegate? = null
}

class BrowserToolbar(context: Context) : TextView(context)
