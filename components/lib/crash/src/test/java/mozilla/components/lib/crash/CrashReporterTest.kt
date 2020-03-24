/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import mozilla.components.lib.crash.service.CrashReporterService
import mozilla.components.support.test.any
import mozilla.components.support.test.eq
import mozilla.components.support.test.expectException
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import java.lang.reflect.Modifier

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class CrashReporterTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val scope = TestCoroutineScope(testDispatcher)

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Before
    fun setUp() {
        CrashReporter.reset()
    }

    @Test
    fun `Calling install() will setup uncaught exception handler`() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        CrashReporter(
            services = listOf(mock())
        ).install(testContext)

        val newHandler = Thread.getDefaultUncaughtExceptionHandler()
        assertNotNull(newHandler)

        assertNotEquals(defaultHandler, newHandler)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CrashReporter throws if no service is defined`() {
        CrashReporter(emptyList())
            .install(testContext)
    }

    @Test
    fun `CrashReporter will submit report immediately if setup with Prompt-NEVER`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.NEVER,
            scope = scope
        ).install(testContext))

        val crash: Crash.UncaughtExceptionCrash = mock()

        reporter.onCrash(testContext, crash)

        verify(reporter).sendCrashTelemetry(testContext, crash)
        verify(reporter).sendCrashReport(testContext, crash)
        verify(reporter, never()).showPrompt(any(), eq(crash))
    }

    @Test
    fun `CrashReporter will show prompt if setup with Prompt-ALWAYS`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.ALWAYS,
            scope = scope
        ).install(testContext))

        val crash: Crash.UncaughtExceptionCrash = mock()

        reporter.onCrash(testContext, crash)

        verify(reporter).sendCrashTelemetry(testContext, crash)
        verify(reporter, never()).sendCrashReport(testContext, crash)
        verify(reporter).showPrompt(any(), eq(crash))
    }

    @Test
    fun `CrashReporter will submit report immediately for non native crash and with setup Prompt-ONLY_NATIVE_CRASH`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.ONLY_NATIVE_CRASH,
            scope = scope
        ).install(testContext))

        val crash: Crash.UncaughtExceptionCrash = mock()

        reporter.onCrash(testContext, crash)

        verify(reporter).sendCrashTelemetry(testContext, crash)
        verify(reporter).sendCrashReport(testContext, crash)
        verify(reporter, never()).showPrompt(any(), eq(crash))
    }

    @Test
    fun `CrashReporter will show prompt for fatal native crash and with setup Prompt-ONLY_NATIVE_CRASH`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.ONLY_NATIVE_CRASH,
            scope = scope
        ).install(testContext))

        val crash = Crash.NativeCodeCrash(
            "dump.path",
            true,
            "extras.path",
            isFatal = true,
            breadcrumbs = arrayListOf())

        reporter.onCrash(testContext, crash)

        verify(reporter).sendCrashTelemetry(testContext, crash)
        verify(reporter).showPrompt(any(), eq(crash))
        verify(reporter, never()).sendCrashReport(testContext, crash)
        verify(service, never()).report(crash)
    }

    @Test
    fun `CrashReporter will submit crash telemetry even if crash report requires prompt`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.ALWAYS
        ).install(testContext))

        val crash: Crash.UncaughtExceptionCrash = mock()

        reporter.onCrash(testContext, crash)

        verify(reporter).sendCrashTelemetry(testContext, crash)
        verify(reporter, never()).sendCrashReport(testContext, crash)
        verify(reporter).showPrompt(any(), eq(crash))
    }

    @Test
    fun `CrashReporter will not prompt the user if there is no crash services`() {
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.ALWAYS
        ).install(testContext))

        val crash: Crash.UncaughtExceptionCrash = mock()

        reporter.onCrash(testContext, crash)

        verify(reporter).sendCrashTelemetry(testContext, crash)
        verify(reporter, never()).sendCrashReport(testContext, crash)
        verify(reporter, never()).showPrompt(any(), eq(crash))
    }

    @Test
    fun `CrashReporter will not send crash telemetry if there is no telemetry service`() {
        val service: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            shouldPrompt = CrashReporter.Prompt.ALWAYS
        ).install(testContext))

        val crash: Crash.UncaughtExceptionCrash = mock()

        reporter.onCrash(testContext, crash)

        verify(reporter, never()).sendCrashTelemetry(testContext, crash)
        verify(reporter).showPrompt(any(), eq(crash))
    }

    @Test
    fun `Calling install() with no crash services or telemetry crash services will throw exception`() {
        var exceptionThrown = false

        try {
            CrashReporter(
                shouldPrompt = CrashReporter.Prompt.ALWAYS
            ).install(testContext)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }

        assert(exceptionThrown)
    }

    @Test
    fun `Calling install() with at least one crash service or telemetry crash service will not throw exception`() {
        var exceptionThrown = false

        try {
            CrashReporter(
                services = listOf(mock())
            ).install(testContext)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assert(!exceptionThrown)

        try {
            CrashReporter(
                telemetryServices = listOf(mock())
            ).install(testContext)
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assert(!exceptionThrown)
    }

    @Test
    fun `CrashReporter is enabled by default`() {
        val reporter = spy(CrashReporter(
            services = listOf(mock()),
            shouldPrompt = CrashReporter.Prompt.ONLY_NATIVE_CRASH
        ).install(testContext))

        assertTrue(reporter.enabled)
    }

    @Test
    fun `CrashReporter will not prompt and not submit report if not enabled`() {
        val service: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            shouldPrompt = CrashReporter.Prompt.ALWAYS,
            scope = scope
        ).install(testContext))

        reporter.enabled = false

        val crash: Crash.UncaughtExceptionCrash = mock()
        reporter.onCrash(testContext, crash)

        verify(reporter, never()).sendCrashReport(testContext, crash)
        verify(reporter, never()).sendCrashTelemetry(testContext, crash)
        verify(reporter, never()).showPrompt(any(), eq(crash))

        verify(service, never()).report(crash)
    }

    @Test
    fun `CrashReporter submits crashes`() {
        val crash = mock<Crash>()
        val service = mock<CrashReporterService>()
        val reporter = spy(CrashReporter(
            services = listOf(service),
            shouldPrompt = CrashReporter.Prompt.NEVER,
            scope = scope
        ).install(testContext))

        reporter.onCrash(testContext, crash)
        verify(reporter, never()).sendCrashTelemetry(testContext, crash)
    }

    @Test
    fun `CrashReporter forwards uncaught exception crashes to service`() {
        var exceptionCrash = false

        val service = object : CrashReporterService {
            override fun report(crash: Crash.UncaughtExceptionCrash) {
                exceptionCrash = true
            }

            override fun report(crash: Crash.NativeCodeCrash) {
            }

            override fun report(throwable: Throwable) {
            }
        }

        val reporter = spy(CrashReporter(
            services = listOf(service),
            shouldPrompt = CrashReporter.Prompt.NEVER
        ).install(testContext))

        reporter.submitReport(
            Crash.UncaughtExceptionCrash(RuntimeException(), arrayListOf())
        ).joinBlocking()
        assertTrue(exceptionCrash)
    }

    @Test
    fun `CrashReporter forwards native crashes to service`() {
        var nativeCrash = false

        val service = object : CrashReporterService {
            override fun report(crash: Crash.UncaughtExceptionCrash) {
            }

            override fun report(crash: Crash.NativeCodeCrash) {
                nativeCrash = true
            }

            override fun report(throwable: Throwable) {
            }
        }

        val reporter = spy(CrashReporter(
            services = listOf(service),
            shouldPrompt = CrashReporter.Prompt.NEVER
        ).install(testContext))

        reporter.submitReport(
            Crash.NativeCodeCrash("", true, "", false, arrayListOf())
        ).joinBlocking()
        assertTrue(nativeCrash)
    }

    @Test
    fun `CrashReporter forwards caught exception crashes to service`() {
        var exceptionCrash = false

        val service = object : CrashReporterService {
            override fun report(crash: Crash.UncaughtExceptionCrash) {
            }

            override fun report(crash: Crash.NativeCodeCrash) {
            }

            override fun report(throwable: Throwable) {
                exceptionCrash = true
            }
        }

        val reporter = spy(CrashReporter(
            services = listOf(service),
            shouldPrompt = CrashReporter.Prompt.NEVER
        ).install(testContext))

        reporter.submitCaughtException(
            RuntimeException()
        ).joinBlocking()

        assertTrue(exceptionCrash)
    }

    @Test
    fun `CrashReporter forwards native crashes to telemetry service`() {
        var nativeCrash = false

        val telemetryService = object : CrashReporterService {
            override fun report(crash: Crash.UncaughtExceptionCrash) {
            }

            override fun report(crash: Crash.NativeCodeCrash) {
                nativeCrash = true
            }

            override fun report(throwable: Throwable) {
            }
        }

        val reporter = spy(CrashReporter(
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.NEVER
        ).install(testContext))

        reporter.submitCrashTelemetry(
            Crash.NativeCodeCrash("", true, "", false, arrayListOf())
        ).joinBlocking()
        assertTrue(nativeCrash)
    }

    @Test
    fun `Internal reference is set after calling install`() {
        expectException(IllegalStateException::class) {
            CrashReporter.requireInstance
        }

        val reporter = CrashReporter(
            services = listOf(mock())
        )

        expectException(IllegalStateException::class) {
            CrashReporter.requireInstance
        }

        reporter.install(testContext)

        assertNotNull(CrashReporter.requireInstance)
    }

    @Test
    fun `CrashReporter invokes PendingIntent if provided`() {
        val context = Robolectric.setupActivity(Activity::class.java)

        val intent = Intent("action")
        val pendingIntent = spy(PendingIntent.getActivity(context, 0, intent, 0))

        val reporter = CrashReporter(
            shouldPrompt = CrashReporter.Prompt.ALWAYS,
            services = listOf(mock()),
            nonFatalCrashIntent = pendingIntent
        ).install(context)

        val nativeCrash = Crash.NativeCodeCrash(
            "dump.path",
            true,
            "extras.path",
            isFatal = false,
            breadcrumbs = arrayListOf())
        reporter.onCrash(context, nativeCrash)

        verify(pendingIntent).send(eq(context), eq(0), any())

        val receivedIntent = shadowOf(context).nextStartedActivity

        val receivedCrash = Crash.fromIntent(receivedIntent) as? Crash.NativeCodeCrash
            ?: throw AssertionError("Expected NativeCodeCrash instance")

        assertEquals(nativeCrash, receivedCrash)
        assertEquals("dump.path", receivedCrash.minidumpPath)
        assertEquals(true, receivedCrash.minidumpSuccess)
        assertEquals("extras.path", receivedCrash.extrasPath)
        assertEquals(false, receivedCrash.isFatal)
    }

    @Test
    fun `CrashReporter sends telemetry but don't send native crash if the crash is non-fatal and nonFatalPendingIntent is not null`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.NEVER,
            nonFatalCrashIntent = mock(),
            scope = scope
        ).install(testContext))

        val nativeCrash = Crash.NativeCodeCrash(
            "dump.path",
            true,
            "extras.path",
            isFatal = false,
            breadcrumbs = arrayListOf())
        reporter.onCrash(testContext, nativeCrash)

        verify(reporter, never()).sendCrashReport(testContext, nativeCrash)
        verify(reporter, times(1)).sendCrashTelemetry(testContext, nativeCrash)
        verify(reporter, never()).showPrompt(any(), eq(nativeCrash))
    }

    @Test
    fun `CrashReporter sends telemetry and crash if the crash is non-fatal and nonFatalPendingIntent is null`() {
        val service: CrashReporterService = mock()
        val telemetryService: CrashReporterService = mock()

        val reporter = spy(CrashReporter(
            services = listOf(service),
            telemetryServices = listOf(telemetryService),
            shouldPrompt = CrashReporter.Prompt.NEVER,
            scope = scope
        ).install(testContext))

        val nativeCrash = Crash.NativeCodeCrash(
            "dump.path",
            true,
            "extras.path",
            isFatal = false,
            breadcrumbs = arrayListOf())
        reporter.onCrash(testContext, nativeCrash)

        verify(reporter, times(1)).sendCrashReport(testContext, nativeCrash)
        verify(reporter, times(1)).sendCrashTelemetry(testContext, nativeCrash)
        verify(reporter, never()).showPrompt(any(), eq(nativeCrash))
    }

    @Test
    fun `CrashReporter instance writes are visible across threads`() {
        val instanceField = CrashReporter::class.java.getDeclaredField("instance")
        assertTrue(Modifier.isVolatile(instanceField.modifiers))
    }
}
