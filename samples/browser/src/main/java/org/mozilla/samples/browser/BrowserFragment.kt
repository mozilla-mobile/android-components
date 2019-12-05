/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import kotlinx.android.synthetic.main.fragment_browser.view.*
import mozilla.components.feature.awesomebar.AwesomeBarFeature
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.session.ThumbnailsFeature
import mozilla.components.feature.tabs.WindowFeature
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.WebExtensionToolbarFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.ktx.android.arch.lifecycle.addObservers
import org.mozilla.samples.browser.ext.components
import org.mozilla.samples.browser.integration.P2PIntegration
import org.mozilla.samples.browser.integration.ReaderViewIntegration

/**
 * Fragment used for browsing the web within the main app.
 */
class BrowserFragment : BaseBrowserFragment(), UserInteractionHandler {
    private val thumbnailsFeature = ViewBoundFeatureWrapper<ThumbnailsFeature>()
    private val readerViewFeature = ViewBoundFeatureWrapper<ReaderViewIntegration>()
    private val p2pIntegration = ViewBoundFeatureWrapper<P2PIntegration>()
    private val webExtToolbarFeature = ViewBoundFeatureWrapper<WebExtensionToolbarFeature>()

    @Suppress("LongMethod")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = super.onCreateView(inflater, container, savedInstanceState)

        ToolbarAutocompleteFeature(layout.toolbar).apply {
            addHistoryStorageProvider(components.historyStorage)
            addDomainProvider(components.shippedDomainsProvider)
        }

        TabsToolbarFeature(layout.toolbar, components.sessionManager, sessionId, ::showTabs)

        AwesomeBarFeature(layout.awesomeBar, layout.toolbar, layout.engineView, components.icons)
            .addHistoryProvider(
                components.historyStorage,
                components.sessionUseCases.loadUrl
            )
            .addSessionProvider(components.sessionManager, components.tabsUseCases.selectTab)
            .addSearchProvider(
                requireContext(),
                components.searchEngineManager,
                components.searchUseCases.defaultSearch,
                fetchClient = components.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS
            )
            .addClipboardProvider(requireContext(), components.sessionUseCases.loadUrl)

        readerViewFeature.set(
            feature = ReaderViewIntegration(
                requireContext(),
                components.engine,
                components.sessionManager,
                layout.toolbar,
                layout.readerViewBar,
                layout.readerViewAppearanceButton
            ),
            owner = this,
            view = layout
        )

        thumbnailsFeature.set(
            feature = ThumbnailsFeature(
                requireContext(),
                layout.engineView,
                components.sessionManager
            ),
            owner = this,
            view = layout
        )

        p2pIntegration.set(
            feature = P2PIntegration(
                store = components.store,
                engine = components.engine,
                view = layout.p2p,
                thunk = { -> components.nearbyConnection },
                sessionManager = components.sessionManager,
                tabsUseCases = components.tabsUseCases,
                sessionUseCases = components.sessionUseCases
            ) { permissions ->
                requestPermissions(permissions, REQUEST_CODE_P2P_PERMISSIONS)
            },
            owner = this,
            view = layout
        )

        val p2pFeature = p2pIntegration.get()!!.feature

        webExtToolbarFeature.set(
            feature = WebExtensionToolbarFeature(
                layout.toolbar,
                components.store
            ),
            owner = this,
            view = layout
        )

        val windowFeature = WindowFeature(components.store, components.tabsUseCases)
        lifecycle.addObservers(windowFeature, p2pFeature)

        return layout
    }

    private fun showTabs() {
        // For now we are performing manual fragment transactions here. Once we can use the new
        // navigation support library we may want to pass navigation graphs around.
        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.container, TabsTrayFragment())
            commit()
        }
    }

    override fun onBackPressed(): Boolean =
        readerViewFeature.onBackPressed() || super.onBackPressed()

    @CallSuper
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_P2P_PERMISSIONS) {
            p2pIntegration.get()!!.feature.onPermissionsResult(permissions, grantResults)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val REQUEST_CODE_P2P_PERMISSIONS = MAX_REQUEST_CODE_PERMISSIONS + 1

        fun create(sessionId: String? = null) = BrowserFragment().apply {
            arguments = Bundle().apply {
                putSessionId(sessionId)
            }
        }
    }
}
