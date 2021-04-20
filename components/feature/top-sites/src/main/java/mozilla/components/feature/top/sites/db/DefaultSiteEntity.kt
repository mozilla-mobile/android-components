/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.top.sites.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Internal entity representing a default site.
 */
@Entity(tableName = "default_top_sites")
internal data class DefaultSiteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long? = null,

    @ColumnInfo(name = "region")
    var region: String,

    @ColumnInfo(name = "language")
    var language: String,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "url")
    var url: String
) {
    internal fun toDefaultSite(): DefaultSite {
        return DefaultSite(
            region,
            language,
            title,
            url
        )
    }
}

/**
 * Represents a default site.
 *
 * @property region The country region to add this default site for.
 * (Example: US, CN, XX - catch all for all regions)
 * @property language The language to add this default site for.
 * (Example: en, XX - catch all for all languages)
 * @property title The title of the default site.
 * @property url The url of the default site.
 **/
data class DefaultSite(
    val region: String,
    val language: String,
    val title: String,
    val url: String
)
