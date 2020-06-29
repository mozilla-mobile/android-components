/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.icons

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.icons.decoder.ICOIconDecoder
import mozilla.components.browser.icons.extension.IconMessageHandler
import mozilla.components.browser.icons.generator.DefaultIconGenerator
import mozilla.components.browser.icons.generator.IconGenerator
import mozilla.components.browser.icons.loader.DataUriIconLoader
import mozilla.components.browser.icons.loader.DiskIconLoader
import mozilla.components.browser.icons.loader.HttpIconLoader
import mozilla.components.browser.icons.loader.IconLoader
import mozilla.components.browser.icons.loader.MemoryIconLoader
import mozilla.components.browser.icons.pipeline.IconResourceComparator
import mozilla.components.browser.icons.preparer.DiskIconPreparer
import mozilla.components.browser.icons.preparer.IconPreprarer
import mozilla.components.browser.icons.preparer.MemoryIconPreparer
import mozilla.components.browser.icons.preparer.TippyTopIconPreparer
import mozilla.components.browser.icons.processor.DiskIconProcessor
import mozilla.components.browser.icons.processor.IconProcessor
import mozilla.components.browser.icons.processor.MemoryIconProcessor
import mozilla.components.support.images.CancelOnDetach
import mozilla.components.browser.icons.utils.IconDiskCache
import mozilla.components.browser.icons.utils.IconMemoryCache
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.fetch.Client
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.base.memory.MemoryConsumer
import mozilla.components.support.images.DesiredSize
import mozilla.components.support.images.decoder.AndroidImageDecoder
import mozilla.components.support.images.decoder.ImageDecoder
import mozilla.components.support.ktx.kotlinx.coroutines.flow.filterChanged
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

private const val MAXIMUM_SCALE_FACTOR = 2.0f

private const val EXTENSION_MESSAGING_NAME = "MozacBrowserIcons"

// Number of worker threads we are using internally.
private const val THREADS = 3

internal val sharedMemoryCache = IconMemoryCache()
internal val sharedDiskCache = IconDiskCache()

/**
 * Entry point for loading icons for websites.
 *
 * @param generator The [IconGenerator] to generate an icon if no icon could be loaded.
 * @param decoders List of [IconDecoder] instances to use when decoding a loaded icon into a [android.graphics.Bitmap].
 */
