/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.sync.logins

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import mozilla.components.concept.storage.Login
import mozilla.components.concept.storage.LoginsStorage
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GeckoLoginStorageDelegateTest {

    private lateinit var loginsStorage: LoginsStorage
    private lateinit var delegate: GeckoLoginStorageDelegate
    private lateinit var scope: TestCoroutineScope

    @Before
    @Config(sdk = [21])
    fun before() {
        loginsStorage = mockLoginsStorage()
        scope = TestCoroutineScope()
        delegate = GeckoLoginStorageDelegate(loginsStorage, { false }, scope)
    }

    @Test
    @Config(sdk = [21])
    fun `WHEN passed false for shouldAutofill onLoginsFetch returns early`() {
        scope.launch {
            delegate.onLoginFetch("login")
            verify(loginsStorage, times(0)).touch(any())
        }
    }

    @Test
    @Config(sdk = [21])
    fun `WHEN passed true for shouldAutofill onLoginsFetch does not return early`() {
        delegate = GeckoLoginStorageDelegate(loginsStorage, { true }, scope)

        scope.launch {
            delegate.onLoginFetch("login")
            verify(loginsStorage, times(1)).touch(any())
        }
    }

    @Test
    @Config(sdk = [21])
    fun `WHEN onLoginsUsed is used THEN loginStorage should be touched`() {
        scope.launch {
            val login = createLogin("guid")

            delegate.onLoginUsed(login)
            verify(loginsStorage, times(1)).touch(any())
        }
    }
}

fun mockLoginsStorage(): LoginsStorage {
    val loginsStorage = mock<LoginsStorage>()
    return loginsStorage
}

fun createLogin(
    guid: String = "id",
    password: String = "password",
    username: String = "username",
    origin: String = "hostname",
    httpRealm: String = "httpRealm",
    formActionOrigin: String = "formsubmiturl",
    usernameField: String = "usernameField",
    passwordField: String = "passwordField"

) = Login(
    guid = guid,
    origin = origin,
    password = password,
    username = username,
    httpRealm = httpRealm,
    formActionOrigin = formActionOrigin,
    usernameField = usernameField,
    passwordField = passwordField
)
