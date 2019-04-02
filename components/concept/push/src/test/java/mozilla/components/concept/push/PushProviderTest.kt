/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.concept.push

import mozilla.components.support.test.mock
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class PushProviderTest {

    @Before
    fun setup() {
        PushProvider.reset()
    }

    @Test
    fun install() {
        val processor: PushProcessor = mock()

        PushProvider.install(processor)

        assertNotNull(PushProvider.requireInstance)
    }

    @Test(expected = IllegalStateException::class)
    fun `requireInstance throws if install not called first`() {
        PushProvider.requireInstance
    }
}