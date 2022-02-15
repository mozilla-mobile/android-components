/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.CancellableOperation
import org.mozilla.geckoview.GeckoResult
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Wait for a GeckoResult to be complete in a co-routine.
 */
suspend fun <T> GeckoResult<T>.await() = suspendCoroutine<T?> { continuation ->
    Log.w("gri", "2.1")
    Log.w("gri", "looper: $looper")
    then(
        {
            Log.w("gri", "2.2")
            continuation.resume(it)
            Log.w("gri", "2.3")
            GeckoResult<Void>()
        },
        {
            Log.w("gri", "2.4")
            continuation.resumeWithException(it)
            Log.w("gri", "2.5")
            GeckoResult<Void>()
        }
    )
}

/**
 * Converts a [GeckoResult] to a [CancellableOperation].
 */
fun <T> GeckoResult<T>.asCancellableOperation(): CancellableOperation {
    val geckoResult = this
    return object : CancellableOperation {
        override fun cancel(): Deferred<Boolean> {
            val result = CompletableDeferred<Boolean>()
            geckoResult.cancel().then(
                {
                    result.complete(it ?: false)
                    GeckoResult<Void>()
                },
                { throwable ->
                    result.completeExceptionally(throwable)
                    GeckoResult<Void>()
                }
            )
            return result
        }
    }
}

/**
 * Create a GeckoResult from a co-routine.
 */
@Suppress("TooGenericExceptionCaught")
fun <T> CoroutineScope.launchGeckoResult(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = GeckoResult<T>().apply {
    Log.w("gri", "1.1")
    launch(context, start) {
        Log.w("gri", "1.2")
        try {
            Log.w("gri", "1.3")
            val value = block()
            Log.w("gri", "1.4")
            complete(value)
            Log.w("gri", "1.5")
        } catch (exception: Throwable) {
            Log.w("gri", "1.6")
            completeExceptionally(exception)
        }
    }
}
