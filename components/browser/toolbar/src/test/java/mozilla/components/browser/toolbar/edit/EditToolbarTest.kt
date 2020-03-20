/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.toolbar.edit

import android.view.KeyEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.R
import mozilla.components.concept.toolbar.AutocompleteDelegate
import mozilla.components.support.base.Component
import mozilla.components.support.base.facts.Action
import mozilla.components.support.base.facts.processor.CollectionProcessor
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class EditToolbarTest {
    private fun createEditToolbar(): Pair<BrowserToolbar, EditToolbar> {
        val toolbar: BrowserToolbar = mock()
        val displayToolbar = EditToolbar(
            testContext, toolbar,
            View.inflate(testContext, R.layout.mozac_browser_toolbar_edittoolbar, null)
        )
        return Pair(toolbar, displayToolbar)
    }

    @Test
    fun `entered text is forwarded to async autocomplete filter`() {
        val toolbar = BrowserToolbar(testContext)

        toolbar.edit.views.url.onAttachedToWindow()

        val latch = CountDownLatch(1)
        var invokedWithParams: List<Any?>? = null
        toolbar.setAutocompleteListener { p1, p2 ->
            invokedWithParams = listOf(p1, p2)
            latch.countDown()
        }

        toolbar.edit.views.url.setText("Hello")

        // Autocomplete filter will be invoked on a worker thread.
        // Serialize here for the sake of tests.
        runBlocking {
            latch.await()
        }

        assertEquals("Hello", invokedWithParams!![0])
        assertTrue(invokedWithParams!![1] is AutocompleteDelegate)
    }

    @Test
    fun `focus change is forwarded to listener`() {
        var listenerInvoked = false
        var value = false

        val toolbar = BrowserToolbar(testContext)
        toolbar.edit.setOnEditFocusChangeListener { hasFocus ->
            listenerInvoked = true
            value = hasFocus
        }

        // Switch to editing mode and focus view.
        toolbar.editMode()
        toolbar.edit.views.url.requestFocus()

        assertTrue(listenerInvoked)
        assertTrue(value)

        // Switch back to display mode
        listenerInvoked = false
        toolbar.displayMode()

        assertTrue(listenerInvoked)
        assertFalse(value)
    }

    @Test
    fun `entering text emits commit fact`() {
        CollectionProcessor.withFactCollection { facts ->
            val toolbar = BrowserToolbar(testContext)
            toolbar.edit.views.url.onAttachedToWindow()

            assertEquals(0, facts.size)

            toolbar.edit.views.url.setText("https://www.mozilla.org")
            toolbar.edit.views.url.dispatchKeyEvent(KeyEvent(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER,
                0))

            assertEquals(1, facts.size)

            val fact = facts[0]
            assertEquals(Component.BROWSER_TOOLBAR, fact.component)
            assertEquals(Action.COMMIT, fact.action)
            assertEquals("toolbar", fact.item)
            assertNull(fact.value)

            val metadata = fact.metadata
            assertNotNull(metadata!!)
            assertEquals(1, metadata.size)
            assertTrue(metadata.contains("autocomplete"))
            assertTrue(metadata["autocomplete"] is Boolean)
            assertFalse(metadata["autocomplete"] as Boolean)
        }
    }

    @Test
    fun `entering text emits commit fact with autocomplete metadata`() {
        CollectionProcessor.withFactCollection { facts ->
            val toolbar = BrowserToolbar(testContext)
            toolbar.edit.views.url.onAttachedToWindow()

            assertEquals(0, facts.size)

            toolbar.edit.views.url.setText("https://www.mozilla.org")

            // Fake autocomplete
            toolbar.edit.views.url.autocompleteResult = InlineAutocompleteEditText.AutocompleteResult(
                text = "hello world",
                source = "test-source",
                totalItems = 100)

            toolbar.edit.views.url.dispatchKeyEvent(KeyEvent(
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER,
                0))

            assertEquals(1, facts.size)

            val fact = facts[0]
            assertEquals(Component.BROWSER_TOOLBAR, fact.component)
            assertEquals(Action.COMMIT, fact.action)
            assertEquals("toolbar", fact.item)
            assertNull(fact.value)

            val metadata = fact.metadata
            assertNotNull(metadata!!)
            assertEquals(2, metadata.size)

            assertTrue(metadata.contains("autocomplete"))
            assertTrue(metadata["autocomplete"] is Boolean)
            assertTrue(metadata["autocomplete"] as Boolean)

            assertTrue(metadata.contains("source"))
            assertEquals("test-source", metadata["source"])
        }
    }

    @Test
    fun `clearView gone on init`() {
        val (_, editToolbar) = createEditToolbar()
        val clearView = editToolbar.views.clear
        assertTrue(clearView.visibility == View.GONE)
    }

    @Test
    fun `clearView clears text in urlView`() {
        val (_, editToolbar) = createEditToolbar()
        val clearView = editToolbar.views.clear

        editToolbar.views.url.setText("https://www.mozilla.org")
        assertTrue(editToolbar.views.url.text.isNotBlank())

        assertNotNull(clearView)
        clearView.performClick()
        assertTrue(editToolbar.views.url.text.isBlank())
    }
}
