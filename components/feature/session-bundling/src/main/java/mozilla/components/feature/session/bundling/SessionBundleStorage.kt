/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.session.bundling

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.DataSource
import android.content.Context
import android.support.annotation.CheckResult
import android.support.annotation.VisibleForTesting
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.storage.AutoSave
import mozilla.components.feature.session.bundling.adapter.SessionBundleAdapter
import mozilla.components.feature.session.bundling.db.BundleDatabase
import mozilla.components.feature.session.bundling.db.BundleEntity
import mozilla.components.feature.session.bundling.ext.toBundleEntity
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit

/**
 * A [Session] storage implementation that saves snapshots as a [SessionBundle].
 *
 * @param bundleLifetime The lifetime of a bundle controls whether a bundle will be restored or whether this bundle is
 * considered expired and a new bundle will be used.
 */
class SessionBundleStorage(
    context: Context,
    private val bundleLifetime: Pair<Long, TimeUnit>
) : AutoSave.Storage {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var databaseInitializer = {
        BundleDatabase.get(context)
    }

    private val database by lazy { databaseInitializer() }
    private var lastBundle: BundleEntity? = null

    /**
     * Restores the last [SessionBundle] if there is one without expired lifetime.
     */
    @Synchronized
    fun restore(): SessionBundle? {
        val since = now() - bundleLifetime.second.toMillis(bundleLifetime.first)

        val entity = database
            .bundleDao()
            .getLastBundle(since)
            .also { lastBundle = it }

        return entity?.let { SessionBundleAdapter(it) }
    }

    /**
     * Saves the [SessionManager.Snapshot] as a bundle. If a bundle was restored previously then this bundle will be
     * updated with the data from the snapshot. If no bundle was restored a new bundle will be created.
     */
    @Synchronized
    override fun save(snapshot: SessionManager.Snapshot): Boolean {
        var bundle = lastBundle

        if (bundle == null) {
            bundle = snapshot.toBundleEntity().also { lastBundle = it }
            bundle.id = database.bundleDao().insertBundle(bundle)
        } else {
            bundle.updateFrom(snapshot)
            database.bundleDao().updateBundle(bundle)
        }

        return true
    }

    /**
     * Removes the given [SessionBundle] permanently. If this is the active bundle then a new one will be created the
     * next time a [SessionManager.Snapshot] is saved.
     */
    @Synchronized
    fun remove(bundle: SessionBundle) {
        if (bundle !is SessionBundleAdapter) {
            throw IllegalArgumentException("Unexpected bundle type")
        }

        if (bundle.actual == lastBundle) {
            lastBundle = null
        }

        bundle.actual.let { database.bundleDao().deleteBundle(it) }
    }

    /**
     * Removes all saved [SessionBundle] instances permanently.
     */
    @Synchronized
    fun removeAll() {
        lastBundle = null
        database.clearAllTables()
    }

    /**
     * Returns the currently used [SessionBundle] for saving [SessionManager.Snapshot] instances. Or null if no bundle
     * is in use currently.
     */
    @Synchronized
    fun current(): SessionBundle? {
        return lastBundle?.let { SessionBundleAdapter(it) }
    }

    /**
     * Explicitly uses the given [SessionBundle] (even if not active) to save [SessionManager.Snapshot] instances to.
     */
    @Synchronized
    fun use(bundle: SessionBundle) {
        if (bundle !is SessionBundleAdapter) {
            throw IllegalArgumentException("Unexpected bundle type")
        }

        lastBundle = bundle.actual
    }

    /**
     * Returns the last saved [SessionBundle] instances (up to [limit]) as a [LiveData] list.
     */
    fun bundles(limit: Int = 20): LiveData<List<SessionBundle>> {
        return Transformations.map(
            database
            .bundleDao()
            .getBundles(limit)
        ) { list ->
            list.map { SessionBundleAdapter(it) }
        }
    }

    /**
     * Returns all saved [SessionBundle] instances as a [DataSource.Factory].
     *
     * A consuming app can transform the data source into a `LiveData<PagedList>` of when using RxJava2 into a
     * `Flowable<PagedList>` or `Observable<PagedList>`, that can be observed.
     *
     * - https://developer.android.com/topic/libraries/architecture/paging/data
     * - https://developer.android.com/topic/libraries/architecture/paging/ui
     */
    fun bundlesPaged(): DataSource.Factory<Int, SessionBundle> {
        return database
            .bundleDao()
            .getBundlesPaged()
            .map { entity -> SessionBundleAdapter(entity) }
    }

    /**
     * Starts configuring automatic saving of the state.
     */
    @CheckResult
    fun autoSave(
        sessionManager: SessionManager,
        interval: Long = AutoSave.DEFAULT_INTERVAL_MILLISECONDS,
        unit: TimeUnit = TimeUnit.MILLISECONDS
    ): AutoSave {
        return AutoSave(sessionManager, this, unit.toMillis(interval))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun now() = System.currentTimeMillis()
}
