/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa

import java.io.Closeable
import com.sun.jna.Pointer
import kotlinx.coroutines.experimental.launch

/**
 * Base class that wraps an non-optional [Pointer] representing a pointer to a Rust object.
 * This class implements [Closeable] but does not provide an implementation, forcing all
 * subclasses to implement it. This ensures that all classes that inherit from RustObject
 * will have their [Pointer] destroyed when the Java wrapper is destroyed.
 */
abstract class RustObject<T> : Closeable {
    open var rawPointer: T? = null

    val isConsumed: Boolean
        get() = this.rawPointer == null

    fun validPointer(): T {
        return this.rawPointer!!
    }

    fun consumePointer(): T {
        val p = this.rawPointer!!
        this.rawPointer = null
        return p
    }

    protected abstract fun destroy(p: T)

    override fun close() {
        synchronized(FxaClient.INSTANCE) {
            if (rawPointer != null) {
                destroy(consumePointer())
            }
        }
    }

    companion object {
        fun getAndConsumeString(stringPtr: Pointer?): String? {
            if (stringPtr == null) {
                return null
            }
            try {
                return stringPtr.getString(0, "utf8")
            } finally {
                FxaClient.INSTANCE.fxa_str_free(stringPtr)
            }
        }

        fun <U> safeAsync(callback: (Error.ByReference) -> U): FxaResult<U> {
            val result = FxaResult<U>()
            val e = Error.ByReference()
            launch {
                synchronized(FxaClient.INSTANCE) {
                    val ret = callback(e)
                    if (e.isFailure()) {
                        result.completeExceptionally(FxaException.fromConsuming(e))
                    } else {
                        result.complete(ret)
                    }
                }
            }
            return result
        }

        fun <U> safeSync(callback: (Error.ByReference) -> U): U {
            val e = Error.ByReference()
            return synchronized(FxaClient.INSTANCE) {
                val ret = callback(e)
                if (e.isFailure()) throw FxaException.fromConsuming(e)
                ret
            }
        }
    }
}
