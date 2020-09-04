/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.kotlinx.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class InvalidDispatcherTest {

    private val topScope = CoroutineScope(InvalidDispatcher)

    @Throws(UnsupportedOperationException::class)
    @Test
    fun `invalid dispatcher should throw when launching`() = runBlocking {
        topScope.launch {
            fail()
        }.join()
    }

    @Test
    fun `using custom dispatcher should not throw`() = runBlocking {
        topScope.launch(coroutineContext) {
            assertTrue(true)
        }.join()
    }
}
