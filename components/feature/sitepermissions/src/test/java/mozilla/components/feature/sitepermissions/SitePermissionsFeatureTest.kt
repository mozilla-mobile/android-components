/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.sitepermissions

import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.view.View
import androidx.test.core.app.ApplicationProvider
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.concept.engine.permission.Permission.ContentAudioCapture
import mozilla.components.concept.engine.permission.Permission.ContentAudioMicrophone
import mozilla.components.concept.engine.permission.Permission.ContentGeoLocation
import mozilla.components.concept.engine.permission.Permission.ContentNotification
import mozilla.components.concept.engine.permission.Permission.ContentVideoCamera
import mozilla.components.concept.engine.permission.Permission.ContentVideoCapture
import mozilla.components.concept.engine.permission.PermissionRequest
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.test.mock
import mozilla.components.ui.doorhanger.DoorhangerPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.doReturn
import org.robolectric.RobolectricTestRunner
import java.security.InvalidParameterException

@RunWith(RobolectricTestRunner::class)
class SitePermissionsFeatureTest {

    private lateinit var anchorView: View
    private lateinit var mockSessionManager: SessionManager
    private lateinit var sitePermissionFeature: SitePermissionsFeature
    private lateinit var mockOnNeedToRequestPermissions: OnNeedToRequestPermissions

    @Before
    fun setup() {
        val engine = Mockito.mock(Engine::class.java)
        anchorView = View(ApplicationProvider.getApplicationContext())
        mockSessionManager = Mockito.spy(SessionManager(engine))
        mockOnNeedToRequestPermissions = mock()

        sitePermissionFeature = SitePermissionsFeature(
            anchorView = anchorView,
            sessionManager = mockSessionManager,
            onNeedToRequestPermissions = mockOnNeedToRequestPermissions
        )
    }

    @Test
    fun `a new onAppPermissionRequested will call onNeedToRequestPermissions`() {

        val session = getSelectedSession()

        var wasCalled = false

        sitePermissionFeature = SitePermissionsFeature(
            anchorView = anchorView,
            sessionManager = mockSessionManager,
            onNeedToRequestPermissions = {
                wasCalled = true
            })

        sitePermissionFeature.start()

        val mockPermissionRequest: PermissionRequest = mock()
        session.appPermissionRequest = Consumable.from(mockPermissionRequest)

        assertTrue(wasCalled)
    }

    @Test(expected = InvalidParameterException::class)
    fun `requesting an invalid content permission request will throw an exception`() {
        val session = getSelectedSession()

        sitePermissionFeature = SitePermissionsFeature(
            anchorView = anchorView,
            sessionManager = mockSessionManager,
            onNeedToRequestPermissions = {
            })

        sitePermissionFeature.start()

        val mockPermissionRequest: PermissionRequest = mock()

        doReturn(listOf(Permission.Generic("", ""))).`when`(mockPermissionRequest).permissions

        session.contentPermissionRequest = Consumable.from(mockPermissionRequest)
    }

    @Test
    fun `after calling stop the feature will not be notified by new incoming permissionRequests`() {

        val session = getSelectedSession()

        var wasCalled = false

        sitePermissionFeature = SitePermissionsFeature(
            anchorView = anchorView,
            sessionManager = mockSessionManager,
            onNeedToRequestPermissions = {
                wasCalled = true
            })

        sitePermissionFeature.start()

        val mockPermissionRequest: PermissionRequest = mock()
        session.appPermissionRequest = Consumable.from(mockPermissionRequest)

        assertTrue(wasCalled)

        wasCalled = false

        sitePermissionFeature.stop()
        session.appPermissionRequest = Consumable.from(mockPermissionRequest)

        assertFalse(wasCalled)
    }

