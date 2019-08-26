/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.service.fxa.sync

import android.content.Context
import androidx.annotation.UiThread
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import mozilla.appservices.syncmanager.SyncParams
import mozilla.appservices.syncmanager.SyncReason
import mozilla.appservices.syncmanager.SyncServiceStatus
import mozilla.appservices.syncmanager.SyncManager as RustSyncManager
import mozilla.components.concept.sync.AuthException
import mozilla.components.concept.sync.AuthExceptionType
import mozilla.components.service.fxa.Engine
import mozilla.components.service.fxa.SyncAuthInfoCache
import mozilla.components.service.fxa.SyncConfig
import mozilla.components.service.fxa.manager.SyncEngineManager
import mozilla.components.service.fxa.manager.authErrorRegistry
import mozilla.components.service.fxa.manager.declinedEnginesRegistry
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import java.io.Closeable
import java.util.concurrent.TimeUnit

private enum class SyncWorkerTag {
    Common,
    Immediate,
    Periodic
}

private enum class SyncWorkerName {
    Periodic,
    Immediate
}

private const val KEY_DATA_STORES = "stores"

/**
 * A [SyncManager] implementation which uses WorkManager APIs to schedule sync tasks.
 *
 * Must be initialized on the main thread.
 */
internal class WorkManagerSyncManager(syncConfig: SyncConfig) : SyncManager(syncConfig) {
    override val logger = Logger("BgSyncManager")

    init {
        WorkersLiveDataObserver.init()

        if (syncConfig.syncPeriodInMinutes == null) {
            logger.info("Periodic syncing is disabled.")
        } else {
            logger.info("Periodic syncing enabled at a ${syncConfig.syncPeriodInMinutes} interval")
        }
    }

    override fun createDispatcher(stores: Set<String>): SyncDispatcher {
        return WorkManagerSyncDispatcher(stores)
    }

    override fun dispatcherUpdated(dispatcher: SyncDispatcher) {
        WorkersLiveDataObserver.setDispatcher(dispatcher)
    }
}

/**
 * A singleton wrapper around the the LiveData "forever" observer - i.e. an observer not bound
 * to a lifecycle owner. This observer is always active.
 * We will have different dispatcher instances throughout the lifetime of the app, but always a
 * single LiveData instance.
 */
object WorkersLiveDataObserver {
    private val workersLiveData = WorkManager.getInstance().getWorkInfosByTagLiveData(
        SyncWorkerTag.Common.name
    )

    private var dispatcher: SyncDispatcher? = null

    @UiThread
    fun init() {
        // Only set our observer once.
        if (workersLiveData.hasObservers()) return

        // This must be called on the UI thread.
        workersLiveData.observeForever {
            val isRunning = when (it?.any { worker -> worker.state == WorkInfo.State.RUNNING }) {
                null -> false
                false -> false
                true -> true
            }

            dispatcher?.workersStateChanged(isRunning)

            // TODO process errors coming out of worker.outputData
        }
    }

    fun setDispatcher(dispatcher: SyncDispatcher) {
        this.dispatcher = dispatcher
    }
}

