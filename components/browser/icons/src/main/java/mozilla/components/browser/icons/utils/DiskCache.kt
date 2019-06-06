/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import com.jakewharton.disklrucache.DiskLruCache
import mozilla.components.browser.icons.Icon
import mozilla.components.browser.icons.IconRequest
import mozilla.components.browser.icons.extension.toIconResources
import mozilla.components.browser.icons.extension.toJSON
import mozilla.components.browser.icons.loader.DiskIconLoader
import mozilla.components.browser.icons.preparer.DiskIconPreparer
import mozilla.components.browser.icons.processor.DiskIconProcessor
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlin.sha1
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException

private const val RESOURCES_DISK_CACHE_VERSION = 1
private const val ICON_DATA_DISK_CACHE_VERSION = 1

private const val MAXIMUM_CACHE_RESOURCES_BYTES: Long = 1024 * 1024 * 10 // 10 MB
private const val MAXIMUM_CACHE_ICON_DATA_BYTES: Long = 1024 * 1024 * 100 // 100 MB

private const val WEBP_QUALITY = 90

/**
 * Caching bitmaps and resource URLs on disk.
 */
internal class DiskCache :
    DiskIconLoader.LoaderDiskCache, DiskIconPreparer.PreparerDiskCache, DiskIconProcessor.ProcessorDiskCache {
    private val logger = Logger("Icons/DiskCache")
    private var iconResourcesCache: DiskLruCache? = null
    private var iconDataCache: DiskLruCache? = null
    private val iconResourcesCacheWriteLock = Any()
    private val iconDataCacheWriteLock = Any()

    override fun getResources(context: Context, request: IconRequest): List<IconRequest.Resource> {
        val key = createKey(request.url)
        val snapshot: DiskLruCache.Snapshot = getIconResourcesCache(context).get(key)
            ?: return emptyList()

        try {
            val data = snapshot.getInputStream(0).use {
                it.buffered().reader().readText()
            }

            return JSONArray(data).toIconResources()
        } catch (e: IOException) {
            logger.info("Failed to load resources from disk", e)
        } catch (e: JSONException) {
            logger.warn("Failed to parse resources from disk", e)
        }

        return emptyList()
    }

    internal fun putResources(context: Context, request: IconRequest) {
        try {
            synchronized(iconResourcesCacheWriteLock) {
                val key = createKey(request.url)
                val editor = getIconResourcesCache(context)
                    .edit(key) ?: return

                val data = request.resources.toJSON().toString()
                editor.set(0, data)

                editor.commit()
            }
        } catch (e: IOException) {
            logger.info("Failed to save resources to disk", e)
        } catch (e: JSONException) {
            logger.warn("Failed to serialize resources")
        }
    }

    override fun put(context: Context, request: IconRequest, resource: IconRequest.Resource, icon: Icon) {
        putResources(context, request)

        if (icon.bitmap != null) {
            putIconBitmap(context, resource, icon.bitmap)
        }
    }

    override fun getIconData(context: Context, resource: IconRequest.Resource): ByteArray? {
        val key = createKey(resource.url)

        val snapshot = getIconDataCache(context).get(key)
            ?: return null

        return try {
            snapshot.getInputStream(0).use {
                it.buffered().readBytes()
            }
        } catch (e: IOException) {
            logger.info("Failed to read icon bitmap from disk", e)
            null
        }
    }

    internal fun putIconBitmap(context: Context, resource: IconRequest.Resource, bitmap: Bitmap) {
        try {
            synchronized(iconDataCacheWriteLock) {
                val editor = getIconDataCache(context)
                    .edit(createKey(resource.url)) ?: return

                editor.newOutputStream(0).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, stream)
                }

                editor.commit()
            }
        } catch (e: IOException) {
            logger.info("Failed to save icon bitmap to disk", e)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    internal fun clear(context: Context) {
        getIconResourcesCache(context).delete()
        getIconDataCache(context).delete()

        iconResourcesCache = null
        iconDataCache = null
    }

    @Synchronized
    private fun getIconResourcesCache(context: Context): DiskLruCache {
        iconResourcesCache?.let { return it }

        return DiskLruCache.open(
            getIconResourcesCacheDirectory(context),
            RESOURCES_DISK_CACHE_VERSION,
            1,
            MAXIMUM_CACHE_RESOURCES_BYTES
        ).also { iconResourcesCache = it }
    }

    private fun getIconResourcesCacheDirectory(context: Context): File {
        val cacheDirectory = File(context.cacheDir, "mozac_browser_icons")
        return File(cacheDirectory, "resources")
    }

    @Synchronized
    private fun getIconDataCache(context: Context): DiskLruCache {
        iconDataCache?.let { return it }

        return DiskLruCache.open(
            getIconDataCacheDirectory(context),
            ICON_DATA_DISK_CACHE_VERSION,
            1,
            MAXIMUM_CACHE_ICON_DATA_BYTES
        ).also { iconDataCache = it }
    }

    private fun getIconDataCacheDirectory(context: Context): File {
        val cacheDirectory = File(context.cacheDir, "mozac_browser_icons")
        return File(cacheDirectory, "icons")
    }
}

private fun createKey(rawKey: String): String = rawKey.sha1()
