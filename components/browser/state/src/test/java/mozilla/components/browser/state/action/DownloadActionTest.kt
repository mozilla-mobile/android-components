/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.action

import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadActionTest {

    @Test
    fun `DownloadCompletedAction - Appends to completedDownload set`() {
        val store = BrowserStore()

        assertEquals(0, store.state.completedDownloads.count())

        store.dispatch(DownloadAction.DownloadCompletedAction(1L)).joinBlocking()
        store.dispatch(DownloadAction.DownloadCompletedAction(2L)).joinBlocking()

        assertEquals(setOf(1L, 2L), store.state.completedDownloads)
    }
}