class WorkManagerSyncDispatcher(
    private val stores: Set<String>
) : SyncDispatcher, Observable<SyncStatusObserver> by ObserverRegistry(), Closeable {
    private val logger = Logger("WMSyncDispatcher")

    // TODO does this need to be volatile?
    private var isSyncActive = false

    init {
        // Stop any currently active periodic syncing. Consumers of this class are responsible for
        // starting periodic syncing via [startPeriodicSync] if they need it.
        stopPeriodicSync()
    }

    override fun workersStateChanged(isRunning: Boolean) {
        if (isSyncActive && !isRunning) {
            notifyObservers { onIdle() }
            isSyncActive = false
        } else if (!isSyncActive && isRunning) {
            notifyObservers { onStarted() }
            isSyncActive = true
        }
    }

    override fun isSyncActive(): Boolean {
        return isSyncActive
    }

    override fun syncNow(startup: Boolean) {
        logger.debug("Immediate sync requested, startup = $startup")
        val delayMs = if (startup) {
            // Startup delay is there to avoid SQLITE_BUSY crashes, since we currently do a poor job
            // of managing database connections, and we expect there to be database writes at the start.
            // FIXME https://github.com/mozilla-mobile/android-components/issues/1369
            SYNC_STARTUP_DELAY_MS
        } else {
            0L
        }
        WorkManager.getInstance().beginUniqueWork(
            SyncWorkerName.Immediate.name,
            // Use the 'keep' policy to minimize overhead from multiple "sync now" operations coming in
            // at the same time.
            ExistingWorkPolicy.KEEP,
            immediateSyncWorkRequest(delayMs)
        ).enqueue()
    }

    override fun close() {
        stopPeriodicSync()
    }

    /**
     * Periodic background syncing is mainly intended to reduce workload when we sync during
     * application startup.
     */
    override fun startPeriodicSync(unit: TimeUnit, period: Long) {
        logger.debug("Starting periodic syncing, period = $period, time unit = $unit")
        // Use the 'replace' policy as a simple way to upgrade periodic worker configurations across
        // application versions. We do this instead of versioning workers.
        WorkManager.getInstance().enqueueUniquePeriodicWork(
            SyncWorkerName.Periodic.name,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicSyncWorkRequest(unit, period)
        )
    }

    /**
     * Disables periodic syncing in the background. Currently running syncs may continue until completion.
     * Safe to call this even if periodic syncing isn't currently enabled.
     */
    override fun stopPeriodicSync() {
        logger.debug("Cancelling periodic syncing")
        WorkManager.getInstance().cancelUniqueWork(SyncWorkerName.Periodic.name)
    }

    private fun periodicSyncWorkRequest(unit: TimeUnit, period: Long): PeriodicWorkRequest {
        val data = getWorkerData()
        // Periodic interval must be at least PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
        // e.g. not more frequently than 15 minutes.
        return PeriodicWorkRequestBuilder<WorkManagerSyncWorker>(period, unit)
                .setConstraints(
                        Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                )
                .setInputData(data)
                // TODO  	setBackoffCriteria
                .addTag(SyncWorkerTag.Common.name)
                .addTag(SyncWorkerTag.Periodic.name)
                .build()
    }

    private fun immediateSyncWorkRequest(delayMs: Long = 0L): OneTimeWorkRequest {
        val data = getWorkerData()
        return OneTimeWorkRequestBuilder<WorkManagerSyncWorker>()
                .setConstraints(
                        Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                )
                .setInputData(data)
                .addTag(SyncWorkerTag.Common.name)
                .addTag(SyncWorkerTag.Immediate.name)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                // todo should the immediate sync
                // .setBackoffCriteria(BackoffPolicy.EXPONENTIAL)
                .build()
    }

    private fun getWorkerData(): Data {
        val dataBuilder = Data.Builder().putStringArray(KEY_DATA_STORES, stores.toTypedArray())

        return dataBuilder.build()
    }
}

