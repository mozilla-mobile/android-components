/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.storage.FrecencyThresholdOption
import mozilla.components.feature.top.sites.TopSite.Type.DEFAULT
import mozilla.components.feature.top.sites.TopSite.Type.FRECENT
import mozilla.components.feature.top.sites.db.DefaultSite
import mozilla.components.feature.top.sites.ext.hasUrl
import mozilla.components.feature.top.sites.ext.toTopSite
import mozilla.components.feature.top.sites.facts.emitTopSitesCountFact
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.support.locale.LocaleManager
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of [TopSitesStorage].
 *
 * @param pinnedSitesStorage An instance of [PinnedSiteStorage], used for storing pinned sites.
 * @param historyStorage An instance of [PlacesHistoryStorage], used for retrieving top frecent
 * sites from history.
 * @param region The region of the user.
 * @param isDefaultSiteAdded Whether or not the default sites were already added from Fenix.
 */
@Suppress("LongParameterList")
class DefaultTopSitesStorage(
    context: Context,
    private val pinnedSitesStorage: PinnedSiteStorage,
    private val historyStorage: PlacesHistoryStorage,
    private val region: RegionState?,
    private val isDefaultSiteAdded: Boolean,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : TopSitesStorage, Observable<TopSitesStorage.Observer> by ObserverRegistry() {

    private val sharedPref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    private var scope = CoroutineScope(coroutineContext)

    // Cache of the last retrieved top sites
    var cachedTopSites = listOf<TopSite>()

    init {
        if (sharedPref.getInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 0) !=
            sharedPref.getInt(MOZAC_DEFAULT_TOP_SITES_VERSION, 1)
        ) {
            scope.launch {
                initializeDefaultTopSites(context)

                if (!isDefaultSiteAdded) {
                    // Add default top sites for new installs into the pinned sites database table
                    // based on the user's region and language.

                    // If the current region is not CN, RU, TR, KZ or BY, default to a generic XX
                    // region.
                    val countryCode =
                        if (region?.current != null && listOf(
                                CN_REGION,
                                RU_REGION,
                                TR_REGION,
                                KZ_REGION,
                                BY_REGION
                            ).contains(region.current)
                        ) region.current else XX_REGION

                    val locale = LocaleManager.getCurrentLocale(context)
                        ?: LocaleManager.getSystemDefault()
                    val language =
                        if (locale.language == Locale("en").language) EN_LANGUAGE else XX_LANGUAGE

                    val defaultTopSites =
                        pinnedSitesStorage.getDefaultSites(countryCode, language)
                            .map { entity -> Pair(entity.title, entity.url) }
                    pinnedSitesStorage.addAllPinnedSites(defaultTopSites, isDefault = true)
                } else if (listOf(CN_REGION, RU_REGION, TR_REGION, KZ_REGION, BY_REGION).contains(
                        region?.current
                    )
                ) {
                    // Remove existing Google default top sites for CN, RU, TR, KZ, BY regions.
                    pinnedSitesStorage.getPinnedSites()
                        .find { it.type == DEFAULT && it.url == GOOGLE_URL }?.let {
                            removeTopSite(it)
                        }
                }
            }

            sharedPref.edit().putInt(MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION, 1).apply()
        }
    }

    /**
     * Adds all the default top sites into the default top sites database table.
     */
    @Suppress("LongMethod")
    private suspend fun initializeDefaultTopSites(context: Context) {
        pinnedSitesStorage.addAllDefaultSites(
            listOf(
                DefaultSite(
                    CN_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_baidu),
                    BAIDU_URL
                ),
                DefaultSite(
                    CN_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_jd),
                    JD_URL
                ),
                DefaultSite(
                    RU_REGION,
                    EN_LANGUAGE,
                    context.getString(R.string.pocket_pinned_top_articles),
                    POCKET_TRENDING_URL
                ),
                DefaultSite(
                    RU_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_wikipedia),
                    WIKIPEDIA_URL
                ),
                DefaultSite(
                    TR_REGION,
                    EN_LANGUAGE,
                    context.getString(R.string.pocket_pinned_top_articles),
                    POCKET_TRENDING_URL
                ),
                DefaultSite(
                    TR_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_wikipedia),
                    WIKIPEDIA_URL
                ),
                DefaultSite(
                    KZ_REGION,
                    EN_LANGUAGE,
                    context.getString(R.string.pocket_pinned_top_articles),
                    POCKET_TRENDING_URL
                ),
                DefaultSite(
                    KZ_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_wikipedia),
                    WIKIPEDIA_URL
                ),
                DefaultSite(
                    BY_REGION,
                    EN_LANGUAGE,
                    context.getString(R.string.pocket_pinned_top_articles),
                    POCKET_TRENDING_URL
                ),
                DefaultSite(
                    BY_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_wikipedia),
                    WIKIPEDIA_URL
                ),
                DefaultSite(
                    XX_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_google),
                    GOOGLE_URL
                ),
                DefaultSite(
                    XX_REGION,
                    EN_LANGUAGE,
                    context.getString(R.string.pocket_pinned_top_articles),
                    POCKET_TRENDING_URL
                ),
                DefaultSite(
                    XX_REGION,
                    XX_LANGUAGE,
                    context.getString(R.string.default_top_site_wikipedia),
                    WIKIPEDIA_URL
                )
            )
        )
    }

    override fun addTopSite(title: String, url: String, isDefault: Boolean) {
        scope.launch {
            pinnedSitesStorage.addPinnedSite(title, url, isDefault)
            notifyObservers { onStorageUpdated() }
        }
    }

    override fun removeTopSite(topSite: TopSite) {
        scope.launch {
            if (topSite.type != FRECENT) {
                pinnedSitesStorage.removePinnedSite(topSite)
            }

            // Remove the top site from both history and pinned sites storage to avoid having it
            // show up as a frecent site if it is a pinned site.
            historyStorage.deleteVisitsFor(topSite.url)

            notifyObservers { onStorageUpdated() }
        }
    }

    override fun updateTopSite(topSite: TopSite, title: String, url: String) {
        scope.launch {
            if (topSite.type != FRECENT) {
                pinnedSitesStorage.updatePinnedSite(topSite, title, url)
            }

            notifyObservers { onStorageUpdated() }
        }
    }

    override suspend fun getTopSites(
        totalSites: Int,
        frecencyConfig: FrecencyThresholdOption?
    ): List<TopSite> {
        val topSites = ArrayList<TopSite>()
        val pinnedSites = pinnedSitesStorage.getPinnedSites().take(totalSites)
        val numSitesRequired = totalSites - pinnedSites.size
        topSites.addAll(pinnedSites)

        if (frecencyConfig != null && numSitesRequired > 0) {
            // Get 'totalSites' sites for duplicate entries with
            // existing pinned sites
            val frecentSites = historyStorage
                .getTopFrecentSites(totalSites, frecencyConfig)
                .map { it.toTopSite() }
                .filter { !pinnedSites.hasUrl(it.url) }
                .take(numSitesRequired)

            topSites.addAll(frecentSites)
        }

        emitTopSitesCountFact(pinnedSites.size)
        cachedTopSites = topSites

        return topSites
    }

    companion object {
        internal const val PREFERENCE_NAME = "mozac_feature_top_sites"

        // The user's local default top sites version.
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION =
            "MOZAC_LOCAL_DEFAULT_TOP_SITES_VERSION"
        // This is a MOZAC managed versioning number to upgrade default top sites in the future.
        private const val MOZAC_DEFAULT_TOP_SITES_VERSION =
            "MOZAC_DEFAULT_TOP_SITES_VERSION"

        internal const val CN_REGION = "CN"
        internal const val RU_REGION = "RU"
        internal const val TR_REGION = "TR"
        internal const val KZ_REGION = "KZ"
        internal const val BY_REGION = "BY"
        // Catch all for other regions that does not fall into CN, RU, TR, KZ, or BY.
        internal const val XX_REGION = "XX"

        internal const val EN_LANGUAGE = "en"
        // Catch all for other languages that does not fall into "en".
        internal const val XX_LANGUAGE = "XX"

        internal const val POCKET_TRENDING_URL = "https://getpocket.com/fenix-top-articles"
        internal const val WIKIPEDIA_URL = "https://www.wikipedia.org/"
        internal const val GOOGLE_URL = "https://www.google.com/"
        internal const val BAIDU_URL = "https://m.baidu.com/?from=1000969a"
        internal const val JD_URL = "https://union-click.jd.com/jdc" +
            "?e=&p=AyIGZRprFDJWWA1FBCVbV0IUWVALHFRBEwQAQB1AWQkFVUVXfFkAF14lRFRbJXstVWR3WQ1rJ08AZnhS" +
            "HDJBYh4LZR9eEAMUBlccWCUBEQZRGFoXCxc3ZRteJUl8BmUZWhQ" +
            "AEwdRGF0cMhIAVB5ZFAETBVAaXRwyFQdcKydLSUpaCEtYFAIXN2UrWCUyIgdVK1slXVZaCCtZFAMWDg%3D%3D"
    }
}
