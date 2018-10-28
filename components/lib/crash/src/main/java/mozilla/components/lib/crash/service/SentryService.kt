/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.lib.crash.service

import android.content.Context
import android.net.Uri
import io.sentry.SentryClient
import io.sentry.SentryClientFactory
import io.sentry.android.AndroidSentryClientFactory
import mozilla.components.lib.crash.Crash

/**
 * A [CrashReporterService] implementation that uploads crash reports to a Sentry server.
 *
 * @param context The application [Context].
 * @param dsn Data Source Name of the Sentry server.
 * @param tags A list of additional tags that will be sent together with crash reports.
 */
class SentryService(
    context: Context,
    dsn: String,
    tags: Map<String, String> = emptyMap(),
    private val sendEventForNativeCrashes: Boolean = false,
    clientFactory: SentryClientFactory = AndroidSentryClientFactory(context)
) : CrashReporterService {
    private val client: SentryClient = SentryClientFactory.sentryClient(
        Uri.parse(dsn).buildUpon()
            .appendQueryParameter("uncaught.handler.enabled", "false")
            .build()
            .toString(),
        clientFactory).apply {
        tags.forEach { entry ->
            addTag(entry.key, entry.value)
        }
    }

    override fun report(crash: Crash.UncaughtExceptionCrash) {
        client.sendException(crash.throwable)
    }

    override fun report(crash: Crash.NativeCodeCrash) {
        if (sendEventForNativeCrashes) {
            client.sendMessage(createMessage(crash))
        }
    }
}

private fun createMessage(crash: Crash.NativeCodeCrash): String {
    val fatal = crash.isFatal.toString()
    val minidumpSuccess = crash.minidumpSuccess

    return "NativeCodeCrash(fatal=$fatal, minidumpSuccess=$minidumpSuccess)"
}
