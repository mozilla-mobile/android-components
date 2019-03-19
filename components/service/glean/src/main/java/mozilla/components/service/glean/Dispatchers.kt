/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.glean

import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

@ObsoleteCoroutinesApi
internal object Dispatchers {
    class WaitableCoroutineScope(val coroutineScope: CoroutineScope) {
        // Holds the Job returned from launch{} for awaiting purposes
        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        var ioTask: Job? = null

        companion object {
            private const val JOB_TIMEOUT_MS = 500L
        }

        fun launch(
            block: suspend CoroutineScope.() -> Unit
        ): Job {
            ioTask = coroutineScope.launch(block = block)
            return ioTask!!
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun awaitJob(timeout: Long = JOB_TIMEOUT_MS) {
            ioTask?.let { job ->
                runBlocking() {
                    withTimeout(timeout) {
                        job.join()
                    }
                }
            }
        }
    }

    /**
     * A coroutine scope to make it easy to dispatch API calls off the main thread.
     * This needs to be a `var` so that our tests can override this.
     */
    var API = WaitableCoroutineScope(CoroutineScope(newSingleThreadContext("GleanAPIPool")))
}
