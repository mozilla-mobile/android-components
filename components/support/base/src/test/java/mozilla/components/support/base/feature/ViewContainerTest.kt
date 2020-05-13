/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.base.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import mozilla.components.support.base.R
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.initMocks

class ViewContainerTest {

    @Mock private lateinit var activity: Activity
    @Mock private lateinit var fragment: Fragment

    @Before
    fun setup() {
        initMocks(this)
    }

    @Test
    fun `get context from activity`() {
        val container: ViewContainer = ViewContainer.Activity(activity)
        assertEquals(activity, container.context)
    }

    @Test
    fun `get context from fragment`() {
        val mockContext: Context = mock()
        val container = ViewContainer.Fragment(fragment)
        doReturn(mockContext).`when`(fragment).requireContext()

        assertEquals(mockContext, container.context)
    }

    @Test
    fun `startActivityForResult must delegate its calls either to an activity or a fragment`() {
        val intent: Intent = mock()
        val code = 1

        var container: ViewContainer = ViewContainer.Activity(activity)
        container.startActivityForResult(intent, code)
        verify(activity).startActivityForResult(intent, code)

        container = ViewContainer.Fragment(fragment)
        container.startActivityForResult(intent, code)
        verify(fragment).startActivityForResult(intent, code)
    }

    @Test
    fun `getString must delegate its calls either to an activity or a fragment`() {
        doReturn("").`when`(activity).getString(anyInt())
        doReturn("").`when`(fragment).getString(anyInt())

        var container: ViewContainer = ViewContainer.Activity(activity)
        container.getString(R.string.mozac_support_base_locale_preference_key_locale)
        verify(activity).getString(R.string.mozac_support_base_locale_preference_key_locale)

        container = ViewContainer.Fragment(fragment)
        container.getString(R.string.mozac_support_base_locale_preference_key_locale)
        verify(fragment).getString(R.string.mozac_support_base_locale_preference_key_locale)
    }
}