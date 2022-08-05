/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.state

import mozilla.components.support.test.ext.joinBlocking
import org.junit.Assert
import org.junit.Test


class CombineReducerTest {

    @Test
    fun `Dispatching Action executes all reducers and creates new State`() {
        val store = Store(
            TestNumberStore(),
            combineReducers
        )

        store.dispatch(TestNumberAction.Save(payload = 2)).joinBlocking()

        Assert.assertEquals(2, store.state.number)
        Assert.assertEquals(listOf(2), store.state.evenNumberHistory)
        Assert.assertEquals(true, store.state.oddNumberHistory.isEmpty())

        store.dispatch(TestNumberAction.Save(payload = 3)).joinBlocking()
        store.dispatch(TestNumberAction.Save(payload = 6)).joinBlocking()

        Assert.assertEquals(6, store.state.number)
        Assert.assertEquals(listOf(2, 6), store.state.evenNumberHistory)
        Assert.assertEquals(listOf(3), store.state.oddNumberHistory)
    }
}

data class TestNumberStore(
    val number: Int = 0,
    val evenNumberHistory: List<Int> = emptyList(),
    val oddNumberHistory: List<Int> = emptyList()
) : State

sealed class TestNumberAction : Action {
    data class Save(val payload: Int) : TestNumberAction()
}

val reducerForSaveNumber = object : Reducer<TestNumberStore, TestNumberAction> {
    override fun invoke(state: TestNumberStore, action: TestNumberAction): TestNumberStore {
        return when (action) {
            is TestNumberAction.Save -> {
                state.copy(number = action.payload)
            }
        }
    }
}


val reducerForEvenNumbers = object : Reducer<TestNumberStore, TestNumberAction> {
    override fun invoke(state: TestNumberStore, action: TestNumberAction): TestNumberStore {
        return when (action) {
            is TestNumberAction.Save -> {
                if (action.payload.mod(2) == 0) {
                    state.copy(evenNumberHistory = state.evenNumberHistory
                        .toMutableList()
                        .apply {
                            add(action.payload)
                        })
                } else {
                    state
                }
            }
        }
    }
}


val reducerForOddNumbers = object : Reducer<TestNumberStore, TestNumberAction> {
    override fun invoke(state: TestNumberStore, action: TestNumberAction): TestNumberStore {
        return when (action) {
            is TestNumberAction.Save -> {
                if (action.payload.mod(2) != 0) {
                    state.copy(oddNumberHistory = state.oddNumberHistory
                        .toMutableList()
                        .apply {
                            add(action.payload)
                        })
                } else {
                    state
                }
            }
        }
    }
}

val combineReducers = combineReducers(
    reducerForSaveNumber,
    reducerForEvenNumbers,
    reducerForOddNumbers
)