class WorkManagerSyncWorker(
    private val context: Context,
    private val params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val logger = Logger("SyncWorker")

    private fun isPeriodic(): Boolean {
        return params.tags.contains(SyncWorkerTag.Periodic.name)
    }

    private fun lastSyncedWithinStaggerBuffer(): Boolean {
        val lastSyncedTs = getLastSynced(context)
        return lastSyncedTs != 0L && (System.currentTimeMillis() - lastSyncedTs) < SYNC_STAGGER_BUFFER_MS
    }

    @Suppress("ReturnCount", "LongMethod")
    override suspend fun doWork(): Result {
        logger.debug("Starting sync... Tagged as: ${params.tags}")

        // If this is a periodic sync task, and we've very recently synced successfully, skip it.
        // There could have been a manual or heuristic-driven syc very recently, and it's unlikely
        // that an additional sync so shortly afterward will be valuable.
        // NB: this does not affect manually triggered syncing in any way.
        if (isPeriodic() && lastSyncedWithinStaggerBuffer()) {
            return Result.success()
        }

        // Otherwise, proceed as normal.
        // In order to sync, we'll need:
        // - a list of SyncableStores...
        val syncableStores = params.inputData.getStringArray(KEY_DATA_STORES)!!.associate {
            it to GlobalSyncableStoreProvider.getStore(it)!!
        }.ifEmpty {
            // Short-circuit if there are no configured stores.
            // Don't update the "last-synced" timestamp because we haven't actually synced anything.
            return Result.success()
        }

        // "bookmarks" - does it matter which one we use, "history" or "bookmarks"
        // TODO use enums for names of stores
        syncableStores.entries.forEach {
            // assuming they're all places related
            when (it.key) {
                // These are the same handle!
                "history" -> RustSyncManager.setPlaces(it.value.getHandle())
                "bookmarks" -> RustSyncManager.setPlaces(it.value.getHandle())
                "logins" -> RustSyncManager.setLogins(it.value.getHandle())
            }
        }

        // - and a cached "sync auth info" object.
        val syncAuthInfo = SyncAuthInfoCache(context).getCached() ?: return Result.failure()

        val currentSyncState = getSyncState(context)

        val syncParams = SyncParams(
            // todo pass in "cwts changed" reason?
            reason = when (isPeriodic()) {
                true -> SyncReason.SCHEDULED
                false -> SyncReason.USER
            },
            // sync all, which is an intersection of stores for which we've set a handle and that are enabled
            engines = null,
            authInfo = syncAuthInfo.toNative(),

            // This needs to be the correct set on first sync after sign-up (to populate the 'disabled'
            // list correctly).
            // And every time user changes the CWTS selection.
            // Internally, library checks the 'disabled' list on the server, and ignores this map
            // unless there's a delta.

            // OPTIONS:
            // 1) via `params.inputData` - not good! enabled engines can change in-between, but inputData is created once
            // 2) global state!
            // - sharedPrefs
            // - map of <engine, enabledBooleanFlag>

            enabledChanges = getNativeEnabledEngines(),
            persistedState = currentSyncState
        )
        val syncResult = RustSyncManager.sync(syncParams)
        // In the future, we want to encrypt the state see for more info:
        // https://github.com/mozilla-mobile/android-components/issues/3981
        setSyncState(context, syncResult.persistedState)

        syncResult.failures.entries.forEach {
            logger.error("Failed to sync ${it.key}, reason: ${it.value}")
        }

        syncResult.successful.forEach {
            logger.info("Successfully synced $it")
        }

        syncResult.declined?.let {
            val engines = it.mapNotNull { value -> nativeEngineToLocal(value) }

            declinedEnginesRegistry.notifyObservers { onUpdatedDeclinedEngines(engines.toSet(), isLocalChange = false) }
        }

        // TODO process this into GLEAN sync pings, just like Connection.kt@assembleBookmarksPing
        // syncResult.telemetry

        return when (syncResult.status) {
            SyncServiceStatus.AUTH_ERROR -> {
                logger.error("Auth error")
                authErrorRegistry.notifyObservers {
                    // TODO change this... make exception not necessary
                    // TODO kill AuthExceptionType
                    onAuthErrorAsync(AuthException(AuthExceptionType.UNAUTHORIZED))
                }
                Result.failure()
            }

            // TODO FOR ALL retries: look at workerParams.mRunAttemptCount, don't retry after a certain number.

            SyncServiceStatus.NETWORK_ERROR -> {
                logger.error("Network error")
                Result.retry()
            }
            SyncServiceStatus.BACKED_OFF -> {
                logger.error("Backed-off error")
                Result.retry()
            }
            SyncServiceStatus.SERVICE_ERROR -> {
                logger.error("Service error")
                // TODO should SERVICE_ERROR follow a retry pattern? don't want to ddos our servers when they're having problems.
                Result.retry()
            }
            SyncServiceStatus.OTHER_ERROR -> {
                logger.error("'Other' error :(")
                Result.retry()
            }
            SyncServiceStatus.OK -> {
                logger.error("All good")
                // TODO think about error reporting.
                // Worker should set the "last-synced" timestamp, and since we have a single timestamp,
                // it's not clear if a single failure should prevent its update. That's the current behaviour
                // in Fennec, but for very specific reasons that aren't relevant here. We could have
                // a timestamp per store, or whatever we want here really.
                // For now, we just update it every time we succeed to sync.
                setLastSynced(context, System.currentTimeMillis())
                Result.success()
            }
        }
    }



    private fun getNativeEnabledEngines(): Map<String, Boolean> {
        val enabledChanges = mutableMapOf<String, Boolean>()

        // On first sync the native side is expecting an empty map,
        // we are filtering to match this behavior.
        SyncEngineManager(context).getEnginesStatus()
                .filter { (_, value) -> value != null }
                .forEach {
                    it.key.toNativeEngine() to it.value
                    enabledChanges[it.key.toNativeEngine()] = requireNotNull(it.value)
                }

        return enabledChanges
    }

    private fun nativeEngineToLocal(engine: String): Engine? {
        return when (engine) {
            "history" -> Engine.History
            "bookmarks" -> Engine.Bookmarks
            "passwords" -> Engine.Logins
            else -> {
                logger.debug("$engine unknown engine")
                null
            }
        }
    }

    private fun Engine.toNativeEngine() : String {
       return when(this){
            Engine.History -> "history"
            Engine.Bookmarks -> "bookmarks"
            Engine.Logins -> "passwords"
        }
    }
}

private const val SYNC_STATE_PREFS_KEY = "syncPrefs"
private const val SYNC_LAST_SYNCED_KEY = "lastSynced"
private const val SYNC_STATE_KEY = "persistedState"

private const val SYNC_STAGGER_BUFFER_MS = 10 * 60 * 1000L // 10 minutes.
private const val SYNC_STARTUP_DELAY_MS = 5 * 1000L // 5 seconds.

fun getLastSynced(context: Context): Long {
    return context
        .getSharedPreferences(SYNC_STATE_PREFS_KEY, Context.MODE_PRIVATE)
        .getLong(SYNC_LAST_SYNCED_KEY, 0)
}

fun setLastSynced(context: Context, ts: Long) {
    context
        .getSharedPreferences(SYNC_STATE_PREFS_KEY, Context.MODE_PRIVATE)
        .edit()
        .putLong(SYNC_LAST_SYNCED_KEY, ts)
        .apply()
}

fun getSyncState(context: Context): String? {
    return context
        .getSharedPreferences(SYNC_STATE_PREFS_KEY, Context.MODE_PRIVATE)
        .getString(SYNC_STATE_KEY, null)
}

fun setSyncState(context: Context, state: String) {
    context
        .getSharedPreferences(SYNC_STATE_PREFS_KEY, Context.MODE_PRIVATE)
        .edit()
        .putString(SYNC_STATE_KEY, state)
        .apply()
}
