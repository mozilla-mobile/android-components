/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.ktx.java.io

import org.junit.Assert.assertEquals
import org.junit.Test

class InputStreamKtTest {
    private fun testReadAll(s: String) {
        return assertEquals(s, s.byteInputStream(Charsets.UTF_8).readAll())
    }

    @Test
    fun readAll() {
        testReadAll("")
        testReadAll("hello")
    }
}
