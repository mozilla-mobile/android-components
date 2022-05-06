/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.biometric

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
class BiometricPromptFeatureTest {

    private lateinit var biometricPromptFeature: BiometricPromptFeature
    private lateinit var biometricManager: BiometricManager
    private lateinit var fragment: Fragment

    @Before
    fun setup() {
        fragment = createAddedTestFragment { Fragment() }
        biometricPromptFeature = spy(
            BiometricPromptFeature(
                testContext,
                fragment,
                object : AuthenticationCallbacks {
                    override val onAuthFailure: () -> Unit
                        get() = {}
                    override val onAuthSuccess: () -> Unit
                        get() = { }
                    override val onAuthError: (errorText: String) -> Unit
                        get() = {}
                }
            )
        )
        biometricManager = mock()

        doReturn(biometricManager).`when`(biometricPromptFeature)
            .getAndroidBiometricManager(testContext)
    }

    @Config(sdk = [LOLLIPOP])
    @Test
    fun `canUseFeature checks for SDK compatible`() {
        assertFalse(biometricPromptFeature.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is available and biometric is enrolled THEN canUseFeature return true`() {
        doReturn(true).`when`(biometricPromptFeature).isHardwareAvailable(biometricManager)
        doReturn(true).`when`(biometricPromptFeature).isEnrolled(biometricManager)
        assertTrue(biometricPromptFeature.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is available and biometric is not enrolled THEN canUseFeature return false`() {
        doReturn(false).`when`(biometricPromptFeature).isHardwareAvailable(biometricManager)
        doReturn(true).`when`(biometricPromptFeature).isEnrolled(biometricManager)
        assertFalse(biometricPromptFeature.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is not available and biometric is not enrolled THEN canUseFeature return false`() {
        doReturn(false).`when`(biometricPromptFeature).isHardwareAvailable(biometricManager)
        doReturn(false).`when`(biometricPromptFeature).isEnrolled(biometricManager)
        assertFalse(biometricPromptFeature.canUseFeature(testContext))
    }

    @Config(sdk = [M])
    @Test
    fun `GIVEN canUseFeature is called WHEN hardware is not available and biometric is enrolled THEN canUseFeature return false`() {
        doReturn(false).`when`(biometricPromptFeature).isHardwareAvailable(biometricManager)
        doReturn(true).`when`(biometricPromptFeature).isEnrolled(biometricManager)
        assertFalse(biometricPromptFeature.canUseFeature(testContext))
    }

    @Test
    fun `prompt is created and destroyed on start and stop`() {
        assertNull(biometricPromptFeature.biometricPrompt)

        biometricPromptFeature.start()

        assertNotNull(biometricPromptFeature.biometricPrompt)

        biometricPromptFeature.stop()

        assertNull(biometricPromptFeature.biometricPrompt)
    }

    @Test
    fun `requestAuthentication invokes biometric prompt`() {
        val prompt: BiometricPrompt = mock()

        biometricPromptFeature.biometricPrompt = prompt

        biometricPromptFeature.requestAuthentication("title", "subtitle")

        verify(prompt).authenticate(any())
    }

    @Test
    fun `promptCallback fires feature callbacks`() {
        val promptCallback: BiometricPromptFeature.PromptCallback = mock()
        val prompt = BiometricPrompt(fragment, promptCallback)
        biometricPromptFeature.biometricPrompt = prompt

        promptCallback.onAuthenticationError(0, "")

        verify(promptCallback).onAuthenticationError(0, "")

        promptCallback.onAuthenticationFailed()

        verify(promptCallback).onAuthenticationFailed()

        promptCallback.onAuthenticationSucceeded(any())

        verify(promptCallback).onAuthenticationSucceeded(any())
    }
}
