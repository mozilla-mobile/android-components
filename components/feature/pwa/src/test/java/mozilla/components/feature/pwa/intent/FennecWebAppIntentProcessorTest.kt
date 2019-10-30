/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.pwa.intent

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.state.ExternalAppType
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.feature.intent.ext.getSessionId
import mozilla.components.feature.pwa.ManifestStorage
import mozilla.components.feature.pwa.ext.getWebAppManifest
import mozilla.components.feature.pwa.intent.FennecWebAppIntentProcessor.Companion.ACTION_FENNEC_WEBAPP
import mozilla.components.feature.pwa.intent.FennecWebAppIntentProcessor.Companion.EXTRA_FENNEC_MANIFEST_PATH
import mozilla.components.feature.pwa.intent.WebAppIntentProcessor.Companion.ACTION_VIEW_PWA
import mozilla.components.support.test.mock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class FennecWebAppIntentProcessorTest {

    private lateinit var storage: ManifestStorage

    @Before
    fun setup() {
        storage = mock()
    }

    @Test
    fun `matches checks if intent action is WEBAPP`() {
        val processor = FennecWebAppIntentProcessor(mock(), mock(), storage)

        assertTrue(processor.matches(Intent(ACTION_FENNEC_WEBAPP)))
        assertFalse(processor.matches(Intent(ACTION_VIEW_PWA)))
        assertFalse(processor.matches(Intent(ACTION_VIEW)))
    }

    @Test
    fun `process checks if intent action is not valid`() = runBlockingTest {
        val processor = FennecWebAppIntentProcessor(mock(), mock(), storage)

        assertFalse(processor.process(Intent(ACTION_FENNEC_WEBAPP, null)))
        assertFalse(processor.process(Intent(ACTION_FENNEC_WEBAPP, "".toUri())))
    }

    @Test
    fun `process returns false if no manifest is in storage`() = runBlockingTest {
        val processor = spy(FennecWebAppIntentProcessor(mock(), mock(), storage))

        doReturn(null).`when`(storage).loadManifest("https://mozilla.org")
        doReturn(null).`when`(processor).fromFile("mock/path")

        val intent = Intent(ACTION_FENNEC_WEBAPP, "https://mozilla.org".toUri()).apply {
            putExtra(EXTRA_FENNEC_MANIFEST_PATH, "mock/path")
        }
        assertFalse(processor.process(intent))

        val intentNoPath = Intent(ACTION_FENNEC_WEBAPP, "https://firefox.com".toUri())
        assertFalse(processor.process(intentNoPath))
    }

    @Test
    fun `process adds session ID and manifest to intent`() = runBlockingTest {
        val processor = spy(FennecWebAppIntentProcessor(mock(), mock(), storage))

        val manifest = WebAppManifest(
            name = "Test Manifest",
            startUrl = "https://mozilla.com"
        )
        doReturn(null).`when`(storage).loadManifest("https://mozilla.com")
        doReturn(manifest).`when`(processor).fromFile("mock/path")

        val intent = Intent(ACTION_FENNEC_WEBAPP, "https://mozilla.com".toUri()).apply {
            putExtra(EXTRA_FENNEC_MANIFEST_PATH, "mock/path")
        }
        assertTrue(processor.process(intent))
        assertNotNull(intent.getSessionId())
        assertEquals(manifest, intent.getWebAppManifest())

        verify(storage).saveManifest(manifest)
    }

    @Test
    fun `process adds custom tab config`() = runBlockingTest {
        val intent = Intent(ACTION_FENNEC_WEBAPP, "https://mozilla.com".toUri()).apply {
            putExtra(EXTRA_FENNEC_MANIFEST_PATH, "mock/path")
        }

        val store = BrowserStore()
        val sessionManager = SessionManager(mock(), store)
        val processor = spy(FennecWebAppIntentProcessor(sessionManager, mock(), storage))

        val manifest = WebAppManifest(
            name = "Test Manifest",
            startUrl = "https://mozilla.com"
        )
        doReturn(manifest).`when`(storage).loadManifest("https://mozilla.com")

        assertTrue(processor.process(intent))
        val sessionState = store.state.customTabs.first()
        assertNotNull(sessionState.config)
        assertEquals(ExternalAppType.PROGRESSIVE_WEB_APP, sessionState.config.externalAppType)
    }
}
