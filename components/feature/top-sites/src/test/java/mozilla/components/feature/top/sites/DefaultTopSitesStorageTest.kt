/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.concept.storage.TopFrecentSiteInfo
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.BAIDU_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.BY_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.CN_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.EN_LANGUAGE
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.GOOGLE_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.JD_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.KZ_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.POCKET_TRENDING_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.PREFERENCE_NAME
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.RU_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.TR_REGION
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.WIKIPEDIA_URL
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.XX_LANGUAGE
import mozilla.components.feature.top.sites.DefaultTopSitesStorage.Companion.XX_REGION
import mozilla.components.feature.top.sites.db.DefaultSite
import mozilla.components.feature.top.sites.ext.toTopSite
import mozilla.components.support.locale.LocaleManager
import mozilla.components.support.test.any
import mozilla.components.support.test.mock
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.whenever
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DefaultTopSitesStorageTest {

    private val pinnedSitesStorage: PinnedSiteStorage = mock()
    private val historyStorage: PlacesHistoryStorage = mock()
    private val region: RegionState = mock()

    @Before
    fun setup() {
        // Set the local default top sites version to the latest to avoid the
        // DefaultTopSitesStorage.init(). Tests should specify the correct local default
        // top sites version to test DefaultTopSitesStorage.init().
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 1).apply()
    }

    @After
    fun shutdown() {
        preference(testContext).edit().remove(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION).apply()
    }

    @Test
    fun `default top sites are added to pinned site storage on init`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        // This is same list called in `initializeDefaultTopSites` in [DefaultTopSitesStorage].
        val defaultSites = listOf(
            DefaultSite(
                CN_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_baidu),
                BAIDU_URL
            ),
            DefaultSite(
                CN_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_jd),
                JD_URL
            ),
            DefaultSite(
                RU_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                RU_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            ),
            DefaultSite(
                TR_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                TR_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            ),
            DefaultSite(
                KZ_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                KZ_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            ),
            DefaultSite(
                BY_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                BY_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            ),
            DefaultSite(
                XX_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_google),
                DefaultTopSitesStorage.GOOGLE_URL
            ),
            DefaultSite(
                XX_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                XX_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).addAllDefaultSites(defaultSites)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `default sites added to pinned sites storage for CN region`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                CN_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_baidu),
                BAIDU_URL
            ),
            DefaultSite(
                CN_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_jd),
                JD_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(CN_REGION, CN_REGION),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(CN_REGION, EN_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `default sites added to pinned sites storage for RU region`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                RU_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                RU_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(RU_REGION, RU_REGION),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(RU_REGION, EN_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `default sites added to pinned sites storage for RU region and XX language`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                RU_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                RU_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        LocaleManager.setNewLocale(testContext, "es")

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(RU_REGION, RU_REGION),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(RU_REGION, XX_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)

        LocaleManager.resetToSystemDefault(testContext)
    }

    @Test
    fun `default sites added to pinned sites storage for TR region`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                TR_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                TR_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(TR_REGION, TR_REGION),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(TR_REGION, EN_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `default sites added to pinned sites storage for KZ region`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                KZ_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                KZ_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(KZ_REGION, KZ_REGION),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(KZ_REGION, EN_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `default sites added to pinned sites storage for BY region`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                BY_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                BY_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(BY_REGION, BY_REGION),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(BY_REGION, EN_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `default sites added to pinned sites storage for US region`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSites = listOf(
            DefaultSite(
                XX_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_google),
                GOOGLE_URL
            ),
            DefaultSite(
                XX_REGION,
                EN_LANGUAGE,
                testContext.getString(R.string.pocket_pinned_top_articles),
                POCKET_TRENDING_URL
            ),
            DefaultSite(
                XX_REGION,
                XX_LANGUAGE,
                testContext.getString(R.string.default_top_site_wikipedia),
                WIKIPEDIA_URL
            )
        )
        val defaultTopSites = defaultSites.map { entity -> Pair(entity.title, entity.url) }

        whenever(pinnedSitesStorage.getDefaultSites(anyString(), anyString())).thenReturn(
            defaultSites
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState("US", "US"),
            isDefaultSiteAdded = false,
            coroutineContext
        )

        verify(pinnedSitesStorage).getDefaultSites(XX_REGION, EN_LANGUAGE)
        verify(pinnedSitesStorage).addAllPinnedSites(defaultTopSites, isDefault = true)
    }

    @Test
    fun `update existing Google default top sites according to region CN, RU, TR, KZ, BY`() = runBlockingTest {
        preference(testContext).edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0).apply()

        val defaultSiteGoogle = TopSite(
            id = 1,
            title = "Google",
            url = GOOGLE_URL,
            createdAt = 1,
            type = TopSite.Type.DEFAULT
        )
        val pinnedSite = TopSite(
            id = 2,
            title = "Wikipedia",
            url = "https://wikipedia.com",
            createdAt = 2,
            type = TopSite.Type.PINNED
        )
        whenever(pinnedSitesStorage.getPinnedSites()).thenReturn(
            listOf(
                defaultSiteGoogle,
                pinnedSite
            )
        )

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(CN_REGION, CN_REGION),
            isDefaultSiteAdded = true,
            coroutineContext
        )

        verify(historyStorage).deleteVisitsFor(defaultSiteGoogle.url)
        verify(pinnedSitesStorage).removePinnedSite(defaultSiteGoogle)

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(RU_REGION, RU_REGION),
            isDefaultSiteAdded = true,
            coroutineContext
        )

        verify(historyStorage).deleteVisitsFor(defaultSiteGoogle.url)
        verify(pinnedSitesStorage).removePinnedSite(defaultSiteGoogle)

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(TR_REGION, TR_REGION),
            isDefaultSiteAdded = true,
            coroutineContext
        )

        verify(historyStorage).deleteVisitsFor(defaultSiteGoogle.url)
        verify(pinnedSitesStorage).removePinnedSite(defaultSiteGoogle)

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(KZ_REGION, KZ_REGION),
            isDefaultSiteAdded = true,
            coroutineContext
        )

        verify(historyStorage).deleteVisitsFor(defaultSiteGoogle.url)
        verify(pinnedSitesStorage).removePinnedSite(defaultSiteGoogle)

        DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = RegionState(BY_REGION, BY_REGION),
            isDefaultSiteAdded = true,
            coroutineContext
        )

        verify(historyStorage).deleteVisitsFor(defaultSiteGoogle.url)
        verify(pinnedSitesStorage).removePinnedSite(defaultSiteGoogle)
    }

    @Test
    fun `addPinnedSite`() = runBlockingTest {
        val defaultTopSitesStorage = DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = true,
            coroutineContext
        )
        defaultTopSitesStorage.addTopSite("Mozilla", "https://mozilla.com", isDefault = false)

        verify(pinnedSitesStorage).addPinnedSite(
            "Mozilla",
            "https://mozilla.com",
            isDefault = false
        )
    }

    @Test
    fun `removeTopSite`() = runBlockingTest {
        val defaultTopSitesStorage = DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = true,
            coroutineContext
        )

        val frecentSite = TopSite(
            id = 1,
            title = "Mozilla",
            url = "https://mozilla.com",
            createdAt = 1,
            type = TopSite.Type.FRECENT
        )
        defaultTopSitesStorage.removeTopSite(frecentSite)

        verify(historyStorage).deleteVisitsFor(frecentSite.url)

        val pinnedSite = TopSite(
            id = 2,
            title = "Firefox",
            url = "https://firefox.com",
            createdAt = 2,
            type = TopSite.Type.PINNED
        )
        defaultTopSitesStorage.removeTopSite(pinnedSite)

        verify(pinnedSitesStorage).removePinnedSite(pinnedSite)
        verify(historyStorage).deleteVisitsFor(pinnedSite.url)

        val defaultSite = TopSite(
            id = 3,
            title = "Wikipedia",
            url = "https://wikipedia.com",
            createdAt = 3,
            type = TopSite.Type.DEFAULT
        )
        defaultTopSitesStorage.removeTopSite(defaultSite)

        verify(pinnedSitesStorage).removePinnedSite(defaultSite)
        verify(historyStorage).deleteVisitsFor(defaultSite.url)
    }

    @Test
    fun `updateTopSite`() = runBlockingTest {
        val defaultTopSitesStorage = DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = true,
            coroutineContext
        )

        val defaultSite = TopSite(
            id = 1,
            title = "Firefox",
            url = "https://firefox.com",
            createdAt = 1,
            type = TopSite.Type.DEFAULT
        )
        defaultTopSitesStorage.updateTopSite(defaultSite, "Mozilla Firefox", "https://mozilla.com")

        verify(pinnedSitesStorage).updatePinnedSite(defaultSite, "Mozilla Firefox", "https://mozilla.com")

        val pinnedSite = TopSite(
            id = 2,
            title = "Wikipedia",
            url = "https://wikipedia.com",
            createdAt = 2,
            type = TopSite.Type.PINNED
        )
        defaultTopSitesStorage.updateTopSite(pinnedSite, "Wiki", "https://en.wikipedia.org/wiki/Wiki")

        verify(pinnedSitesStorage).updatePinnedSite(pinnedSite, "Wiki", "https://en.wikipedia.org/wiki/Wiki")

        val frecentSite = TopSite(
            id = 1,
            title = "Mozilla",
            url = "https://mozilla.com",
            createdAt = 1,
            type = TopSite.Type.FRECENT
        )
        defaultTopSitesStorage.updateTopSite(frecentSite, "Moz", "")

        verify(pinnedSitesStorage, never()).updatePinnedSite(frecentSite, "Moz", "")
    }

    @Test
    fun `getTopSites returns only default and pinned sites when frecencyConfig is null`() = runBlockingTest {
        val defaultTopSitesStorage = DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = true,
            coroutineContext
        )

        val defaultSite = TopSite(
            id = 1,
            title = "Firefox",
            url = "https://firefox.com",
            createdAt = 1,
            type = TopSite.Type.DEFAULT
        )
        val pinnedSite = TopSite(
            id = 2,
            title = "Wikipedia",
            url = "https://wikipedia.com",
            createdAt = 2,
            type = TopSite.Type.PINNED
        )
        whenever(pinnedSitesStorage.getPinnedSites()).thenReturn(
            listOf(
                defaultSite,
                pinnedSite
            )
        )
        whenever(pinnedSitesStorage.getPinnedSitesCount()).thenReturn(2)

        var topSites = defaultTopSitesStorage.getTopSites(0, frecencyConfig = null)
        assertTrue(topSites.isEmpty())
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        topSites = defaultTopSitesStorage.getTopSites(1, frecencyConfig = null)
        assertEquals(1, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        topSites = defaultTopSitesStorage.getTopSites(2, frecencyConfig = null)
        assertEquals(2, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(pinnedSite, topSites[1])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        topSites = defaultTopSitesStorage.getTopSites(5, frecencyConfig = null)
        assertEquals(2, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(pinnedSite, topSites[1])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)
    }

    @Test
    fun `getTopSites returns pinned and frecent sites when frecencyConfig is specified`() = runBlockingTest {
        val defaultTopSitesStorage = DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = true,
            coroutineContext
        )

        val defaultSite = TopSite(
            id = 1,
            title = "Firefox",
            url = "https://firefox.com",
            createdAt = 1,
            type = TopSite.Type.DEFAULT
        )
        val pinnedSite = TopSite(
            id = 2,
            title = "Wikipedia",
            url = "https://wikipedia.com",
            createdAt = 2,
            type = TopSite.Type.PINNED
        )
        whenever(pinnedSitesStorage.getPinnedSites()).thenReturn(
            listOf(
                defaultSite,
                pinnedSite
            )
        )
        whenever(pinnedSitesStorage.getPinnedSitesCount()).thenReturn(2)

        val frecentSite1 = TopFrecentSiteInfo("https://mozilla.com", "Mozilla")
        whenever(historyStorage.getTopFrecentSites(anyInt(), any())).thenReturn(listOf(frecentSite1))

        var topSites = defaultTopSitesStorage.getTopSites(0, frecencyConfig = FrecencyThresholdOption.NONE)
        assertTrue(topSites.isEmpty())

        topSites = defaultTopSitesStorage.getTopSites(1, frecencyConfig = FrecencyThresholdOption.NONE)
        assertEquals(1, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        topSites = defaultTopSitesStorage.getTopSites(2, frecencyConfig = FrecencyThresholdOption.NONE)
        assertEquals(2, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(pinnedSite, topSites[1])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        topSites = defaultTopSitesStorage.getTopSites(5, frecencyConfig = FrecencyThresholdOption.NONE)
        assertEquals(3, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(pinnedSite, topSites[1])
        assertEquals(frecentSite1.toTopSite(), topSites[2])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        val frecentSite2 = TopFrecentSiteInfo("https://example.com", "Example")
        val frecentSite3 = TopFrecentSiteInfo("https://getpocket.com", "Pocket")
        whenever(historyStorage.getTopFrecentSites(anyInt(), any())).thenReturn(
            listOf(
                frecentSite1,
                frecentSite2,
                frecentSite3
            )
        )

        topSites = defaultTopSitesStorage.getTopSites(5, frecencyConfig = FrecencyThresholdOption.NONE)
        assertEquals(5, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(pinnedSite, topSites[1])
        assertEquals(frecentSite1.toTopSite(), topSites[2])
        assertEquals(frecentSite2.toTopSite(), topSites[3])
        assertEquals(frecentSite3.toTopSite(), topSites[4])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)

        val frecentSite4 = TopFrecentSiteInfo("https://example2.com", "Example2")
        whenever(historyStorage.getTopFrecentSites(anyInt(), any())).thenReturn(
            listOf(
                frecentSite1,
                frecentSite2,
                frecentSite3,
                frecentSite4
            )
        )

        topSites = defaultTopSitesStorage.getTopSites(5, frecencyConfig = FrecencyThresholdOption.NONE)
        assertEquals(5, topSites.size)
        assertEquals(defaultSite, topSites[0])
        assertEquals(pinnedSite, topSites[1])
        assertEquals(frecentSite1.toTopSite(), topSites[2])
        assertEquals(frecentSite2.toTopSite(), topSites[3])
        assertEquals(frecentSite3.toTopSite(), topSites[4])
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)
    }

    @Test
    fun `getTopSites filters out frecent sites that already exist in pinned sites`() = runBlockingTest {
        val defaultTopSitesStorage = DefaultTopSitesStorage(
            testContext,
            pinnedSitesStorage,
            historyStorage,
            region = region,
            isDefaultSiteAdded = true,
            coroutineContext
        )

        val defaultSiteFirefox = TopSite(
            id = 1,
            title = "Firefox",
            url = "https://firefox.com",
            createdAt = 1,
            type = TopSite.Type.DEFAULT
        )
        val pinnedSite1 = TopSite(
            id = 2,
            title = "Wikipedia",
            url = "https://wikipedia.com",
            createdAt = 2,
            type = TopSite.Type.PINNED
        )
        val pinnedSite2 = TopSite(
            id = 3,
            title = "Example",
            url = "https://example.com",
            createdAt = 3,
            type = TopSite.Type.PINNED
        )
        whenever(pinnedSitesStorage.getPinnedSites()).thenReturn(
            listOf(
                defaultSiteFirefox,
                pinnedSite1,
                pinnedSite2
            )
        )
        whenever(pinnedSitesStorage.getPinnedSitesCount()).thenReturn(3)

        val frecentSiteWithNoTitle = TopFrecentSiteInfo("https://mozilla.com", "")
        val frecentSiteFirefox = TopFrecentSiteInfo("https://firefox.com", "Firefox")
        val frecentSite1 = TopFrecentSiteInfo("https://getpocket.com", "Pocket")
        val frecentSite2 = TopFrecentSiteInfo("https://www.example.com", "Example")
        whenever(historyStorage.getTopFrecentSites(anyInt(), any())).thenReturn(
            listOf(
                frecentSiteWithNoTitle,
                frecentSiteFirefox,
                frecentSite1,
                frecentSite2
            )
        )

        val topSites = defaultTopSitesStorage.getTopSites(5, frecencyConfig = FrecencyThresholdOption.NONE)

        verify(historyStorage).getTopFrecentSites(5, frecencyThreshold = FrecencyThresholdOption.NONE)

        assertEquals(5, topSites.size)
        assertEquals(defaultSiteFirefox, topSites[0])
        assertEquals(pinnedSite1, topSites[1])
        assertEquals(pinnedSite2, topSites[2])
        assertEquals(frecentSiteWithNoTitle.toTopSite(), topSites[3])
        assertEquals(frecentSite1.toTopSite(), topSites[4])
        assertEquals("mozilla.com", frecentSiteWithNoTitle.toTopSite().title)
        assertEquals(defaultTopSitesStorage.cachedTopSites, topSites)
    }

    companion object {
        private fun preference(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        }
    }
}
