/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash.handler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import mozilla.components.lib.crash.Crash
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.service.CrashReporterService
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExceptionHandlerTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `ExceptionHandler forwards crashes to CrashReporter`() {
        var capturedCrash: Crash? = null
        val service = object : CrashReporterService {
            override fun report(crash: Crash.UncaughtExceptionCrash) {
                capturedCrash = crash
            }

            override fun report(crash: Crash.NativeCodeCrash) {
                fail("Did not expect native crash")
            }
        }

        val crashReporter = spy(CrashReporter(
            shouldPrompt = CrashReporter.Prompt.NEVER,
            services = listOf(service)
        ))

        val handler = ExceptionHandler(
            context,
            crashReporter)

        val exception = RuntimeException("Hello World")
        handler.uncaughtException(Thread.currentThread(), exception)

        verify(crashReporter).onCrash(any(), any())

        assertNotNull(capturedCrash)

        val crash = capturedCrash as? Crash.UncaughtExceptionCrash
            ?: throw AssertionError("Expected UncaughtExceptionCrash instance")

        assertEquals(exception, crash.throwable)
    }

    @Test
    fun `ExceptionHandler invokes default exception handler`() {
        val defaultExceptionHandler: Thread.UncaughtExceptionHandler = mock()

        val handler = ExceptionHandler(
            context,
            mock(),
            defaultExceptionHandler
        )

        verify(defaultExceptionHandler, never()).uncaughtException(any(), any())

        val exception = RuntimeException()
        handler.uncaughtException(Thread.currentThread(), exception)

        verify(defaultExceptionHandler).uncaughtException(Thread.currentThread(), exception)
    }
}