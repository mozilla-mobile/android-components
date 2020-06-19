/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.ui.tabcounter

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.ui.tabcounter.TabCounter.Companion.INTERNAL_COUNT
import mozilla.components.ui.tabcounter.TabCounter.Companion.ONE_DIGIT_SIZE_RATIO
import mozilla.components.ui.tabcounter.TabCounter.Companion.SO_MANY_TABS_OPEN
import mozilla.components.ui.tabcounter.TabCounter.Companion.TWO_DIGITS_SIZE_RATIO
import org.junit.Assert.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

// We need order because we want to run default tab count function first.
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TabCounterTest {

    @Test
    fun `A Default tab count is a 0`() {
        val tabCounter = TabCounter(testContext)

        assertEquals(INTERNAL_COUNT.toString(), tabCounter.getText())

        assertEquals(ONE_DIGIT_SIZE_RATIO, tabCounter.currentRatio)
    }

    @Test
    fun `Set tab count as 1`() {
        val tabCounter = TabCounter(testContext)

        tabCounter.setCount(1)

        assertEquals("1", tabCounter.getText())

        assertEquals(ONE_DIGIT_SIZE_RATIO, tabCounter.currentRatio)
    }

    @Test
    fun `Set tab count as 99`() {
        val tabCounter = TabCounter(testContext)

        tabCounter.setCount(99)

        assertEquals("99", tabCounter.getText())

        assertEquals(TWO_DIGITS_SIZE_RATIO, tabCounter.currentRatio)
    }

    @Test
    fun `Set tab count as 100`() {
        val tabCounter = TabCounter(testContext)

        tabCounter.setCount(100)

        assertEquals(SO_MANY_TABS_OPEN, tabCounter.getText())

        assertEquals(ONE_DIGIT_SIZE_RATIO, tabCounter.currentRatio)
    }
}
