/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils.observer

import java.util.LinkedList

/**
 * This helper class prevents side effects (glitches) in an <code>Observable</code> implementation
 * where an observer can modify the internal state of an <code>Observable</code> from an observer
 * callback. In this situation follow-up observers may see a modified state that may conflict with
 * the change the observer was notified about.
 *
 * To avoid those side effects in an <code>Observable</code> create an internal instance of
 * <code>SideEffects</code> and wrap methods modifying the <code>Observable</code> state inside
 * <code>modifyWithoutSideEffects()</code>. Methods that just read the current state can be wrapped
 * in <code>readWithLock</code>.
 *
 * SideEffects queues modifications to an <code>Observable</code> until all observers have been
 * notified and then performs them sequentially
 *
 * For an example of observer side effects / glitches see SideEffectsTest<code>.
 */
class SideEffects {
    // Those properties have 'internal' visibility with @PublishedApi annotation so that they can be
    // inlined by the compiler.
    @PublishedApi internal val lock = Any()
    @PublishedApi internal val queue: LinkedList<() -> Unit> = LinkedList()
    @PublishedApi internal var isProcessing = false

    inline fun modifyWithoutSideEffects(crossinline action: () -> Unit) {
        synchronized(lock) {
            if (isProcessing) {
                queue.add { action() }
            } else {
                isProcessing = true

                try {
                    action()
                } finally {
                    processQueue()
                }

                isProcessing = false
            }
        }
    }

    @PublishedApi internal fun processQueue() {
        while (!queue.isEmpty()) {
            val action = queue.poll()
            action()
        }
    }

    inline fun <R> readWithLock(action: () -> R): R = synchronized(lock) {
        action()
    }
}
