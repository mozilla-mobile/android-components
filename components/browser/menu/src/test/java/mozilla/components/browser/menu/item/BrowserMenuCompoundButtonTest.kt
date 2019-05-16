package mozilla.components.browser.menu.item

import android.view.LayoutInflater
import android.widget.CheckBox
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.R
import mozilla.components.support.test.robolectric.applicationContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BrowserMenuCompoundButtonTest {

    private val context by applicationContext()

    @Test
    fun `simple menu items are always visible by default`() {
        val item = SimpleTestBrowserCompoundButton("Hello") {
            // do nothing
        }

        assertTrue(item.visible())
    }

    @Test
    fun `layout resource can be inflated`() {
        val item = SimpleTestBrowserCompoundButton("Hello") {
            // do nothing
        }

        val view = LayoutInflater.from(context)
            .inflate(item.getLayoutResource(), null)

        assertNotNull(view)
    }

    @Test
    fun `clicking bound view will invoke callback and dismiss menu`() {
        var callbackInvoked = false

        val item = SimpleTestBrowserCompoundButton("Hello") { checked ->
            callbackInvoked = checked
        }

        val menu = mock(BrowserMenu::class.java)
        val view = CheckBox(context)

        item.bind(menu, view)

        view.isChecked = true

        assertTrue(callbackInvoked)
        verify(menu).dismiss()
    }

    @Test
    fun `initialState is invoked on bind`() {
        val initialState: () -> Boolean = { true }
        val item = SimpleTestBrowserCompoundButton("Hello", initialState) {}

        val menu = mock(BrowserMenu::class.java)
        val view = spy(CheckBox(context))
        item.bind(menu, view)

        verify(view).isChecked = true
    }

    class SimpleTestBrowserCompoundButton(
        label: String,
        initialState: () -> Boolean = { false },
        listener: (Boolean) -> Unit
    ) : BrowserMenuCompoundButton(label, initialState, listener) {
        override fun getLayoutResource(): Int = R.layout.mozac_browser_menu_item_simple
    }
}
