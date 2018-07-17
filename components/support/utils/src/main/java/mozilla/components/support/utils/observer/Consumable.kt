/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils.observer

class Consumable<T> private constructor(
    var value: T?
) {
    inline fun consume(block: (value: T) -> Boolean) {
        if (value?.let(block) == true) {
            value = null
        }
    }

    fun consume(blocks: List<(value: T) -> Boolean>) {
        value?.let { x ->
            if (blocks.map { f -> f(x) }.contains(true)) {
                value = null
            }
        }
    }

    fun peek(): T = value!!

    fun isConsumed() = value == null

    companion object {
        fun <T> from(value: T): Consumable<T> = Consumable(value)

        fun <T> empty(): Consumable<T> = Consumable(null)
    }
}