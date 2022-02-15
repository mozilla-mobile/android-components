/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.gecko.util.ThreadUtils
import org.mozilla.geckoview.GeckoResult

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GeckoResultTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun awaitWithResult() = runTest(UnconfinedTestDispatcher()) {
        val result = GeckoResult.fromValue(42).await()
        assertEquals(42, result)
    }

    @Test(expected = IllegalStateException::class)
    fun awaitWithException() = runTest(UnconfinedTestDispatcher()) {
        GeckoResult.fromException<Unit>(IllegalStateException()).await()
    }

    @Test
    fun fromResult() = runTest(dispatcher) {
        Dispatchers.setMain(dispatcher)
        Log.w("gri", "1")
        if (ThreadUtils.isOnUiThread()) {
            Log.w("gri", "isOnUiThread")
        }
        val result = launchGeckoResult { Log.w("gri", "2"); 42 }
        Log.w("gri", "3")

        result.then<Int> {
            Log.w("gri", "4")
            assertEquals(42, it)
            GeckoResult.fromValue(null)
        }.await()
        Log.w("gri", "5")
        Dispatchers.resetMain()
    }

    @Test
    fun fromException() = runTest(UnconfinedTestDispatcher()) {
        Log.w("gri", "1")
        val result = this.launchGeckoResult { Log.w("gri", "2"); throw IllegalStateException() }
        Log.w("gri", "3")
        result.then<Unit>(
            {
                Log.w("gri", "4")
                assertTrue("Invalid branch", false)
                GeckoResult.fromValue(null)
            },
            {
                Log.w("gri", "5")
                assertTrue(it is IllegalStateException)
                GeckoResult.fromValue(null)
            }
        ).await()
        Log.w("gri", "6")
    }

    @Test
    fun asCancellableOperation() = runTest(UnconfinedTestDispatcher()) {
        val geckoResult: GeckoResult<Int> = mock()
        val op = geckoResult.asCancellableOperation()

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromValue(false))
        assertFalse(op.cancel().await())

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromValue(null))
        assertFalse(op.cancel().await())

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromValue(true))
        assertTrue(op.cancel().await())

        whenever(geckoResult.cancel()).thenReturn(GeckoResult.fromException(IllegalStateException()))
        try {
            op.cancel().await()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
    }
}
