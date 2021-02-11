/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.ui.tabcounter

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.ui.tabcounter.TabCounter.Companion.SO_MANY_TABS_OPEN
import mozilla.components.ui.tabcounter.databinding.MozacUiTabcounterLayoutBinding
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabCounterTest {
    @Test
    fun `Default tab count is set to zero`() {
        val tabCounter = TabCounter(testContext)
        val binding = MozacUiTabcounterLayoutBinding.bind(tabCounter.rootView)
        assertEquals("0", binding.counterText.text)
    }

    @Test
    fun `Set tab count as single digit value shows count`() {
        val tabCounter = TabCounter(testContext)
        tabCounter.setCount(1)
        val binding = MozacUiTabcounterLayoutBinding.bind(tabCounter.rootView)
        assertEquals("1", binding.counterText.text)
    }

    @Test
    fun `Set tab count as two digit number shows count`() {
        val tabCounter = TabCounter(testContext)
        tabCounter.setCount(99)
        val binding = MozacUiTabcounterLayoutBinding.bind(tabCounter.rootView)
        assertEquals("99", binding.counterText.text)
    }

    @Test
    fun `Setting tab count as three digit value shows correct icon`() {
        val tabCounter = TabCounter(testContext)
        tabCounter.setCount(100)
        val binding = MozacUiTabcounterLayoutBinding.bind(tabCounter.rootView)
        assertEquals(SO_MANY_TABS_OPEN, binding.counterText.text)
    }
}