class BrowserIcons(
    private val context: Context,
    private val httpClient: Client,
    private val generator: IconGenerator = DefaultIconGenerator(),
    private val preparers: List<IconPreprarer> = listOf(
        TippyTopIconPreparer(context.assets),
        MemoryIconPreparer(sharedMemoryCache),
        DiskIconPreparer(sharedDiskCache)
    ),
    private val loaders: List<IconLoader> = listOf(
        MemoryIconLoader(sharedMemoryCache),
        DiskIconLoader(sharedDiskCache),
        HttpIconLoader(httpClient),
        DataUriIconLoader()
    ),
    private val decoders: List<ImageDecoder> = listOf(
        AndroidImageDecoder(),
        ICOIconDecoder()
    ),
    private val processors: List<IconProcessor> = listOf(
        MemoryIconProcessor(sharedMemoryCache),
        DiskIconProcessor(sharedDiskCache)
    ),
    jobDispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(THREADS).asCoroutineDispatcher()
) : MemoryConsumer {
    private val logger = Logger("BrowserIcons")
    private val maximumSize = context.resources.getDimensionPixelSize(R.dimen.mozac_browser_icons_maximum_size)
    private val scope = CoroutineScope(jobDispatcher)

    /**
     * Asynchronously loads an [Icon] for the given [IconRequest].
     */
    fun loadIcon(request: IconRequest): Deferred<Icon> = scope.async {
        loadIconInternal(request).also { loadedIcon ->
            logger.debug("Loaded icon (source = ${loadedIcon.source}): ${request.url}")
        }
    }

    @WorkerThread
    private fun loadIconInternal(initialRequest: IconRequest): Icon {
        val desiredSize = DesiredSize(
            targetSize = context.resources.getDimensionPixelSize(initialRequest.size.dimen),
            maxSize = maximumSize,
            maxScaleFactor = MAXIMUM_SCALE_FACTOR
        )

        // (1) First prepare the request.
        val request = prepare(context, preparers, initialRequest)

        // (2) Then try to load an icon.
        val (icon, resource) = load(context, request, loaders, decoders, desiredSize)
            ?: generator.generate(context, request) to null

        // (3) Finally process the icon.
        return process(context, processors, request, resource, icon, desiredSize)
            ?: generator.generate(context, request)
    }

    /**
     * Installs the "icons" extension in the engine in order to dynamically load icons for loaded websites.
     */
    fun install(engine: Engine, store: BrowserStore) {
        engine.installWebExtension(
            id = "mozacBrowserIcons",
            url = "resource://android/assets/extensions/browser-icons/",
            allowContentMessaging = true,
            onSuccess = { extension ->
                Logger.debug("Installed browser-icons extension")

                store.flowScoped { flow -> subscribeToUpdates(store, flow, extension) }
            },
            onError = { _, throwable ->
                Logger.error("Could not install browser-icons extension", throwable)
            })
    }

    /**
     * Loads an icon asynchronously using [BrowserIcons] and then displays it in the [ImageView].
     * If the view is detached from the window before loading is completed, then loading is cancelled.
     *
     * @param view [ImageView] to load icon into.
     * @param request Load icon for this given [IconRequest].
     * @param placeholder [Drawable] to display while icon is loading.
     * @param error [Drawable] to display if loading fails.
     */
    fun loadIntoView(
        view: ImageView,
        request: IconRequest,
        placeholder: Drawable? = null,
        error: Drawable? = null
    ) = scope.launch(Dispatchers.Main) {
        loadIntoViewInternal(WeakReference(view), request, placeholder, error)
    }

    @MainThread
    private suspend fun loadIntoViewInternal(
        view: WeakReference<ImageView>,
        request: IconRequest,
        placeholder: Drawable?,
        error: Drawable?
    ) {
        // If we previously started loading into the view, cancel the job.
        val existingJob = view.get()?.getTag(R.id.mozac_browser_icons_tag_job) as? Job
        existingJob?.cancel()

        view.get()?.setImageDrawable(placeholder)

        // Create a loading job
        val deferredIcon = loadIcon(request)

        view.get()?.setTag(R.id.mozac_browser_icons_tag_job, deferredIcon)
        val onAttachStateChangeListener = CancelOnDetach(deferredIcon).also {
            view.get()?.addOnAttachStateChangeListener(it)
        }

        try {
            val icon = deferredIcon.await()
            view.get()?.setImageBitmap(icon.bitmap)
        } catch (e: CancellationException) {
            view.get()?.setImageDrawable(error)
        } finally {
            view.get()?.removeOnAttachStateChangeListener(onAttachStateChangeListener)
            view.get()?.setTag(R.id.mozac_browser_icons_tag_job, null)
        }
    }

    /**
     * The device is running low on memory. This component should trim its memory usage.
     */
    @Deprecated("Use onTrimMemory instead.", replaceWith = ReplaceWith("onTrimMemory"))
    fun onLowMemory() {
        sharedMemoryCache.clear()
    }

    override fun onTrimMemory(level: Int) {
        val shouldClearMemoryCache = when (level) {
            // Foreground: The device is running much lower on memory. The app is running and not killable, but the
            // system wants us to release unused resources to improve system performance.
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            // Foreground: The device is running extremely low on memory. The app is not yet considered a killable
            // process, but the system will begin killing background processes if apps do not release resources.
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> true

            // Background: The system is running low on memory and our process is near the middle of the LRU list.
            // If the system becomes further constrained for memory, there's a chance our process will be killed.
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            // Background: The system is running low on memory and our process is one of the first to be killed
            // if the system does not recover memory now.
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> true

            else -> false
        }

        if (shouldClearMemoryCache) {
            sharedMemoryCache.clear()
        }
    }

    private suspend fun subscribeToUpdates(
        store: BrowserStore,
        flow: Flow<BrowserState>,
        extension: WebExtension
    ) {
        // Whenever we see a new EngineSession in the store then we register our content message
        // handler if it has not been added yet.

        flow.map { it.tabs }
            .filterChanged { it.engineState.engineSession }
            .collect { state ->
                val engineSession = state.engineState.engineSession ?: return@collect

                if (extension.hasContentMessageHandler(engineSession, EXTENSION_MESSAGING_NAME)) {
                    return@collect
                }

                val handler = IconMessageHandler(store, state.id, state.content.private, this)
                extension.registerContentMessageHandler(engineSession, EXTENSION_MESSAGING_NAME, handler)
            }
    }
}

private fun prepare(context: Context, preparers: List<IconPreprarer>, request: IconRequest): IconRequest =
    preparers.fold(request) { preparedRequest, preparer ->
        preparer.prepare(context, preparedRequest)
    }

private fun load(
    context: Context,
    request: IconRequest,
    loaders: List<IconLoader>,
    decoders: List<ImageDecoder>,
    desiredSize: DesiredSize
): Pair<Icon, IconRequest.Resource>? {
    request.resources
        .asSequence()
        .distinct()
        .sortedWith(IconResourceComparator)
        .forEach { resource ->
            loaders.forEach { loader ->
                val result = loader.load(context, request, resource)

                val icon = decodeIconLoaderResult(result, decoders, desiredSize)

                if (icon != null) {
                    return Pair(icon, resource)
                }
            }
        }

    return null
}

private fun decodeIconLoaderResult(
    result: IconLoader.Result,
    decoders: List<ImageDecoder>,
    desiredSize: DesiredSize
): Icon? = when (result) {
    IconLoader.Result.NoResult -> null

    is IconLoader.Result.BitmapResult -> Icon(result.bitmap, source = result.source)

    is IconLoader.Result.BytesResult ->
        decodeBytes(result.bytes, decoders, desiredSize)?.let { Icon(it, source = result.source) }
}

private fun decodeBytes(
    data: ByteArray,
    decoders: List<ImageDecoder>,
    desiredSize: DesiredSize
): Bitmap? {
    decoders.forEach { decoder ->
        val bitmap = decoder.decode(data, desiredSize)

        if (bitmap != null) {
            return bitmap
        }
    }

    return null
}

@Suppress("LongParameterList")
private fun process(
    context: Context,
    processors: List<IconProcessor>,
    request: IconRequest,
    resource: IconRequest.Resource?,
    icon: Icon?,
    desiredSize: DesiredSize
): Icon? =
    processors.fold(icon) { processedIcon, processor ->
        if (processedIcon == null) return null
        processor.process(context, request, resource, processedIcon, desiredSize)
    }
