/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.auth

import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.M
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.createAddedTestFragment
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BiometricPromptAuthTest {

    private lateinit var biometricPromptAuth: BiometricPromptAuth
    private lateinit var biometricManager: BiometricManager
    private lateinit var fragment: Fragment
    private lateinit var biometricUtils: BiometricUtils

    @Before
    fun setup() {
        fragment = createAddedTestFragment { Fragment() }
        biometricUtils = spy(BiometricUtils())
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
        biometricManager = mock()
        doReturn(biometricManager).`when`(biometricUtils).getAndroidBiometricManager(testContext)
    }

    @Config(sdk = [LOLLIPOP])
    @Test
    fun `canUseFeature checks for SDK compatible`() {
        assertFalse(biometricUtils.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is available and biometric is enrolled THEN canUseFeature return true`() {
        doReturn(true).`when`(biometricUtils).isHardwareAvailable(biometricManager)
        doReturn(true).`when`(biometricUtils).isEnrolled(biometricManager)
        assertTrue(biometricUtils.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is available and biometric is not enrolled THEN canUseFeature return false`() {
        doReturn(false).`when`(biometricUtils).isHardwareAvailable(biometricManager)
        doReturn(true).`when`(biometricUtils).isEnrolled(biometricManager)
        assertFalse(biometricUtils.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is not available and biometric is not enrolled THEN canUseFeature return false`() {
        doReturn(false).`when`(biometricUtils).isHardwareAvailable(biometricManager)
        doReturn(false).`when`(biometricUtils).isEnrolled(biometricManager)
        assertFalse(biometricUtils.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is not available and biometric is enrolled THEN canUseFeature return false`() {
        doReturn(false).`when`(biometricUtils).isHardwareAvailable(biometricManager)
        doReturn(true).`when`(biometricUtils).isEnrolled(biometricManager)
        assertFalse(biometricUtils.canUseFeature(testContext))
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
