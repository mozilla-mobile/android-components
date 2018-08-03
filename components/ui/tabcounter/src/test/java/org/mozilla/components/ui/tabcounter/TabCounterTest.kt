package org.mozilla.components.ui.tabcounter

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.components.ui.tabcounter.TabCounter.Companion.DEFAULT_TABS_COUNTER_TEXT
import org.mozilla.components.ui.tabcounter.TabCounter.Companion.ONE_DIGIT_SIZE_RATIO
import org.mozilla.components.ui.tabcounter.TabCounter.Companion.SO_MANY_TABS_OPEN
import org.mozilla.components.ui.tabcounter.TabCounter.Companion.TWO_DIGITS_SIZE_RATIO
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TabCounterTest {


    @Test
    fun `Default tab count is a smiley face`() {
        val tabCounter = TabCounter(RuntimeEnvironment.application)

        assertEquals(DEFAULT_TABS_COUNTER_TEXT, tabCounter.getText())

        assertEquals(0.toFloat(), tabCounter.currentTextRatio)

    }

    @Test
    fun `Set tab count as 1`() {
        val tabCounter = TabCounter(RuntimeEnvironment.application)

        tabCounter.setCount(1)

        assertEquals("1", tabCounter.getText())

        assertEquals(ONE_DIGIT_SIZE_RATIO, tabCounter.currentTextRatio)
    }

    @Test
    fun `Set tab count as 99`() {
        val tabCounter = TabCounter(RuntimeEnvironment.application)

        tabCounter.setCount(99)

        assertEquals("99", tabCounter.getText())

        assertEquals(TWO_DIGITS_SIZE_RATIO, tabCounter.currentTextRatio)
    }

    @Test
    fun `Set tab count as 100`() {
        val tabCounter = TabCounter(RuntimeEnvironment.application)

        tabCounter.setCount(100)

        assertEquals(SO_MANY_TABS_OPEN, tabCounter.getText())

        assertEquals(ONE_DIGIT_SIZE_RATIO, tabCounter.currentTextRatio)

    }
}