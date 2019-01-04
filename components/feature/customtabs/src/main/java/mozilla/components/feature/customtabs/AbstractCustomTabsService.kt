/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.customtabs

import android.app.Service
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsService
import android.support.customtabs.CustomTabsSessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mozilla.components.concept.engine.Engine
import mozilla.components.support.base.log.logger.Logger

/**
 * Maximum number of speculative connections we will open when an app calls into
 * [AbstractCustomTabsService.mayLaunchUrl] with a list of URLs.
 */
private const val MAX_SPECULATIVE_URLS = 50

/**
 * [Service] providing Custom Tabs related functionality.
 */
abstract class AbstractCustomTabsService : CustomTabsService() {
    private val logger = Logger("CustomTabsService")

    abstract val engine: Engine

    override fun warmup(flags: Long): Boolean {
        // We need to run this on the main thread since that's where GeckoRuntime expects to get initialized (if needed)
        return runBlocking(Dispatchers.Main) {
            // Just accessing the engine here will make sure that it is created and initialized.
            logger.debug("Warm up for engine: ${engine.name()}")
            true
        }
    }

    override fun requestPostMessageChannel(sessionToken: CustomTabsSessionToken?, postMessageOrigin: Uri?): Boolean {
        return false
    }

    override fun newSession(sessionToken: CustomTabsSessionToken?): Boolean {
        return true
    }

    override fun extraCommand(commandName: String?, args: Bundle?): Bundle? {
        return null
    }

    override fun mayLaunchUrl(
        sessionToken: CustomTabsSessionToken?,
        url: Uri?,
        extras: Bundle?,
        otherLikelyBundles: MutableList<Bundle>?
    ): Boolean {
        logger.debug("Opening speculative connections")

        // Most likely URL for a future navigation: Open a speculative connection.
        url?.let { engine.speculativeConnect(it.toString()) }

        // A list of other likely URLs. Let's open a speculative connection for them up to a limit.
        otherLikelyBundles?.take(MAX_SPECULATIVE_URLS)?.forEach { bundle ->
            bundle.getParcelable<Uri>(KEY_URL)?.let { uri ->
                engine.speculativeConnect(uri.toString())
            }
        }

        return true
    }

    override fun postMessage(sessionToken: CustomTabsSessionToken?, message: String?, extras: Bundle?): Int {
        return 0
    }

    override fun validateRelationship(
        sessionToken: CustomTabsSessionToken?,
        relation: Int,
        origin: Uri?,
        extras: Bundle?
    ): Boolean {
        return false
    }

    override fun updateVisuals(sessionToken: CustomTabsSessionToken?, bundle: Bundle?): Boolean {
        return false
    }
}
