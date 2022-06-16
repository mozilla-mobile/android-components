/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.auth

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.createAddedTestFragment
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class BiometricPromptAuthTest {

    private lateinit var biometricPromptAuth: BiometricPromptAuth
    private lateinit var fragment: Fragment

    @Before
    fun setup() {
        fragment = createAddedTestFragment { Fragment() }
        biometricPromptAuth = spy(
            BiometricPromptAuth(
                testContext,
                fragment,
                object : AuthenticationDelegate {
                    override fun onAuthFailure() {
                    }

                    override fun onAuthSuccess() {
                    }

                    override fun onAuthError(errorText: String) {
                    }
                }
            )
        )
    }

    @Test
    fun `prompt is created and destroyed on start and stop`() {
        assertNull(biometricPromptAuth.biometricPrompt)

        biometricPromptAuth.start()

        assertNotNull(biometricPromptAuth.biometricPrompt)

        biometricPromptAuth.stop()

        assertNull(biometricPromptAuth.biometricPrompt)
    }

    @Test
    fun `requestAuthentication invokes biometric prompt`() {
        val prompt: BiometricPrompt = mock()

        biometricPromptAuth.biometricPrompt = prompt

        biometricPromptAuth.requestAuthentication("title", "subtitle")

        verify(prompt).authenticate(any())
    }

    @Test
    fun `promptCallback fires feature callbacks`() {
        val promptCallback: BiometricPromptAuth.PromptCallback = mock()
        val prompt = BiometricPrompt(fragment, promptCallback)
        biometricPromptAuth.biometricPrompt = prompt

        promptCallback.onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "")

        verify(promptCallback).onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "")

        promptCallback.onAuthenticationFailed()

        verify(promptCallback).onAuthenticationFailed()

        promptCallback.onAuthenticationSucceeded(any())

        verify(promptCallback).onAuthenticationSucceeded(any())
    }
}
