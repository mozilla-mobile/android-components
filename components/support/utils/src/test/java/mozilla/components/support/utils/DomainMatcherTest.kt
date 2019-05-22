/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DomainMatcherTest {

    @Test
    fun `should perform basic domain matching for a given query`() {
        assertNull(segmentAwareDomainMatch("moz", listOf()))

        val urls = listOf(
                "http://www.mozilla.org", "http://Firefox.com",
                "https://mobile.twitter.com", "https://m.youtube.com",
                "https://en.Wikipedia.org/Wiki/Mozilla",
                "http://192.168.254.254:8000", "http://192.168.254.254:8000/admin",
                "http://иННая.локаль", // TODO add more test data for non-english locales
                "about:config", "about:crashes"
        )
        // Full url matching.
        assertEquals(
                DomainMatch("http://www.mozilla.org", "http://www.mozilla.org"),
                segmentAwareDomainMatch("http://www.m", urls)
        )
        // Protocol stripping.
        assertEquals(
                DomainMatch("http://www.mozilla.org", "www.mozilla.org"),
                segmentAwareDomainMatch("www.moz", urls)
        )
        // Subdomain stripping.
        assertEquals(
                DomainMatch("http://www.mozilla.org", "mozilla.org"),
                segmentAwareDomainMatch("moz", urls)
        )
        assertEquals(
                DomainMatch("https://mobile.twitter.com", "twitter.com"),
                segmentAwareDomainMatch("twit", urls)
        )
        assertEquals(
                DomainMatch("https://m.youtube.com", "youtube.com"),
                segmentAwareDomainMatch("yo", urls)
        )
        // Case insensitivity in the host and in the path. Subdomain matching and stripping.
        assertEquals(
                DomainMatch("https://en.wikipedia.org/wiki/mozilla", "en.wikipedia.org/wiki/mozilla"),
                segmentAwareDomainMatch("en", urls)
        )
        assertEquals(
                DomainMatch("https://en.wikipedia.org/wiki/mozilla", "en.wikipedia.org/wiki/mozilla"),
                segmentAwareDomainMatch("en.wikipedia.org/wi", urls)
        )
        assertEquals(
                DomainMatch("http://firefox.com", "firefox.com"),
                segmentAwareDomainMatch("fire", urls)
        )
        // Urls with ports.
        assertEquals(
                DomainMatch("http://192.168.254.254:8000", "192.168.254.254:8000"),
                segmentAwareDomainMatch("192", urls)
        )
        assertEquals(
                DomainMatch("http://192.168.254.254:8000/admin", "192.168.254.254:8000/admin"),
                segmentAwareDomainMatch("192.168.254.254:8000/a", urls)
        )

        // About urls.
        assertEquals(
                DomainMatch("about:config", "about:config"),
                segmentAwareDomainMatch("abo", urls)
        )
        assertEquals(
                DomainMatch("about:config", "about:config"),
                segmentAwareDomainMatch("about:", urls)
        )
        assertEquals(
                DomainMatch("about:crashes", "about:crashes"),
                segmentAwareDomainMatch("about:cr", urls)
        )

        // Non-english locale.
        assertEquals(
            DomainMatch("http://инная.локаль", "инная.локаль"),
            segmentAwareDomainMatch("ин", urls)
        )

        assertNull(segmentAwareDomainMatch("nomatch", urls))
    }
}
