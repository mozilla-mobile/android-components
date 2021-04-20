/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites.db

import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * Internal DAO for accessing [DefaultSiteEntity] instances.
 */
@Dao
internal interface DefaultSiteDao {
    @WorkerThread
    @Insert
    fun insertDefaultSite(site: DefaultSiteEntity): Long

    @WorkerThread
    @Transaction
    fun insertAllDefaultSites(sites: List<DefaultSiteEntity>): List<Long> {
        return sites.map { entity ->
            val id = insertDefaultSite(entity)
            entity.id = id
            id
        }
    }

    @WorkerThread
    @Query("""
        SELECT * FROM default_top_sites
        WHERE (region = :countryCode
        AND (language = :language OR language = 'XX'))
    """)
    fun getDefaultSites(countryCode: String, language: String): List<DefaultSiteEntity>
}
