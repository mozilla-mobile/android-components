/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fretboard

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JexlExtensionsTest {
    @Test
    fun `generated jexl context is valid`() {
        val provider = object : ValuesProvider() {
            override fun getRegion(context: Context): String? {
                return "UK"
            }

            override fun getReleaseChannel(context: Context): String? {
                return "alpha"
            }
        }
        provider.putValue("test-key", "test-value")
        provider.putValue("second-key", "second-value")
        val jexlContext = provider.toJexlContext(mockContext())
        assertEquals("eng", jexlContext.get("language").toString())
        assertEquals("test.appId", jexlContext.get("appId").toString())
        assertEquals("test.version", jexlContext.get("version").toString())
        assertEquals("unknown", jexlContext.get("manufacturer").toString())
        assertEquals("robolectric", jexlContext.get("device").toString())
        assertEquals("USA", jexlContext.get("country").toString())
        assertEquals("UK", jexlContext.get("region").toString())
        assertEquals("alpha", jexlContext.get("releaseChannel").toString())
        assertNotNull("clientId", jexlContext.get("clientId").toString())
        assertEquals("test-value", jexlContext.get("test-key").toString())
        assertEquals("second-value", jexlContext.get("second-key").toString())
    }

    private fun mockContext(): Context {
        val context = mock(Context::class.java)
        `when`(context.packageName).thenReturn("test.appId")
        val sharedPreferences = mock(SharedPreferences::class.java)
        `when`(sharedPreferences.getBoolean(ArgumentMatchers.eq("testexperiment"), ArgumentMatchers.anyBoolean())).thenAnswer { invocation -> invocation.arguments[1] as Boolean }
        `when`(context.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.eq(Context.MODE_PRIVATE))).thenReturn(sharedPreferences)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(editor.putString(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(editor)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        val packageManager = mock(PackageManager::class.java)
        val packageInfo = PackageInfo()
        packageInfo.versionName = "test.version"
        `when`(packageManager.getPackageInfo(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).thenReturn(packageInfo)
        `when`(context.packageManager).thenReturn(packageManager)
        return context
    }
}