    @Test
    fun `granting a content permission must call grant and consume contentPermissionRequest`() {
        val session = getSelectedSession()
        var grantWasCalled = false

        val permissions = listOf(
            ContentGeoLocation(),
            ContentNotification(),
            ContentAudioCapture(),
            ContentAudioMicrophone()
        )

        permissions.forEach { permission ->

            val permissionRequest: PermissionRequest = object : PermissionRequest {
                override val uri: String?
                    get() = "http://www.mozilla.org"
                override val permissions: List<Permission>
                    get() = listOf(permission)

                override fun grant(permissions: List<Permission>) {
                    grantWasCalled = true
                }

                override fun reject() = Unit
            }

            session.contentPermissionRequest = Consumable.from(permissionRequest)

            val prompt = sitePermissionFeature.onContentPermissionRequested(session, permissionRequest)

            val positiveButton = prompt.buttons.find { it.positive }
            positiveButton?.onClick?.invoke()

            assertTrue(grantWasCalled)
            assertTrue(session.contentPermissionRequest.isConsumed())
        }
    }

    @Test
    fun `rejecting a content permission must call reject and consume contentPermissionRequest`() {
        val session = getSelectedSession()
        var rejectWasCalled = false

        val permissions = listOf(
            ContentGeoLocation(),
            ContentNotification(),
            ContentAudioCapture(),
            ContentAudioMicrophone()
        )

        permissions.forEach { permission ->
            val permissionRequest: PermissionRequest = object : PermissionRequest {
                override val uri: String?
                    get() = "http://www.mozilla.org"
                override val permissions: List<Permission>
                    get() = listOf(permission)

                override fun reject() {
                    rejectWasCalled = true
                }

                override fun grant(permissions: List<Permission>) = Unit
            }

            session.contentPermissionRequest = Consumable.from(permissionRequest)

            val prompt = sitePermissionFeature.onContentPermissionRequested(session, permissionRequest)

            val negativeButton = prompt.buttons.find { !it.positive }
            negativeButton!!.onClick.invoke()

            assertTrue(rejectWasCalled)
            assertTrue(session.contentPermissionRequest.isConsumed())
        }
    }

    @Test
    fun `granting a camera permission must call grant and consume contentPermissionRequest`() {
        val session = getSelectedSession()
        var grantWasCalled = false

        val permissions = listOf(
                ContentVideoCapture("", "back camera"),
                ContentVideoCamera("", "front camera")
        )

        permissions.forEachIndexed { index, _ ->

            val permissionRequest: PermissionRequest = object : PermissionRequest {
                override val uri: String?
                    get() = "http://www.mozilla.org"

                override val permissions: List<Permission>
                    get() = if (index > 0) permissions.reversed() else permissions

                override fun grant(permissions: List<Permission>) {
                    grantWasCalled = true
                }

                override fun reject() = Unit
            }

            session.contentPermissionRequest = Consumable.from(permissionRequest)

            val prompt = sitePermissionFeature.onContentPermissionRequested(session, permissionRequest)

            val radioButton = prompt.controlGroups.first().controls[index] as DoorhangerPrompt.Control.RadioButton

            // Simulating a user click either on the back/front camera option
            radioButton.checked = true

            val positiveButton = prompt.buttons.find { it.positive }
            positiveButton?.onClick?.invoke()

            assertTrue(grantWasCalled)
            assertTrue(session.contentPermissionRequest.isConsumed())
        }
    }

    @Test
    fun `rejecting a camera content permission must call reject and consume contentPermissionRequest`() {
        val session = getSelectedSession()
        var rejectWasCalled = false

        val permissions = listOf(
                ContentVideoCapture("", "back camera"),
                ContentVideoCamera("", "front camera")
        )

        permissions.forEachIndexed { index, _ ->
            val permissionRequest: PermissionRequest = object : PermissionRequest {
                override val uri: String?
                    get() = "http://www.mozilla.org"

                override val permissions: List<Permission>
                    get() = if (index > 0) permissions.reversed() else permissions

                override fun reject() {
                    rejectWasCalled = true
                }

                override fun grant(permissions: List<Permission>) = Unit
            }

            session.contentPermissionRequest = Consumable.from(permissionRequest)

            val prompt = sitePermissionFeature.onContentPermissionRequested(session, permissionRequest)

            val negativeButton = prompt.buttons.find { !it.positive }
            negativeButton!!.onClick.invoke()

            assertTrue(rejectWasCalled)
            assertTrue(session.contentPermissionRequest.isConsumed())
        }
    }

