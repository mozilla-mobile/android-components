/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_browser.view.*
import mozilla.components.feature.awesomebar.AwesomeBarFeature
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import mozilla.components.feature.contextmenu.ContextMenuFeature
import mozilla.components.feature.customtabs.CustomTabsToolbarFeature
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.session.CoordinateScrollingFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.WindowFeature
import mozilla.components.feature.tabs.toolbar.TabsToolbarFeature
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.support.ktx.android.content.isPermissionGranted
import org.mozilla.samples.browser.ext.components

class BrowserFragment : Fragment(), BackHandler {
    private lateinit var sessionFeature: SessionFeature
    private lateinit var toolbarFeature: ToolbarFeature
    private lateinit var toolbarAutocompleteFeature: ToolbarAutocompleteFeature
    private lateinit var tabsToolbarFeature: TabsToolbarFeature
    private lateinit var downloadsFeature: DownloadsFeature
    private lateinit var scrollFeature: CoordinateScrollingFeature
    private lateinit var contextMenuFeature: ContextMenuFeature
    private lateinit var promptFeature: PromptFeature
    private lateinit var windowFeature: WindowFeature
    private lateinit var customTabsToolbarFeature: CustomTabsToolbarFeature

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_browser, container, false)

        layout.toolbar.setMenuBuilder(components.menuBuilder)

        val sessionId = arguments?.getString(SESSION_ID)

        sessionFeature = SessionFeature(
                components.sessionManager,
                components.sessionUseCases,
                layout.engineView,
                sessionId)

        toolbarFeature = ToolbarFeature(
                layout.toolbar,
                components.sessionManager,
                components.sessionUseCases.loadUrl,
                components.defaultSearchUseCase,
                sessionId)

        toolbarAutocompleteFeature = ToolbarAutocompleteFeature(layout.toolbar).apply {
            this.addHistoryStorageProvider(components.historyStorage)
            this.addDomainProvider(components.shippedDomainsProvider)
        }

        tabsToolbarFeature = TabsToolbarFeature(layout.toolbar, components.sessionManager, ::showTabs)

        AwesomeBarFeature(layout.awesomeBar, layout.toolbar, layout.engineView)
            .addHistoryProvider(components.historyStorage, components.sessionUseCases.loadUrl)
            .addSessionProvider(components.sessionManager, components.tabsUseCases.selectTab)
            .addSearchProvider(
                components.searchEngineManager.getDefaultSearchEngine(requireContext()),
                components.searchUseCases.defaultSearch)
            .addClipboardProvider(requireContext(), components.sessionUseCases.loadUrl)

        downloadsFeature = DownloadsFeature(
            requireContext(),
            sessionManager = components.sessionManager,
            fragmentManager = childFragmentManager
        )

        downloadsFeature.onNeedToRequestPermissions = { _, _ ->
            requestPermissions(arrayOf(WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_STORAGE_REQUEST)
        }

        scrollFeature = CoordinateScrollingFeature(components.sessionManager, layout.engineView, layout.toolbar)

        contextMenuFeature = ContextMenuFeature(
            requireFragmentManager(),
            components.sessionManager,
            ContextMenuCandidate.defaultCandidates(
                requireContext(),
                components.tabsUseCases,
                layout))

        promptFeature = PromptFeature(
            fragment = this,
            sessionManager = components.sessionManager,
            fragmentManager = requireFragmentManager()
        ) { _, permissions, requestCode ->
            requestPermissions(permissions, requestCode)
        }

        windowFeature = WindowFeature(components.engine, components.sessionManager)

        customTabsToolbarFeature = CustomTabsToolbarFeature(
            components.sessionManager,
            layout.toolbar,
            sessionId
        )

        // Observe the lifecycle for supported features
        lifecycle.addObservers(
            sessionFeature,
            toolbarFeature,
            downloadsFeature,
            scrollFeature,
            contextMenuFeature,
            promptFeature,
            windowFeature,
            customTabsToolbarFeature
        )

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

    override fun onBackPressed(): Boolean {
        if (toolbarFeature.handleBackPressed()) {
            return true
        }

        if (sessionFeature.handleBackPressed()) {
            return true
        }

        return false
    }

    private fun isStoragePermissionAvailable() = requireContext().isPermissionGranted(WRITE_EXTERNAL_STORAGE)

    companion object {
        private const val SESSION_ID = "session_id"
        private const val PERMISSION_WRITE_STORAGE_REQUEST = 1

        fun create(sessionId: String? = null): BrowserFragment = BrowserFragment().apply {
            arguments = Bundle().apply {
                putString(SESSION_ID, sessionId)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_WRITE_STORAGE_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) &&
                    isStoragePermissionAvailable()) {
                    // permission was granted, yay!
                    downloadsFeature.onPermissionsGranted()
                }
            }
        }
        promptFeature.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        promptFeature.onActivityResult(requestCode, resultCode, data)
    }

    private fun Lifecycle.addObservers(vararg observers: LifecycleObserver) = observers.forEach { addObserver(it) }
}
