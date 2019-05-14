/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.app.links

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppLinksUseCasesTest {
    private val appUrl = "https://example.com"
    private val appPackage = "com.example.app"
    private val browserPackage = "com.browser"

    private fun createContext(vararg packages: String): Context {
        val context = mock(Context::class.java)
        val packageManager = mock(PackageManager::class.java)

        fun resolveInfoFor(packageName: String): ResolveInfo {
            val activityInfo = ActivityInfo()
            activityInfo.packageName = packageName

            val resolveInfo = ResolveInfo()
            resolveInfo.activityInfo = activityInfo
            return resolveInfo
        }

        val resolveInfos = packages.map(::resolveInfoFor)

        `when`(packageManager.queryIntentActivities(any(), anyInt()))
            .thenReturn(resolveInfos)

        `when`(context.packageManager)
            .thenReturn(packageManager)
        return context
    }

    @Test
    fun `A URL that matches zero apps is not an app link`() {
        val context = createContext()
        val subject = AppLinksUseCases(context, emptySet())

        assertFalse(subject.isAppLink.invoke(appUrl))
    }

    @Test
    fun `A URL that matches more than zero apps is an app link`() {
        val context = createContext(appPackage)
        val subject = AppLinksUseCases(context, emptySet())

        assertTrue(subject.isAppLink.invoke(appUrl))
    }

    @Test
    fun `A URL that matches only excluded packages is not an app link`() {
        val context = createContext(browserPackage)
        val subject = AppLinksUseCases(context, setOf(browserPackage))

        assertFalse(subject.isAppLink.invoke(appUrl))
    }

    @Test
    fun `A URL that also matches excluded packages is an app link`() {
        val context = createContext(appPackage, browserPackage)
        val subject = AppLinksUseCases(context, setOf(browserPackage))

        assertTrue(subject.isAppLink.invoke(appUrl))
    }

    @Test
    fun `A list of browser package names can be generated if not supplied`() {
        val context = createContext(browserPackage)
        val subject = AppLinksUseCases(context)

        assertEquals(subject.browserPackageNames, setOf(browserPackage))
    }
}