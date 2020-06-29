/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.integration

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_browser.view.*
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.app.links.AppLinksUseCases
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyImageLocationCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createCopyLinkCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createOpenImageInNewTabCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createSaveImageCandidate
import mozilla.components.feature.contextmenu.ContextMenuCandidate.Companion.createShareLinkCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.contextmenu.ContextMenuUseCases
import mozilla.components.feature.contextmenu.DefaultSnackbarDelegate
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.base.feature.LifecycleAwareFeature

@Suppress("LongParameterList", "UndocumentedPublicClass")
class ContextMenuIntegration(
    context: Context,
    fragmentManager: FragmentManager,
    browserStore: BrowserStore,
    tabsUseCases: TabsUseCases,
    contextMenuUseCases: ContextMenuUseCases,
    parentView: View,
    sessionId: String? = null
) : LifecycleAwareFeature {

    private val candidates = run {
        if (sessionId != null) {
            val snackbarDelegate = DefaultSnackbarDelegate()
            listOf(
                createCopyLinkCandidate(context, parentView, snackbarDelegate),
                createShareLinkCandidate(context),
                createOpenImageInNewTabCandidate(context, tabsUseCases, parentView, snackbarDelegate),
                createSaveImageCandidate(context, contextMenuUseCases),
                createCopyImageLocationCandidate(context, parentView, snackbarDelegate)
            )
        } else {
            val appLinksCandidate = ContextMenuCandidate.createOpenInExternalAppCandidate(
                context = context,
                appLinksUseCases = AppLinksUseCases(
                    context = context,
                    launchInApp = { true }
                )
            )
            ContextMenuCandidate.defaultCandidates(
                context,
                tabsUseCases,
                contextMenuUseCases,
                parentView
            ) + appLinksCandidate
        }
    }

    private val feature = ContextMenuFeature(
        fragmentManager, browserStore, candidates, parentView.engineView, contextMenuUseCases
    )

    override fun start() {
        feature.start()
    }

    override fun stop() {
        feature.stop()
    }
}
