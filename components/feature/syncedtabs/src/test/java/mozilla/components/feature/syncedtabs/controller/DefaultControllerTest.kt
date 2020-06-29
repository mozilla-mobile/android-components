/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.syncedtabs.controller

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.storage.sync.SyncedDeviceTabs
import mozilla.components.concept.sync.ConstellationState
import mozilla.components.concept.sync.DeviceConstellation
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.feature.syncedtabs.storage.SyncedTabsStorage
import mozilla.components.feature.syncedtabs.view.SyncedTabsView
import mozilla.components.feature.syncedtabs.view.SyncedTabsView.ErrorType
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.support.test.mock
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never

@RunWith(AndroidJUnit4::class)
class DefaultControllerTest {
    private val storage: SyncedTabsStorage = mock()
    private val accountManager: FxaAccountManager = mock()
    private val view: SyncedTabsView = mock()
    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `update view only when no account available`() = runBlockingTest {
        val controller = DefaultController(
            storage,
            accountManager,
            view,
            coroutineContext
        )

        controller.refreshSyncedTabs()

        verify(view).startLoading()
        verify(view).stopLoading()

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `notify if there are no other devices synced`() = runBlockingTest {
        val controller = DefaultController(
            storage,
            accountManager,
            view,
            coroutineContext
        )
        val account: OAuthAccount = mock()
        val constellation: DeviceConstellation = mock()
        val state: ConstellationState = mock()

        `when`(accountManager.authenticatedAccount()).thenReturn(account)
        `when`(account.deviceConstellation()).thenReturn(constellation)
        `when`(constellation.state()).thenReturn(state)
        `when`(state.otherDevices).thenReturn(emptyList())

        `when`(storage.getSyncedDeviceTabs()).thenReturn(emptyList())

        controller.refreshSyncedTabs()

        verify(view).onError(ErrorType.MULTIPLE_DEVICES_UNAVAILABLE)
    }

    @Test
    fun `notify if there are no tabs from other devices to sync`() = runBlockingTest {
        val controller = DefaultController(
            storage,
            accountManager,
            view,
            coroutineContext
        )
        val account: OAuthAccount = mock()
        val constellation: DeviceConstellation = mock()
        val state: ConstellationState = mock()

        `when`(accountManager.authenticatedAccount()).thenReturn(account)
        `when`(account.deviceConstellation()).thenReturn(constellation)
        `when`(constellation.state()).thenReturn(state)
        `when`(state.otherDevices).thenReturn(listOf(mock()))

        `when`(storage.getSyncedDeviceTabs()).thenReturn(emptyList())

        controller.refreshSyncedTabs()

        verify(view, never()).onError(ErrorType.MULTIPLE_DEVICES_UNAVAILABLE)
        verify(view).onError(ErrorType.NO_TABS_AVAILABLE)
    }

    @Test
    fun `display synced tabs`() = runBlockingTest {
        val controller = DefaultController(
            storage,
            accountManager,
            view,
            coroutineContext
        )
        val account: OAuthAccount = mock()
        val constellation: DeviceConstellation = mock()
        val state: ConstellationState = mock()
        val syncedDeviceTabs = SyncedDeviceTabs(mock(), listOf(mock()))
        val listOfSyncedDeviceTabs = listOf(syncedDeviceTabs)

        `when`(accountManager.authenticatedAccount()).thenReturn(account)
        `when`(account.deviceConstellation()).thenReturn(constellation)
        `when`(constellation.state()).thenReturn(state)
        `when`(state.otherDevices).thenReturn(listOf(mock()))

        `when`(storage.getSyncedDeviceTabs()).thenReturn(listOfSyncedDeviceTabs)

        controller.refreshSyncedTabs()

        verify(view, never()).onError(ErrorType.MULTIPLE_DEVICES_UNAVAILABLE)
        verify(view, never()).onError(ErrorType.NO_TABS_AVAILABLE)
        verify(view).displaySyncedTabs(listOfSyncedDeviceTabs)
    }

    @Test
    fun `syncAccount refreshes devices and syncs`() = runBlockingTest {
        val controller = DefaultController(
            storage,
            accountManager,
            view,
            coroutineContext
        )
        val account: OAuthAccount = mock()
        val constellation: DeviceConstellation = mock()

        `when`(accountManager.authenticatedAccount()).thenReturn(account)
        `when`(account.deviceConstellation()).thenReturn(constellation)
        `when`(constellation.refreshDevicesAsync()).thenReturn(CompletableDeferred(true))

        controller.syncAccount()

        verify(constellation).refreshDevicesAsync()
        verify(accountManager).syncNowAsync(SyncReason.User, false)
    }
}
