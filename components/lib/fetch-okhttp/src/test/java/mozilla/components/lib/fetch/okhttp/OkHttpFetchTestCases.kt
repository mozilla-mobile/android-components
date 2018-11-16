/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.fetch.okhttp

import mozilla.components.concept.fetch.Client
import org.junit.Assert
import org.junit.Test

class OkHttpFetchTestCases : mozilla.components.tooling.fetch.tests.FetchTestCases() {
    override fun createNewClient(): Client = OkHttpClient(okhttp3.OkHttpClient())

    // Inherits test methods from generic test suite base class

    @Test
    fun `Client instance`() {
        // We need at least one test case defined here so that this is recognized as test class.
        Assert.assertTrue(createNewClient() is OkHttpClient)
    }
}
