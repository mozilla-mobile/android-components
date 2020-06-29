/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.browser.tabstray.BrowserTabsTray
import mozilla.components.browser.tabstray.DefaultTabViewHolder
import mozilla.components.browser.tabstray.TabsAdapter
import mozilla.components.browser.tabstray.ViewHolderProvider
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.feature.intent.ext.getSessionId
import mozilla.components.feature.contextmenu.ext.DefaultSelectionActionDelegate
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.ktx.android.content.share
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupFeature
import org.mozilla.samples.browser.addons.WebExtensionActionPopupActivity
import org.mozilla.samples.browser.ext.components

/**
 * Activity that holds the [BrowserFragment].
 */
open class BrowserActivity : AppCompatActivity(), ComponentCallbacks2 {
    private val webExtensionPopupFeature by lazy {
        WebExtensionPopupFeature(components.store, ::openPopup)
    }

    /**
     * Returns a new instance of [BrowserFragment] to display.
     */
    open fun createBrowserFragment(sessionId: String?): Fragment =
        BrowserFragment.create(sessionId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val sessionId = SafeIntent(intent).getSessionId()
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.container, createBrowserFragment(sessionId))
                commit()
            }
        }

        lifecycle.addObserver(webExtensionPopupFeature)
    }

    override fun onBackPressed() {
        supportFragmentManager.fragments.forEach {
            if (it is UserInteractionHandler && it.onBackPressed()) {
                return
            }
        }

        super.onBackPressed()
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        when (name) {
            EngineView::class.java.name -> components.engine.createView(context, attrs).apply {
                selectionActionDelegate = DefaultSelectionActionDelegate(
                    components.store,
                    context,
                    "Sample Browser"
                ) {
                    share(it)
                }
            }.asView()
            TabsTray::class.java.name -> createTabsTray(context, attrs)
            else -> super.onCreateView(parent, name, context, attrs)
        }

    private fun openPopup(webExtensionState: WebExtensionState) {
        val intent = Intent(this, WebExtensionActionPopupActivity::class.java)
        intent.putExtra("web_extension_id", webExtensionState.id)
        intent.putExtra("web_extension_name", webExtensionState.name)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun createTabsTray(context: Context, attrs: AttributeSet): BrowserTabsTray {
        val thumbnailLoader = ThumbnailLoader(components.thumbnailStorage)
        val viewHolderProvider: ViewHolderProvider = { viewGroup, tabsTray ->
            DefaultTabViewHolder(
                LayoutInflater.from(viewGroup.context).inflate(
                    R.layout.mozac_browser_tabstray_item,
                    viewGroup,
                    false
                ),
                tabsTray,
                thumbnailLoader
            )
        }

        val adapter = TabsAdapter(thumbnailLoader, viewHolderProvider)

        return BrowserTabsTray(context, attrs, 0, adapter)
    }
}
