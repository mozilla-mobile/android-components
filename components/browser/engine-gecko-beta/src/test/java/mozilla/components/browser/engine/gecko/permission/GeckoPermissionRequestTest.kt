/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.permission

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.concept.engine.permission.Permission
import mozilla.components.support.test.mock
import mozilla.components.test.ReflectionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.MediaSource
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION

@RunWith(AndroidJUnit4::class)
class GeckoPermissionRequestTest {

    @Test
    fun `create content permission request`() {
        val callback: GeckoSession.PermissionDelegate.Callback = mock()
        val uri = "https://mozilla.org"

        var request = GeckoPermissionRequest.Content(uri, PERMISSION_DESKTOP_NOTIFICATION, callback)
        assertEquals(uri, request.uri)
        assertEquals(listOf(Permission.ContentNotification()), request.permissions)

        request = GeckoPermissionRequest.Content(uri, PERMISSION_GEOLOCATION, callback)
        assertEquals(uri, request.uri)
        assertEquals(listOf(Permission.ContentGeoLocation()), request.permissions)

        request = GeckoPermissionRequest.Content(uri, 1234, callback)
        assertEquals(uri, request.uri)
        assertEquals(listOf(Permission.Generic("1234", "Gecko permission type = 1234")), request.permissions)
    }

    @Test
    fun `grant content permission request`() {
        val callback: GeckoSession.PermissionDelegate.Callback = mock()
        val uri = "https://mozilla.org"

        val request = GeckoPermissionRequest.Content(uri, PERMISSION_GEOLOCATION, callback)
        request.grant()
        verify(callback).grant()
    }

    @Test
    fun `reject content permission request`() {
        val callback: GeckoSession.PermissionDelegate.Callback = mock()
        val uri = "https://mozilla.org"

        val request = GeckoPermissionRequest.Content(uri, PERMISSION_GEOLOCATION, callback)
        request.reject()
        verify(callback).reject()
    }

    @Test
    fun `create app permission request`() {
        val callback: GeckoSession.PermissionDelegate.Callback = mock()
        val permissions = listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                "unknown app permission")

        val mappedPermissions = listOf(
                Permission.AppLocationCoarse(Manifest.permission.ACCESS_COARSE_LOCATION),
                Permission.AppLocationFine(Manifest.permission.ACCESS_FINE_LOCATION),
                Permission.AppCamera(Manifest.permission.CAMERA),
                Permission.AppAudio(Manifest.permission.RECORD_AUDIO),
                Permission.Generic("unknown app permission")
        )

