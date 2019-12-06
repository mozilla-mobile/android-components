package mozilla.components.feature.p2p

import android.content.pm.PackageManager
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mozilla.components.feature.p2p.view.P2PView
import mozilla.components.lib.nearby.NearbyConnection
import mozilla.components.support.test.mock
import mozilla.components.support.test.whenever
import mozilla.components.support.webextensions.WebExtensionController
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class P2PFeatureTest {
    @Before
    fun setup() {
        WebExtensionController.installedExtensions.clear()
    }

    @Test
    fun `start does not proceed if permissions ungranted`() {
        var requestedPermissions: List<String>? = null
        val onNeedToRequestPermissions = { perms: Array<String> -> requestedPermissions = perms.toList() }
        val mockP2PView = mock<P2PView>()
        val mockView = mock<View>()
        whenever(mockP2PView.asView()).thenReturn(mockView)
        whenever(mockView.context).thenReturn(InstrumentationRegistry.getInstrumentation().context)

        val p2pFeature = P2PFeature(
            mockP2PView,
            mock(), // BrowserStore
            mock(), // Engine
            mock(), // thunk
            mock(), // TabsUseCases
            mock(), // SessionUseCases
            mock(), // SessionManager
            onNeedToRequestPermissions,
            mock() // close
        )
        p2pFeature.start()

        // Because no permissions are granted by the instrumentation context,
        // all will be requested.
        val neededPermissions = NearbyConnection.PERMISSIONS + P2PFeature.LOCAL_PERMISSIONS
        assertNotNull("No permissions were requested.", requestedPermissions)
        requestedPermissions?.run {
            neededPermissions.forEach {
                assertTrue(this.contains(it))
            }
        }

        // Deny one permission, which will prevent the P2PController from being constructed
        // and keep the web extension from being installed. Attempting to the latter would
        // cause mocks to throw exceptions.
        val results = neededPermissions.map { PackageManager.PERMISSION_GRANTED }.toIntArray()
        results[0] = PackageManager.PERMISSION_DENIED // ugly
        p2pFeature.onPermissionsResult(neededPermissions, results)
        assertNull(p2pFeature.controller)
        assertNull(p2pFeature.extensionController)
    }
}