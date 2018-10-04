/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.content.Context
import android.util.AttributeSet
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.Settings
import org.mozilla.geckoview.GeckoRuntime

/**
 * Gecko-based implementation of Engine interface.
 */
class GeckoEngine(
    context: Context,
    private val defaultSettings: Settings? = null,
    private val runtime: GeckoRuntime = GeckoRuntime.getDefault(context)
) : Engine {

    /**
     * Creates a new Gecko-based EngineView.
     */
    override fun createView(context: Context, attrs: AttributeSet?): EngineView {
        return GeckoEngineView(context, attrs)
    }

    /**
     * Creates a new Gecko-based EngineSession.
     */
    override fun createSession(private: Boolean): EngineSession {
        return GeckoEngineSession(runtime, private, defaultSettings)
    }

    override fun name(): String = "Gecko"

    /**
     * See [Engine.errorTypeFromCode]
     */
    override fun errorTypeFromCode(errorCode: Int, errorCategory: Int?): ErrorPages.ErrorType {
        throw IllegalArgumentException("No error categories defined in this version")
    }

    /**
     * See [Engine.settings]
     */
    override val settings: Settings = object : Settings() {
        override var javascriptEnabled: Boolean
            get() = runtime.settings.javaScriptEnabled
            set(value) { runtime.settings.javaScriptEnabled = value }

        override var webFontsEnabled: Boolean
            get() = runtime.settings.webFontsEnabled
            set(value) { runtime.settings.webFontsEnabled = value }

        override var trackingProtectionPolicy: TrackingProtectionPolicy?
            get() = TrackingProtectionPolicy.select(runtime.settings.trackingProtectionCategories)
            set(value) {
                value?.let {
                    runtime.settings.trackingProtectionCategories = it.categories
                }
            }

        override var remoteDebuggingEnabled: Boolean
            get() = runtime.settings.remoteDebuggingEnabled
            set(value) { runtime.settings.remoteDebuggingEnabled = value }
    }.apply {
        defaultSettings?.let {
            this.javascriptEnabled = it.javascriptEnabled
            this.webFontsEnabled = it.webFontsEnabled
            this.trackingProtectionPolicy = it.trackingProtectionPolicy
            this.remoteDebuggingEnabled = it.remoteDebuggingEnabled
        }
    }
}
