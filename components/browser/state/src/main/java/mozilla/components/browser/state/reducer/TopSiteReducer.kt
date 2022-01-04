/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.state.reducer

import mozilla.components.browser.state.action.TopSiteAction
import mozilla.components.browser.state.state.BrowserState

internal object TopSiteReducer {
    fun reduce(state: BrowserState, action: TopSiteAction): BrowserState {
        return when (action) {
            is TopSiteAction.AddTopSiteAction -> {
                state.copy(
                    topSites = state.topSites + action.topSite
                )
            }
            is TopSiteAction.AddTopSitesAction -> {
                state.copy(
                    topSites = state.topSites + action.topSites
                )
            }
            is TopSiteAction.RemoveTopSiteAction -> {
                state.copy(
                    topSites = state.topSites - action.topSite
                )
            }
            is TopSiteAction.UpdateTopSiteAction -> {
                val topSites = state.topSites
                val topSiteIndex = topSites.indexOfFirst { it == action.topSite }

                if (topSiteIndex == -1) {
                    state
                } else {
                    state.copy(
                        topSites = topSites.subList(0, topSiteIndex) +
                            topSites[topSiteIndex].copy(
                                title = action.title,
                                url = action.url
                            ) +
                            topSites.subList(topSiteIndex + 1, topSites.size)
                    )
                }
            }
        }
    }
}
