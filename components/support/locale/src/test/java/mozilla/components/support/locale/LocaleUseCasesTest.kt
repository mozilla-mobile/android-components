/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.locale

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.state.action.UpdateLocaleAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleUseCasesTest {

    private lateinit var browserStore: BrowserStore

    @Before
    fun setup() {
        browserStore = mock()
    }

    @Test
    fun `UpdateLocaleUseCase`() {
        val useCases = LocaleUseCases(browserStore)
        val locale = Locale("MyFavoriteLanguage")

        useCases.notifyLocaleChanged(locale)

        Mockito.verify(browserStore).dispatch(UpdateLocaleAction(locale))
    }
}
