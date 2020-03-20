/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.migration

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileNotFoundException

@RunWith(AndroidJUnit4::class)
class FennecSessionMigrationTest {
    @Test
    fun `Migrate multiple open tabs`() {
        val profilePath = File(getTestPath("sessions"), "test-case1")
        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value

        assertEquals(6, snapshot.sessions.size)
            assertEquals(5, snapshot.selectedSessionIndex)

        snapshot.sessions[0].also {
            assertEquals("https://en.m.wikipedia.org/wiki/James_Park_Woods",
                it.session.url)

            assertEquals("James Park Woods - Wikipedia",
                it.session.title)
        }

        snapshot.sessions[1].also {
            assertEquals("https://m.youtube.com/watch?v=fBbKagy1dD8",
                it.session.url)

            assertEquals("35 DIY IDEAS YOU NEED IN YOUR LIFE RIGHT NOW - YouTube",
                it.session.title)
        }

        snapshot.sessions[2].also {
            assertEquals("about:addons",
                it.session.url)

            assertEquals("Add-ons",
                it.session.title)
        }

        snapshot.sessions[3].also {
            assertEquals("about:firefox",
                it.session.url)

            assertEquals("About Fennec",
                it.session.title)
        }

        snapshot.sessions[4].also {
            assertEquals("https://www.theverge.com/2019/9/18/20871860/huawei-mate-30-photos-videos-leak-watch-gt-2-fitness-band-tv-android-tablet-harmony-os",
                it.session.url)

            assertEquals("Huawei’s Thursday event lineup apparently leaks in full",
                it.session.title)
        }

        snapshot.sessions[5].also {
            assertEquals("https://www.microsoft.com/de-de/p/surface-pro-6/8ZCNC665SLQ5?activetab=pivot%3aoverviewtab",
                it.session.url)

            assertEquals("Entdecken Sie das Surface Pro 6 – Ultraleicht und vielseitig – Microsoft Surface",
                it.session.title)
        }
    }

    @Test
    fun `profile not existing`() {
        val profilePath = File(getTestPath("sessions"), "not-existing")
        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Failure)

        val throwables = (result as Result.Failure).throwables
        assertEquals(1, throwables.size)
        assertTrue(throwables[0] is FileNotFoundException)
    }

    @Test
    fun `broken JSON with fallback`() {
        val profilePath = File(getTestPath("sessions"), "broken-json")
        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertEquals(1, snapshot.sessions.size)
        assertEquals(0, snapshot.selectedSessionIndex)

        snapshot.sessions[0].also {
            assertEquals("https://en.m.wikipedia.org/wiki/Main_Page",
                it.session.url)

            assertEquals("Wikipedia, the free encyclopedia",
                it.session.title)
        }
    }

    @Test
    fun `no windows in session store`() {
        val profilePath = File(getTestPath("sessions"), "no-windows")
        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertTrue(snapshot.sessions.isEmpty())
        assertEquals(-1, snapshot.selectedSessionIndex)
    }

    @Test
    fun `with empty tab entries and unneeded backup`() {
        val profilePath = File(getTestPath("sessions"), "test-case2")
        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertEquals(2, snapshot.sessions.size)
        assertEquals(1, snapshot.selectedSessionIndex)

        snapshot.sessions[0].also {
            assertEquals("https://en.m.wikipedia.org/wiki/Climbing",
                it.session.url)

            assertEquals("Climbing - Wikipedia",
                it.session.title)
        }

        snapshot.sessions[1].also {
            assertEquals("https://www.mozilla.org/en-US/firefox/accounts/",
                it.session.url)

            assertEquals("There is a way to protect your privacy. Join Firefox.",
                it.session.title)
        }
    }

    @Test
    fun `large session store`() {
        val profilePath = File(getTestPath("sessions"), "large")

        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertEquals(21, snapshot.sessions.size)
        assertEquals(12, snapshot.selectedSessionIndex)
    }

    /**
     * We expect in this test run to:
     * - Filter out the about:home tab.
     * - Rewrite the about:reader tab to its original URL.
     * - Rewrite the selected index after about:home was filtered out.
     */
    @Test
    fun `about-home and about-reader URLs`() {
        val profilePath = File(getTestPath("sessions"), "about-urls")

        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertEquals(2, snapshot.sessions.size)
        assertEquals(1, snapshot.selectedSessionIndex)

        snapshot.sessions[0].also {
            assertEquals("https://www.spiegel.de/",
                it.session.url)

            assertEquals("DER SPIEGEL | Online-Nachrichten",
                it.session.title)
        }

        snapshot.sessions[1].also {
            assertEquals("https://www.spiegel.de/politik/deutschland/fdp-parteivorstand-spricht-lindner-nach-kemmerich-wahl-vertrauen-aus-a-47e0a21c-7617-4549-b6dc-716c0363cbc2",
                it.session.url)

            assertEquals("FDP-Parteivorstand spricht Lindner Vertrauen aus - DER SPIEGEL - Politik",
                it.session.title)
        }
    }

    /**
     * We expect in this test run to:
     * - Filter out the single about:home tab.
     * - Reset the selected index to -1.
     */
    @Test
    fun `only about-home`() {
        val profilePath = File(getTestPath("sessions"), "only-about-home")

        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertEquals(0, snapshot.sessions.size)
        assertEquals(-1, snapshot.selectedSessionIndex)
    }

    @Test
    fun `Tab with null title`() {
        val profilePath = File(getTestPath("sessions"), "null-title")

        val result = FennecSessionMigration.migrate(profilePath, mock())

        assertTrue(result is Result.Success)
        val snapshot = (result as Result.Success).value
        assertEquals(1, snapshot.sessions.size)
        assertEquals(0, snapshot.selectedSessionIndex)

        snapshot.sessions[0].also {
            assertEquals("https://www.mozilla.org/",
                it.session.url)

            assertEquals("https://www.mozilla.org/",
                it.session.title)
        }
    }
}
