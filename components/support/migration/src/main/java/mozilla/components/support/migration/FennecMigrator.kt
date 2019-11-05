/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.support.migration

import android.content.Context
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.log.logger.Logger
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executors
import kotlin.IllegalStateException
import kotlin.coroutines.CoroutineContext

/**
 * Supported Fennec migrations and their current versions.
 */
sealed class Migration(val currentVersion: Int) {
    /**
     * Migrates history (both "places" and "visits").
     */
    object History : Migration(currentVersion = 1)

    /**
     * Migrates bookmarks. Must run after history was migrated.
     */
    object Bookmarks : Migration(currentVersion = 1)

    /**
     * Migrates open tabs.
     */
    object OpenTabs : Migration(currentVersion = 1)

    /**
     * Migrates FxA state.
     */
    object FxA : Migration(currentVersion = 1)
}

/**
 * Describes a [Migration] at a specific version, enforcing in-range version specification.
 *
 * @property migration A [Migration] in question.
 * @property version Version of the [migration], defaulting to the current version.
 */
data class VersionedMigration(val migration: Migration, val version: Int = migration.currentVersion) {
    init {
        require(version <= migration.currentVersion && version >= 1) {
            "Migration version must be between 1 and current version"
        }
    }
}

/**
 * Entrypoint for Fennec data migration. See [Builder] for public API.
 *
 * @param context Application context used for accessing the file system.
 * @param migrations Describes ordering and versioning of migrations to run.
 * @param historyStorage An optional instance of [PlacesHistoryStorage] used to store migrated history data.
 * @param bookmarksStorage An optional instance of [PlacesBookmarksStorage] used to store migrated bookmarks data.
 * @param coroutineContext An instance of [CoroutineContext] used for executing async migration tasks.
 */
