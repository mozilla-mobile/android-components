/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.sitepermissions

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.permission.Permission.ContentAudioCapture
import mozilla.components.concept.engine.permission.Permission.ContentGeoLocation
import mozilla.components.concept.engine.permission.Permission.ContentNotification
import mozilla.components.concept.engine.permission.Permission.ContentVideoCapture
import mozilla.components.concept.engine.permission.Permission.Generic
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.feature.sitepermissions.SitePermissionsRules.Action.ASK_TO_ALLOW
import mozilla.components.feature.sitepermissions.SitePermissionsRules.Action.BLOCKED
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SitePermissionsRulesTest {

    private lateinit var anchorView: View
    private lateinit var mockSessionManager: SessionManager
    private lateinit var rules: SitePermissionsFeature
    private lateinit var mockOnNeedToRequestPermissions: OnNeedToRequestPermissions
    private lateinit var mockStorage: SitePermissionsStorage
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        val engine = Mockito.mock(Engine::class.java)
        anchorView = View(ApplicationProvider.getApplicationContext())
        mockSessionManager = Mockito.spy(SessionManager(engine))
        mockOnNeedToRequestPermissions = mock()
        mockStorage = mock()

        rules = SitePermissionsFeature(
            context = context,
            sessionManager = mockSessionManager,
            onNeedToRequestPermissions = mockOnNeedToRequestPermissions,
            storage = mockStorage,
            fragmentManager = mock()
        )
    }

    @Test
    fun `getActionFrom must return the right action per permission`() {
        val rules = SitePermissionsRules(
            camera = ASK_TO_ALLOW,
            location = BLOCKED,
            notification = ASK_TO_ALLOW,
            microphone = BLOCKED
        )

        val mockRequest: PermissionRequest = mock()

        doReturn(listOf(ContentGeoLocation())).`when`(mockRequest).permissions
        var action = rules.getActionFrom(mockRequest)
        assertEquals(action, rules.location)

        doReturn(listOf(ContentNotification())).`when`(mockRequest).permissions
        action = rules.getActionFrom(mockRequest)
        assertEquals(action, rules.notification)

        doReturn(listOf(ContentAudioCapture())).`when`(mockRequest).permissions
        action = rules.getActionFrom(mockRequest)
        assertEquals(action, rules.microphone)

        doReturn(listOf(ContentVideoCapture())).`when`(mockRequest).permissions
        action = rules.getActionFrom(mockRequest)
        assertEquals(action, rules.camera)

        doReturn(listOf(Generic("", ""))).`when`(mockRequest).permissions
        action = rules.getActionFrom(mockRequest)
        assertEquals(action, rules.camera)
    }

    @Test
    fun `getActionFrom must return the right action for a Camera + Microphone permission`() {
        var rules = SitePermissionsRules(
            camera = ASK_TO_ALLOW,
            location = BLOCKED,
            notification = ASK_TO_ALLOW,
            microphone = BLOCKED
        )

        val mockRequest: PermissionRequest = mock()
        doReturn(true).`when`(mockRequest).containsVideoAndAudioSources()

        var action = rules.getActionFrom(mockRequest)
        assertEquals(action, BLOCKED)

        rules = SitePermissionsRules(
            camera = ASK_TO_ALLOW,
            location = BLOCKED,
            notification = ASK_TO_ALLOW,
            microphone = ASK_TO_ALLOW
        )

        action = rules.getActionFrom(mockRequest)
        assertEquals(action, ASK_TO_ALLOW)
    }
}
