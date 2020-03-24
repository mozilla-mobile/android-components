/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser.addons

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_add_on_settings.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.samples.browser.R
import org.mozilla.samples.browser.ext.components

/**
 * An activity to show the pop up action of a web extension.
 */
class WebExtensionActionPopupActivity : AppCompatActivity() {
    private lateinit var webExtensionId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_on_settings)

        webExtensionId = requireNotNull(intent.getStringExtra("web_extension_id"))
        intent.getStringExtra("web_extension_name")?.let {
            title = it
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.addonSettingsContainer, WebExtensionActionPopupFragment.create(webExtensionId))
            .commit()
    }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? =
        when (name) {
            EngineView::class.java.name -> components.engine.createView(context, attrs).asView()
            else -> super.onCreateView(parent, name, context, attrs)
        }

    /**
     * A fragment to show the web extension action popup with [EngineView].
     */
    class WebExtensionActionPopupFragment : Fragment() {
        private var engineSession: EngineSession? = null
        private lateinit var webExtensionId: String

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            webExtensionId = requireNotNull(arguments?.getString("web_extension_id"))
            engineSession = components.store.state.extensions[webExtensionId]?.popupSession

            return inflater.inflate(R.layout.fragment_add_on_settings, container, false)
        }

        @ExperimentalCoroutinesApi
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val session = engineSession
            if (session != null) {
                addonSettingsEngineView.render(session)
                consumePopupSession()
            } else {
                consumeFrom(context!!.components.store) { state ->
                    state.extensions[webExtensionId]?.let { extState ->
                        extState.popupSession?.let {
                            if (engineSession == null) {
                                addonSettingsEngineView.render(it)
                                consumePopupSession()
                                engineSession = it
                            }
                        }
                    }
                }
            }
        }

        private fun consumePopupSession() {
            components.store.dispatch(
                WebExtensionAction.UpdatePopupSessionAction(webExtensionId, popupSession = null)
            )
        }

        companion object {
            /**
             * Create an [WebExtensionActionPopupFragment] with webExtensionId as a required parameter.
             */
            fun create(webExtensionId: String) = WebExtensionActionPopupFragment().apply {
                arguments = Bundle().apply {
                    putString("web_extension_id", webExtensionId)
                }
            }
        }
    }
}