    @Test
    fun `granting a camera and microphone permission must call grant and consume contentPermissionRequest`() {
        val session = getSelectedSession()
        var grantWasCalled = false
        val permissionRequest: PermissionRequest = object : PermissionRequest {
            override val uri: String?
                get() = "http://www.mozilla.org"

            override val permissions: List<Permission>
                get() = listOf(
                    ContentVideoCapture("", "back camera"),
                    ContentVideoCamera("", "front camera"),
                    ContentAudioMicrophone()
                )

            override fun grant(permissions: List<Permission>) {
                grantWasCalled = true
            }
            override fun containsVideoAndAudioSources() = true

            override fun reject() = Unit
        }

        session.contentPermissionRequest = Consumable.from(permissionRequest)

        val prompt = sitePermissionFeature.onContentPermissionRequested(session, permissionRequest)

        prompt.controlGroups.forEach { control ->
            val radioButton = control.controls.first() as DoorhangerPrompt.Control.RadioButton
            radioButton.checked = true
        }

        val positiveButton = prompt.buttons.find { it.positive }
        positiveButton?.onClick?.invoke()

        assertTrue(grantWasCalled)
        assertTrue(session.contentPermissionRequest.isConsumed())
    }

    @Test
    fun `rejecting a camera and microphone permission must call reject and consume contentPermissionRequest`() {
        val session = getSelectedSession()
        var rejectWasCalled = false
        val permissionRequest: PermissionRequest = object : PermissionRequest {
            override val uri: String?
                get() = "http://www.mozilla.org"

            override val permissions: List<Permission>
                get() = listOf(
                    ContentVideoCapture("", "back camera"),
                    ContentVideoCamera("", "front camera"),
                    ContentAudioMicrophone()
                )
            override fun reject() {
                rejectWasCalled = true
            }

            override fun containsVideoAndAudioSources() = true

            override fun grant(permissions: List<Permission>) = Unit
        }

        session.contentPermissionRequest = Consumable.from(permissionRequest)

        val prompt = sitePermissionFeature.onContentPermissionRequested(session, permissionRequest)

        val positiveButton = prompt.buttons.find { !it.positive }
        positiveButton?.onClick?.invoke()

        assertTrue(rejectWasCalled)
        assertTrue(session.contentPermissionRequest.isConsumed())
    }

    @Test(expected = NoSuchElementException::class)
    fun `trying to find an option permission when none of the options are checked will throw an exception`() {
        val permissions = listOf(
            ContentVideoCapture("", "back camera"),
            ContentVideoCamera("", "front camera")
        )

        val cameraControlGroup = sitePermissionFeature.createControlGroupForCameraPermission(
            cameraPermissions = permissions
        )

        cameraControlGroup.controls.forEach { control ->
            val radioButton = control as DoorhangerPrompt.Control.RadioButton
            radioButton.checked = false
        }

        sitePermissionFeature.findSelectedPermission(cameraControlGroup, permissions)
    }

    @Test
    fun `calling onPermissionsResult with all permissions granted will call grant on the permissionsRequest and consume it`() {
        val session = getSelectedSession()
        val mockPermissionRequest: PermissionRequest = mock()

        sitePermissionFeature.start()

        session.appPermissionRequest = Consumable.from(mockPermissionRequest)

        sitePermissionFeature.onPermissionsResult(intArrayOf(PERMISSION_GRANTED))

        verify(mockPermissionRequest).grant(emptyList())
        assertTrue(session.appPermissionRequest.isConsumed())
    }

    @Test
    fun `calling onPermissionsResult with NOT all permissions granted will call reject on the permissionsRequest and consume it`() {
        val session = getSelectedSession()
        val mockPermissionRequest: PermissionRequest = mock()

        sitePermissionFeature.start()

        session.appPermissionRequest = Consumable.from(mockPermissionRequest)

        sitePermissionFeature.onPermissionsResult(intArrayOf(PERMISSION_DENIED))

        verify(mockPermissionRequest).reject()
        assertTrue(session.appPermissionRequest.isConsumed())
    }

    private fun getSelectedSession(): Session {
        val session = Session("")
        mockSessionManager.add(session)
        mockSessionManager.select(session)
        return session
    }
}