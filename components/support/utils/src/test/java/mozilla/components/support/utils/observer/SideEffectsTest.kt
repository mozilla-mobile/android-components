/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils.observer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * This test class demonstrates the side effects / glitches that can happen when an
 * <code>Observable</code> is modified from an observer. The test cases demonstrate that those
 * side effects can be prevent by wrapping code in the helper methods provided by the SideEffects
 * class.
 *
 * Scenario: An implementation of <code>ObservableCharacters</code> is an <code>Observable</code>
 * that allows adding characters and removing characters. Registered observer get notified whenever
 * a character gets added or removed.
 *
 * There are two registered observers:
 * (1) <code>NeverEmptyObserver</code> makes sure that we are never running out of characters. If
 * <code>ObservableCharacters</code> is empty it will add an "X" to it so that there will always
 * be at least one character in it.
 * (2) <code>TestPrintCharacterObserver</code> listens to changes and prints the added and removed
 * characters prefixed with a "+" or "-" depending on whether they got added or removed. If
 * <code>ObservableCharacters</code> is empty it will print "EMPTY".
 *
 * In the test cases we will add "A" and "B" to <code>ObservableCharacters</code>. After that we
 * will remove "B" and "A" in opposite order again. It is expected that
 * <code>TestPrintCharacterObserver</code> will print: +A, +B, -B, -A, EMPTY, +X. However the actual
 * output of <code>TestPrintCharacterObserver</code> will be: +A, +B, -B, +X, -A. In the actual
 * output X gets added before A gets removed and <code>TestPrintCharacterObserver</code> never saw
 * the empty state of <code>ObservableCharacters</code>.
 *
 * The reason for that is more obvious when looking at the order of callbacks:
 * .
 * ├─ addCharacter(A) -> (A)
 * │  ├─ NeverEmptyObserver.onCharacterAdded(A): (Does nothing)
 * │  └─ TestPrintCharacterObserver.onCharacterAdded(A): Prints +A
 * ├─ addCharacter(B) -> (A, B)
 * │  ├─ NeverEmptyObserver.onCharacterAdded(B): (Does nothing)
 * │  └─ TestPrintCharacterObserver.onCharacterAdded(B): Prints +B
 * ├─ onCharacterRemoved(B) -> (A)
 * │  ├─ NeverEmptyObserver.onCharacterRemoved(B): (Does nothing)
 * │  └─ TestPrintCharacterObserver.onCharacterRemoved(B): Prints -B
 * └── onCharacterRemoved(A) -> ()
 *    ├─ NeverEmptyObserver.onCharacterRemoved(A): Adds an X because the list of characters is empty.
 *    │  └─ addCharacter(X) -> (X)
 *    │     ├─ NeverEmptyObserver.onCharacterAdded(X): (Does nothing)
 *    │     └─ TestPrintCharacterObserver.onCharacterAdded(X): Prints +X
 *    └─ TestPrintCharacterObserver.onCharacterRemoved(A): Only prints -A (List ist not empty anymore)
 *
 * SideEffects queues modifications to an <code>Observable</code> and performs them sequentially.
 */
class SideEffectsTest {
    @Test
    fun `registry prevents side effects if observer callback modifies observable`() {
        val noSideEffects = ObservableCharactersWithoutSideEffects()
        val withSideEffects = ObservableCharactersWithSideEffects()

        NeverEmptyObserver().also {
            noSideEffects.register(it)
            withSideEffects.register(it)
        }

        val printNoSideEffects = TestPrintCharacterObserver().also { noSideEffects.register(it) }
        val printWidthSideEffects = TestPrintCharacterObserver().also { withSideEffects.register(it) }

        'A'.let { character ->
            noSideEffects.addCharacter(character)
            withSideEffects.addCharacter(character)
        }

        'B'.let { character ->
            noSideEffects.addCharacter(character)
            withSideEffects.addCharacter(character)
        }

        'B'.let { character ->
            noSideEffects.removeCharacter(character)
            withSideEffects.removeCharacter(character)
        }

        'A'.let { character ->
            noSideEffects.removeCharacter(character)
            withSideEffects.removeCharacter(character)
        }

        assertEquals(5, printWidthSideEffects.printed.size)
        assertEquals("+A", printWidthSideEffects.printed[0])
        assertEquals("+B", printWidthSideEffects.printed[1])
        assertEquals("-B", printWidthSideEffects.printed[2])
        assertEquals("+X", printWidthSideEffects.printed[3])
        assertEquals("-A", printWidthSideEffects.printed[4])

        assertEquals(6, printNoSideEffects.printed.size)
        assertEquals("+A", printNoSideEffects.printed[0])
        assertEquals("+B", printNoSideEffects.printed[1])
        assertEquals("-B", printNoSideEffects.printed[2])
        assertEquals("-A", printNoSideEffects.printed[3])
        assertEquals("EMPTY", printNoSideEffects.printed[4])
        assertEquals("+X", printNoSideEffects.printed[5])
    }

    private class ObservableCharactersWithoutSideEffects(
        private val registry: ObserverRegistry<TestCharacterObserver> = ObserverRegistry()
    ) : Observable<TestCharacterObserver> by registry, ObservableCharacters {
        private val sideEffects = SideEffects()
        private val characters: MutableList<Char> = mutableListOf()

        override fun isEmpty(): Boolean = sideEffects.readWithLock { characters.isEmpty() }

        override fun addCharacter(character: Char) = sideEffects.modifyWithoutSideEffects {
            characters.add(character)
            notifyObservers { onCharacterAdded(this@ObservableCharactersWithoutSideEffects, character) }
        }

        override fun removeCharacter(character: Char) = sideEffects.modifyWithoutSideEffects {
            if (characters.remove(character)) {
                notifyObservers { onCharacterRemoved(this@ObservableCharactersWithoutSideEffects, character) }
            }
        }
    }

    private class ObservableCharactersWithSideEffects(
        private val registry: ObserverRegistry<TestCharacterObserver> = ObserverRegistry()
    ) : Observable<TestCharacterObserver> by registry, ObservableCharacters {
        private val characters: MutableList<Char> = mutableListOf()

        override fun isEmpty(): Boolean = characters.isEmpty()

        override fun addCharacter(character: Char) {
            characters.add(character)
            notifyObservers { onCharacterAdded(this@ObservableCharactersWithSideEffects, character) }
        }

        override fun removeCharacter(character: Char) {
            if (characters.remove(character)) {
                notifyObservers { onCharacterRemoved(this@ObservableCharactersWithSideEffects, character) }
            }
        }
    }

    private class NeverEmptyObserver : TestCharacterObserver {
        override fun onCharacterAdded(observable: ObservableCharacters, character: Char) = Unit

        override fun onCharacterRemoved(observable: ObservableCharacters, character: Char) {
            if (observable.isEmpty()) {
                observable.addCharacter('X')
            }
        }
    }

    private class TestPrintCharacterObserver : TestCharacterObserver {
        var printed = mutableListOf<String>()

        override fun onCharacterAdded(observable: ObservableCharacters, character: Char) {
            printed.add("+$character")
        }

        override fun onCharacterRemoved(observable: ObservableCharacters, character: Char) {
            printed.add("-$character")

            if (observable.isEmpty()) {
                printed.add("EMPTY")
            }
        }
    }

    private interface TestCharacterObserver {
        fun onCharacterAdded(observable: ObservableCharacters, character: Char)
        fun onCharacterRemoved(observable: ObservableCharacters, character: Char)
    }

    private interface ObservableCharacters {
        fun isEmpty(): Boolean
        fun addCharacter(character: Char)
        fun removeCharacter(character: Char)
    }
}