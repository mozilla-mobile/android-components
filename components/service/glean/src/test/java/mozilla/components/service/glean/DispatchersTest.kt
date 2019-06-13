/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class DispatchersTest {

    @Test
    @Suppress("EXPERIMENTAL_API_USAGE")
    fun `API scope runs off the main thread`() = runBlocking {
        val mainThread = Thread.currentThread()
        var threadCanary = false

        Dispatchers.API.setTestingMode(false)

        Dispatchers.API.launch {
            assertNotSame(mainThread, Thread.currentThread())
            // Use the canary bool to make sure this is getting called before
            // the test completes.
            assertEquals(false, threadCanary)
            threadCanary = true
        }!!.join()

        Dispatchers.API.setTestingMode(true)
        assertEquals(true, threadCanary)
        assertSame(mainThread, Thread.currentThread())
    }
}
