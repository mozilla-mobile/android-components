/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import android.content.Context
import android.util.AtomicFile
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.engine.gecko.content.blocking.GeckoTrackingProtectionException
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.content.blocking.TrackingProtectionException
import mozilla.components.concept.engine.content.blocking.TrackingProtectionExceptionStorage
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.util.readAndDeserialize
import org.json.JSONArray
import org.mozilla.geckoview.ContentBlockingController.ContentBlockingException
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.PERMISSION_TRACKING
import java.io.File

internal const val STORE_FILE_NAME =
    "mozilla_components_tracking_protection_storage_gecko.json"

/**
 * A [TrackingProtectionExceptionStorage] implementation to store tracking protection exceptions.
 */
internal class TrackingProtectionExceptionFileStorage(
    private val context: Context,
    private val runtime: GeckoRuntime
) : TrackingProtectionExceptionStorage {
    private val fileLock = Any()
    internal var scope = CoroutineScope(Dispatchers.IO)
    private val logger = Logger("TrackingProtectionExceptionFileStorage")

    /**
     * Restore all exceptions from the [STORE_FILE_NAME] file,
     * and provides them to the gecko [runtime].
     */
    override fun restore() {
        if (!isMigrationOver(context)) {
            logger.info("Starting tracking protection exceptions migration")
            migrateExceptions()
        }
    }

    override fun contains(session: EngineSession, onResult: (Boolean) -> Unit) {
        val url = (session as GeckoEngineSession).currentUrl
        if (!url.isNullOrEmpty()) {
            runtime.storageController.getPermissions(url).accept { permissions ->
                val contains = permissions.filterTrackingProtectionExceptions().isNotEmpty()
                onResult(contains)
            }
        } else {
            onResult(false)
        }
    }

    override fun fetchAll(onResult: (List<TrackingProtectionException>) -> Unit) {
        runtime.storageController.allPermissions.accept { permissions ->
            val trackingExceptions = permissions.filterTrackingProtectionExceptions()
            onResult(trackingExceptions.map { exceptions -> exceptions.toTrackingProtectionException() })
        }
    }

    private fun List<ContentPermission>?.filterTrackingProtectionExceptions() =
        this.orEmpty().filter { it.isExcluded }

    private val ContentPermission.isExcluded: Boolean
        get() = this.permission == PERMISSION_TRACKING && value == VALUE_ALLOW

    override fun add(session: EngineSession) {
        val geckoEngineSession = (session as GeckoEngineSession)
        runtime.contentBlockingController.addException(geckoEngineSession.geckoSession)
        geckoEngineSession.notifyObservers {
            onExcludedOnTrackingProtectionChange(true)
        }
    }

    override fun remove(session: EngineSession) {
        val geckoEngineSession = (session as GeckoEngineSession)
        val url = geckoEngineSession.currentUrl ?: return
        geckoEngineSession.notifyObservers {
            onExcludedOnTrackingProtectionChange(false)
        }
        remove(url)
    }

    override fun remove(exception: TrackingProtectionException) {
        if (exception is GeckoTrackingProtectionException) {
            remove(exception.contentPermission)
        } else {
            remove(exception.url)
        }
    }

    @VisibleForTesting
    internal fun remove(url: String) {
        val storage = runtime.storageController
        storage.getPermissions(url).accept { permissions ->
            permissions.filterTrackingProtectionExceptions().forEach { geckoPermissions ->
                storage.setPermission(geckoPermissions, VALUE_DENY)
            }
        }
    }

    @VisibleForTesting
    internal fun remove(contentPermission: ContentPermission) {
        runtime.storageController.setPermission(contentPermission, VALUE_DENY)
    }

    override fun removeAll(activeSessions: List<EngineSession>?) {
        val storage = runtime.storageController
        activeSessions?.forEach { engineSession ->
            engineSession.notifyObservers {
                onExcludedOnTrackingProtectionChange(false)
            }
        }
        storage.allPermissions.accept { permissions ->
            val trackingExceptions = permissions.filterTrackingProtectionExceptions()
            trackingExceptions.forEach {
                storage.setPermission(it, VALUE_DENY)
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getFile(context: Context): AtomicFile {
        return AtomicFile(
            File(
                context.filesDir,
                STORE_FILE_NAME
            )
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun isMigrationOver(context: Context): Boolean {
        /*
        * We only keep around the exceptions file until
        * the migration is over [STORE_FILE_NAME],
        * after we migrate exceptions we delete the file.
        * */
        return File(context.filesDir, STORE_FILE_NAME).exists()
    }

    /**
     * As part of the migration process, we have to load all exceptions from our
     * file [STORE_FILE_NAME] into geckoView once, after that we can remove our,
     * file [STORE_FILE_NAME].
     */
    internal fun migrateExceptions() {
        scope.launch {
            synchronized(fileLock) {
                getFile(context).readAndDeserialize { json ->
                    if (json.isNotEmpty()) {
                        val jsonArray = JSONArray(json)
                        val exceptionList = (0 until jsonArray.length()).map { index ->
                            val jsonObject = jsonArray.getJSONObject(index)
                            ContentBlockingException.fromJson(jsonObject)
                        }
                        runtime.contentBlockingController.restoreExceptionList(exceptionList)
                    }
                }
                removeFileFromDisk(context)
                logger.debug("Finished tracking protection exceptions migration")
            }
        }
    }

    @VisibleForTesting
    internal fun removeFileFromDisk(context: Context) {
        scope.launch {
            synchronized(fileLock) {
                getFile(context)
                    .delete()
            }
        }
    }
}

private fun ContentPermission.toTrackingProtectionException(): GeckoTrackingProtectionException {
    return GeckoTrackingProtectionException(uri, privateMode, this)
}