        val request = GeckoPermissionRequest.App(permissions, callback)
        assertEquals(mappedPermissions, request.permissions)
    }

    @Test
    fun `grant app permission request`() {
        val callback: GeckoSession.PermissionDelegate.Callback = mock()

        val request = GeckoPermissionRequest.App(listOf(Manifest.permission.CAMERA), callback)
        request.grant()
        verify(callback).grant()
    }

    @Test
    fun `reject app permission request`() {
        val callback: GeckoSession.PermissionDelegate.Callback = mock()

        val request = GeckoPermissionRequest.App(listOf(Manifest.permission.CAMERA), callback)
        request.reject()
        verify(callback).reject()
    }

    @Test
    fun `create media permission request`() {
        val callback: GeckoSession.PermissionDelegate.MediaCallback = mock()
        val uri = "https://mozilla.org"

        val audioMicrophone = MockMediaSource("audioMicrophone", "audioMicrophone",
                MediaSource.SOURCE_MICROPHONE, MediaSource.TYPE_AUDIO)
        val audioCapture = MockMediaSource("audioCapture", "audioCapture",
                MediaSource.SOURCE_AUDIOCAPTURE, MediaSource.TYPE_AUDIO)
        val audioOther = MockMediaSource("audioOther", "audioOther",
                MediaSource.SOURCE_OTHER, MediaSource.TYPE_AUDIO)

        val videoCamera = MockMediaSource("videoCamera", "videoCamera",
                MediaSource.SOURCE_CAMERA, MediaSource.TYPE_VIDEO)
        val videoBrowser = MockMediaSource("videoBrowser", "videoBrowser",
                MediaSource.SOURCE_BROWSER, MediaSource.TYPE_VIDEO)
        val videoApplication = MockMediaSource("videoApplication", "videoApplication",
                MediaSource.SOURCE_APPLICATION, MediaSource.TYPE_VIDEO)
        val videoScreen = MockMediaSource("videoScreen", "videoScreen",
                MediaSource.SOURCE_SCREEN, MediaSource.TYPE_VIDEO)
        val videoWindow = MockMediaSource("videoWindow", "videoWindow",
                MediaSource.SOURCE_WINDOW, MediaSource.TYPE_VIDEO)
        val videoOther = MockMediaSource("videoOther", "videoOther",
                MediaSource.SOURCE_OTHER, MediaSource.TYPE_VIDEO)

        val audioSources = listOf(audioCapture, audioMicrophone, audioOther)
        val videoSources = listOf(videoApplication, videoBrowser, videoCamera, videoOther, videoScreen, videoWindow)

        val mappedPermissions = listOf(
                Permission.ContentVideoCamera("videoCamera", "videoCamera"),
                Permission.ContentVideoBrowser("videoBrowser", "videoBrowser"),
                Permission.ContentVideoApplication("videoApplication", "videoApplication"),
                Permission.ContentVideoScreen("videoScreen", "videoScreen"),
                Permission.ContentVideoWindow("videoWindow", "videoWindow"),
                Permission.ContentVideoOther("videoOther", "videoOther"),
                Permission.ContentAudioMicrophone("audioMicrophone", "audioMicrophone"),
                Permission.ContentAudioCapture("audioCapture", "audioCapture"),
                Permission.ContentAudioOther("audioOther", "audioOther")
        )

        val request = GeckoPermissionRequest.Media(uri, videoSources, audioSources, callback)
        assertEquals(uri, request.uri)
        assertEquals(mappedPermissions.size, request.permissions.size)
        assertTrue(request.permissions.containsAll(mappedPermissions))
    }

    @Test
    fun `grant media permission request`() {
        val callback: GeckoSession.PermissionDelegate.MediaCallback = mock()
        val uri = "https://mozilla.org"

        val audioMicrophone = MockMediaSource("audioMicrophone", "audioMicrophone",
                MediaSource.SOURCE_MICROPHONE, MediaSource.TYPE_AUDIO)
        val videoCamera = MockMediaSource("videoCamera", "videoCamera",
                MediaSource.SOURCE_CAMERA, MediaSource.TYPE_VIDEO)

        val audioSources = listOf(audioMicrophone)
        val videoSources = listOf(videoCamera)

        val request = GeckoPermissionRequest.Media(uri, videoSources, audioSources, callback)
        request.grant(request.permissions)
        verify(callback).grant(videoCamera, audioMicrophone)
    }

    @Test
    fun `reject media permission request`() {
        val callback: GeckoSession.PermissionDelegate.MediaCallback = mock()
        val uri = "https://mozilla.org"

        val audioMicrophone = MockMediaSource("audioMicrophone", "audioMicrophone",
                MediaSource.SOURCE_MICROPHONE, MediaSource.TYPE_AUDIO)
        val videoCamera = MockMediaSource("videoCamera", "videoCamera",
                MediaSource.SOURCE_CAMERA, MediaSource.TYPE_VIDEO)

        val audioSources = listOf(audioMicrophone)
        val videoSources = listOf(videoCamera)

        val request = GeckoPermissionRequest.Media(uri, videoSources, audioSources, callback)
        request.reject()
        verify(callback).reject()
    }

    class MockMediaSource(id: String, name: String, source: Int, type: Int) : MediaSource() {
        init {
            ReflectionUtils.setField(this, "id", id)
            ReflectionUtils.setField(this, "name", name)
            ReflectionUtils.setField(this, "source", source)
            ReflectionUtils.setField(this, "type", type)
        }
    }
}