@Suppress("LargeClass")
class FennecMigrator private constructor(
    private val context: Context,
    private val crashReporter: CrashReporter,
    private val migrations: List<VersionedMigration>,
    private val historyStorage: PlacesHistoryStorage?,
    private val bookmarksStorage: PlacesBookmarksStorage?,
    private val sessionManager: SessionManager?,
    private val accountManager: FxaAccountManager?,
    private val profile: FennecProfile?,
    private val fxaState: File?,
    private val browserDbName: String,
    private val coroutineContext: CoroutineContext
) {
    /**
     * Data migration builder. Allows configuring which migrations to run, their versions and relative order.
     */
    class Builder(private val context: Context, private val crashReporter: CrashReporter) {
        private var historyStorage: PlacesHistoryStorage? = null
        private var bookmarksStorage: PlacesBookmarksStorage? = null
        private var sessionManager: SessionManager? = null
        private var accountManager: FxaAccountManager? = null

        private val migrations: MutableList<VersionedMigration> = mutableListOf()

        // Single-thread executor to ensure we don't accidentally parallelize migrations.
        private var coroutineContext: CoroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        private var fxaState = File("${context.filesDir}", "fxa.account.json")
        private var fennecProfile = FennecProfile.findDefault(context)
        private var browserDbName = "browser.db"

        /**
         * Enable history migration.
         *
         * @param storage An instance of [PlacesHistoryStorage], used for storing data.
         * @param version Version of the migration; defaults to the current version.
         */
        fun migrateHistory(storage: PlacesHistoryStorage, version: Int = Migration.History.currentVersion): Builder {
            check(migrations.find { it.migration is Migration.FxA } == null) {
                "FxA migration, if desired, must run after history"
            }
            historyStorage = storage
            migrations.add(VersionedMigration(Migration.History, version))
            return this
        }

        /**
         * Enable bookmarks migration. Must be called after [migrateHistory].
         *
         * @param storage An instance of [PlacesBookmarksStorage], used for storing data.
         * @param version Version of the migration; defaults to the current version.
         */
        fun migrateBookmarks(
            storage: PlacesBookmarksStorage,
            version: Int = Migration.Bookmarks.currentVersion
        ): Builder {
            check(migrations.find { it.migration is Migration.FxA } == null) {
                "FxA migration, if desired, must run after bookmarks"
            }
            check(migrations.find { it.migration is Migration.History } != null) {
                "To migrate bookmarks, you must first migrate history"
            }
            bookmarksStorage = storage
            migrations.add(VersionedMigration(Migration.Bookmarks, version))
            return this
        }

        /**
         * Enable open tabs migration.
         *
         * @param sessionManager An instance of [SessionManager] used for restoring migrated [SessionManager.Snapshot].
         * @param version Version of the migration; defaults to the current version.
         */
        fun migrateOpenTabs(sessionManager: SessionManager, version: Int = Migration.OpenTabs.currentVersion): Builder {
            this.sessionManager = sessionManager
            migrations.add(VersionedMigration(Migration.OpenTabs, version))
            return this
        }

        /**
         * Enable FxA state migration.
         *
         * @param accountManager An instance of [FxaAccountManager] used for authenticating using a migrated account.
         * @param version Version of the migration; defaults to the current version.
         */
        fun migrateFxa(accountManager: FxaAccountManager, version: Int = Migration.FxA.currentVersion): Builder {
            this.accountManager = accountManager
            migrations.add(VersionedMigration(Migration.FxA, version))
            return this
        }

        /**
         * Constructs a [FennecMigrator] based on the current configuration.
         */
        fun build(): FennecMigrator {
            return FennecMigrator(
                context,
                crashReporter,
                migrations,
                historyStorage,
                bookmarksStorage,
                sessionManager,
                accountManager,
                fennecProfile,
                fxaState,
                browserDbName,
                coroutineContext
            )
        }

        // The rest of the setters are useful for unit tests.
        @VisibleForTesting
        internal fun setCoroutineContext(coroutineContext: CoroutineContext): Builder {
            this.coroutineContext = coroutineContext
            return this
        }

        @VisibleForTesting
        internal fun setBrowserDbName(name: String): Builder {
            browserDbName = name
            return this
        }

        @VisibleForTesting
        internal fun setProfile(profile: FennecProfile): Builder {
            fennecProfile = profile
            return this
        }

        @VisibleForTesting
        internal fun setFxaState(state: File): Builder {
            fxaState = state
            return this
        }
    }

    private val logger = Logger("FennecMigrator")

    // Used to ensure migration runs do not overlap.
    private val migrationLock = Object()

    private val dbPath = profile?.let { "${it.path}/$browserDbName" }

    /**
     * Performs configured data migration. See [Builder] for how to configure a data migration.
     *
     * @return A deferred [MigrationResults], describing which migrations were performed and if they succeeded.
     */
    fun migrateAsync(): Deferred<MigrationResults> = synchronized(migrationLock) {
        val migrationRecord = MigrationResultsStore(context)
        val migrationHistory = migrationRecord.getCached()

        // Either didn't run before, or ran with an older version than current migration's version.
        val migrationsToRun = migrations.filter { versionedMigration ->
            val pastVersion = migrationHistory?.get(versionedMigration.migration)?.version
            if (pastVersion == null) {
                true
            } else {
                versionedMigration.version > pastVersion
            }
        }

        // Short-circuit if there's nothing to do.
        if (migrationsToRun.isEmpty()) {
            logger.debug("No migrationz to run.")
            val result = CompletableDeferred<MigrationResults>()
            result.complete(emptyMap())
            return result
        }

        return runMigrationsAsync(migrationsToRun)
    }

    private fun runMigrationsAsync(
        migrations: List<VersionedMigration>
    ): Deferred<MigrationResults> = CoroutineScope(coroutineContext).async {

        // Note that we're depending on coroutineContext to be backed by a single-threaded executor, in order to ensure
        // non-overlapping execution of our migrations.

        val results = mutableMapOf<Migration, MigrationRun>()

        migrations.forEach { versionedMigration ->
            logger.debug("Executing $versionedMigration")

            val migrationResult = when (versionedMigration.migration) {
                Migration.History -> migrateHistory()
                Migration.Bookmarks -> migrateBookmarks()
                Migration.OpenTabs -> migrateOpenTabs()
                Migration.FxA -> migrateFxA()
            }

            results[versionedMigration.migration] = when (migrationResult) {
                is Result.Failure<*> -> {
                    logger.error(
                        "Failed to migrate $versionedMigration",
                        migrationResult.throwables.first()
                    )
                    MigrationRun(versionedMigration.version, false)
                }
                is Result.Success<*> -> {
                    logger.debug(
                        "Migrated $versionedMigration"
                    )
                    MigrationRun(versionedMigration.version, true)
                }
            }
        }

        val migrationStore = MigrationResultsStore(context)
        migrationStore.setOrUpdate(results)
        results
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun migrateHistory(): Result<Unit> {
        checkNotNull(historyStorage) { "History storage must be configured to migrate history" }

        if (dbPath == null) {
            crashReporter.submitCaughtException(IllegalStateException("Missing DB path during history migration"))
            return Result.Failure(IllegalStateException("Missing DB path during history migration"))
        }
        return try {
            logger.debug("Migrating history...")
            historyStorage.importFromFennec(dbPath)
            logger.debug("Migrated history.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun migrateBookmarks(): Result<Unit> {
        checkNotNull(bookmarksStorage) { "Bookmarks storage must be configured to migrate bookmarks" }

        if (dbPath == null) {
            crashReporter.submitCaughtException(IllegalStateException("Missing DB path during bookmark migration"))
            return Result.Failure(IllegalStateException("Missing DB path during bookmark migration"))
        }
        return try {
            logger.debug("Migrating bookmarks...")
            bookmarksStorage.importFromFennec(dbPath)
            logger.debug("Migrated bookmarks.")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    private fun migrateOpenTabs(): Result<SessionManager.Snapshot> {
        if (profile == null) {
            crashReporter.submitCaughtException(IllegalStateException("Missing Profile path"))
            return Result.Failure(IllegalStateException("Missing Profile path"))
        }

        return try {
            logger.debug("Migrating session...")
            val result = FennecSessionMigration.migrate(File(profile.path))
            if (result is Result.Success<*>) {
                logger.debug("Loading migrated session snapshot...")
                sessionManager!!.restore(result.value as SessionManager.Snapshot)
            }
            result
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private suspend fun migrateFxA(): Result<FxaMigrationResult> {
        val result = FennecFxaMigration.migrate(fxaState!!, context, accountManager!!)

        if (result is Result.Failure<FxaMigrationResult>) {
            val migrationFailureWrapper = result.throwables.first() as FxaMigrationException
            return when (val failure = migrationFailureWrapper.failure) {
                is FxaMigrationResult.Failure.CorruptAccountState -> {
                    logger.error("Detected a corrupt account state: $failure")
                    result
                }
                is FxaMigrationResult.Failure.UnsupportedVersions -> {
                    logger.error("Detected unsupported versions: $failure")
                    result
                }
                is FxaMigrationResult.Failure.FailedToSignIntoAuthenticatedAccount -> {
                    logger.error("Failed to sign-in into an authenticated account")
                    crashReporter.submitCaughtException(migrationFailureWrapper)
                    result
                }
            }
        }

        val migrationSuccess = result as Result.Success<FxaMigrationResult>
        return when (migrationSuccess.value as FxaMigrationResult.Success) {
            // The rest are all successful migrations.
            is FxaMigrationResult.Success.NoAccount -> {
                logger.debug("No Fennec account detected")
                result
            }
            is FxaMigrationResult.Success.UnauthenticatedAccount -> {
                // Here we have an 'email' and a state label.
                // "Bad auth state" could be a few things - unverified account, bad credentials detected by Fennec, etc.
                // We could try using the 'email' address as a starting point in the authentication flow.
                logger.debug("Detected a Fennec account in a bad authentication state: migrationResult")
                result
            }
            is FxaMigrationResult.Success.SignedInIntoAuthenticatedAccount -> {
                logger.debug("Signed-in into a detected Fennec account")
                result
            }
        }
    }
}
