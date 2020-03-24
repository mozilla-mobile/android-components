/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.browser

import android.app.Application
import mozilla.components.browser.session.Session
import mozilla.components.feature.addons.update.GlobalAddonDependencyProvider
import mozilla.components.service.glean.Glean
import mozilla.components.support.base.facts.Facts
import mozilla.components.support.base.facts.processor.LogFactProcessor
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.ktx.android.content.isMainProcess
import mozilla.components.support.ktx.android.content.runOnlyInMainProcess
import mozilla.components.support.webextensions.WebExtensionSupport

class SampleApplication : Application() {
    private val logger = Logger("SampleApplication")

    val components by lazy { Components(this) }

    override fun onCreate() {
        super.onCreate()

        Log.addSink(AndroidLogSink())

        if (!isMainProcess()) {
            return
        }

        // IMPORTANT: the following lines initialize the Glean SDK but disable upload
        // of pings. If, for testing purposes, upload is required to be on, change the
        // next line to `uploadEnabled = true`.
        Glean.initialize(applicationContext, uploadEnabled = false)

        Facts.registerProcessor(LogFactProcessor())

        components.engine.warmUp()

        try {
            GlobalAddonDependencyProvider.initialize(
                components.addonManager,
                components.addonUpdater
            )
            WebExtensionSupport.initialize(
                components.engine,
                components.store,
                onNewTabOverride = {
                    _, engineSession, url ->
                        val session = Session(url)
                        components.sessionManager.add(session, true, engineSession)
                        session.id
                },
                onCloseTabOverride = {
                    _, sessionId -> components.tabsUseCases.removeTab(sessionId)
                },
                onSelectTabOverride = {
                    _, sessionId ->
                        val selected = components.sessionManager.findSessionById(sessionId)
                        selected?.let { components.tabsUseCases.selectTab(it) }
                },
                onUpdatePermissionRequest = components.addonUpdater::onUpdatePermissionRequest,
                onExtensionsLoaded = { extensions ->
                    components.addonUpdater.registerForFutureUpdates(extensions)
                    components.supportedAddonsChecker.registerForChecks()
                }
            )
        } catch (e: UnsupportedOperationException) {
            // Web extension support is only available for engine gecko
            Logger.error("Failed to initialize web extension support", e)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        logger.debug("onTrimMemory: $level")

        runOnlyInMainProcess {
            components.sessionManager.onTrimMemory(level)
            components.icons.onTrimMemory(level)
        }
    }
}
