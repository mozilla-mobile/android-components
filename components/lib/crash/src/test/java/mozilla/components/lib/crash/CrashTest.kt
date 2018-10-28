/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash

import android.content.Intent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.RuntimeException

@RunWith(RobolectricTestRunner::class)
class CrashTest {
    @Test
    fun `fromIntent() can deserialize a GeckoView crash Intent`() {
        val originalCrash = Crash.NativeCodeCrash(
            "/data/data/org.mozilla.samples.browser/files/mozilla/Crash Reports/pending/3ba5f665-8422-dc8e-a88e-fc65c081d304.dmp",
            true,
            "/data/data/org.mozilla.samples.browser/files/mozilla/Crash Reports/pending/3ba5f665-8422-dc8e-a88e-fc65c081d304.extra",
            false
        )

        val intent = Intent()
        originalCrash.fillIn(intent)

        val recoveredCrash = Crash.fromIntent(intent) as? Crash.NativeCodeCrash
            ?: throw AssertionError("Expected NativeCodeCrash instance")

        assertEquals(recoveredCrash.minidumpSuccess, true)
        assertEquals(recoveredCrash.isFatal, false)
        assertEquals(
            "/data/data/org.mozilla.samples.browser/files/mozilla/Crash Reports/pending/3ba5f665-8422-dc8e-a88e-fc65c081d304.dmp",
            recoveredCrash.minidumpPath
        )
        assertEquals(
            "/data/data/org.mozilla.samples.browser/files/mozilla/Crash Reports/pending/3ba5f665-8422-dc8e-a88e-fc65c081d304.extra",
            recoveredCrash.extrasPath
        )
    }

    @Test
    fun `Serialize and deserialize UncaughtExceptionCrash`() {
        val exception = RuntimeException("Hello World!")

        val originalCrash = Crash.UncaughtExceptionCrash(exception)

        val intent = Intent()
        originalCrash.fillIn(intent)

        val recoveredCrash = Crash.fromIntent(intent) as? Crash.UncaughtExceptionCrash
            ?: throw AssertionError("Expected UncaughtExceptionCrash instance")

        assertEquals(exception, recoveredCrash.throwable)
        assertEquals("Hello World!", recoveredCrash.throwable.message)
        assertArrayEquals(exception.stackTrace, recoveredCrash.throwable.stackTrace)
    }

    @Test
    fun `isCrashIntent()`() {
        assertFalse(Crash.isCrashIntent(Intent()))

        assertFalse(Crash.isCrashIntent(Intent()
            .putExtra("crash", "I am a crash!")))

        assertTrue(Crash.isCrashIntent(
            Intent().apply {
                Crash.UncaughtExceptionCrash(RuntimeException()).fillIn(this)
            }
        ))

        assertTrue(Crash.isCrashIntent(
            Intent().apply {
                val crash = Crash.NativeCodeCrash("", true, "", false)
                crash.fillIn(this)
            }
        ))
    }
}
