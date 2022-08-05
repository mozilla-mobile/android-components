/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.state

/**
 * The combineReducers helper function turns an list of [Reducer]'s into a single [Reducer]
 * you can pass to [State].
 */
fun <S : State, A : Action> combineReducers(vararg reducers: (S, A) -> S): Reducer<S, A> {
    return object : Reducer<S, A> {
        override fun invoke(state: S, action: A): S {
            return reducers.fold(state) { next, reducer ->
                reducer.invoke(next, action)
            }
        }
    }
}