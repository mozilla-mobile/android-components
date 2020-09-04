/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.kotlinx.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * Dummy dispatcher that forces you to pass in a dispatcher when launching a
 * coroutine with `.launch`. Used to enforce always passing in a dispatcher.
 */
object InvalidDispatcher : CoroutineDispatcher() {

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        throw UnsupportedOperationException("The Invalid dispatcher cannot be used to run a coroutine. " +
            "Specify a dispatcher in the launch() function instead.")
    }

    override fun toString(): String = "Invalid"
}
