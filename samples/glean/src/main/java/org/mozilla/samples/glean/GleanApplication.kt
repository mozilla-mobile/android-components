/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.glean

import android.app.Application
import android.content.Context
import android.net.Uri
import mozilla.components.lib.fetch.httpurlconnection.HttpURLConnectionClient
import mozilla.components.service.glean.Glean
import mozilla.components.service.glean.config.Configuration
import mozilla.components.service.glean.net.ConceptFetchHttpUploader
import mozilla.components.service.nimbus.Nimbus
import mozilla.components.service.nimbus.NimbusApi
import mozilla.components.service.nimbus.NimbusServerSettings
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.rusthttp.RustHttpConfig
import mozilla.components.support.rustlog.RustLog
import org.mozilla.samples.glean.GleanMetrics.Basic
import org.mozilla.samples.glean.GleanMetrics.GleanBuildInfo
import org.mozilla.samples.glean.GleanMetrics.Test
import org.mozilla.samples.glean.GleanMetrics.Custom
import org.mozilla.samples.glean.GleanMetrics.Pings

class GleanApplication : Application() {

    companion object {
        lateinit var nimbus: NimbusApi

        const val SAMPLE_GLEAN_PREFERENCES = "sample_glean_preferences"
        const val PREF_IS_FIRST_RUN = "isFirstRun"
    }

    override fun onCreate() {
        super.onCreate()
        val settings = applicationContext.getSharedPreferences(SAMPLE_GLEAN_PREFERENCES, Context.MODE_PRIVATE)
        val isFirstRun = settings.getBoolean(PREF_IS_FIRST_RUN, true)

        // We want the log messages of all builds to go to Android logcat

        Log.addSink(AndroidLogSink())

        // Register the sample application's custom pings.
        Glean.registerPings(Pings)

        // Initialize the Glean library. Ideally, this is the first thing that
        // must be done right after enabling logging.
        val client by lazy { HttpURLConnectionClient() }
        val httpClient = ConceptFetchHttpUploader.fromClient(client)
        val config = Configuration(httpClient = httpClient)
        Glean.initialize(
            applicationContext,
            uploadEnabled = true,
            configuration = config,
            buildInfo = GleanBuildInfo.buildInfo
        )

        /** Begin Nimbus component specific code. Note: this is not relevant to Glean */
        initNimbus(isFirstRun)
        /** End Nimbus specific code. */

        Test.timespan.start()

        Custom.counter.add()

        // Set a sample value for a metric.
        Basic.os.set("Android")

        settings
            .edit()
            .putBoolean(PREF_IS_FIRST_RUN, false)
            .apply()
    }

    /**
     * Initialize the Nimbus experiments library. This is only relevant to the Nimbus library, aside
     * from recording the experiment in Glean.
     */
    private fun initNimbus(isFirstRun: Boolean) {
        RustLog.enable()
        RustHttpConfig.setClient(lazy { HttpURLConnectionClient() })
        val url = Uri.parse(getString(R.string.nimbus_default_endpoint))
        nimbus = Nimbus(this,
            NimbusServerSettings(url)
        ).also { nimbus ->
            if (isFirstRun) {
                // This file is bundled with the app, but derived from the server at build time.
                // We'll use it now, on first run.
                nimbus.setExperimentsLocally(R.raw.initial_experiments)
            }
            // Apply the experiments downloaded on last run, but on first run, it will
            // use the contents of `R.raw.initial_experiments`.
            nimbus.applyPendingExperiments()

            // In a real application, we might want to fetchExperiments() here.
            //
            // We won't do that in this app because:
            //   * the server's experiments will overwrite the current ones
            //   * it's not clear that the server will have a `test-color` experiment
            //     by the time you run this
            //   * an update experiments button is in `MainActivity`
            //
            // nimbus.fetchExperiments()
        }
    }
}
