/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites.db

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * Internal DAO for accessing [TopSiteEntity] instances.
 */
@Dao
internal interface TopSiteDao {
    @Insert
    fun insertTopSite(site: TopSiteEntity): Long

    @Delete
    fun deleteTopSite(site: TopSiteEntity)

    @Transaction
    @Query("SELECT * FROM top_sites")
    fun getTopSites(): LiveData<List<TopSiteEntity>>

    @Transaction
    @Query("SELECT * FROM top_sites")
    fun getTopSitesPaged(): DataSource.Factory<Int, TopSiteEntity>
}
