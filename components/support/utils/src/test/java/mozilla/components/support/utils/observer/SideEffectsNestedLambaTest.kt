/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils.observer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * This test verifies the behavior of SideEffect when observers modify the Observable in a nested
 * structure.
 */
class SideEffectsNestedLambaTest {
    @Test
    fun testReturningFromInlinedLambda() {
        val observable = TestObservable()

        val observer1 = CollectingObserver().also { observable.register(it) }
        val observer2 = AddingMoreObserver().also { observable.register(it) }
        val observer3 = AddingObserver().also { observable.register(it) }
        val observer4 = CollectingObserver().also { observable.register(it) }

        observable.changeValue(2)
        observable.changeValue(0) // Will be ignored by observable
        observable.changeValue(1) // Will emit 23 and this will emit 0 (will ne ignored)
        observable.changeValue(5)

        // Observers should see: 2, 1, 23, 5

        listOf(
            observer1.numbers, observer2.numbers, observer3.numbers, observer4.numbers
        ).forEach { numbers ->
            assertEquals(4, numbers.size)

            assertEquals(2, numbers[0])
            assertEquals(1, numbers[1])
            assertEquals(23, numbers[2])
            assertEquals(5, numbers[3])
        }
    }

    /**
     * This observer just collects all values it receives in a list.
     */
    private open class CollectingObserver(
        val numbers: MutableList<Int> = mutableListOf()
    ) : TestObserver {
        override fun onValueChanges(observable: TestObservable, value: Int) {
            numbers.add(value)
        }
    }

    /**
     * This observer collects all values and adds a 23 whenever it sees a 1.
     */
    private class AddingObserver : CollectingObserver() {
        override fun onValueChanges(observable: TestObservable, value: Int) {
            super.onValueChanges(observable, value)

            if (value == 1) {
                observable.changeValue(23)
            }
        }
    }

    /**
     * This observers collects all values and adds a 0 whenever it sees a 23.
     */
    private class AddingMoreObserver : CollectingObserver() {
        override fun onValueChanges(observable: TestObservable, value: Int) {
            super.onValueChanges(observable, value)

            if (value == 23) {
                observable.changeValue(0)
            }
        }
    }

    /**
     * This observable notifies all observers about changed values except if the value is a 0.
     */
    private class TestObservable(
        private val registry: ObserverRegistry<TestObserver> = ObserverRegistry()
    ) : Observable<TestObserver> by registry {
        private val sideEffects = SideEffects()

        fun changeValue(value: Int) = sideEffects.modifyWithoutSideEffects {
            if (value == 0) {
                return@modifyWithoutSideEffects
            }

            notifyObservers { onValueChanges(this@TestObservable, value) }
        }
    }

    private interface TestObserver {
        fun onValueChanges(observable: TestObservable, value: Int)
    }
}