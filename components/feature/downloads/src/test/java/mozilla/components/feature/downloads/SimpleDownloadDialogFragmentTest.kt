/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.downloads

import android.app.Application
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_POSITIVE
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import mozilla.components.browser.session.Download
import mozilla.components.feature.downloads.SimpleDownloadDialogFragment.DownloadDialogListener
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class SimpleDownloadDialogFragmentTest {

    private lateinit var dialog: SimpleDownloadDialogFragment
    private lateinit var download: Download
    private lateinit var mockFragmentManager: FragmentManager

    @Before
    fun setup() {
        mockFragmentManager = mock(FragmentManager::class.java)
        download = Download(
            "http://ipv4.download.thinkbroadband.com/5MB.zip",
            "5MB.zip", "application/zip", 5242880,
            "Mozilla/5.0 (Linux; Android 7.1.1) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/8.0 Chrome/69.0.3497.100 Mobile Safari/537.36"
        )
        dialog = SimpleDownloadDialogFragment.newInstance()
    }

    @Test
    fun `when the positive button is clicked positiveButtonListener and onStartDownload must be called`() {
        var isPositiveActionCalled = false

        var isOnStartDownloadCalled = false

        val onStartDownload = {
            isOnStartDownloadCalled = true
        }

        dialog.buttonsListener = object : DownloadDialogListener {
            override fun onPositiveButtonClick() {
                isPositiveActionCalled = true
            }
        }
        dialog.onStartDownload = onStartDownload
        dialog.testingContext = RuntimeEnvironment.application

        performClick(BUTTON_POSITIVE)

        assertTrue(isPositiveActionCalled)
        assertTrue(isOnStartDownloadCalled)
    }

    @Test
    fun `when the negative button is clicked negativeButtonListener must be called`() {
        var isNegativeActionCalled = false
        dialog.testingContext = RuntimeEnvironment.application
        dialog.buttonsListener = object : DownloadDialogListener {
            override fun onNegativeButtonClick() {
                isNegativeActionCalled = true
            }
        }

        dialog.setDownload(download)
        performClick(BUTTON_NEGATIVE)

        assertTrue(isNegativeActionCalled)
    }

    @Test(expected = ClassCastException::class)
    fun `when try to use without implementing DownloadDialogListener interface `() {
        dialog.onAttach(RuntimeEnvironment.application)
    }

    private fun performClick(buttonID: Int) {
        val alert = dialog.onCreateDialog(null)

        alert.show()

        val negativeButton = (alert as AlertDialog).getButton(buttonID)

        negativeButton.performClick()
    }
}

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_AppCompat)
    }
}